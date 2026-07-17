package com.theinfectedhour.events;

import com.theinfectedhour.world.MapSection;

/** Published when a MapSection unlocks; camera re-clamps its bounds and the minimap reveals the section. */
public class SectionUnlockedEvent implements GameEvent {

    private final MapSection section;

    public SectionUnlockedEvent(MapSection section) {
        this.section = section;
    }

    public MapSection getSection() {
        return section;
    }
}
