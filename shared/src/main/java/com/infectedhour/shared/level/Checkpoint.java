package com.infectedhour.shared.level;

/**
 * A named point in a level where the game can be saved and resumed.
 *
 * <p>Lives in {@code shared} rather than {@code core} because three modules
 * need it and none of them should depend on libGDX to get it:
 * <ul>
 *   <li>{@code core} spawns the player here when a save is loaded;</li>
 *   <li>{@code fx-launcher} prints {@link #name()} on the Load Game slot card;</li>
 *   <li>{@code backend} validates that an incoming {@code checkpointId} is real
 *       before storing it, so a malformed save can never strand a player at a
 *       checkpoint that does not exist.</li>
 * </ul>
 *
 * @param id          stable key stored in {@code save_slot.checkpoint_id} — never
 *                    renamed once shipped, or existing saves break
 * @param name        player-facing label shown on the slot card
 * @param levelNumber 1..3
 * @param orderInLevel 1-based position within the level; higher means further in,
 *                    which is what lets progress be compared without a map
 * @param spawnTileX  where the player reappears on load, in tile coordinates
 * @param spawnTileY  as above
 */
public record Checkpoint(
        String id,
        String name,
        int levelNumber,
        int orderInLevel,
        float spawnTileX,
        float spawnTileY
) {

    /** "Level 2 — Clinic Sanitation Post", for slot cards and briefing text. */
    public String qualifiedName() {
        return "Level " + levelNumber + " — " + name;
    }
}
