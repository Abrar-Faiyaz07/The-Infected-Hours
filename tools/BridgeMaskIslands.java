import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.*;
import java.util.List;

/**
 * Offline tool: repairs a hand-painted green mask <em>as artwork</em>, before
 * it ever reaches tools/MaskToMapGeneric.java.
 *
 * <p>A painted mask often splits into islands, because the art separates areas
 * with a fence, hedge or barricade that the painter greened on both sides but
 * not across. MaskToMapGeneric's connectivity repair would then bulldoze 3x3
 * blocks through those barriers to join them, which reads as holes punched in
 * the foliage. This tool instead:
 *
 * <ul>
 *   <li>drops green islands below {@code minIsland} tiles — brush bleed, not
 *       real space, and connecting them carves the most for the least gain;</li>
 *   <li>joins every surviving island to the largest region along the
 *       <em>narrowest</em> crossing, painting a corridor {@code bridgeWidth}
 *       tiles wide — the line a person would have painted.</li>
 * </ul>
 *
 * The result is a mask that needs no repair downstream, so the shipped layout
 * is the painted one rather than the algorithm's.
 *
 * <pre>
 *   java tools/BridgeMaskIslands.java in.png out.png 60 40 4 2
 * </pre>
 * Args: inMask, outMask, cols, rows, minIsland, bridgeWidth.
 */
public class BridgeMaskIslands {

    static final int GREEN_RGB = new java.awt.Color(64, 224, 0).getRGB();
    static final int DROP_RGB = new java.awt.Color(24, 24, 24).getRGB();

    /** Must match MaskToMapGeneric.isPaintedGreen — see the note there. */
    static boolean tintStyle = false;

    static boolean isPaintedGreen(int r, int g, int b) {
        if (tintStyle) {
            return g > r;
        }
        return g > 170 && g - r > 60 && g - b > 60;
    }

    static boolean detectTintStyle(BufferedImage img) {
        long red = 0, total = 0;
        for (int y = 0; y < img.getHeight(); y += 3) {
            for (int x = 0; x < img.getWidth(); x += 3) {
                int rgb = img.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF, g = (rgb >> 8) & 0xFF, b = rgb & 0xFF;
                total++;
                if (r - g > 40 && r - b > 40) red++;
            }
        }
        return (double) red / total > 0.15;
    }

