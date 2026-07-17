package com.theinfectedhour.events;

/** Published when a MissionObjective reaches completion; LevelManager reacts by unlocking the next MapSection. */
public class MissionCompletedEvent implements GameEvent {

    private final String missionId;

    public MissionCompletedEvent(String missionId) {
        this.missionId = missionId;
    }

    public String getMissionId() {
        return missionId;
    }
}
