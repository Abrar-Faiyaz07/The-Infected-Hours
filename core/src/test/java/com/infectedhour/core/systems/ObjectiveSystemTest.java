package com.infectedhour.core.systems;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ObjectiveSystemTest {

    @Test
    void objectiveCompletesAtTarget() {
        ObjectiveSystem system = new ObjectiveSystem();
        system.register("rescue", ObjectiveSystem.ObjectiveType.RESCUE_VILLAGER, 4);

        for (int i = 0; i < 4; i++) {
            system.incrementProgress("rescue");
        }

        assertTrue(system.isObjectiveComplete("rescue"));
    }

    @Test
    void progressDoesNotExceedTarget() {
        ObjectiveSystem system = new ObjectiveSystem();
        system.register("sanitation", ObjectiveSystem.ObjectiveType.ACTIVATE_SANITATION, 2);

        for (int i = 0; i < 5; i++) {
            system.incrementProgress("sanitation");
        }

        assertEquals(2, system.getObjectives().get("sanitation").progress);
    }

    @Test
    void allObjectivesCompleteOnlyWhenEveryOneIsDone() {
        ObjectiveSystem system = new ObjectiveSystem();
        system.register("a", ObjectiveSystem.ObjectiveType.COLLECT_SAMPLE, 1);
        system.register("b", ObjectiveSystem.ObjectiveType.ISOLATE_ZONE, 1);

        system.incrementProgress("a");
        assertFalse(system.areAllObjectivesComplete());

        system.incrementProgress("b");
        assertTrue(system.areAllObjectivesComplete());
    }

    @Test
    void unknownObjectiveThrows() {
        ObjectiveSystem system = new ObjectiveSystem();
        assertThrows(IllegalArgumentException.class, () -> system.incrementProgress("nope"));
    }
}
