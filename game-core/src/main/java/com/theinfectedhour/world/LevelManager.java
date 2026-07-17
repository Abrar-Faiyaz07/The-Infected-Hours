package com.theinfectedhour.world;

import com.theinfectedhour.events.EventBus;
import com.theinfectedhour.events.MissionCompletedEvent;
import com.theinfectedhour.events.SectionUnlockedEvent;

/**
 * Owns level progression and the active Level. Level count is read from level
 * config via MapLoader, never hardcoded — shipped content targets exactly 2
 * levels + boss fight (Architecture.md §4a).
 *
 * Section progression (Observer pattern): subscribes to MissionCompletedEvent
 * and unlocks every MapSection gated on that mission, then publishes
 * SectionUnlockedEvent so the camera and minimap can react without coupling.
 */
public class LevelManager {

    private final MapLoader mapLoader = new MapLoader();
    private Level activeLevel;

    public LevelManager() {
        EventBus.getInstance().subscribe(MissionCompletedEvent.class, event -> {
            onMissionCompleted((MissionCompletedEvent) event);
        });
    }

    public void loadLevel(int levelId) {
        // TODO: activeLevel = mapLoader.load("level" + levelId);
    }

    public Level getActiveLevel() {
        return activeLevel;
    }

    private void onMissionCompleted(MissionCompletedEvent event) {
        // TODO: for each locked section whose requiredMissionId matches,
        //       section.unlock() then publish new SectionUnlockedEvent(section)
    }
}
