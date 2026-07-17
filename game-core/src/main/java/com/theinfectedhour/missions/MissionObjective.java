package com.theinfectedhour.missions;

/** Contract for a single completable mission goal (Architecture.md §5). */
public interface MissionObjective {

    boolean isComplete();

    float getProgress();
}
