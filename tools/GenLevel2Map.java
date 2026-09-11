import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public class GenLevel2Map {
    static final int COLS = 60;
    static final int ROWS = 40;

    public static void main(String[] args) throws Exception {
        BufferedImage img = ImageIO.read(new File("assets/map2.png"));
        int imgW = img.getWidth(), imgH = img.getHeight();
        System.out.printf("Generating Level 2 map from %s (%dx%d) for %dx%d grid%n",
                "assets/map2.png", imgW, imgH, COLS, ROWS);

        double tw = (double) imgW / COLS;
        double th = (double) imgH / ROWS;

        boolean[][] open = new boolean[ROWS][COLS];

        // 1. Initial color classification
        for (int y = 0; y < ROWS; y++) {
            for (int x = 0; x < COLS; x++) {
                int px0 = (int) Math.round(x * tw);
                int px1 = (int) Math.round((x + 1) * tw);
                int py0 = (int) Math.round(y * th);
                int py1 = (int) Math.round((y + 1) * th);

                int floorPixels = 0;
                int total = 0;
                for (int py = py0; py < py1; py++) {
                    for (int px = px0; px < px1; px++) {
                        int rgb = img.getRGB(px, py);
                        int r = (rgb >> 16) & 0xFF, g = (rgb >> 8) & 0xFF, b = rgb & 0xFF;
                        total++;
                        if (isMap2Floor(r, g, b)) floorPixels++;
                    }
                }
                open[y][x] = (floorPixels >= total * 0.35);
            }
        }

        // 2. Outer boundary walls
        for (int x = 0; x < COLS; x++) {
            open[0][x] = false;
            open[ROWS - 1][x] = false;
        }
        for (int y = 0; y < ROWS; y++) {
            open[y][0] = false;
            open[y][COLS - 1] = false;
        }

        // 3. Structural Walls from map2.png
        // Top patient rooms & Trauma Bay (x=1..28, y=1..10):
        // Horizontal front wall: y=10
        for (int x = 1; x <= 28; x++) {
            // Doorways into Room 1, Trauma Bay, Room 3, Room 4
            if ((x >= 3 && x <= 5) || (x >= 10 && x <= 12) || (x >= 17 && x <= 19) || (x >= 24 && x <= 26)) {
                open[10][x] = true;
            } else {
                open[10][x] = false;
            }
        }
        // Vertical room dividers
        for (int y = 1; y <= 9; y++) {
            open[y][7] = false;
            open[y][14] = false;
            open[y][21] = false;
            open[y][28] = false;
        }

        // ICU room (x=29..41, y=1..13):
        // Bottom wall: y=13 with wide double doors at x=33..37
        for (int x = 29; x <= 41; x++) {
            if (x >= 33 && x <= 37) {
                open[13][x] = true;
            } else {
                open[13][x] = false;
            }
        }
        for (int y = 1; y <= 13; y++) open[y][41] = false;

        // Bathrooms (x=42..51, y=1..10):
        for (int x = 42; x <= 51; x++) {
            if (x >= 45 && x <= 47) open[10][x] = true; else open[10][x] = false;
        }
        for (int y = 1; y <= 10; y++) open[y][51] = false;

        // Stairs to L1 (x=52..58, y=1..10):
        for (int y = 1; y <= 10; y++) {
            for (int x = 52; x <= 58; x++) open[y][x] = true;
        }

        // Left Consultation Rooms (x=1..6, y=11..37):
        for (int y = 11; y <= 35; y++) {
            if ((y >= 13 && y <= 15) || (y >= 22 && y <= 24) || (y >= 31 && y <= 33)) {
                open[y][6] = true; // door
            } else {
                open[y][6] = false;
            }
        }
        for (int x = 1; x <= 5; x++) {
            open[18][x] = false;
            open[27][x] = false;
        }

        // Counters in center:
        // Upper counter: x=10..23, y=16..17
        for (int y = 16; y <= 17; y++) {
            for (int x = 10; x <= 23; x++) open[y][x] = false;
        }
        // Main reception desk: x=28..45, y=15..17
        for (int y = 15; y <= 17; y++) {
            for (int x = 28; x <= 45; x++) open[y][x] = false;
        }

        // Triage Waiting room (x=10..22, y=21..34):
        for (int x = 10; x <= 22; x++) {
            if (x >= 14 && x <= 17) open[21][x] = true; else open[21][x] = false;
        }
        for (int y = 21; y <= 28; y++) open[y][22] = false;
        for (int x = 10; x <= 22; x++) open[34][x] = false;

        // Right Wing Rooms (x=49..58, y=12..37):
        for (int y = 12; y <= 37; y++) {
            if ((y >= 14 && y <= 16) || (y >= 21 && y <= 23) || (y >= 29 && y <= 31)) {
                open[y][48] = true; open[y][49] = true; // doors
            } else {
                open[y][48] = false;
            }
        }
        for (int x = 50; x <= 58; x++) {
            open[18][x] = false;
            open[25][x] = false;
        }

        // Boarded Emergency Intake doors at bottom (x=31..41, y=37..38)
        for (int y = 37; y <= 38; y++) {
            for (int x = 31; x <= 41; x++) open[y][x] = false;
        }

        // Corridors — open and wide:
        // West corridor (between consultation and counters/triage): x=7..9, y=10..37
        for (int y = 10; y <= 37; y++) {
            for (int x = 7; x <= 9; x++) open[y][x] = true;
        }
        // Bottom hallway connecting SW corner (x=1..6, y=36..38) to west corridor
        for (int y = 36; y <= 38; y++) {
            for (int x = 1; x <= 9; x++) open[y][x] = true;
        }
        // North corridor above reception counters: y=11..14, x=7..48
        for (int y = 11; y <= 14; y++) {
            for (int x = 7; x <= 48; x++) open[y][x] = true;
        }
        // East corridor: x=46..48, y=10..37
        for (int y = 10; y <= 37; y++) {
            for (int x = 46; x <= 48; x++) open[y][x] = true;
        }
        // South corridor in front of emergency intake: y=34..36, x=7..48
        for (int y = 34; y <= 36; y++) {
            for (int x = 7; x <= 48; x++) open[y][x] = true;
        }
        // Grand Central Reception Hall (Star of Life): x=24..45, y=18..33
        for (int y = 18; y <= 33; y++) {
            for (int x = 24; x <= 45; x++) open[y][x] = true;
        }
        // Link between upper counter and reception desk: x=24..27, y=14..20
        for (int y = 14; y <= 20; y++) {
            for (int x = 24; x <= 27; x++) open[y][x] = true;
        }

        // Checkpoints (file coords: cx = worldX, cy = 39 - worldY)
        int[][] checkpoints = {
                {1, 38},   // l2_cp01 (1.5, 1.5)
                {7, 18},   // l2_cp02 (7.5, 21.5)
                {14, 35},  // l2_cp03 (14.5, 4.5)
                {22, 5},   // l2_cp04 (22.5, 34.5)
                {31, 12},  // l2_cp05 (31.5, 27.5)
                {39, 18},  // l2_cp06 (39.5, 21.5)
                {46, 35},  // l2_cp07 (46.5, 4.5)
                {53, 16},  // l2_cp08 (53.5, 23.5)
        };
        for (int[] cp : checkpoints) {
            clearAround(open, cp[0], cp[1], 1);
        }

        // Features & Exit:
        // road_patrol: (22.5, 34.5) -> (22, 5)
        // survivor_market: (7.5, 21.5) -> (7, 18)
        // survivor_shelter: (31.5, 27.5) -> (31, 12)
        // relay_clinic: (14.5, 4.5) -> (14, 35)
        // relay_depot: (39.5, 21.5) -> (39, 18)
        // exit: (53.5, 23.5) -> (53, 16)
        clearAround(open, 53, 16, 1);

        // Verify reachability from spawn (1, 38)
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
        System.out.printf("Level 2 Connectivity: %d open tiles, %d reachable (%.1f%%)%n",
                totalOpen, totalReached, 100.0 * totalReached / totalOpen);

        // Write level2.map
        List<String> lines = new ArrayList<>();
        lines.add("// Level 2 - Hospital Ground Floor, collision grid for map2.png");
        lines.add("// Generated by tools/GenLevel2Map.java");
        lines.add("// subdivisions: 1");
        for (int y = 0; y < ROWS; y++) {
            StringBuilder sb = new StringBuilder(COLS);
            for (int x = 0; x < COLS; x++) {
                sb.append(open[y][x] ? '.' : '#');
            }
            lines.add(sb.toString());
        }

        Path out = Path.of("core/src/main/resources/maps/level2.map");
        Files.write(out, lines);
        System.out.printf("Wrote %s (%d rows x %d cols)%n", out, ROWS, COLS);
    }

    static void clearAround(boolean[][] open, int cx, int cy, int r) {
        for (int y = cy - r; y <= cy + r; y++) {
            for (int x = cx - r; x <= cx + r; x++) {
                if (x > 0 && y > 0 && x < COLS - 1 && y < ROWS - 1) {
                    open[y][x] = true;
                }
            }
        }
    }

    static boolean isMap2Floor(int r, int g, int b) {
        return (r >= 65 && r <= 205 && g >= 85 && g <= 210 && b >= 95 && b <= 225 && b >= r - 20);
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
