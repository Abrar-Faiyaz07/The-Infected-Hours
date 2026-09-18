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
 * Same purpose as tools/MaskToMap.java (build a level's collision grid from a
 * fully hand-painted reference image), generalized for levels whose grid covers
 * the FULL image with no content-box inset and configurable tile counts /
 * subdivisions — i.e. levels 2 and 3, unlike level 1's inset 45x33 scheme.
 *
 * <p>Uses the GREEN-ONLY convention of GenMap.java: the painted green IS the
 * walkable world and anything not green blocks. Level 1's mask is the inverse
 * (red paint marks blocked), which is why MaskToMap.java classifies by red
 * dominance and this does not — a hue-dominance test would read the blue
 * hospital walls in map2.png as walkable.
 *
 * <pre>
 *   java tools/MaskToMapGeneric.java assets/collision_mask2.png core/src/main/resources/maps/level1_part2.map 60 40 1 5.5 4.5 6.5 4.5
 * </pre>
 * Args: maskImage, outMapFile, cols, rows, subdivisions, spawnWorldX, spawnWorldY, [extraWorldX, extraWorldY]...
 *
 * A cell is walkable when the majority of its pixels are strongly green, then
 * the same despeckle + connectivity repair as MaskToMap/ConnectMap runs so the
 * result is guaranteed fully reachable from spawn before it is written.
 */
public class MaskToMapGeneric {

    static final float PLAYER_COLLISION_RADIUS = 0.25f;
    static final int DOORWAY_WIDEN_CELLS = 1;

    public static void main(String[] args) throws Exception {
        BufferedImage img = ImageIO.read(new File(args[0]));
        Path outPath = Path.of(args[1]);
        int cols = Integer.parseInt(args[2]);
        int rows = Integer.parseInt(args[3]);
        int sub = Integer.parseInt(args[4]);
        double radiusCells = PLAYER_COLLISION_RADIUS * sub;

        int iw = img.getWidth(), ih = img.getHeight();
        System.out.printf("mask image %dx%d for %dx%d tiles (subdivisions=%d)%n", iw, ih, cols, rows, sub);
        tintStyle = detectTintStyle(img);

        int cw = cols * sub, ch = rows * sub;
        double cpx = (double) iw / cw, cpy = (double) ih / ch;
        boolean[][] open = new boolean[ch][cw];

        int blockedCells = 0;
        for (int cy = 0; cy < ch; cy++) {
            for (int cx = 0; cx < cw; cx++) {
                int x0 = (int) Math.round(cx * cpx), x1 = (int) Math.round((cx + 1) * cpx);
                int y0 = (int) Math.round(cy * cpy), y1 = (int) Math.round((cy + 1) * cpy);
                x0 = Math.max(0, x0); y0 = Math.max(0, y0);
                x1 = Math.min(iw, x1); y1 = Math.min(ih, y1);

                int greenVotes = 0, total = 0;
                for (int y = y0; y < y1; y++) {
                    for (int x = x0; x < x1; x++) {
                        int rgb = img.getRGB(x, y);
                        int r = (rgb >> 16) & 0xFF, g = (rgb >> 8) & 0xFF, b = rgb & 0xFF;
                        total++;
                        if (isPaintedGreen(r, g, b)) greenVotes++;
                    }
                }
                boolean walkable = total > 0 && greenVotes * 2 > total;
                open[cy][cx] = walkable;
                if (!walkable) blockedCells++;
            }
        }
        System.out.printf("classified %d/%d cells blocked (%.1f%%)%n",
                blockedCells, cw * ch, 100.0 * blockedCells / (cw * ch));

        int despeckled = despeckle(open, cw, ch);
        System.out.printf("despeckle: removed %d stray single/void cells%n", despeckled);

        List<int[]> worldPoints = new ArrayList<>();
        for (int i = 5; i + 1 < args.length; i += 2) {
            double wx = Double.parseDouble(args[i]);
            double wy = Double.parseDouble(args[i + 1]);
            int cx = (int) Math.floor(wx * sub);
            int cyWorld = (int) Math.floor(wy * sub);
            int cyFile = ch - 1 - cyWorld;
            worldPoints.add(new int[] { cx, cyFile });
        }
        int[] spawn = worldPoints.get(0);

        // Anchors are the spawn plus every gameplay point that must be stood on
        // (checkpoints, feature tiles). GenLevel2Map.clearAround did the same;
        // a mask that paints over one of these would otherwise strand an
        // objective inside a wall.
        boolean[][] standable = erode(open, cw, ch, radiusCells);
        List<int[]> forced = new ArrayList<>();
        for (int[] p : worldPoints) {
            if (!standable[p[1]][p[0]]) {
                widenAround(open, cw, ch, p[0], p[1]);
                standable = erode(open, cw, ch, radiusCells);
                forced.add(p);
            }
        }
        for (int[] p : worldPoints) {
            boolean wasForced = forced.stream().anyMatch(f -> f[0] == p[0] && f[1] == p[1]);
            System.out.printf("anchor file(%d,%d) open=%b standable=%b%s%n",
                    p[0], p[1], open[p[1]][p[0]], standable[p[1]][p[0]],
                    wasForced ? "   <- FORCED OPEN (mask painted over it)" : "");
        }

        int connected = 0, dropped = 0, widened = 0;
        for (int pass = 0; pass < 400; pass++) {
            int[][] region = largestStrandedRegion(standable, cw, ch, spawn[0], spawn[1]);
            if (region == null) break;
            int[][] path = pathToReachable(standable, open, cw, ch, region, spawn[0], spawn[1]);
            if (path == null) {
                for (int[] c : region) open[c[1]][c[0]] = false;
                dropped += region.length;
            } else {
                for (int[] c : path) widened += widenAround(open, cw, ch, c[0], c[1]);
                connected++;
            }
            standable = erode(open, cw, ch, radiusCells);
        }
        System.out.printf("connected %d rooms, opened %d cells to widen doorways; dropped %d unreachable cells%n",
                connected, widened, dropped);

        boolean[][] finalStandable = erode(open, cw, ch, radiusCells);
        boolean[][] finalReach = flood(finalStandable, cw, ch, spawn[0], spawn[1]);
        int standableTotal = 0, standableReached = 0;
        for (int y = 0; y < ch; y++) {
            for (int x = 0; x < cw; x++) {
                if (finalStandable[y][x]) {
                    standableTotal++;
                    if (finalReach[y][x]) standableReached++;
                }
            }
        }
        System.out.printf("standable cells %d, reachable %d%s%n", standableTotal, standableReached,
                standableTotal == standableReached ? "  OK" : "  *** STILL DISCONNECTED ***");

        List<String> lines = new ArrayList<>();
        if (Files.exists(outPath)) {
            for (String existing : Files.readAllLines(outPath)) {
                String t = existing.strip();
                if (t.startsWith("//")) lines.add(existing);
                else if (!t.isEmpty()) break;
            }
        }
        if (lines.stream().noneMatch(l -> l.contains("subdivisions:"))) {
            lines.add("// subdivisions: " + sub);
        }
        for (int y = 0; y < ch; y++) {
            StringBuilder sb = new StringBuilder(cw);
            for (int x = 0; x < cw; x++) sb.append(open[y][x] ? '.' : '#');
            lines.add(sb.toString());
        }
        Files.write(outPath, lines);
        System.out.printf("wrote %s: %d rows x %d cols (tiles %dx%d, sub %d)%n",
                outPath, ch, cw, cols, rows, sub);
    }

