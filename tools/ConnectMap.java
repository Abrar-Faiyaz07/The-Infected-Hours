import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Offline tool: takes an already hand-authored {@code .map} file (as produced
 * by e.g. tools/GenLevel2Map.java / GenLevel3Map.java) and repairs it in place
 * so every standable position is reachable from a given world spawn — the
 * same invariant tools/MaskToMap.java already guarantees for level 1, and the
 * one core/src/test/java/.../LevelLoaderTest.java checks for every level.
 *
 * <p>Unlike MaskToMap, this does not rebuild the grid from art; it only widens
 * the minimum number of walls needed to connect stranded rooms to spawn (or
 * drops truly unreachable speck cells), so the hand-authored layout survives
 * intact everywhere it was already connected.
 *
 * <pre>
 *   java tools/ConnectMap.java core/src/main/resources/maps/level1_part2.map 1 5.5 4.5 6.5 4.5
 * </pre>
 * Args: mapFile, subdivisions, spawnWorldX, spawnWorldY, [extraWorldX, extraWorldY]...
 * (the extra points — e.g. the second character's spawn — are forced open and
 * standable too, in addition to the connectivity repair run from the first).
 */
public class ConnectMap {

    static final float PLAYER_COLLISION_RADIUS = 0.25f;
    static final int DOORWAY_WIDEN_CELLS = 1;

    public static void main(String[] args) throws Exception {
        Path mapPath = Path.of(args[0]);
        int sub = Integer.parseInt(args[1]);
        double radiusCells = PLAYER_COLLISION_RADIUS * sub;

        List<String> rawLines = Files.readAllLines(mapPath);
        List<String> header = new ArrayList<>();
        List<String> gridLines = new ArrayList<>();
        for (String line : rawLines) {
            String t = line.strip();
            if (gridLines.isEmpty() && t.startsWith("//")) {
                header.add(line);
            } else if (!t.isEmpty()) {
                gridLines.add(line);
            }
        }

        int ch = gridLines.size();
        int cw = gridLines.get(0).length();
        boolean[][] open = new boolean[ch][cw];
        for (int y = 0; y < ch; y++) {
            String row = gridLines.get(y);
            if (row.length() != cw) {
                throw new IllegalStateException("ragged map: row " + y + " is " + row.length() + " cols, expected " + cw);
            }
            for (int x = 0; x < cw; x++) {
                open[y][x] = row.charAt(x) != '#';
            }
        }
        System.out.printf("loaded %s: %d rows x %d cols%n", mapPath, ch, cw);

        List<int[]> worldPoints = new ArrayList<>();
        for (int i = 2; i + 1 < args.length; i += 2) {
            double wx = Double.parseDouble(args[i]);
            double wy = Double.parseDouble(args[i + 1]);
            int cx = (int) Math.floor(wx * sub);
            int cyWorld = (int) Math.floor(wy * sub);
            int cyFile = ch - 1 - cyWorld;
            worldPoints.add(new int[] { cx, cyFile });
        }
        int[] spawn = worldPoints.get(0);

        int despeckled = despeckle(open, cw, ch);
        System.out.printf("despeckle: removed %d stray single/void cells%n", despeckled);

        boolean[][] standable = erode(open, cw, ch, radiusCells);
        for (int[] p : worldPoints) {
            if (!standable[p[1]][p[0]]) {
                widenAround(open, cw, ch, p[0], p[1]);
                standable = erode(open, cw, ch, radiusCells);
            }
        }
        for (int[] p : worldPoints) {
            System.out.printf("spawn point file(%d,%d) open=%b standable=%b%n",
                    p[0], p[1], open[p[1]][p[0]], standable[p[1]][p[0]]);
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

        List<String> outLines = new ArrayList<>(header);
        for (int y = 0; y < ch; y++) {
            StringBuilder sb = new StringBuilder(cw);
            for (int x = 0; x < cw; x++) sb.append(open[y][x] ? '.' : '#');
            outLines.add(sb.toString());
        }
        Files.write(mapPath, outLines);
        System.out.printf("wrote %s: %d rows x %d cols%n", mapPath, ch, cw);
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
