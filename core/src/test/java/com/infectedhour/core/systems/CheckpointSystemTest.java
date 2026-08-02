package com.infectedhour.core.systems;

import com.infectedhour.core.entities.Player;
import com.infectedhour.shared.level.Checkpoint;
import com.infectedhour.shared.level.CheckpointRegistry;
import com.infectedhour.shared.network.CharacterType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CheckpointSystemTest {

    private static Player playerAt(float x, float y) {
        Player player = new Player("p1", CharacterType.ELRIC);
        player.setPosition(x, y);
        return player;
    }

    @Test
    @DisplayName("a new run starts at the level's first checkpoint")
    void startsAtFirstCheckpoint() {
        CheckpointSystem system = new CheckpointSystem(1);
        assertEquals("l1_cp01_gate", system.getLastReachedId());
        assertEquals(1, system.getCurrentLevel());
    }

    @Test
    @DisplayName("walking onto a checkpoint records it")
    void walkingOntoCheckpointRecordsIt() {
        CheckpointSystem system = new CheckpointSystem(1);
        Checkpoint well = CheckpointRegistry.byId("l1_cp02_well").orElseThrow();

        Optional<Checkpoint> reached = system.updateAndDetectNew(
                List.of(playerAt(well.spawnTileX(), well.spawnTileY())));

        assertTrue(reached.isPresent());
        assertEquals("l1_cp02_well", reached.orElseThrow().id());
        assertEquals("l1_cp02_well", system.getLastReachedId());
    }

    @Test
    @DisplayName("standing far from every checkpoint records nothing")
    void nothingRecordedWhenFarAway() {
        CheckpointSystem system = new CheckpointSystem(1);
        assertTrue(system.updateAndDetectNew(List.of(playerAt(100f, 100f))).isEmpty());
        assertEquals("l1_cp01_gate", system.getLastReachedId(), "the start marker must not move");
    }

    @Test
    @DisplayName("the same checkpoint only fires once")
    void reachingTwiceOnlyFiresOnce() {
        CheckpointSystem system = new CheckpointSystem(1);
        Checkpoint well = CheckpointRegistry.byId("l1_cp02_well").orElseThrow();
        List<Player> players = List.of(playerAt(well.spawnTileX(), well.spawnTileY()));

        assertTrue(system.updateAndDetectNew(players).isPresent());
        assertTrue(system.updateAndDetectNew(players).isEmpty(), "no repeat toast on the second tick");
    }

    @Test
    @DisplayName("backtracking does not rewind the save point")
    void backtrackingDoesNotRewindProgress() {
        CheckpointSystem system = new CheckpointSystem(1);
        Checkpoint farther = CheckpointRegistry.byId("l1_cp05_sample").orElseThrow();
        Checkpoint earlier = CheckpointRegistry.byId("l1_cp02_well").orElseThrow();

        system.updateAndDetectNew(List.of(playerAt(farther.spawnTileX(), farther.spawnTileY())));
        assertEquals("l1_cp05_sample", system.getLastReachedId());

        // Walk back to an earlier checkpoint — progress must hold.
        system.updateAndDetectNew(List.of(playerAt(earlier.spawnTileX(), earlier.spawnTileY())));
        assertEquals("l1_cp05_sample", system.getLastReachedId(),
                "walking backwards must not move the save point back");
    }

    @Test
    @DisplayName("restoring a save marks everything up to that point as reached")
    void restoreMarksPriorCheckpoints() {
        CheckpointSystem system = new CheckpointSystem(1);
        system.restoreTo("l2_cp05_shelter");

        assertEquals(2, system.getCurrentLevel());
        assertEquals("l2_cp05_shelter", system.getLastReachedId());
        assertTrue(system.hasReached("l2_cp01_checkpoint"), "earlier checkpoints count as reached");
        assertTrue(system.hasReached("l2_cp05_shelter"));
        assertFalse(system.hasReached("l2_cp06_depot"), "later ones must not");
    }

    @Test
    @DisplayName("the save prompt only appears within range")
    void promptOnlyInRange() {
        CheckpointSystem system = new CheckpointSystem(1);
        Checkpoint gate = CheckpointRegistry.byId("l1_cp01_gate").orElseThrow();

        assertTrue(system.checkpointInRange(playerAt(gate.spawnTileX(), gate.spawnTileY())).isPresent());
        assertTrue(system.checkpointInRange(playerAt(gate.spawnTileX() + 1f, gate.spawnTileY())).isPresent());
        assertTrue(system.checkpointInRange(playerAt(gate.spawnTileX() + 5f, gate.spawnTileY())).isEmpty(),
                "five tiles away is out of range");
    }

    @Test
    @DisplayName("progress percentage tracks the furthest checkpoint reached")
    void progressTracksFurthest() {
        CheckpointSystem system = new CheckpointSystem(1);
        assertEquals(CheckpointRegistry.campaignProgressPct("l1_cp01_gate"), system.campaignProgressPct());

        system.restoreTo("l3_cp07_core");
        assertEquals(100, system.campaignProgressPct());
    }
}
