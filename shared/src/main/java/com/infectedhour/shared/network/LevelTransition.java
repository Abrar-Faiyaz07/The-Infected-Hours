package com.infectedhour.shared.network;

/** Host → Client(s): tells clients to load the next level / story sequence. */
public class LevelTransition {
    public int nextLevelNumber;
    public boolean bossLevel;

    public LevelTransition() {
    }

    public LevelTransition(int nextLevelNumber, boolean bossLevel) {
        this.nextLevelNumber = nextLevelNumber;
        this.bossLevel = bossLevel;
    }
}
