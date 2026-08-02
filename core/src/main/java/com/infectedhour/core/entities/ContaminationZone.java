package com.infectedhour.core.entities;

import java.util.HashSet;
import java.util.Set;

/**
 * A dynamic toxic cloud. Owns a growing set of tile indices (BFS frontier
 * expansion per TRD §4) rather than a shape — cheap to diff into
 * WorldSnapshot.CloudFrontierDelta for network sync.
 */
public class ContaminationZone {

    private final String cloudId;
    private final Set<Integer> occupiedTileIndices = new HashSet<>();
    private float expansionTimerSeconds;
    private final float expansionIntervalSeconds;

    public ContaminationZone(String cloudId, int originTileIndex, float expansionIntervalSeconds) {
        this.cloudId = cloudId;
        this.occupiedTileIndices.add(originTileIndex);
        this.expansionIntervalSeconds = expansionIntervalSeconds;
        this.expansionTimerSeconds = expansionIntervalSeconds;
    }

    public String getCloudId() {
        return cloudId;
    }

    public Set<Integer> getOccupiedTileIndices() {
        return occupiedTileIndices;
    }

    /**
     * Advances the expansion timer; when it elapses, the caller (ContaminationSystem)
     * should compute the BFS frontier and add new tiles via {@link #addTiles}.
     * Kept dumb here on purpose — this class has no map/pathing knowledge.
     */
    public boolean tickAndCheckShouldExpand(float delta) {
        expansionTimerSeconds -= delta;
        if (expansionTimerSeconds <= 0) {
            expansionTimerSeconds = expansionIntervalSeconds;
            return true;
        }
        return false;
    }

    public void addTiles(Set<Integer> newTiles) {
        occupiedTileIndices.addAll(newTiles);
    }

    public boolean contains(int tileIndex) {
        return occupiedTileIndices.contains(tileIndex);
    }
}