    /**
     * Masks arrive in two styles, and one test cannot serve both.
     *
     * <p>FLAT: opaque green painted over untouched art, so "walkable" means
     * bright saturated green and everything else — including the art's own
     * blue-grey walls, whose green channel beats their red — must read as
     * blocked. Needs the brightness floor.
     *
     * <p>TINT: the whole image washed red or green, so a walkable cell can be
     * as dark as {@code rgb(0,128,32)} and would fail that floor. Here plain
     * hue dominance is right, and is the test tools/MaskToMap.java already
     * uses for level 1's mask.
     *
     * <p>Picking the wrong one silently produces a nearly-all-blocked map, so
     * the style is detected from the share of strongly red-washed pixels and
     * logged rather than assumed.
     */
    static boolean tintStyle = false;

    static boolean isPaintedGreen(int r, int g, int b) {
        if (tintStyle) {
            return g > r;
        }
        return g > 170 && g - r > 60 && g - b > 60;
    }

    /** True when a large share of the image is washed strongly red. */
    static boolean detectTintStyle(BufferedImage img) {
        long red = 0, total = 0;
        for (int y = 0; y < img.getHeight(); y += 3) {
            for (int x = 0; x < img.getWidth(); x += 3) {
                int rgb = img.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF, g = (rgb >> 8) & 0xFF, b = rgb & 0xFF;
                total++;
                if (r - g > 40 && r - b > 40) red++;
            }
        }
        double share = (double) red / total;
        System.out.printf("mask style: %s (%.1f%% strongly red-washed)%n",
                share > 0.15 ? "TINT (walkable = greener than red)" : "FLAT (walkable = bright green paint)",
                100 * share);
        return share > 0.15;
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
                q.add(new int[] { x, y });
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
                            q.add(new int[] { nx, ny });
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

    static boolean[][] erode(boolean[][] open, int cw, int ch, double radiusCells) {
        boolean[][] standable = new boolean[ch][cw];
        int reach = (int) Math.ceil(radiusCells);
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
                        if (dx * dx + dy * dy < radiusCells * radiusCells) fits = false;
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
                q.add(new int[] { x, y }); visited[y][x] = true;
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
            path.add(new int[] { cx, cy });
            cur = prev[cy][cx];
        }
        return path.toArray(new int[0][]);
    }

    static boolean[][] flood(boolean[][] open, int cw, int ch, int sx, int sy) {
        boolean[][] seen = new boolean[ch][cw];
        if (!open[sy][sx]) return seen;
        Deque<int[]> q = new ArrayDeque<>();
        q.add(new int[] { sx, sy }); seen[sy][sx] = true;
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
        return new int[][] { { p[0] + 1, p[1] }, { p[0] - 1, p[1] }, { p[0], p[1] + 1 }, { p[0], p[1] - 1 } };
    }
}
