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
 * Offline tool: traces assets/map.png into the collision grid for level1.map.
 * Not part of the build — run it by hand when the map art changes:
 *
 * <pre>
 *   java tools/GenMap.java assets/map.png out.map
 * </pre>
 *
 * then prepend the comment header from the existing
 * {@code core/src/main/resources/maps/level1.map} (the {@code subdivisions: 3}
 * line is required) and replace that file's grid rows.
 *
 * <p><b>If you change the art</b>, re-measure {@link #OX}/{@link #OY}/
 * {@link #PX}/{@link #PY} first — they come from the art's tile pitch and
 * content box, and a wrong pitch drifts the grid progressively across the map.
 * The matching constants in {@code GameScreen} must be updated to match.
 *
 * <p>{@link #RADIUS_CELLS} must mirror
 * {@code GameConstants.PLAYER_COLLISION_RADIUS}. It is what makes the generator
 * agree with the runtime about which gaps a player fits through; if they drift
 * apart, the map will pass its tests and still be unplayable.
 */
public class GenMap {

    // Grid derived from the art's content box: x 32..1888 = 45 tiles, y 32..1394 = 33 tiles.
    static final double OX = 32.0, OY = 32.0;
    static final int COLS = 45, ROWS = 33;
    static final double PX = 1857.0 / COLS;   // 41.2667
    static final double PY = 1363.0 / ROWS;   // 41.3030
    static final int SUB = 3;                 // collision cells per tile edge
    static final double FLOOR_THRESHOLD = 0.42;

    /**
     * Player collision radius, in cells. Mirrors
     * GameConstants.PLAYER_COLLISION_RADIUS (0.25 tiles) at SUB cells per tile.
     * Connectivity is checked in "eroded" space — where the player's disc
     * actually fits — because a corridor can be open yet too narrow to walk
     * down, which is what made every room unreachable.
     */
    static final double RADIUS_CELLS = 0.25 * SUB;

    /**
     * How far either side of a carved route to open, in cells. 1 gives a 3-cell
     * (1 tile) doorway against a 1.5-cell body — enough clearance that the
     * player does not have to line up with the gap, which is what made movement
     * feel sticky. Connectivity itself is still judged at the true radius.
     */
    static final int DOORWAY_WIDEN_CELLS = 1;

    public static void main(String[] args) throws Exception {
        BufferedImage img = ImageIO.read(new File(args[0]));
        int w = img.getWidth(), h = img.getHeight();
        System.out.printf("image %dx%d%n", w, h);

        // floor mask -> summed-area table for O(1) rectangle queries
        int[][] sat = new int[h + 1][w + 1];
        long floorTotal = 0;
        for (int y = 0; y < h; y++) {
            int run = 0;
            for (int x = 0; x < w; x++) {
                int rgb = img.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF, g = (rgb >> 8) & 0xFF, b = rgb & 0xFF;
                if (isFloor(r, g, b)) { run++; floorTotal++; }
                sat[y + 1][x + 1] = sat[y][x + 1] + run;
            }
        }
        System.out.printf("floor pixels: %d (%.1f%%)%n", floorTotal, 100.0 * floorTotal / (w * (long) h));

        // Separate masks for door and wall material, so drawn doors can be told
        // apart from the wall they sit in.
        int[][] goldSat = new int[h + 1][w + 1];
        int[][] tealSat = new int[h + 1][w + 1];
        for (int y = 0; y < h; y++) {
            int goldRun = 0, tealRun = 0;
            for (int x = 0; x < w; x++) {
                int rgb = img.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF, g = (rgb >> 8) & 0xFF, b = rgb & 0xFF;
                if (isDoorGold(r, g, b)) goldRun++;
                if (isWallTeal(r, g, b)) tealRun++;
                goldSat[y + 1][x + 1] = goldSat[y][x + 1] + goldRun;
                tealSat[y + 1][x + 1] = tealSat[y][x + 1] + tealRun;
            }
        }

        int cw = COLS * SUB, ch = ROWS * SUB;
        double cpx = PX / SUB, cpy = PY / SUB;
        boolean[][] open = new boolean[ch][cw];
        double[][] gold = new double[ch][cw];
        double[][] teal = new double[ch][cw];
        double[][] floorFrac = new double[ch][cw];
        for (int cy = 0; cy < ch; cy++) {
            for (int cx = 0; cx < cw; cx++) {
                int x0 = (int) Math.round(OX + cx * cpx), x1 = (int) Math.round(OX + (cx + 1) * cpx);
                int y0 = (int) Math.round(OY + cy * cpy), y1 = (int) Math.round(OY + (cy + 1) * cpy);
                floorFrac[cy][cx] = frac(sat, w, h, x0, y0, x1, y1);
                open[cy][cx] = floorFrac[cy][cx] >= FLOOR_THRESHOLD;
                gold[cy][cx] = frac(goldSat, w, h, x0, y0, x1, y1);
                teal[cy][cx] = frac(tealSat, w, h, x0, y0, x1, y1);
            }
        }

        int doorCells = openForcedDoors(open, cw, ch);
        System.out.printf("opened %d cells for hand-authored doors%n", doorCells);

        // Optional hand-painted mask, produced with tools/ExportMask.java.
        // A human marking the map directly outranks any tracing heuristic.
        //
        // Two modes:
        //   PATCH  (default) - red blocks, green opens, unpainted keeps the trace.
        //   GREEN-ONLY       - the painted green IS the walkable world; anything
        //                      not green is blocked. Use when you have painted
        //                      the whole map rather than patching a few spots.
        if (args.length > 2) {
            boolean greenOnly = args.length > 3 && args[3].equalsIgnoreCase("green-only");
            BufferedImage mask = ImageIO.read(new File(args[2]));
            // Tolerate a rescaled export rather than refusing outright — paint
            // programs resave at odd sizes and losing the work to a hard error
            // would be worse than sampling proportionally.
            double sx = mask.getWidth() / (double) w, sy = mask.getHeight() / (double) h;
            if (Math.abs(sx - 1) > 0.001 || Math.abs(sy - 1) > 0.001) {
                System.out.printf("note: mask is %dx%d vs map %dx%d - sampling proportionally%n",
                        mask.getWidth(), mask.getHeight(), w, h);
            }

            long greenPixels = 0;
            for (int y = 0; y < mask.getHeight(); y += 4) {
                for (int x = 0; x < mask.getWidth(); x += 4) {
                    int rgb = mask.getRGB(x, y);
                    if (isPaintedGreen((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF)) greenPixels++;
                }
            }
            long sampled = ((long) (mask.getHeight() + 3) / 4) * ((mask.getWidth() + 3) / 4);
            System.out.printf("mask: %.1f%% green%s%n", 100.0 * greenPixels / sampled,
                    greenOnly ? "  (GREEN-ONLY: everything not green will block)" : "");
            if (greenPixels == 0) {
                System.out.println("*** mask has no green paint - is this the unpainted template? ***");
            }

            int opened = 0, blocked = 0;
            for (int cy = 0; cy < ch; cy++) {
                for (int cx = 0; cx < cw; cx++) {
                    int x0 = (int) Math.round((OX + cx * cpx) * sx), x1 = (int) Math.round((OX + (cx + 1) * cpx) * sx);
                    int y0 = (int) Math.round((OY + cy * cpy) * sy), y1 = (int) Math.round((OY + (cy + 1) * cpy) * sy);
                    int red = 0, green = 0, total = 0;
                    for (int y = Math.max(0, y0); y < Math.min(mask.getHeight(), y1); y++) {
                        for (int x = Math.max(0, x0); x < Math.min(mask.getWidth(), x1); x++) {
                            int rgb = mask.getRGB(x, y);
                            int r = (rgb >> 16) & 0xFF, g2 = (rgb >> 8) & 0xFF, b = rgb & 0xFF;
                            total++;
                            if (isPaintedRed(r, g2, b)) red++;
                            else if (isPaintedGreen(r, g2, b)) green++;
                        }
                    }
                    boolean wantOpen;
                    if (greenOnly) {
                        wantOpen = total > 0 && green * 2 > total;   // majority green = floor
                    } else {
                        if (red == 0 && green == 0) continue;        // untouched: keep the trace
                        wantOpen = green > red;
                    }
                    if (open[cy][cx] != wantOpen) {
                        open[cy][cx] = wantOpen;
                        if (wantOpen) opened++; else blocked++;
                    }
                }
            }
            System.out.printf("painted mask applied: %d cells opened, %d cells blocked%n", opened, blocked);
            // No need to suppress FORCED_DOORS here: green-only assigns every
            // cell, so anything they opened earlier is already overwritten.
        }

        // connectivity from the spawn cell (world tile 5.5,4.5 -> cell space)
        int spawnCx = (int) Math.floor(5.5 * SUB);
        int spawnCyWorld = (int) Math.floor(4.5 * SUB);
        int spawnCyFile = ch - 1 - spawnCyWorld;

        int despeckled = despeckle(open, floorFrac, cw, ch);
        System.out.printf("despeckle: removed %d stray blocked cells standing on bare floor%n", despeckled);

        boolean[][] standable = erode(open, cw, ch);
        System.out.printf("spawn cell file(%d,%d) open=%b standable=%b%n",
                spawnCx, spawnCyFile, open[spawnCyFile][spawnCx], standable[spawnCyFile][spawnCx]);
        if (!standable[spawnCyFile][spawnCx]) {
            widenAround(open, cw, ch, spawnCx, spawnCyFile);
            standable = erode(open, cw, ch);
        }

        // Rooms sit behind drawn doors, which are not floor-coloured and so
        // classify as wall. Connect them by carving the cheapest crossing —
        // a doorway is a thin barrier, so the cheapest crossing is the real
        // door — then WIDEN it until the player's disc fits through. Carving a
        // one-cell slit leaves the room reachable on paper but not in play.
        int connected = 0, dropped = 0, widened = 0;
        for (int pass = 0; pass < 200; pass++) {
            int[][] region = largestStrandedRegion(standable, cw, ch, spawnCx, spawnCyFile);
            if (region == null) break;

            int[][] path = pathToReachable(standable, open, cw, ch, region, spawnCx, spawnCyFile);
            if (path == null) {
                // Nothing connects. Close just this pocket so it stops being
                // considered — never bulk-delete open cells, which cascades and
                // erases the map one isolated cell at a time.
                for (int[] c : region) open[c[1]][c[0]] = false;
                dropped += region.length;
            } else {
                for (int[] c : path) widened += widenAround(open, cw, ch, c[0], c[1]);
                connected++;
            }
            standable = erode(open, cw, ch);
        }
        System.out.printf("connected %d rooms, opening %d cells to widen doorways; dropped %d stray cells%n",
                connected, widened, dropped);

        // Final check in the space the player actually moves through.
        boolean[][] finalReach = flood(erode(open, cw, ch), cw, ch, spawnCx, spawnCyFile);
        int standableTotal = 0, standableReached = 0;
        boolean[][] finalStandable = erode(open, cw, ch);
        for (int y = 0; y < ch; y++)
            for (int x = 0; x < cw; x++)
                if (finalStandable[y][x]) { standableTotal++; if (finalReach[y][x]) standableReached++; }
        System.out.printf("standable cells %d, reachable %d%s%n", standableTotal, standableReached,
                standableTotal == standableReached ? "  OK" : "  *** STILL DISCONNECTED ***");

        // Carry over the existing file's comment header rather than making the
        // caller re-attach 39 lines by hand every run. The header holds the
        // `subdivisions: 3` line the loader needs, so losing it silently breaks
        // the map — this is the easiest thing to get wrong manually.
        List<String> lines = new ArrayList<>();
        Path outPath = Path.of(args[1]);
        if (Files.exists(outPath)) {
            for (String existing : Files.readAllLines(outPath)) {
                String t = existing.strip();
                if (t.startsWith("//")) lines.add(existing);
                else if (!t.isEmpty()) break;   // reached the grid
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

    /** Door leaf colour: warm gold, e.g. (216,198,148) / (186,169,122) / (164,147,94). */
    static boolean isDoorGold(int r, int g, int b) {
        return r >= 140 && r <= 235 && r > g && g > b
                && (r - b) >= 45 && (r - b) <= 105 && g >= 120;
    }

    /**
     * Structural wall colour: the saturated cyan outline the hospital is drawn
     * with. Note the floor is also cool-toned, so this deliberately demands a
     * much stronger blue cast than {@link #isFloor} — an earlier version matched
     * the floor itself and made every test using it meaningless.
     */
    static boolean isWallTeal(int r, int g, int b) {
        return b > r && (b - r) >= 55 && g > r;
    }

    /**
     * Opens every DRAWN door, not merely the ones needed to make a room
     * reachable.
     *
     * <p>This is the fix for the bug that mattered most in play: the
     * connectivity pass below only carves an opening when a room is otherwise
     * unreachable, so any room reachable the long way round kept all of its
     * other doors sealed. Walking up to a door that is plainly drawn and
     * finding it solid is the result.
     *
     * <p>A door is identified structurally, because door leaves and wall faces
     * are the same beige: scanning across a wall, a doorway reads
     * floor → thin barrier that is gold and carries no structural teal → floor.
     * A solid wall always has the teal outline somewhere in the middle.
     */
    /**
     * Hand-authored doorways, as inclusive cell rectangles
     * {@code {x0, y0, x1, y1}} in FILE coordinates (x across, y down, matching
     * how level1.map reads in an editor).
     *
     * <p><b>Why these are by hand.</b> Detecting drawn doors by colour does not
     * work on this art: a door leaf, a wall face, a bed frame and a room sign
     * are all the same beige, and every threshold loose enough to catch the
     * doors also tunnelled walkable strips through ward beds and wall faces.
     * The connectivity pass below guarantees a room is *reachable*, but it only
     * opens a route when a room is otherwise sealed — so a room reachable the
     * long way round kept its visible door shut, which is what players actually
     * walk into.
     *
     * <p>Listing the handful that matter is smaller, reviewable, and cannot
     * regress the rest of the map. To add one: run the game, press F1, note the
     * cell, and add a rectangle here.
     */
    static final int[][] FORCED_DOORS = {
            // LAB — door in its right-hand wall onto the north-south corridor.
            // The lab's floor is at x89-101,y88-93, below the bench, so the
            // opening has to run inward and down past the bench end; stopping at
            // the door leaf alone just makes a pocket too narrow to stand in.
            {104, 72, 106, 80},
            {102, 78, 106, 91},

            // PHARMACY — door in its left-hand wall onto the same corridor.
            // Corridor is x106-111, the wall x112-113, the room floor x114-121.
            {112, 89, 114, 93},

            // WARD CORRIDOR — reverted at your request. Re-enable by
            // uncommenting; it widens the north-south link from the main hall up
            // to the four wards, in the gap between the reception counter
            // (x14-22) and the block to its right.
            // {23, 18, 26, 29},

            // STAIRWELL — the treads. Drawn as banded steps, so none of it reads
            // as floor and the whole flight blocked, leaving the top landing and
            // the bottom corridor connected only around the outside. Viewed from
            // above a staircase is walkable ground; the room's own walls at
            // x120-121 and x133+ are left intact.
            {122, 8, 132, 22},
    };

    static int openForcedDoors(boolean[][] open, int cw, int ch) {
        int opened = 0;
        for (int[] r : FORCED_DOORS) {
            for (int y = r[1]; y <= r[3]; y++) {
                for (int x = r[0]; x <= r[2]; x++) {
                    if (!inside(x, y, cw, ch) || open[y][x]) continue;
                    open[y][x] = true;
                    opened++;
                }
            }
        }
        return opened;
    }

    static boolean inside(int x, int y, int cw, int ch) {
        return x >= 0 && y >= 0 && x < cw && y < ch;
    }

    // Deliberately strict, so a soft brush or a resaved JPEG-ish edge does not
    // bleed paint into neighbouring cells. Only strong red / strong green count.
    static boolean isPaintedRed(int r, int g, int b) {
        return r > 170 && g < 90 && b < 90;
    }

    static boolean isPaintedGreen(int r, int g, int b) {
        return g > 170 && r < 90 && b < 90;
    }

    /** Largest blocked island, in cells, still treated as noise rather than real geometry. */
    static final int SPECK_MAX_CELLS = 12;
    /** How much of a speck must be bare floor in the art before it is removed. */
    static final double SPECK_FLOOR_SHARE = 0.55;
    /** Islands this small are removed unconditionally — smaller than the player, so pure snag. */
    static final int SPECK_ALWAYS_CELLS = 3;

    /**
     * Removes stray blocked islands that sit on bare floor.
     *
     * <p>Hand-painted masks and JPEG-compressed edges leave small holes — a cell
     * or two in the middle of a corridor that reads as wall, which in play is an
     * invisible obstacle the player bumps into for no visible reason.
     *
     * <p>Crucially this only removes an island when the ART underneath is mostly
     * floor. Small blocked islands are often legitimate — a bin, a plant pot, a
     * stool — and those sit on furniture pixels, not floor, so they survive.
     * Islands touching the map edge are left alone; they are the outer wall.
     */
    static int despeckle(boolean[][] open, double[][] floorFrac, int cw, int ch) {
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
                    if (comp.size() > SPECK_MAX_CELLS) break;   // too big to be noise
                }
                if (touchesEdge || comp.size() > SPECK_MAX_CELLS) continue;

                // An island smaller than the player's own body (1.5 cells across)
                // cannot read as cover — it is just something invisible to snag
                // on. Those go regardless of what the art shows. Larger islands
                // survive unless the art beneath them is bare floor, which keeps
                // genuine clutter like bins and plant pots solid.
                boolean smallerThanPlayer = comp.size() <= SPECK_ALWAYS_CELLS;
                if (!smallerThanPlayer) {
                    int onFloor = 0;
                    for (int[] c : comp) if (floorFrac[c[1]][c[0]] >= 0.5) onFloor++;
                    if (onFloor < comp.size() * SPECK_FLOOR_SHARE) continue;   // real furniture
                }

                for (int[] c : comp) { open[c[1]][c[0]] = true; removed++; }
            }
        }
        return removed;
    }

    /** Flood-fills from the spawn, then returns the biggest open region it did not reach. */
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

    /**
     * "Standable" cells: those whose centre the player's disc can occupy
     * without overlapping a blocked cell. This is the same circle-vs-cell test
     * CollisionSystem runs at 60Hz, so the generator and the game agree on what
     * counts as passable.
     */
    static boolean[][] erode(boolean[][] open, int cw, int ch) {
        return erode(open, cw, ch, RADIUS_CELLS);
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

    /** Opens a block around a cell, making a gap the player fits comfortably through. */
    static int widenAround(boolean[][] open, int cw, int ch, int x, int y) {
        return widenAround(open, cw, ch, x, y, DOORWAY_WIDEN_CELLS);
    }

    static int widenAround(boolean[][] open, int cw, int ch, int x, int y, int r) {
        int opened = 0;
        for (int ny = y - r; ny <= y + r; ny++) {
            for (int nx = x - r; nx <= x + r; nx++) {
                if (nx < 0 || ny < 0 || nx >= cw || ny >= ch) continue;
                if (!open[ny][nx]) { open[ny][nx] = true; opened++; }
            }
        }
        return opened;
    }

    /**
     * Cheapest route from a stranded region back to the reachable area,
     * measured over standable space but crossing walls where it must.
     * Returns the cells to widen, or null if nothing connects.
     */
    static int[][] pathToReachable(boolean[][] standable, boolean[][] open,
                                   int cw, int ch, int[][] region, int spawnX, int spawnY) {
        boolean[][] reach = flood(standable, cw, ch, spawnX, spawnY);
        int[][] dist = new int[ch][cw];
        int[][] prev = new int[ch][cw];
        for (int[] row : dist) java.util.Arrays.fill(row, Integer.MAX_VALUE);
        for (int[] row : prev) java.util.Arrays.fill(row, -1);

        // 0-1 BFS: moving through standable space is free, forcing a wall costs 1.
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

    /**
     * Every room in this map — corridors, wards, ER, lab, x-ray — is floored
     * with the same cool blue-grey tile, e.g. (165,174,194).
     *
     * <p>The warm beige bands are <b>wall faces</b> drawn in perspective, with
     * the room's sign on them, and the warm greys below are furniture. Do not
     * be tempted to treat warm pixels as floor to "open up" a room: the warm
     * wall face and a warm-lit floor are within a few units of each other, so
     * any rule loose enough to catch one lets players walk on counters, signs
     * and the tops of walls. Rooms that look sealed are a DOORWAY problem, not
     * a floor-detection problem — fix them in the connection pass below.
     */
    static boolean isFloor(int r, int g, int b) {
        return r >= 130 && r <= 210 && b > g && g > r
                && (b - r) >= 14 && (b - r) <= 50 && b >= 160;
    }

    static double frac(int[][] sat, int w, int h, int x0, int y0, int x1, int y1) {
        x0 = Math.max(0, x0); y0 = Math.max(0, y0);
        x1 = Math.min(w, x1); y1 = Math.min(h, y1);
        int rw = x1 - x0, rh = y1 - y0;
        if (rw <= 0 || rh <= 0) return 0;
        int sum = sat[y1][x1] - sat[y0][x1] - sat[y1][x0] + sat[y0][x0];
        return sum / (double) (rw * rh);
    }
}
