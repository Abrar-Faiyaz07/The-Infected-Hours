import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;

/**
 * Offline tool: writes a paintable collision template.
 *
 * <pre>
 *   java tools/ExportMask.java assets/map.png assets/collision_mask.png
 * </pre>
 *
 * <p>The output is the level art with the collision grid drawn over it. Open it
 * in any paint program and mark what you want:
 * <ul>
 *   <li><b>pure red</b>   (255,0,0) — force BLOCKED</li>
 *   <li><b>pure green</b> (0,255,0) — force WALKABLE</li>
 *   <li>anything left unpainted keeps whatever {@code GenMap} traced automatically</li>
 * </ul>
 *
 * <p>Then re-run {@code GenMap} with the painted file as a third argument.
 * Paint roughly — a cell is decided by which colour covers most of it, so you do
 * not need to follow the grid lines precisely. Antialiased edges are ignored,
 * only strong red/green counts, so a soft brush is fine.
 *
 * <p>Do not resize or crop the image: cells are located by pixel position, so it
 * must stay exactly the same dimensions as {@code map.png}.
 */
public class ExportMask {

    static final double OX = 32.0, OY = 32.0;
    static final int COLS = 45, ROWS = 33, SUB = 3;
    static final double PX = 1857.0 / COLS, PY = 1363.0 / ROWS;

    public static void main(String[] args) throws Exception {
        BufferedImage src = ImageIO.read(new File(args[0]));
        BufferedImage out = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        g.drawImage(src, 0, 0, null);

        double cpx = PX / SUB, cpy = PY / SUB;

        // Fine lines on every collision cell, brighter ones on tile boundaries,
        // so the structure is readable without hiding the art underneath.
        for (int c = 0; c <= COLS * SUB; c++) {
            int x = (int) Math.round(OX + c * cpx);
            g.setColor(c % SUB == 0 ? new Color(255, 255, 0, 130) : new Color(255, 255, 255, 55));
            g.drawLine(x, (int) OY, x, (int) Math.round(OY + ROWS * PY));
        }
        for (int r = 0; r <= ROWS * SUB; r++) {
            int y = (int) Math.round(OY + r * cpy);
            g.setColor(r % SUB == 0 ? new Color(255, 255, 0, 130) : new Color(255, 255, 255, 55));
            g.drawLine((int) OX, y, (int) Math.round(OX + COLS * PX), y);
        }
        g.dispose();

        ImageIO.write(out, "png", new File(args[1]));
        System.out.printf("wrote %s (%dx%d, %d x %d cells)%n",
                args[1], out.getWidth(), out.getHeight(), COLS * SUB, ROWS * SUB);
        System.out.println("Paint pure RED to block, pure GREEN to open, leave the rest untouched.");
    }
}
