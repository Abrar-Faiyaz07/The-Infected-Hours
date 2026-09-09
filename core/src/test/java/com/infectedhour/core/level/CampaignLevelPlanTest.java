package com.infectedhour.core.level;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CampaignLevelPlanTest {

    @Test
    void campaignUsesHospitalVillageAndHiddenLabOrder() {
        assertEquals("Ashgrove Hospital", LevelDefinition.level1().name());
        assertEquals("Roadside Village", LevelDefinition.level2().name());
        assertEquals("Hidden Laboratory", LevelDefinition.level3Boss().name());
        assertTrue(LevelDefinition.level3Boss().isBossLevel());
    }

    @Test
    void levelTwoFeatureCountsMatchObjectiveTargets() {
        LevelDefinition level = LevelDefinition.level2();
        Map<String, Long> featuresPerObjective = CampaignLevelPlan.featuresFor(2).stream()
                .collect(Collectors.groupingBy(CampaignLevelPlan.Feature::objectiveId, Collectors.counting()));

        for (LevelDefinition.ObjectiveSpec objective : level.objectives()) {
            assertEquals(objective.target(), featuresPerObjective.getOrDefault(objective.id(), 0L));
        }
    }

    @Test
    void levelTwoFeaturesSitOnWalkableTiles() {
        LevelLoader loader = new LevelLoader();
        TileMap map = loader.loadMap(LevelDefinition.level2());

        assertFalse(CampaignLevelPlan.featuresFor(2).isEmpty());
        for (CampaignLevelPlan.Feature feature : CampaignLevelPlan.featuresFor(2)) {
            assertTrue(map.isWalkable(TileMap.toTile(feature.tileX()), TileMap.toTile(feature.tileY())),
                    feature.actionId() + " must be reachable");
        }
    }

    @Test
    void otherLevelsHaveNoRoadsideFeatures() {
        assertTrue(CampaignLevelPlan.featuresFor(1).isEmpty());
        assertTrue(CampaignLevelPlan.featuresFor(3).isEmpty());
    }
}
