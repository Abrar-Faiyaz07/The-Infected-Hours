package com.infectedhour.core.level;

import java.util.List;
import java.util.Optional;

/**
 * Small, data-only campaign plan for interactable level landmarks.
 *
 * <p>The positions are tile centres so the same data can drive rendering,
 * interaction prompts and host-side validation.  This is intentionally a
 * skeleton: authored maps, NPC AI and encounter directors can replace the
 * visuals later without changing the campaign flow.</p>
 */
public final class CampaignLevelPlan {

    public enum FeatureType {
        ZOMBIE_ENCOUNTER,
        SURVIVOR,
        POWER_RELAY
    }

    public record Feature(
            String actionId,
            String objectiveId,
            FeatureType type,
            String label,
            float tileX,
            float tileY,
            float activationRadius
    ) {
        public boolean contains(float x, float y) {
            float dx = x - tileX;
            float dy = y - tileY;
            return dx * dx + dy * dy <= activationRadius * activationRadius;
        }
    }

    private static final List<Feature> LEVEL_2_FEATURES = List.of(
            new Feature("road_patrol", "l2_infected", FeatureType.ZOMBIE_ENCOUNTER,
                    "Clear the infected road patrol", 22.5f, 34.5f, 6.0f),
            new Feature("survivor_market", "l2_rescue", FeatureType.SURVIVOR,
                    "Escort the market survivor", 7.5f, 21.5f, 1.6f),
            new Feature("survivor_shelter", "l2_rescue", FeatureType.SURVIVOR,
                    "Release the shelter survivor", 31.5f, 27.5f, 1.6f),
            new Feature("relay_clinic", "l2_puzzle", FeatureType.POWER_RELAY,
                    "Route power through the clinic relay", 14.5f, 4.5f, 1.6f),
            new Feature("relay_depot", "l2_puzzle", FeatureType.POWER_RELAY,
                    "Route power through the depot relay", 39.5f, 21.5f, 1.6f)
    );

    private CampaignLevelPlan() {
    }

    public static List<Feature> featuresFor(int levelNumber) {
        return levelNumber == 2 ? LEVEL_2_FEATURES : List.of();
    }

    public static Optional<Feature> findFeature(int levelNumber, String actionId) {
        if (actionId == null) return Optional.empty();
        return featuresFor(levelNumber).stream()
                .filter(feature -> feature.actionId().equals(actionId))
                .findFirst();
    }
}
