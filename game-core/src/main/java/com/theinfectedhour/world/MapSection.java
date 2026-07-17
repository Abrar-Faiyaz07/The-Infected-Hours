package com.theinfectedhour.world;

import com.badlogic.gdx.math.Rectangle;

/**
 * One named region of a Level (Resident-Evil-style map part). Sections are
 * authored in Tiled as rectangles on the "sections" object layer; the first
 * section starts unlocked and later ones unlock when their gating mission
 * completes (see LevelManager).
 */
public class MapSection {

    private final String sectionId;
    private final Rectangle bounds;
    /** Mission that must complete before this section opens; null = open from the start. */
    private final String requiredMissionId;
    private boolean unlocked;

    public MapSection(String sectionId, Rectangle bounds, String requiredMissionId) {
        this.sectionId = sectionId;
        this.bounds = bounds;
        this.requiredMissionId = requiredMissionId;
        this.unlocked = requiredMissionId == null;
    }

    public String getSectionId() {
        return sectionId;
    }

    public Rectangle getBounds() {
        return bounds;
    }

    public String getRequiredMissionId() {
        return requiredMissionId;
    }

    public boolean isUnlocked() {
        return unlocked;
    }

    public void unlock() {
        this.unlocked = true;
    }
}