    public static void main(String[] args) throws Exception {
        BufferedImage img = ImageIO.read(new File(args[0]));
        int cols = Integer.parseInt(args[2]), rows = Integer.parseInt(args[3]);
        int minIsland = Integer.parseInt(args[4]);
        int bridgeWidth = Integer.parseInt(args[5]);

        int iw = img.getWidth(), ih = img.getHeight();
        double px = (double) iw / cols, py = (double) ih / rows;
        tintStyle = detectTintStyle(img);

        boolean[][] open = new boolean[rows][cols];
        for (int cy = 0; cy < rows; cy++) {
            for (int cx = 0; cx < cols; cx++) {
                int g = 0, t = 0;
                for (int y = clamp((int) Math.round(cy * py), 0, ih); y < clamp((int) Math.round((cy + 1) * py), 0, ih); y++) {
                    for (int x = clamp((int) Math.round(cx * px), 0, iw); x < clamp((int) Math.round((cx + 1) * px), 0, iw); x++) {
                        int rgb = img.getRGB(x, y);
                        t++;
                        if (isPaintedGreen((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF)) g++;
                    }
                }
                open[cy][cx] = t > 0 && g * 2 > t;
            }
        }

        List<List<int[]>> islands = components(open, cols, rows);
        islands.sort((a, b) -> Integer.compare(b.size(), a.size()));
        System.out.printf("%d green islands before repair (largest %d tiles)%n",
                islands.size(), islands.isEmpty() ? 0 : islands.get(0).size());
        if (islands.isEmpty()) {
            throw new IllegalStateException("mask has no green paint");
        }

        List<int[]> main = islands.get(0);
        boolean[][] keep = new boolean[rows][cols];
        for (int[] c : main) keep[c[1]][c[0]] = true;

        int dropped = 0, bridged = 0;
        for (int i = 1; i < islands.size(); i++) {
            List<int[]> isl = islands.get(i);
            if (isl.size() < minIsland) {
                for (int[] c : isl) paintTile(img, c[0], c[1], px, py, DROP_RGB);
                dropped++;
                continue;
            }
            int[] best = nearestPair(isl, keep, cols, rows);
            if (best == null) {
                for (int[] c : isl) paintTile(img, c[0], c[1], px, py, DROP_RGB);
                dropped++;
                continue;
            }
            for (int[] t : corridor(best[0], best[1], best[2], best[3], bridgeWidth, cols, rows)) {
                paintTile(img, t[0], t[1], px, py, GREEN_RGB);
                keep[t[1]][t[0]] = true;
            }
            for (int[] c : isl) keep[c[1]][c[0]] = true;
            bridged++;
            System.out.printf("  bridged island of %3d tiles: world (%d,%d) -> (%d,%d)%n",
                    isl.size(), best[0], rows - 1 - best[1], best[2], rows - 1 - best[3]);
        }
        System.out.printf("bridged %d islands, dropped %d specks (< %d tiles)%n", bridged, dropped, minIsland);

        ImageIO.write(img, "png", new File(args[1]));
        System.out.println("wrote " + args[1]);
    }

    static int clamp(int v, int lo, int hi) { return Math.max(lo, Math.min(hi, v)); }

    static void paintTile(BufferedImage img, int cx, int cy, double px, double py, int rgb) {
        int x0 = clamp((int) Math.round(cx * px), 0, img.getWidth());
        int x1 = clamp((int) Math.round((cx + 1) * px), 0, img.getWidth());
        int y0 = clamp((int) Math.round(cy * py), 0, img.getHeight());
        int y1 = clamp((int) Math.round((cy + 1) * py), 0, img.getHeight());
        for (int y = y0; y < y1; y++)
            for (int x = x0; x < x1; x++)
                img.setRGB(x, y, rgb);
    }

    static List<List<int[]>> components(boolean[][] open, int cols, int rows) {
        boolean[][] seen = new boolean[rows][cols];
        List<List<int[]>> out = new ArrayList<>();
        for (int y = 0; y < rows; y++)
            for (int x = 0; x < cols; x++) {
                if (!open[y][x] || seen[y][x]) continue;
                List<int[]> comp = new ArrayList<>();
                Deque<int[]> q = new ArrayDeque<>();
                q.add(new int[]{x, y});
                seen[y][x] = true;
                while (!q.isEmpty()) {
                    int[] p = q.poll();
                    comp.add(p);
                    for (int[] n : nbs(p)) {
                        if (n[0] < 0 || n[1] < 0 || n[0] >= cols || n[1] >= rows) continue;
                        if (open[n[1]][n[0]] && !seen[n[1]][n[0]]) { seen[n[1]][n[0]] = true; q.add(n); }
                    }
                }
                out.add(comp);
            }
        return out;
    }

    /** Closest island-cell / main-cell pair, i.e. the narrowest place to cross. */
    static int[] nearestPair(List<int[]> island, boolean[][] keep, int cols, int rows) {
        int[] best = null;
        long bestD = Long.MAX_VALUE;
        for (int[] a : island) {
            for (int y = 0; y < rows; y++) {
                for (int x = 0; x < cols; x++) {
                    if (!keep[y][x]) continue;
                    long dx = a[0] - x, dy = a[1] - y;
                    long d = dx * dx + dy * dy;
                    if (d < bestD) { bestD = d; best = new int[]{a[0], a[1], x, y}; }
                }
            }
        }
        return best;
    }

    /**
     * Axis-aligned L-shaped corridor, thickened to the given width.
     *
     * <p>Deliberately not a Bresenham line: a diagonal one-tile line is
     * 8-connected but not 4-connected, so it would leave the two regions
     * disconnected under the same 4-neighbour flood the map loader uses — the
     * bridge would look painted and still not join anything.
     */
    static List<int[]> corridor(int x0, int y0, int x1, int y1, int width, int cols, int rows) {
        List<int[]> out = new ArrayList<>();
        int lo = -((width - 1) / 2), hi = width / 2;
        for (int x = Math.min(x0, x1); x <= Math.max(x0, x1); x++) {
            for (int o = lo; o <= hi; o++) add(out, x, y0 + o, cols, rows);
        }
        for (int y = Math.min(y0, y1); y <= Math.max(y0, y1); y++) {
            for (int o = lo; o <= hi; o++) add(out, x1 + o, y, cols, rows);
        }
        return out;
    }

    static void add(List<int[]> out, int x, int y, int cols, int rows) {
        if (x >= 0 && y >= 0 && x < cols && y < rows) out.add(new int[]{x, y});
    }

    static int[][] nbs(int[] p) {
        return new int[][]{{p[0]+1,p[1]},{p[0]-1,p[1]},{p[0],p[1]+1},{p[0],p[1]-1}};
    }
}
