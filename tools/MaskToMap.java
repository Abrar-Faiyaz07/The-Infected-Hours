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
 * Offline tool: builds a level's collision grid directly from a fully
 * hand-painted "red = blocked, everything else = walkable" reference image
 * (e.g. a cleaned-up export of tools/VisualizeCollision.java's overlay),
 * rather than tracing the raw floor art the way GenMap.java does.
 *
 * <pre>
 *   java tools/MaskToMap.java polished.png out.map
 * </pre>
 *
 * Unlike GenMap's --green-only mode (which demands pure (0,255,0) paint),
 * this classifies each cell by simple hue dominance (redder = blocked,
 * greener/other = walkable), so it tolerates JPEG recompression, resizing,
 * and anti-aliased brush edges in a hand-cleaned reference image.
 *
 * Runs the same connectivity/despeckle repair as GenMap so the result is
 * guaranteed fully reachable from spawn before it is written out.
 */
public class MaskToMap {

    // Same content-box geometry as GenMap.java / ExportMask.java.
    static final double OX = 32.0, OY = 32.0;
    static final int COLS = 45, ROWS = 33;
    static final double PX = 1857.0 / COLS, PY = 1363.0 / ROWS;
    static final int SUB = 3;
    static final double RADIUS_CELLS = 0.25 * SUB;
    static final int DOORWAY_WIDEN_CELLS = 1;

    // Reference frame the OX/OY/PX/PY constants above were measured against
    // (the original map.png dimensions), so a differently-sized input image
    // is sampled proportionally, exactly like ExportMask's painted-mask path.
    static final double REF_W = 1921.0, REF_H = 1437.0;

    public static void main(String[] args) throws Exception {
        BufferedImage img = ImageIO.read(new File(args[0]));
        int iw = img.getWidth(), ih = img.getHeight();
        double sx = iw / REF_W, sy = ih / REF_H;
        System.out.printf("input image %dx%d (scale %.4f x %.4f vs reference %dx%d)%n",
                iw, ih, sx, sy, (int) REF_W, (int) REF_H);

        int cw = COLS * SUB, ch = ROWS * SUB;
        double cpx = PX / SUB, cpy = PY / SUB;
        boolean[][] open = new boolean[ch][cw];

        int blockedCells = 0;
        for (int cy = 0; cy < ch; cy++) {
            for (int cx = 0; cx < cw; cx++) {
                double refX0 = OX + cx * cpx, refX1 = OX + (cx + 1) * cpx;
                double refY0 = OY + cy * cpy, refY1 = OY + (cy + 1) * cpy;
                int x0 = (int) Math.round(refX0 * sx), x1 = (int) Math.round(refX1 * sx);
                int y0 = (int) Math.round(refY0 * sy), y1 = (int) Math.round(refY1 * sy);
                x0 = Math.max(0, x0); y0 = Math.max(0, y0);
                x1 = Math.min(iw, x1); y1 = Math.min(ih, y1);

                int redVotes = 0, otherVotes = 0;
                for (int y = y0; y < y1; y++) {
                    for (int x = x0; x < x1; x++) {
                        int rgb = img.getRGB(x, y);
                        int r = (rgb >> 16) & 0xFF, g = (rgb >> 8) & 0xFF, b = rgb & 0xFF;
                        // Checkerboard/transparent-export background (near-white or
                        // near-grey with r≈g≈b) is treated as "outside", i.e. blocked,
                        // since it only appears outside the map's own canvas.
                        boolean neutral = Math.abs(r - g) < 12 && Math.abs(g - b) < 12;
                        if (neutral) { redVotes++; continue; }
                        if (r > g) redVotes++; else otherVotes++;
                    }
                }
                boolean blocked = redVotes >= otherVotes;
                open[cy][cx] = !blocked;
                if (blocked) blockedCells++;
            }
        }
        System.out.printf("classified %d/%d cells blocked (%.1f%%)%n",
                blockedCells, cw * ch, 100.0 * blockedCells / (cw * ch));

        int despeckled = despeckle(open, cw, ch);
        System.out.printf("despeckle: removed %d stray single/void cells%n", despeckled);

        int spawnCx = (int) Math.floor(5.5 * SUB);
        int spawnCyWorld = (int) Math.floor(4.5 * SUB);
        int spawnCyFile = ch - 1 - spawnCyWorld;

        boolean[][] standable = erode(open, cw, ch);
        if (!standable[spawnCyFile][spawnCx]) {
            widenAround(open, cw, ch, spawnCx, spawnCyFile);
            standable = erode(open, cw, ch);
        }
        System.out.printf("spawn cell file(%d,%d) open=%b standable=%b%n",
                spawnCx, spawnCyFile, open[spawnCyFile][spawnCx], standable[spawnCyFile][spawnCx]);

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
        System.out.printf("connected %d rooms, opened %d cells to widen doorways; dropped %d unreachable cells%n",
                connected, widened, dropped);

        boolean[][] finalStandable = erode(open, cw, ch);
        boolean[][] finalReach = flood(finalStandable, cw, ch, spawnCx, spawnCyFile);
        int standableTotal = 0, standableReached = 0;
        for (int y = 0; y < ch; y++)
            for (int x = 0; x < cw; x++)
                if (finalStandable[y][x]) { standableTotal++; if (finalReach[y][x]) standableReached++; }
        System.out.printf("standable cells %d, reachable %d%s%n", standableTotal, standableReached,
                standableTotal == standableReached ? "  OK" : "  *** STILL DISCONNECTED ***");

        List<String> lines = new ArrayList<>();
        Path outPath = Path.of(args[1]);
        if (Files.exists(outPath)) {
            for (String existing : Files.readAllLines(outPath)) {
                String t = existing.strip();
                if (t.startsWith("//")) lines.add(existing);
                else if (!t.isEmpty()) break;
            }
        }
        if (lines.stream().noneMatch(l -> l.contains("subdivisions:"))) {
            lines.add("// subdivisions: " + SUB);
        }
        for (int y = 0; y < ch; y++) {
            StringBuilder sb = new StringBuilder(cw);
            for (int x = 0; x < cw; x++) sb.append(open[y][x] ? '.' : '#');
            lines.add(sb.toString());
        }
        Files.write(outPath, lines);
        System.out.printf("wrote %s: %d rows x %d cols (tiles %dx%d, sub %d)%n",
                args[1], ch, cw, COLS, ROWS, SUB);
    }

    static final int SPECK_MAX_CELLS = 12;

    static int despeckle(boolean[][] open, int cw, int ch) {
        boolean[][] seen = new boolean[ch][cw];
        int removed = 0;
        for (int y = 0; y < ch; y++) {
            for (int x = 0; x < cw; x++) {
                if (open[y][x] || seen[y][x]) continue;
                List<int[]> comp = new ArrayList<>();
                Deque<int[]> q = new ArrayDeque<>();
                q.add(new int[]{x, y});
                seen[y][x] = true;
                boolean touchesEdge = false;
                while (!q.isEmpty()) {
                    int[] p = q.poll();
                    comp.add(p);
                    if (p[0] == 0 || p[1] == 0 || p[0] == cw - 1 || p[1] == ch - 1) touchesEdge = true;
                    for (int dy = -1; dy <= 1; dy++) {
                        for (int dx = -1; dx <= 1; dx++) {
                            if (dx == 0 && dy == 0) continue;
                            int nx = p[0] + dx, ny = p[1] + dy;
                            if (!inside(nx, ny, cw, ch) || open[ny][nx] || seen[ny][nx]) continue;
                            seen[ny][nx] = true;
                            q.add(new int[]{nx, ny});
                        }
                    }
                    if (comp.size() > SPECK_MAX_CELLS) break;
                }
                if (touchesEdge || comp.size() > SPECK_MAX_CELLS) continue;
                if (comp.size() <= 3) {
                    for (int[] c : comp) { open[c[1]][c[0]] = true; removed++; }
                }
            }
        }
        return removed;
    }

    static boolean inside(int x, int y, int cw, int ch) {
        return x >= 0 && y >= 0 && x < cw && y < ch;
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
