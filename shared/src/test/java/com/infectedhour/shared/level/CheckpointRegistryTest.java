package com.infectedhour.shared.level;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The registry is a contract, not just data: checkpoint ids are written into
 * {@code save_slot.checkpoint_id}, so these tests exist to make a careless edit
 * fail the build rather than silently break every existing save.
 */
class CheckpointRegistryTest {

    @Test
    @DisplayName("there are exactly 22 checkpoints, split 7 / 8 / 7")
    void campaignHasTwentyTwoCheckpoints() {
        assertEquals(22, CheckpointRegistry.TOTAL_CHECKPOINTS);
        assertEquals(22, CheckpointRegistry.all().size());
        assertEquals(7, CheckpointRegistry.forLevel(1).size());
        assertEquals(8, CheckpointRegistry.forLevel(2).size());
        assertEquals(7, CheckpointRegistry.forLevel(3).size());
    }

    @Test
    @DisplayName("every id is unique — a duplicate would make saves ambiguous")
    void idsAreUnique() {
        Set<String> ids = new HashSet<>();
        for (Checkpoint checkpoint : CheckpointRegistry.all()) {
            assertTrue(ids.add(checkpoint.id()), "duplicate id: " + checkpoint.id());
        }
        assertEquals(22, ids.size());
    }

    @Test
    @DisplayName("orderInLevel runs 1..n with no gaps, per level")
    void orderWithinEachLevelIsContiguous() {
        for (int level = 1; level <= 3; level++) {
            List<Checkpoint> checkpoints = CheckpointRegistry.forLevel(level);
            for (int i = 0; i < checkpoints.size(); i++) {
                assertEquals(i + 1, checkpoints.get(i).orderInLevel(),
                        "level " + level + " order is not contiguous");
                assertEquals(level, checkpoints.get(i).levelNumber());
            }
        }
    }

    @Test
    @DisplayName("lookup by id resolves, and unknown ids do not")
    void lookupWorks() {
        assertTrue(CheckpointRegistry.byId("l2_cp03_clinic").isPresent());
        assertEquals("Clinic Power Relay",
                CheckpointRegistry.byId("l2_cp03_clinic").orElseThrow().name());

        assertFalse(CheckpointRegistry.exists("does_not_exist"));
        assertFalse(CheckpointRegistry.byId(null).isPresent());
    }

    @Test
    @DisplayName("next() walks the campaign in order and stops at the end")
    void nextWalksTheCampaign() {
        assertEquals("l1_cp02_well", CheckpointRegistry.next("l1_cp01_gate").orElseThrow().id());
        // crosses a level boundary
        assertEquals("l2_cp01_checkpoint", CheckpointRegistry.next("l1_cp07_crossing").orElseThrow().id());
        assertTrue(CheckpointRegistry.next("l3_cp07_core").isEmpty(), "the last checkpoint has no successor");
    }

    @Test
    @DisplayName("campaign progress runs from first to last")
    void progressPercentage() {
        assertEquals(5, CheckpointRegistry.campaignProgressPct("l1_cp01_gate"));   // 1/22
        assertEquals(100, CheckpointRegistry.campaignProgressPct("l3_cp07_core")); // 22/22
        assertEquals(0, CheckpointRegistry.campaignProgressPct("unknown"));
    }

    @Test
    @DisplayName("each level has a defined starting checkpoint")
    void firstOfEachLevel() {
        assertEquals("l1_cp01_gate", CheckpointRegistry.firstOf(1).id());
        assertEquals("l2_cp01_checkpoint", CheckpointRegistry.firstOf(2).id());
        assertEquals("l3_cp01_descent", CheckpointRegistry.firstOf(3).id());
    }
}
