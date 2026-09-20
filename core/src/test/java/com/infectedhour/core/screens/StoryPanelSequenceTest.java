package com.infectedhour.core.screens;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StoryPanelSequenceTest {

    @Test
    void debugCoopCanOpenTheCorrectCinematicBeforeEveryMap() {
        assertEquals(StoryPanelScreen.Sequence.INTRO, StoryPanelScreen.Sequence.beforeLevel(1));
        assertEquals(StoryPanelScreen.Sequence.LEVEL_2_START, StoryPanelScreen.Sequence.beforeLevel(2));
        assertEquals(StoryPanelScreen.Sequence.LEVEL_3_START, StoryPanelScreen.Sequence.beforeLevel(3));
        assertEquals(StoryPanelScreen.Sequence.LEVEL_4_START, StoryPanelScreen.Sequence.beforeLevel(4));
        assertEquals(StoryPanelScreen.Sequence.LEVEL_5_START, StoryPanelScreen.Sequence.beforeLevel(5));
        assertEquals(StoryPanelScreen.Sequence.LEVEL_6_START, StoryPanelScreen.Sequence.beforeLevel(6));
    }

    @Test
    void campaignCompletionSelectsTheSameTransitionUsedByCoop() {
        for (int completedLevel = 1; completedLevel <= 5; completedLevel++) {
            assertEquals(
                    StoryPanelScreen.Sequence.beforeLevel(completedLevel + 1),
                    StoryPanelScreen.Sequence.afterCompletedLevel(completedLevel)
            );
        }
        assertEquals(StoryPanelScreen.Sequence.ENDING,
                StoryPanelScreen.Sequence.afterCompletedLevel(6));
    }
}
