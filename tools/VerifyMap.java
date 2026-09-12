import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

public class VerifyMap {
    public static void main(String[] args) throws Exception {
        String path = args[0];
        int sub = 1;
        List<String> rows = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            String line;
            while ((line = br.readLine()) != null) {
                String t = line.trim();
                if (t.isEmpty()) continue;
                if (t.startsWith("//")) {
                    if (t.contains("subdivisions:")) {
                        sub = Integer.parseInt(t.split("subdivisions:")[1].trim());
                    }
                    continue;
                }
                rows.add(t);
            }
        }
        int ch = rows.size();
        int cw = rows.get(0).length();
        int widthTiles = cw / sub;
        int heightTiles = ch / sub;
        System.out.printf("Map %s: %d x %d cells (tiles %d x %d, sub=%d)%n",
                path, cw, ch, widthTiles, heightTiles, sub);

        // Check test points passed in args: name:x,y
        for (int i = 1; i < args.length; i++) {
            String[] parts = args[i].split(":");
            String name = parts[0];
            String[] xy = parts[1].split(",");
            float wx = Float.parseFloat(xy[0]);
            float wy = Float.parseFloat(xy[1]);

            // World to cell:
            // World y=0 is bottom row (file row ch-1)
            // World x=0 is left col (file col 0)
            int cx = (int) Math.floor(wx * sub);
            int cyWorld = (int) Math.floor(wy * sub);
            int cyFile = ch - 1 - cyWorld;

            if (cx < 0 || cx >= cw || cyFile < 0 || cyFile >= ch) {
                System.out.printf("FAIL: %s at (%.2f, %.2f) -> cell (%d, %d) is OUT OF BOUNDS%n",
                        name, wx, wy, cx, cyFile);
            } else {
                char c = rows.get(cyFile).charAt(cx);
                boolean open = (c != '#');
                System.out.printf("%s: %s at (%.2f, %.2f) -> file cell (%d, %d) = '%c'%n",
                        open ? "PASS" : "FAIL", name, wx, wy, cx, cyFile, c);
            }
        }
    }
}
