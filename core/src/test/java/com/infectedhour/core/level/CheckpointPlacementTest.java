package com.infectedhour.core.level;

import com.infectedhour.shared.constants.GameConstants;
import com.infectedhour.shared.level.Checkpoint;
import com.infectedhour.shared.level.CheckpointRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Every checkpoint and spawn point must sit somewhere a player can stand.
 *
 * <p>This exists because the first version of the registry was written against
 * an <b>assumed</b> 60x40 grid while level 1 is really 45x33. Two checkpoints
 * ended up outside the map and two inside walls, and the spawn point itself was
 * inside a wall. Nothing crashed and no test failed — the player simply could
 * not move, and checkpoints silently never fired. That class of bug is worth a
 * test precisely because it produces no error message.
 *
 * <p>It lives in {@code core} rather than {@code shared} because the collision
 * grids are core resources; {@code shared} deliberately cannot see them.
 */
class CheckpointPlacementTest {

    private static final float RADIUS = GameConstants.PLAYER_COLLISION_RADIUS;

    @Test
    @DisplayName("every checkpoint is inside its map and not inside a wall")
    void everyCheckpointStandsOnWalkableGround() {
        LevelLoader loader = new LevelLoader();
        List<String> failures = new ArrayList<>();

        for (int level = 1; level <= GameConstants.LEVEL_COUNT; level++) {
            TileMap map = loader.loadTileMap("maps/level" + level + ".map");

            for (Checkpoint checkpoint : CheckpointRegistry.forLevel(level)) {
                float x = checkpoint.spawnTileX();
                float y = checkpoint.spawnTileY();

                if (x < 0 || y < 0 || x >= map.getWidth() || y >= map.getHeight()) {
                    failures.add(String.format("%s at (%.1f, %.1f) is outside the %dx%d map",
                            checkpoint.id(), x, y, map.getWidth(), map.getHeight()));
                    continue;
                }
                if (!isClear(map, x, y)) {
                    failures.add(String.format("%s at (%.1f, %.1f) is inside a wall",
                            checkpoint.id(), x, y));
                }
            }
        }

        assertTrue(failures.isEmpty(),
                "Checkpoints a player cannot stand on:\n  " + String.join("\n  ", failures));
    }

    @Test
    @DisplayName("the level 1 spawn points are walkable")
    void spawnPointsAreWalkable() {
        TileMap map = new LevelLoader().loadTileMap("maps/level1.map");

        // Mirrors GameServer.spawnX/spawnY. A blocked spawn is worse than a
        // blocked checkpoint: the player starts embedded in geometry and cannot
        // move at all, which reads as "the controls are broken".
        assertTrue(isClear(map, 9.5f, 8.5f), "Elric's spawn is inside a wall");
        assertTrue(isClear(map, 10.5f, 8.5f), "Jane's spawn is inside a wall");
    }

    /** Samples every collision cell the player's circle would cover. */
    private static boolean isClear(TileMap map, float worldX, float worldY) {
        int minCellX = map.toCell(worldX - RADIUS);
        int maxCellX = map.toCell(worldX + RADIUS);
        int minCellY = map.toCell(worldY - RADIUS);
        int maxCellY = map.toCell(worldY + RADIUS);

        for (int cy = minCellY; cy <= maxCellY; cy++) {
            for (int cx = minCellX; cx <= maxCellX; cx++) {
                if (!map.isCellWalkable(cx, cy)) {
                    return false;
                }
            }
        }
        return true;
    }
}
