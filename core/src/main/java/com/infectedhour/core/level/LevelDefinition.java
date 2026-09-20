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
        return new LevelDefinition(1, "St. Mercy Hospital",
                "maps/level1_hospital.tmx", "maps/level1.map",
                List.of(), false);
    }

    public static LevelDefinition level2() {
        return new LevelDefinition(2, "St. Mercy Upper Wing",
                "maps/level2_roadside_village.tmx", "maps/level2.map",
                List.of(
                        new ObjectiveSpec("l2_infected", "CLEAR_INFECTED", 1),
                        new ObjectiveSpec("l2_rescue", "RESCUE_VILLAGER", 2),
                        new ObjectiveSpec("l2_puzzle", "SOLVE_PUZZLE", 2)
                ), false);
    }

    public static LevelDefinition level3() {
        return new LevelDefinition(3, "St. Mercy Hospital Grounds",
                "maps/level3_road.tmx", "maps/map2.map",
                List.of(), false);
    }

    /** Compatibility for legacy unit tests referencing level3Boss(). */
    public static LevelDefinition level3Boss() {
        return new LevelDefinition(3, "St. Mercy Hospital Grounds",
                "maps/level3_hidden_laboratory.tmx", "maps/map2.map",
                List.of(), true);
    }

    public static LevelDefinition level4() {
        return new LevelDefinition(4, "Abandoned Hospital District",
                "maps/level4_corridor.tmx", "maps/map2_part2.map",
                List.of(), false);
    }

    public static LevelDefinition level5() {
        return new LevelDefinition(5, "Ashgrove Research Perimeter",
                "maps/level5_facility.tmx", "maps/level3.map",
                List.of(), false);
    }

    /**
     * Teammate-owned final level. Their Level 6 map, boss, and gameplay work is
     * authoritative; preserve it when editing shared campaign code.
     */
    public static LevelDefinition level6Boss() {
        return new LevelDefinition(6, "Ashgrove Research Laboratory",
                "maps/level6_laboratory.tmx", "maps/level_final.map",
                List.of(), true);
    }
}
