package com.infectedhour.core.level;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LevelExitTest {

    @Test
    void levelOneExitCoversTheUpstairsSignButNotTheNearbyCorridor() {
        LevelExit exit = LevelExit.forLevel(1).orElseThrow();

        assertTrue(exit.contains(42.1f, 30.4f));
        assertTrue(exit.contains(41.0f, 30.5f));
        assertFalse(exit.contains(38.0f, 30.5f));
    }

    @Test
    void bossLevelHasNoNextLevelExit() {
        assertTrue(LevelExit.forLevel(3).isEmpty());
    }
}
