import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

public class ExportCollisionOverlay {

    public static void main(String[] args) throws Exception {
        if (args.length < 3) {
            System.out.println("Usage: java ExportCollisionOverlay <imagePath> <mapPath> <outputPath> [levelNum]");
            return;
        }
        String imgPath = args[0];
        String mapPath = args[1];
        String outPath = args[2];
        int levelNum = args.length > 3 ? Integer.parseInt(args[3]) : 1;

        BufferedImage img = ImageIO.read(new File(imgPath));
        int imgW = img.getWidth(), imgH = img.getHeight();

        // Read .map file
        List<String> rows = new ArrayList<>();
        int subdivisions = 1;
        try (BufferedReader br = new BufferedReader(new FileReader(mapPath))) {
            String line;
            while ((line = br.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.isEmpty()) continue;
                if (trimmed.startsWith("//")) {
                    if (trimmed.contains("subdivisions:")) {
                        subdivisions = Integer.parseInt(trimmed.split("subdivisions:")[1].trim());
                    }
                    continue;
                }
                rows.add(trimmed);
            }
        }

        int ch = rows.size();
        int cw = rows.get(0).length();
        System.out.printf("Loaded map: %d rows x %d cols, sub=%d, img=%dx%d%n", ch, cw, subdivisions, imgW, imgH);

        BufferedImage overlay = new BufferedImage(imgW, imgH, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = overlay.createGraphics();
        g.drawImage(img, 0, 0, null);

        // Determine coordinate mapping
        if (levelNum == 1) {
            double ox = 32.0, oy = 32.0;
            int cols = 45, rowsTiles = 33;
            double px = 1857.0 / cols;
            double py = 1363.0 / rowsTiles;
            double cpx = px / subdivisions;
            double cpy = py / subdivisions;

            for (int cy = 0; cy < ch; cy++) {
                String row = rows.get(cy);
                for (int cx = 0; cx < cw; cx++) {
                    char c = row.charAt(cx);
                    int x0 = (int) Math.round(ox + cx * cpx);
                    int x1 = (int) Math.round(ox + (cx + 1) * cpx);
                    int y0 = (int) Math.round(oy + cy * cpy);
                    int y1 = (int) Math.round(oy + (cy + 1) * cpy);

                    if (c == '#') {
                        // Blocked: red tint
                        g.setColor(new Color(255, 0, 0, 90));
                        g.fillRect(x0, y0, x1 - x0, y1 - y0);
                    } else {
                        // Walkable: subtle green dot or faint tint
                        // keep clear
                    }
                }
            }
        } else {
            // Whole-image mapping (for level 2 and level 3)
            double cpx = (double) imgW / cw;
            double cpy = (double) imgH / ch;

            for (int cy = 0; cy < ch; cy++) {
                String row = rows.get(cy);
                for (int cx = 0; cx < cw; cx++) {
                    char c = row.charAt(cx);
                    int x0 = (int) Math.round(cx * cpx);
                    int x1 = (int) Math.round((cx + 1) * cpx);
                    int y0 = (int) Math.round(cy * cpy);
                    int y1 = (int) Math.round((cy + 1) * cpy);

                    if (c == '#') {
                        g.setColor(new Color(255, 0, 0, 90));
                        g.fillRect(x0, y0, x1 - x0, y1 - y0);
                    }
                }
            }
        }

        g.dispose();
        ImageIO.write(overlay, "png", new File(outPath));
        System.out.printf("Wrote overlay: %s%n", outPath);
    }
}
