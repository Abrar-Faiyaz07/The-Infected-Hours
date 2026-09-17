import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Offline tool: re-classifies a handful of PIXEL-SPACE rectangles of an
 * already-generated .map file from a reference image, leaving every other
 * cell untouched, then re-runs connectivity repair so a corrected wall can
 * never reseal a room that was reachable only through it.
 *
 * <pre>
 *   java tools/PatchFromImage.java reference.png level1.map x0,y0,x1,y1 ...
 * </pre>
 *
 * Each rectangle is in the SAME pixel space as the reference image (which
 * must be the same resolution as the art the map was traced from — no
 * proportional rescaling, unlike MaskToMap.java, since patches need to be
 * pixel-precise). Cells inside are classified red-dominant = blocked, else
 * walkable, same rule as MaskToMap.java.
 */
public class PatchFromImage {

    static final double OX = 32.0, OY = 32.0;
    static final int COLS = 45, ROWS = 33;
    static final double PX = 1857.0 / COLS, PY = 1363.0 / ROWS;
    static final int SUB = 3;
    static final double RADIUS_CELLS = 0.25 * SUB;
    static final int DOORWAY_WIDEN_CELLS = 1;

    public static void main(String[] args) throws Exception {
        BufferedImage img = ImageIO.read(new File(args[0]));
        Path mapPath = Path.of(args[1]);
        List<String> lines = Files.readAllLines(mapPath);

        int gridStart = -1;
        for (int i = 0; i < lines.size(); i++) {
            String t = lines.get(i).strip();
            if (!t.isEmpty() && !t.startsWith("//")) { gridStart = i; break; }
        }
        List<char[]> grid = new ArrayList<>();
        for (int i = gridStart; i < lines.size(); i++) grid.add(lines.get(i).toCharArray());
        int ch = grid.size(), cw = grid.get(0).length;

        boolean[][] open = new boolean[ch][cw];
        for (int y = 0; y < ch; y++)
            for (int x = 0; x < cw; x++)
                open[y][x] = grid.get(y)[x] != '#';

        double cpx = PX / SUB, cpy = PY / SUB;
        int patched = 0;
        for (int i = 2; i < args.length; i++) {
            String[] parts = args[i].split(",");
            int px0 = Integer.parseInt(parts[0]), py0 = Integer.parseInt(parts[1]);
            int px1 = Integer.parseInt(parts[2]), py1 = Integer.parseInt(parts[3]);

            int col0 = (int) Math.floor((px0 - OX) / cpx), col1 = (int) Math.ceil((px1 - OX) / cpx);
            int row0 = (int) Math.floor((py0 - OY) / cpy), row1 = (int) Math.ceil((py1 - OY) / cpy);
            System.out.printf("rect px(%d,%d)-(%d,%d) -> cells col %d..%d, row %d..%d%n",
                    px0, py0, px1, py1, col0, col1, row0, row1);

            for (int row = Math.max(0, row0); row <= Math.min(ch - 1, row1); row++) {
                for (int col = Math.max(0, col0); col <= Math.min(cw - 1, col1); col++) {
                    int x0 = (int) Math.round(OX + col * cpx), x1 = (int) Math.round(OX + (col + 1) * cpx);
                    int y0 = (int) Math.round(OY + row * cpy), y1 = (int) Math.round(OY + (row + 1) * cpy);
                    int redVotes = 0, otherVotes = 0;
                    for (int y = Math.max(0, y0); y < Math.min(img.getHeight(), y1); y++) {
                        for (int x = Math.max(0, x0); x < Math.min(img.getWidth(), x1); x++) {
                            int rgb = img.getRGB(x, y);
                            int r = (rgb >> 16) & 0xFF, g = (rgb >> 8) & 0xFF, b = rgb & 0xFF;
                            boolean neutral = Math.abs(r - g) < 12 && Math.abs(g - b) < 12;
                            if (neutral) { redVotes++; continue; }
                            if (r > g) redVotes++; else otherVotes++;
                        }
                    }
                    boolean blocked = redVotes >= otherVotes;
                    if (open[row][col] == blocked) patched++;
                    open[row][col] = !blocked;
                }
            }
        }
        System.out.println("cells changed: " + patched);

        int spawnCx = (int) Math.floor(5.5 * SUB);
        int spawnCyWorld = (int) Math.floor(4.5 * SUB);
        int spawnCyFile = ch - 1 - spawnCyWorld;

        boolean[][] standable = erode(open, cw, ch);
        if (!standable[spawnCyFile][spawnCx]) {
            widenAround(open, cw, ch, spawnCx, spawnCyFile);
            standable = erode(open, cw, ch);
        }

        int connected = 0, dropped = 0, widened = 0;
        for (int pass = 0; pass < 200; pass++) {
            int[][] region = largestStrandedRegion(standable, cw, ch, spawnCx, spawnCyFile);
            if (region == null) break;
            int[][] path = pathToReachable(standable, open, cw, ch, region, spawnCx, spawnCyFile);
            if (path == null) {
                for (int[] c : region) open[c[1]][c[0]] = false;
                dropped += region.length;
            } else {
                for (int[] c : path) widened += widenAround(open, cw, ch, c[0], c[1]);
                connected++;
            }
            standable = erode(open, cw, ch);
        }
        System.out.printf("post-patch connectivity: connected %d, widened %d cells, dropped %d unreachable cells%n",
                connected, widened, dropped);

        boolean[][] finalStandable = erode(open, cw, ch);
        boolean[][] finalReach = flood(finalStandable, cw, ch, spawnCx, spawnCyFile);
        int standableTotal = 0, standableReached = 0;
        for (int y = 0; y < ch; y++)
            for (int x = 0; x < cw; x++)
                if (finalStandable[y][x]) { standableTotal++; if (finalReach[y][x]) standableReached++; }
        System.out.printf("standable %d, reachable %d%s%n", standableTotal, standableReached,
                standableTotal == standableReached ? "  OK" : "  *** STILL DISCONNECTED ***");

        List<String> out = new ArrayList<>(lines.subList(0, gridStart));
        for (int y = 0; y < ch; y++) {
            StringBuilder sb = new StringBuilder(cw);
            for (int x = 0; x < cw; x++) sb.append(open[y][x] ? '.' : '#');
            out.add(sb.toString());
        }
        Files.write(mapPath, out);
        System.out.println("wrote " + mapPath);
    }

