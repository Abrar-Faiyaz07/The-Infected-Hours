package com.theinfectedhour.ui.hud;

import java.util.HashSet;
import java.util.Set;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.theinfectedhour.entities.Renderable;
import com.theinfectedhour.world.MapSection;

/**
 * HUD widget rendering a small overview map with player and objective markers.
 * Only UNLOCKED sections are drawn — locked map parts stay hidden until their
 * gating mission completes (revealed via SectionUnlockedEvent).
 */
public class MiniMap implements Renderable {

    private final Set<String> revealedSectionIds = new HashSet<>();

    /** Called when a SectionUnlockedEvent arrives: this section now shows on the minimap. */
    public void revealSection(MapSection section) {
        revealedSectionIds.add(section.getSectionId());
    }

    @Override
    public void render(SpriteBatch batch) {
        // TODO: draw scaled-down outlines of revealed sections, then player
        //       markers and active objective markers on top
    }
}
