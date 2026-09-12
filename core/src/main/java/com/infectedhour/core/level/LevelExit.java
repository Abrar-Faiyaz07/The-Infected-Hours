package com.infectedhour.core.level;

import java.util.Optional;

/**
 * Authoritative interaction zone that ends a playable level. Coordinates are
 * tile centres, matching player and checkpoint positions throughout core.
 */
public record LevelExit(float tileX, float tileY, float activationRadius) {

    private static final LevelExit LEVEL_1_STAIRS = new LevelExit(42.0f, 30.5f, 1.75f);
    private static final LevelExit LEVEL_2_TUNNEL = new LevelExit(53.5f, 23.5f, 1.75f);

    public static Optional<LevelExit> forLevel(int levelNumber) {
        return switch (levelNumber) {
            case 1 -> Optional.of(LEVEL_1_STAIRS);
            case 2 -> Optional.of(LEVEL_2_TUNNEL);
            default -> Optional.empty();
        };
    }

    public boolean contains(float x, float y) {
        float dx = x - tileX;
        float dy = y - tileY;
        return dx * dx + dy * dy <= activationRadius * activationRadius;
    }
}
