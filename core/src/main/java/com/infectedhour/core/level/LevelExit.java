package com.infectedhour.core.level;

import java.util.Optional;

/**
 * Authoritative interaction zone that ends a playable level. Coordinates are
 * tile centres, matching player and checkpoint positions throughout core.
 */
public record LevelExit(float tileX, float tileY, float activationRadius) {

    private static final LevelExit LEVEL_1_STAIRS = new LevelExit(42.0f, 30.0f, 1.75f);
    private static final LevelExit LEVEL_2_TUNNEL = new LevelExit(26.75f, 1.3f, 2.0f);
    private static final LevelExit LEVEL_3_AMBULANCE = new LevelExit(20.0f, 26.0f, 2.0f);
    private static final LevelExit LEVEL_4_DOOR = new LevelExit(25.31f, 4.48f, 2.0f);
    private static final LevelExit LEVEL_5_DOOR = new LevelExit(31.77f, 27.08f, 2.0f);

    public static Optional<LevelExit> forLevel(int levelNumber) {
        return switch (levelNumber) {
            case 1 -> Optional.of(LEVEL_1_STAIRS);
            case 2 -> Optional.of(LEVEL_2_TUNNEL);
            case 3 -> Optional.of(LEVEL_3_AMBULANCE);
            case 4 -> Optional.of(LEVEL_4_DOOR);
            case 5 -> Optional.of(LEVEL_5_DOOR);
            default -> Optional.empty();
        };
    }

    public boolean contains(float x, float y) {
        float dx = x - tileX;
        float dy = y - tileY;
        return dx * dx + dy * dy <= activationRadius * activationRadius;
    }
}
