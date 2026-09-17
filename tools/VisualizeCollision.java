import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Offline tool: paints the CURRENT collision grid from a .map file directly
 * over the level art, so a human can see exactly what blocks and what
 * doesn't without opening any code.
 *
 * <pre>
 *   java tools/VisualizeCollision.java assets/map.png core/src/main/resources/maps/level1.map out.png
 * </pre>
 *
 * Red = blocked, untinted = walkable. Uses the same OX/OY/PX/PY/SUB as
 * GenMap.java / ExportMask.java, so cells line up with the real grid.
 */
public class VisualizeCollision {

    static final double OX = 32.0, OY = 32.0;
    static final int COLS = 45, ROWS = 33;
    static final double PX = 1857.0 / COLS, PY = 1363.0 / ROWS;

    public static void main(String[] args) throws Exception {
        BufferedImage src = ImageIO.read(new File(args[0]));
        List<String> rawLines = Files.readAllLines(Path.of(args[1]));

        int subdivisions = 1;
        java.util.List<String> rows = new java.util.ArrayList<>();
        for (String line : rawLines) {
            String t = line.strip();
            if (t.isEmpty()) continue;
            if (t.startsWith("//")) {
                if (t.contains("subdivisions:")) {
                    subdivisions = Integer.parseInt(t.replaceAll(".*subdivisions:\\s*(\\d+).*", "$1"));
                }
                continue;
            }
            rows.add(line);
        }

        int cellsHigh = rows.size();
        int cellsWide = rows.get(0).length();
        System.out.printf("map grid: %d x %d cells (subdivisions=%d)%n", cellsWide, cellsHigh, subdivisions);

        BufferedImage out = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        g.drawImage(src, 0, 0, null);

        double cpx = PX / subdivisions, cpy = PY / subdivisions;

        // file row 0 is the TOP of the image (matches how TileMap.fromRows reads it)
        for (int row = 0; row < cellsHigh; row++) {
            String line = rows.get(row);
            for (int col = 0; col < cellsWide && col < line.length(); col++) {
                boolean blocked = line.charAt(col) == '#';
                int x0 = (int) Math.round(OX + col * cpx);
                int y0 = (int) Math.round(OY + row * cpy);
                int w = (int) Math.ceil(cpx) + 1;
                int h = (int) Math.ceil(cpy) + 1;
                if (blocked) {
                    g.setColor(new Color(255, 0, 0, 110));
                    g.fillRect(x0, y0, w, h);
                } else {
                    g.setColor(new Color(0, 255, 0, 40));
                    g.fillRect(x0, y0, w, h);
                }
            }
        }

        // grid lines on top so cells are still countable
        g.setColor(new Color(255, 255, 255, 40));
        for (int c = 0; c <= COLS * subdivisions; c++) {
            int x = (int) Math.round(OX + c * cpx);
            g.drawLine(x, (int) OY, x, (int) Math.round(OY + ROWS * PY));
        }
        for (int r = 0; r <= ROWS * subdivisions; r++) {
            int y = (int) Math.round(OY + r * cpy);
            g.drawLine((int) OX, y, (int) Math.round(OX + COLS * PX), y);
        }
        g.dispose();

        ImageIO.write(out, "png", new File(args[2]));
        System.out.println("wrote " + args[2] + " — RED = currently blocked, tinted GREEN = currently walkable");
    }
}
