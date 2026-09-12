package tools;

import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

public class GenerateHealSprites {

    public static void main(String[] args) throws Exception {
        generateHealIcon(new File("assets/heal_icon.png"));
        generateHealEffect(new File("assets/heal_effect.png"));
        System.out.println("Generated assets/heal_icon.png and assets/heal_effect.png successfully.");
    }

    private static void generateHealIcon(File outputFile) throws Exception {
        int w = 64;
        int h = 64;
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Outer glow
        g.setColor(new Color(16, 185, 129, 60));
        g.fill(new RoundRectangle2D.Float(2, 2, 60, 60, 16, 16));

        // Background medkit container
        GradientPaint bgGrad = new GradientPaint(0, 4, new Color(20, 35, 30), 0, 60, new Color(10, 20, 18));
        g.setPaint(bgGrad);
        g.fill(new RoundRectangle2D.Float(4, 4, 56, 56, 14, 14));

        // Border - emerald accent
        g.setColor(new Color(16, 185, 129, 220));
        g.setStroke(new BasicStroke(2.5f));
        g.draw(new RoundRectangle2D.Float(5, 5, 54, 54, 13, 13));

        // Inner shadow / vignette
        g.setColor(new Color(5, 15, 12, 100));
        g.setStroke(new BasicStroke(1.5f));
        g.draw(new RoundRectangle2D.Float(7, 7, 50, 50, 11, 11));

        // Cross soft glow
        g.setColor(new Color(52, 211, 153, 90));
        int cx = w / 2;
        int cy = h / 2;
        int armL = 16;
        int armThick = 10;
        g.fillRect(cx - armThick / 2 - 2, cy - armL - 2, armThick + 4, armL * 2 + 4);
        g.fillRect(cx - armL - 2, cy - armThick / 2 - 2, armL * 2 + 4, armThick + 4);

        // White Cross
        g.setColor(new Color(245, 255, 250));
        g.fillRect(cx - armThick / 2, cy - armL, armThick, armL * 2);
        g.fillRect(cx - armL, cy - armThick / 2, armL * 2, armThick);

        // Cross subtle highlight
        g.setColor(new Color(255, 255, 255, 180));
        g.fillRect(cx - armThick / 2 + 1, cy - armL + 1, armThick - 2, 3);
        g.fillRect(cx - armL + 1, cy - armThick / 2 + 1, 3, armThick - 2);

        // Little corner accent dots
        g.setColor(new Color(16, 185, 129, 200));
        g.fillOval(8, 8, 4, 4);
        g.fillOval(52, 8, 4, 4);
        g.fillOval(8, 52, 4, 4);
        g.fillOval(52, 52, 4, 4);

        g.dispose();
        ImageIO.write(img, "PNG", outputFile);
    }

    private static void generateHealEffect(File outputFile) throws Exception {
        int size = 128;
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int cx = size / 2;
        int cy = size / 2;

        // Radial glowing ring
        for (int r = 56; r >= 10; r -= 4) {
            float alpha = (float) Math.sin((r / 56.0) * Math.PI) * 0.28f;
            g.setColor(new Color(16, 185, 129, (int) (alpha * 255)));
            g.setStroke(new BasicStroke(4f));
            g.drawOval(cx - r, cy - r, r * 2, r * 2);
        }

        // Concentric inner aura
        for (int r = 36; r >= 4; r -= 4) {
            float alpha = 0.15f + (1.0f - (r / 36.0f)) * 0.25f;
            g.setColor(new Color(52, 211, 153, (int) (alpha * 255)));
            g.fillOval(cx - r, cy - r, r * 2, r * 2);
        }

        // Sparkling rising medical crosses
        int[][] crossPositions = {
            {cx, cy, 7, 3, 255},
            {cx - 24, cy - 14, 5, 2, 220},
            {cx + 26, cy - 18, 6, 2, 230},
            {cx - 16, cy + 22, 5, 2, 190},
            {cx + 20, cy + 20, 5, 2, 190},
            {cx - 36, cy + 4, 4, 2, 170},
            {cx + 38, cy - 2, 4, 2, 170},
            {cx - 8, cy - 36, 5, 2, 200},
            {cx + 12, cy - 38, 4, 2, 180},
        };

        for (int[] cp : crossPositions) {
            int px = cp[0];
            int py = cp[1];
            int arm = cp[2];
            int th = cp[3];
            int a = cp[4];

            // glow
            g.setColor(new Color(110, 231, 183, a / 2));
            g.fillRect(px - th / 2 - 1, py - arm - 1, th + 2, arm * 2 + 2);
            g.fillRect(px - arm - 1, py - th / 2 - 1, arm * 2 + 2, th + 2);

            // solid cross
            g.setColor(new Color(255, 255, 255, a));
            g.fillRect(px - th / 2, py - arm, th, arm * 2);
            g.fillRect(px - arm, py - th / 2, arm * 2, th);
        }

        g.dispose();
        ImageIO.write(img, "PNG", outputFile);
    }
}
