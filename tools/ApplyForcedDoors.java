import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Offline tool: force-opens specific door rectangles in an already-generated
 * .map file, in FILE cell coordinates {x0,y0,x1,y1} inclusive (row 0 = top,
 * matching how the file reads in an editor).
 *
 * <pre>
 *   java tools/ApplyForcedDoors.java core/src/main/resources/maps/level1.map
 * </pre>
 *
 * Only ever widens walkable space, so it cannot disconnect anything that was
 * already reachable — safe to run after any regeneration.
 */
public class ApplyForcedDoors {

    // Same physical doors GenMap.FORCED_DOORS already identified by hand for
    // this art (LAB right-wall door, PHARMACY left-wall door, STAIRWELL
    // treads) — the automatic red/green classification in MaskToMap.java
    // didn't carry these fixes over.
    static final int[][] FORCED_DOORS = {
            {104, 72, 106, 80},   // LAB — door onto the north-south corridor
            {102, 78, 106, 91},   // LAB — widen inward/down past the bench
            {112, 89, 114, 93},   // PHARMACY — door onto the same corridor
            {122, 8, 132, 22},    // STAIRWELL — the treads
    };

    public static void main(String[] args) throws Exception {
        Path path = Path.of(args[0]);
        List<String> lines = Files.readAllLines(path);

        int gridStart = -1;
        for (int i = 0; i < lines.size(); i++) {
            String t = lines.get(i).strip();
            if (!t.isEmpty() && !t.startsWith("//")) { gridStart = i; break; }
        }
        if (gridStart < 0) throw new IllegalStateException("no grid rows found in " + path);

        List<char[]> grid = new ArrayList<>();
        for (int i = gridStart; i < lines.size(); i++) {
            grid.add(lines.get(i).toCharArray());
        }

        int opened = 0;
        for (int[] r : FORCED_DOORS) {
            for (int y = r[1]; y <= r[3]; y++) {
                if (y < 0 || y >= grid.size()) continue;
                char[] row = grid.get(y);
                for (int x = r[0]; x <= r[2]; x++) {
                    if (x < 0 || x >= row.length) continue;
                    if (row[x] == '#') { row[x] = '.'; opened++; }
                }
            }
        }
        System.out.println("opened " + opened + " cells across " + FORCED_DOORS.length + " forced doors");

        List<String> out = new ArrayList<>(lines.subList(0, gridStart));
        for (char[] row : grid) out.add(new String(row));
        Files.write(path, out);
        System.out.println("wrote " + path);
    }
}
