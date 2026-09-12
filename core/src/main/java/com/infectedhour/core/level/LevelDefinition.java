package com.infectedhour.core.level;

import java.util.List;

/**
 * Static per-level data: which map to load and which objectives to
 * register on ObjectiveSystem (PRD §7 table).
 *
 * @param tmxPath          the Tiled map for rendering (TRD §4). Not yet authored —
 *                         see {@link LevelLoader} for why collision does not read it.
 * @param collisionMapPath classpath resource holding the walkability grid the host
 *                         simulation collides against; parsed by {@link TileMap#fromRows}.
 */
public record LevelDefinition(
        int levelNumber,
        String name,
        String tmxPath,
        String collisionMapPath,
        List<ObjectiveSpec> objectives,
        boolean isBossLevel
) {
    public record ObjectiveSpec(String id, String type, int target) {
    }

    public static LevelDefinition level1() {
        return new LevelDefinition(1, "Ashgrove Hospital",
                "maps/level1_hospital.tmx", "maps/level1.map",
                List.of(), false);
    }

    public static LevelDefinition level2() {
        return new LevelDefinition(2, "Roadside Village",
                "maps/level2_roadside_village.tmx", "maps/level2.map",
                List.of(
                        new ObjectiveSpec("l2_infected", "CLEAR_INFECTED", 1),
                        new ObjectiveSpec("l2_rescue", "RESCUE_VILLAGER", 2),
                        new ObjectiveSpec("l2_puzzle", "SOLVE_PUZZLE", 2)
                ), false);
    }

    public static LevelDefinition level3Boss() {
        return new LevelDefinition(3, "Hidden Laboratory",
                "maps/level3_hidden_laboratory.tmx", "maps/level3.map",
                List.of(), true);
    }
}