    static boolean[][] erode(boolean[][] open, int cw, int ch) {
        boolean[][] standable = new boolean[ch][cw];
        int reach = (int) Math.ceil(RADIUS_CELLS);
        for (int y = 0; y < ch; y++) {
            for (int x = 0; x < cw; x++) {
                if (!open[y][x]) continue;
                double px = x + 0.5, py = y + 0.5;
                boolean fits = true;
                for (int ny = y - reach; ny <= y + reach && fits; ny++) {
                    for (int nx = x - reach; nx <= x + reach && fits; nx++) {
                        boolean blocked = nx < 0 || ny < 0 || nx >= cw || ny >= ch || !open[ny][nx];
                        if (!blocked) continue;
                        double dx = px - clamp(px, nx, nx + 1.0);
                        double dy = py - clamp(py, ny, ny + 1.0);
                        if (dx * dx + dy * dy < RADIUS_CELLS * RADIUS_CELLS) fits = false;
                    }
                }
                standable[y][x] = fits;
            }
        }
        return standable;
    }

    static double clamp(double v, double lo, double hi) { return v < lo ? lo : Math.min(v, hi); }

    static int widenAround(boolean[][] open, int cw, int ch, int x, int y) {
        int opened = 0;
        for (int ny = y - DOORWAY_WIDEN_CELLS; ny <= y + DOORWAY_WIDEN_CELLS; ny++) {
            for (int nx = x - DOORWAY_WIDEN_CELLS; nx <= x + DOORWAY_WIDEN_CELLS; nx++) {
                if (nx < 0 || ny < 0 || nx >= cw || ny >= ch) continue;
                if (!open[ny][nx]) { open[ny][nx] = true; opened++; }
            }
        }
        return opened;
    }

