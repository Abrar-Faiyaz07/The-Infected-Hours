package com.infectedhour.core.assets;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Loads game textures without letting a missing file kill the application.
 *
 * <p>libGDX's {@code new Texture(Gdx.files.internal("map.png"))} throws
 * {@link com.badlogic.gdx.utils.GdxRuntimeException} when the file is absent,
 * and because textures are loaded inside {@code Screen.show()} that exception
 * propagates out of the render loop and terminates the whole game. One
 * un-committed PNG therefore takes down every other feature, including
 * networking that has nothing to do with art.
 *
 * <p>This class substitutes a generated placeholder instead. The game keeps
 * running, the placeholder is visually obvious (magenta/black checkerboard,
 * the long-standing convention for "missing texture"), and
 * {@link #getMissingAssets()} lists what still needs to be supplied so it can
 * be shown on screen rather than discovered in a stack trace.
 *
 * <p>Placeholder dimensions are caller-supplied because sprite sheets are
 * consumed with {@code TextureRegion.split(texture, w/cols, h/rows)} — a
 * placeholder of the wrong size would produce a divide-by-zero or wrongly
 * sliced frames, trading one crash for another.
 */
public final class GameAssets {

    private static final Set<String> MISSING = new LinkedHashSet<>();

    private GameAssets() {
    }

    /**
     * @param path           classpath/asset-relative name, e.g. {@code "map.png"}
     * @param fallbackWidth  width of the generated placeholder if the file is absent
     * @param fallbackHeight height of the generated placeholder if the file is absent
     */
    public static Texture texture(String path, int fallbackWidth, int fallbackHeight) {
        try {
            FileHandle handle = Gdx.files.internal(path);
            if (handle.exists()) {
                Texture texture = new Texture(handle);
                texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
                return texture;
            }
        } catch (RuntimeException e) {
            Gdx.app.error("GameAssets", "Could not load " + path + " — using a placeholder", e);
        }
        MISSING.add(path);
        Gdx.app.log("GameAssets", "Missing asset: " + path + " — using a placeholder");
        return placeholder(fallbackWidth, fallbackHeight);
    }

    /**
     * Sprite-sheet variant. The placeholder is sized to an exact multiple of the
     * grid so {@code TextureRegion.split} still yields {@code rows x cols} frames.
     */
    public static Texture spriteSheet(String path, int columns, int rows, int cellSize) {
        return texture(path, Math.max(1, columns) * cellSize, Math.max(1, rows) * cellSize);
    }

    /** Magenta/black checkerboard — the conventional "texture not found" marker. */
    private static Texture placeholder(int width, int height) {
        int w = Math.max(2, width);
        int h = Math.max(2, height);
        int cell = Math.max(2, Math.min(w, h) / 8);

        Pixmap pixmap = new Pixmap(w, h, Pixmap.Format.RGBA8888);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                boolean even = ((x / cell) + (y / cell)) % 2 == 0;
                pixmap.setColor(even ? Color.MAGENTA : Color.BLACK);
                pixmap.drawPixel(x, y);
            }
        }
        Texture texture = new Texture(pixmap);
        texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        pixmap.dispose(); // the GPU has its own copy now
        return texture;
    }

    /** Asset names that fell back to a placeholder, in the order they were requested. */
    public static List<String> getMissingAssets() {
        return new ArrayList<>(MISSING);
    }

    public static boolean hasMissingAssets() {
        return !MISSING.isEmpty();
    }

    /** One-line summary for an on-screen warning. */
    public static String missingSummary() {
        return MISSING.isEmpty() ? "" : "Missing art (" + MISSING.size() + "): " + String.join(", ", MISSING);
    }
}
