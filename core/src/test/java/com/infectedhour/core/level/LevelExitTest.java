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
    void levelThreeEndsAtJanesAmbulancePosition() {
        LevelExit exit = LevelExit.forLevel(3).orElseThrow();

        assertTrue(exit.contains(20.09f, 26.22f));
        assertFalse(exit.contains(36.10f, 22.63f));
    }

    @Test
    void levelFourEndsAtElricsMarkedPosition() {
        LevelExit exit = LevelExit.forLevel(4).orElseThrow();

        assertTrue(exit.contains(25.31f, 4.48f));
        assertFalse(exit.contains(40.63f, 36.27f));
    }

    @Test
    void levelFiveEndsAtTheLaboratoryGate() {
        LevelExit exit = LevelExit.forLevel(5).orElseThrow();

        assertTrue(exit.contains(31.77f, 27.08f));
        assertFalse(exit.contains(28.68f, 4.43f));
    }
}
