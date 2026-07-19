package com.infectedhour.core.ui;

/**
 * Scene2D HUD stage matching the exact layout in UI/UX doc §3:
 * top-left player HP/contamination bars, top-right global contamination
 * meter, bottom-right minimap, bottom-left objective list, bottom-center
 * inventory hotbar. Composes MiniMap + InventoryHotbar rather than
 * cramming everything into one class.
 */
public class Hud {

    private final MiniMap miniMap = new MiniMap();
    private final InventoryHotbar hotbar = new InventoryHotbar();

    // Palette tokens (UI/UX doc §1) — kept here so HUD styling has one source of truth.
    public static final float[] ACCENT_GOLD = hex("#E8B02A");
    public static final float[] ACCENT_RED = hex("#E85A4F");
    public static final float[] SAFE_GREEN = hex("#4CAF6D");
    public static final float[] TOXIC_PURPLE = hex("#7B4FA6");

    public void show() {
        // ================ TEAMMATE TASK: HUD LAYOUT ================
        // TODO(ui): build the Scene2D Stage per the ASCII diagram in
        // docs/03_UI_UX_DESIGN1.md par.3:
        //  - Root Table (fillParent) with anchored regions:
        //    top-left:      P1 + P2 portraits, HP bar (SAFE_GREEN) and
        //                   personal contamination bar (TOXIC_PURPLE) each
        //    top-right:     GLOBAL contamination meter (wide, gold frame)
        //    bottom-left:   objective list (auto-updating labels)
        //    bottom-center: InventoryHotbar    bottom-right: MiniMap
        //  - Simple generated drawables are fine — no skin file needed.
        //  - Minimum text size 14px at 1280x720 (doc par.1).
        // ===========================================================
        miniMap.show();
        hotbar.show();
    }

    public void render(float delta) {
        // ============== TEAMMATE TASK: HUD LIVE UPDATES ==============
        // TODO(ui): add an updateFrom(WorldSnapshot) method GameScreen calls
        // each frame, then per doc par.3:
        //  1. HP bars: width = hp%; on a decrease -> 0.2s shake + red
        //     vignette flash.
        //  2. Personal contamination: purple fill; at >=75% start a pulse
        //     tween + loop the heartbeat .ogg (stop below the threshold).
        //  3. Global meter: gold frame; at >=80% tint red + draw a purple
        //     creep at the screen edges (translucent gradient quads).
        //  4. Objectives: update "Rescue villagers 2/4" style labels; on any
        //     progress change flash the row accent-gold ~0.5s + play chime.
        // =============================================================
        miniMap.render(delta);
        hotbar.render(delta);
    }

    public void resize(int width, int height) {
    }

    public void dispose() {
    }

    private static float[] hex(String hex) {
        int r = Integer.valueOf(hex.substring(1, 3), 16);
        int g = Integer.valueOf(hex.substring(3, 5), 16);
        int b = Integer.valueOf(hex.substring(5, 7), 16);
        return new float[]{r / 255f, g / 255f, b / 255f, 1f};
    }
}