    static int[][] largestStrandedRegion(boolean[][] open, int cw, int ch, int spawnX, int spawnY) {
        boolean[][] reach = flood(open, cw, ch, spawnX, spawnY);
        boolean[][] visited = new boolean[ch][cw];
        int[][] best = null;
        for (int y = 0; y < ch; y++) {
            for (int x = 0; x < cw; x++) {
                if (!open[y][x] || reach[y][x] || visited[y][x]) continue;
                List<int[]> region = new ArrayList<>();
                Deque<int[]> q = new ArrayDeque<>();
                q.add(new int[]{x, y}); visited[y][x] = true;
                while (!q.isEmpty()) {
                    int[] p = q.poll(); region.add(p);
                    for (int[] n : neighbours(p)) {
                        if (n[0] < 0 || n[1] < 0 || n[0] >= cw || n[1] >= ch) continue;
                        if (open[n[1]][n[0]] && !visited[n[1]][n[0]]) { visited[n[1]][n[0]] = true; q.add(n); }
                    }
                }
                if (best == null || region.size() > best.length) best = region.toArray(new int[0][]);
            }
        }
        return best;
    }

    static int[][] pathToReachable(boolean[][] standable, boolean[][] open,
                                    int cw, int ch, int[][] region, int spawnX, int spawnY) {
        boolean[][] reach = flood(standable, cw, ch, spawnX, spawnY);
        int[][] dist = new int[ch][cw];
        int[][] prev = new int[ch][cw];
        for (int[] row : dist) java.util.Arrays.fill(row, Integer.MAX_VALUE);
        for (int[] row : prev) java.util.Arrays.fill(row, -1);

        Deque<int[]> dq = new ArrayDeque<>();
        for (int[] c : region) { dist[c[1]][c[0]] = 0; dq.add(c); }
        int[] hit = null;
        while (!dq.isEmpty() && hit == null) {
            int[] p = dq.pollFirst();
            for (int[] n : neighbours(p)) {
                if (n[0] < 0 || n[1] < 0 || n[0] >= cw || n[1] >= ch) continue;
                boolean free = standable[n[1]][n[0]];
                int cost = dist[p[1]][p[0]] + (free ? 0 : 1);
                if (cost >= dist[n[1]][n[0]]) continue;
                dist[n[1]][n[0]] = cost;
                prev[n[1]][n[0]] = p[1] * cw + p[0];
                if (reach[n[1]][n[0]]) { hit = n; break; }
                if (free) dq.addFirst(n); else dq.addLast(n);
            }
        }
        if (hit == null) return null;

        List<int[]> path = new ArrayList<>();
        int cur = hit[1] * cw + hit[0];
        while (cur != -1) {
            int cx = cur % cw, cy = cur / cw;
            path.add(new int[]{cx, cy});
            cur = prev[cy][cx];
        }
        return path.toArray(new int[0][]);
    }

    static boolean[][] flood(boolean[][] open, int cw, int ch, int sx, int sy) {
        boolean[][] seen = new boolean[ch][cw];
        if (!open[sy][sx]) return seen;
        Deque<int[]> q = new ArrayDeque<>();
        q.add(new int[]{sx, sy}); seen[sy][sx] = true;
        while (!q.isEmpty()) {
            int[] p = q.poll();
            for (int[] n : neighbours(p)) {
                if (n[0] < 0 || n[1] < 0 || n[0] >= cw || n[1] >= ch) continue;
                if (open[n[1]][n[0]] && !seen[n[1]][n[0]]) { seen[n[1]][n[0]] = true; q.add(n); }
            }
        }
        return seen;
    }

    static int[][] neighbours(int[] p) {
        return new int[][]{{p[0]+1,p[1]},{p[0]-1,p[1]},{p[0],p[1]+1},{p[0],p[1]-1}};
    }
}
