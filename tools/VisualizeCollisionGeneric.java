import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Same purpose as tools/VisualizeCollision.java (paint the CURRENT collision
 * grid from a .map file over the level art so a human can see exactly what
 * blocks and what doesn't), generalized for levels whose grid covers the
 * FULL image with no content-box inset — i.e. levels 2 and 3, whose
 * tools/GenLevel2Map.java / GenLevel3Map.java sample tw = imgW/COLS,
 * th = imgH/ROWS starting at (0,0), unlike level 1's inset 45x33 box.
 *
 * <pre>
 *   java tools/VisualizeCollisionGeneric.java assets/map2.png core/src/main/resources/maps/level2.map out2.png 60 40
 * </pre>
 * Args: artImage, mapFile, outImage, cols, rows.
 * Red = blocked, green-tinted = walkable.
 */
public class VisualizeCollisionGeneric {

    public static void main(String[] args) throws Exception {
        BufferedImage src = ImageIO.read(new File(args[0]));
        List<String> rawLines = Files.readAllLines(Path.of(args[1]));
        int cols = Integer.parseInt(args[3]);
        int rows = Integer.parseInt(args[4]);

        int subdivisions = 1;
        List<String> gridRows = new java.util.ArrayList<>();
        for (String line : rawLines) {
            String t = line.strip();
            if (t.isEmpty()) continue;
            if (t.startsWith("//")) {
                if (t.contains("subdivisions:")) {
                    subdivisions = Integer.parseInt(t.replaceAll(".*subdivisions:\\s*(\\d+).*", "$1"));
                }
                continue;
            }
            gridRows.add(line);
        }

        int cellsHigh = gridRows.size();
        int cellsWide = gridRows.get(0).length();
        System.out.printf("map grid: %d x %d cells (subdivisions=%d) over %d x %d tiles%n",
                cellsWide, cellsHigh, subdivisions, cols, rows);

        BufferedImage out = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        g.drawImage(src, 0, 0, null);

        double px = (double) src.getWidth() / cols;
        double py = (double) src.getHeight() / rows;
        double cpx = px / subdivisions, cpy = py / subdivisions;

        // file row 0 is the TOP of the image (matches how TileMap.fromRows reads it)
        for (int row = 0; row < cellsHigh; row++) {
            String line = gridRows.get(row);
            for (int col = 0; col < cellsWide && col < line.length(); col++) {
                boolean blocked = line.charAt(col) == '#';
                int x0 = (int) Math.round(col * cpx);
                int y0 = (int) Math.round(row * cpy);
                int w = (int) Math.ceil(cpx) + 1;
                int h = (int) Math.ceil(cpy) + 1;
                g.setColor(blocked ? new Color(255, 0, 0, 110) : new Color(0, 255, 0, 40));
                g.fillRect(x0, y0, w, h);
            }
        }

        g.setColor(new Color(255, 255, 255, 40));
        for (int c = 0; c <= cols * subdivisions; c++) {
            int x = (int) Math.round(c * cpx);
            g.drawLine(x, 0, x, src.getHeight());
        }
        for (int r = 0; r <= rows * subdivisions; r++) {
            int y = (int) Math.round(r * cpy);
            g.drawLine(0, y, src.getWidth(), y);
        }
        g.dispose();

        ImageIO.write(out, "png", new File(args[2]));
        System.out.println("wrote " + args[2] + " — RED = currently blocked, tinted GREEN = currently walkable");
    }
}
