package com.infectedhour.core.ui;

/**
 * Bottom-right minimap (UI/UX doc §3). Jane's passive gives a larger reveal
 * radius (PRD §4) — this class needs to know which local character is
 * active to size that radius, not just draw dots.
 */
public class MiniMap {

    private float revealRadius = 10f; // Elric default; Jane gets a larger radius

    public void setRevealRadius(float radius) {
        this.revealRadius = radius;
    }

    public void show() {
        // TODO(ui): create a small FrameBuffer (e.g. 160x160) to draw the
        // minimap into, or just draw scaled shapes directly in the corner —
        // the FrameBuffer keeps clipping tidy.
    }

    public void render(float delta) {
        // ============== TEAMMATE TASK: MINIMAP CONTENT ==============
        // TODO(ui): draw, scaled world->minimap, ONLY within revealRadius of
        // the local player (Jane's radius is larger — PRD par.4, so call
        // setRevealRadius based on the local character at screen setup):
        //  - objectives: gold diamonds     - contamination tiles: purple dots
        //  - villagers: white dots         - partner: small portrait arrow
        //  - local player: center dot; M key toggles zoom (doc par.4).
        // ============================================================
    }
}
