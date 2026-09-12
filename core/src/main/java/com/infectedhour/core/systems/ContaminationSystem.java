package com.infectedhour.core.systems;

import com.infectedhour.core.entities.ContaminationZone;
import com.infectedhour.core.entities.Player;
import com.infectedhour.shared.constants.GameConstants;

import java.util.HashSet;
import java.util.Set;

/**
 * Owns BOTH doom clocks from PRD §10:
 *  - the level's global contamination meter (rises over time / cloud expansion,
 *    lowered by barricade/sanitation objectives — fail at 100%)
 *  - each player's personal contamination bar (rises in toxic zones, damages
 *    HP continuously at 100% until cleansed at a sanitation point)
 *
 * Kept free of any Gdx graphics dependency so it stays unit-testable
 * headless per TRD §10.
 */
public class ContaminationSystem {

    private float globalContaminationPct = 0f;
    private static final float PASSIVE_GLOBAL_RISE_PER_SEC = 0.15f;
    private static final float PERSONAL_CONTAMINATION_RISE_PER_SEC_IN_ZONE = 8f;
    private static final float PERSONAL_CONTAMINATION_HP_DRAIN_PER_SEC = 5f;
    private static final float ELRIC_CONTAMINATION_RESISTANCE = 0.30f; // PRD §4 passive

    public float getGlobalContaminationPct() {
        return globalContaminationPct;
    }

    public boolean isGlobalContaminationLethal() {
        return globalContaminationPct >= GameConstants.GLOBAL_CONTAMINATION_MAX;
    }

    public void tickGlobal(float delta) {
        globalContaminationPct = Math.min(
                GameConstants.GLOBAL_CONTAMINATION_MAX,
                globalContaminationPct + PASSIVE_GLOBAL_RISE_PER_SEC * delta);
    }

    /** Barricade/sanitation objectives call this to push the doom clock back (PRD §6). */
    public void reduceGlobal(float amount) {
        globalContaminationPct = Math.max(0f, globalContaminationPct - amount);
    }

    /**
     * Set the meter outright when loading a save. Distinct from
     * {@link #reduceGlobal} on purpose: that one is a gameplay reward and is
     * relative, this one restores a value the host already simulated.
     */
    public void setGlobalContaminationPct(float pct) {
        this.globalContaminationPct = Math.max(0f,
                Math.min(GameConstants.GLOBAL_CONTAMINATION_MAX, pct));
    }

    /** Call each tick for a player standing inside a ContaminationZone. */
    public void tickPlayerInZone(Player player, float delta) {
        float resistance = player.getCharacter() == com.infectedhour.shared.network.CharacterType.ELRIC
                ? ELRIC_CONTAMINATION_RESISTANCE
                : 0f;
        float rise = PERSONAL_CONTAMINATION_RISE_PER_SEC_IN_ZONE * (1 - resistance) * delta;
        player.addContamination(rise);

        if (player.getPersonalContaminationPct() >= GameConstants.PERSONAL_CONTAMINATION_MAX) {
            player.applyDamage(PERSONAL_CONTAMINATION_HP_DRAIN_PER_SEC * delta);
        }
    }

    /** Sanitation station hold-interact completion (PRD §6) cleanses the player. */
    public void cleanseAtSanitationStation(Player player) {
        player.cleanseContamination();
    }

    /**
     * BFS frontier expansion for a cloud (TRD §4). Skeleton signature only —
     * real implementation needs the Tiled map's walkable-tile graph, which
     * belongs in level/LevelLoader.
     */
    public Set<Integer> computeFrontierExpansion(ContaminationZone zone, int mapWidthInTiles) {
        // ================ TEAMMATE TASK: CLOUD EXPANSION (BFS) ================
        // TODO(contamination): implement 1-step BFS frontier growth (TRD par.4).
        //  1. For every tile index T in zone.getOccupiedTileIndices():
        //       neighbors = { T-1, T+1, T-mapWidthInTiles, T+mapWidthInTiles }
        //       (skip left/right neighbors that wrap across a row edge!)
        //  2. Keep a neighbor only if: walkable (ask LevelLoader's grid),
        //     not already inside the zone, and within the map bounds.
        //  3. Return that set — GameServer adds it via zone.addTiles(...) and
        //     ships it to the client as WorldSnapshot.CloudFrontierDelta.
        //  4. Optional pacing: expand only a random ~30% subset per step so
        //     growth looks organic instead of a perfect diamond.
        // ======================================================================
        return new HashSet<>();
    }
}
