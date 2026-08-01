package com.infectedhour.core.level;

import java.util.List;

/**
 * Static per-level data: which .tmx to load and which objectives to
 * register on ObjectiveSystem (PRD §7 table).
 */
public record LevelDefinition(
        int levelNumber,
        String name,
        String tmxPath,
        List<ObjectiveSpec> objectives,
        boolean isBossLevel
) {
    public record ObjectiveSpec(String id, String type, int target) {
    }

    public static LevelDefinition level1() {
        return new LevelDefinition(1, "Village Outskirts", "maps/level1_village_outskirts.tmx",
                List.of(
                        new ObjectiveSpec("l1_barricades", "ISOLATE_ZONE", 3),
                        new ObjectiveSpec("l1_samples", "COLLECT_SAMPLE", 2)
                ), false);
    }

    public static LevelDefinition level2() {
        return new LevelDefinition(2, "Market District", "maps/level2_market_district.tmx",
                List.of(
                        new ObjectiveSpec("l2_medicine", "DELIVER_MEDICINE", 3),
                        new ObjectiveSpec("l2_rescue", "RESCUE_VILLAGER", 4),
                        new ObjectiveSpec("l2_sanitation", "ACTIVATE_SANITATION", 2)
                ), false);
    }

    public static LevelDefinition level3Boss() {
        return new LevelDefinition(3, "The Virus Heart", "maps/level3_virus_heart.tmx",
                List.of(), true);
    }
}