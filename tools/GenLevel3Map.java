import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public class GenLevel3Map {
    static final int COLS = 60;
    static final int ROWS = 40;

    public static void main(String[] args) throws Exception {
        BufferedImage img = ImageIO.read(new File("assets/map3.png"));
        int imgW = img.getWidth(), imgH = img.getHeight();
        System.out.printf("Generating Level 3 map from %s (%dx%d) for %dx%d grid%n",
                "assets/map3.png", imgW, imgH, COLS, ROWS);

        boolean[][] open = new boolean[ROWS][COLS];

        // By default in a rural map, the whole open landscape is walkable ground:
        for (int y = 0; y < ROWS; y++) {
            for (int x = 0; x < COLS; x++) {
                open[y][x] = true;
            }
        }

        // 1. Outer dense wilderness boundaries:
        // North dense trees and cliffs: y = 0..3
        for (int y = 0; y <= 3; y++) {
            for (int x = 0; x < COLS; x++) {
                if (x >= 29 && x <= 32 && y >= 2) continue; // vertical road north exit
                open[y][x] = false;
            }
        }
        // South dense forest: y = 37..39
        for (int y = 37; y < ROWS; y++) {
            for (int x = 0; x < COLS; x++) {
                // Keep road exits and checkpoint clearings open
                if (x >= 28 && x <= 33) continue; // vertical road south exit
                if ((x >= 1 && x <= 3) && y == 38) continue; // spawn path
                if (x >= 40 && x <= 44) continue; // SE clearing
                open[y][x] = false;
            }
        }
        // West ridge/forest: x = 0..1
        for (int y = 0; y < ROWS; y++) {
            if (y >= 19 && y <= 22) continue; // west road exit
            if (y >= 37 && y <= 38) continue; // SW spawn exit
            open[y][0] = false;
            open[y][1] = false;
        }
        // East forest/cliff: x = 58..59
        for (int y = 0; y < ROWS; y++) {
            if (y >= 19 && y <= 22) continue; // east road exit
            open[y][58] = false;
            open[y][59] = false;
        }

        // 2. Obstacles: Cabins, Houses, Sheds (Blocked '#')
        // NW Cabin (x=13..23, y=6..14)
        for (int y = 6; y <= 14; y++) {
            for (int x = 13; x <= 23; x++) {
                open[y][x] = false; // NW house structure
            }
        }
        // NW fence & rock clutter (x=8..12, y=9..10 and x=20..25, y=15..16)
        for (int x = 8; x <= 12; x++) open[10][x] = false;
        for (int x = 21; x <= 26; x++) open[16][x] = false;

        // NE Cabin (x=36..46, y=5..15)
        for (int y = 5; y <= 15; y++) {
            for (int x = 36; x <= 46; x++) {
                open[y][x] = false; // NE house structure
            }
        }
        // NE stone well: x=49..52, y=12..14
        for (int y = 12; y <= 14; y++) {
            for (int x = 49; x <= 52; x++) open[y][x] = false;
        }
        // NE fence: x=34..35, y=10..15
        for (int y = 10; y <= 15; y++) open[y][34] = false;

        // SW Cabin (x=7..16, y=26..35)
        for (int y = 26; y <= 35; y++) {
            for (int x = 7; x <= 16; x++) {
                open[y][x] = false; // SW house structure
            }
        }
        // SW stone well: x=21..23, y=30..32
        for (int y = 30; y <= 32; y++) {
            for (int x = 21; x <= 23; x++) open[y][x] = false;
        }
        // SW overgrown field fence: x=19..25, y=24..25
        for (int x = 20; x <= 25; x++) open[24][x] = false;

        // SE Large Cabin/Barn (x=37..47, y=27..36)
        for (int y = 27; y <= 36; y++) {
            for (int x = 37; x <= 47; x++) {
                open[y][x] = false; // SE large barn
            }
        }
        // SE Small Shed (x=49..54, y=28..34)
        for (int y = 28; y <= 34; y++) {
            for (int x = 49; x <= 54; x++) {
                open[y][x] = false; // SE shed
            }
        }
        // Abandoned cart in SE yard: x=31..34, y=32..33
        for (int y = 32; y <= 33; y++) {
            for (int x = 31; x <= 34; x++) open[y][x] = false;
        }

        // Overturned carts/crates near crossroads:
        // Cart at x=23..25, y=22..23 (slightly north-west of intersection)
        open[22][23] = false; open[22][24] = false;
        open[26][17] = false; open[27][17] = false;

        // 3. Roads & Pathways (Fully Walkable '.')
        // Main horizontal dirt highway: y=19..23, x=0..59
        for (int y = 19; y <= 23; y++) {
            for (int x = 0; x < COLS; x++) {
                open[y][x] = true;
            }
        }
        // Main vertical dirt road: x=28..33, y=3..37
        for (int y = 3; y <= 37; y++) {
            for (int x = 28; x <= 33; x++) {
                open[y][x] = true;
            }
        }
        // Crossroads central intersection: x=26..35, y=18..24
        for (int y = 18; y <= 24; y++) {
            for (int x = 26; x <= 35; x++) {
                open[y][x] = true;
            }
        }

        // Stone pathways and yards:
        // SW path from road to SW cabin: x=17..19, y=23..29
        for (int y = 23; y <= 29; y++) {
            open[y][17] = true; open[y][18] = true;
        }
        // NW path to NW cabin: x=17..19, y=14..19
        for (int y = 14; y <= 19; y++) {
            open[y][17] = true; open[y][18] = true;
        }
        // NE path: x=38..40, y=15..19
        for (int y = 15; y <= 19; y++) {
            open[y][38] = true; open[y][39] = true;
        }
        // SE path: x=35..37, y=23..27
        for (int y = 23; y <= 27; y++) {
            open[y][35] = true; open[y][36] = true;
        }

        // 4. Guarantee all 7 Level 3 Checkpoints are OPEN:
        // l3_cp01: (1.5, 1.5)   -> cx = 1,  cy = 38
        // l3_cp02: (8.5, 26.5)  -> cx = 8,  cy = 13
        // l3_cp03: (16.5, 32.5) -> cx = 16, cy = 7
        // l3_cp04: (25.5, 10.5) -> cx = 25, cy = 29
        // l3_cp05: (33.5, 25.5) -> cx = 33, cy = 14
        // l3_cp06: (42.5, 3.5)  -> cx = 42, cy = 36
        // l3_cp07: (50.5, 9.5)  -> cx = 50, cy = 30
        int[][] checkpoints = {
                {1, 38},
                {8, 13},
                {16, 7},
                {25, 29},
                {33, 14},
                {42, 36},
                {50, 30}
        };
        for (int[] cp : checkpoints) {
            clearAround(open, cp[0], cp[1], 1);
        }

        // 5. Reachability check from spawn (1, 38)
        boolean[][] reached = flood(open, COLS, ROWS, 1, 38);
        int totalOpen = 0, totalReached = 0;
        for (int y = 0; y < ROWS; y++) {
            for (int x = 0; x < COLS; x++) {
                if (open[y][x]) {
                    totalOpen++;
                    if (reached[y][x]) totalReached++;
                }
            }
        }
        System.out.printf("Level 3 Connectivity: %d open tiles, %d reachable (%.1f%%)%n",
                totalOpen, totalReached, 100.0 * totalReached / totalOpen);

        // 6. Write core/src/main/resources/maps/level3.map
        List<String> lines = new ArrayList<>();
        lines.add("// Level 3 - Countryside Village, collision grid for map3.png");
        lines.add("// Generated by tools/GenLevel3Map.java");
        lines.add("// subdivisions: 1");
        for (int y = 0; y < ROWS; y++) {
            StringBuilder sb = new StringBuilder(COLS);
            for (int x = 0; x < COLS; x++) {
                sb.append(open[y][x] ? '.' : '#');
            }
            lines.add(sb.toString());
        }

        Path out = Path.of("core/src/main/resources/maps/level3.map");
        Files.write(out, lines);
        System.out.printf("Wrote %s (%d rows x %d cols)%n", out, ROWS, COLS);
    }

    static void clearAround(boolean[][] open, int cx, int cy, int r) {
        for (int y = cy - r; y <= cy + r; y++) {
            for (int x = cx - r; x <= cx + r; x++) {
                if (x >= 0 && y >= 0 && x < COLS && y < ROWS) {
                    open[y][x] = true;
                }
            }
        }
    }

    static boolean[][] flood(boolean[][] open, int w, int h, int sx, int sy) {
        boolean[][] seen = new boolean[h][w];
        if (!open[sy][sx]) return seen;
        Deque<int[]> q = new ArrayDeque<>();
        q.add(new int[]{sx, sy}); seen[sy][sx] = true;
        while (!q.isEmpty()) {
            int[] p = q.poll();
            int[][] nbs = {{p[0]+1, p[1]}, {p[0]-1, p[1]}, {p[0], p[1]+1}, {p[0], p[1]-1}};
            for (int[] n : nbs) {
                if (n[0] < 0 || n[1] < 0 || n[0] >= w || n[1] >= h) continue;
                if (open[n[1]][n[0]] && !seen[n[1]][n[0]]) {
                    seen[n[1]][n[0]] = true;
                    q.add(n);
                }
            }
        }
        return seen;
    }
}
