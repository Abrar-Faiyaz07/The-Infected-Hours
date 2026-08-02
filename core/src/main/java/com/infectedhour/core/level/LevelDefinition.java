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
        return new LevelDefinition(1, "Village Outskirts",
                "maps/level1_village_outskirts.tmx", "maps/level1.map",
                List.of(
                        new ObjectiveSpec("l1_barricades", "ISOLATE_ZONE", 3),
                        new ObjectiveSpec("l1_samples", "COLLECT_SAMPLE", 2)
                ), false);
    }

    public static LevelDefinition level2() {
        return new LevelDefinition(2, "Market District",
                "maps/level2_market_district.tmx", "maps/level2.map",
                List.of(
                        new ObjectiveSpec("l2_medicine", "DELIVER_MEDICINE", 3),
                        new ObjectiveSpec("l2_rescue", "RESCUE_VILLAGER", 4),
                        new ObjectiveSpec("l2_sanitation", "ACTIVATE_SANITATION", 2)
                ), false);
    }

    public static LevelDefinition level3Boss() {
        return new LevelDefinition(3, "The Virus Heart",
                "maps/level3_virus_heart.tmx", "maps/level3.map",
                List.of(), true);
    }
}
