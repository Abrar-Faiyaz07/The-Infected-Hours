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
            TileMap map = loader.loadMap(loader.loadDefinition(level));

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
        assertTrue(isClear(map, 40.0f, 8.0f), "Jane's spawn is inside a wall");
        assertTrue(isClear(map, 12.0f, 8.5f), "Bomb spawn is inside a wall");
    }

    @Test
    @DisplayName("the complete Level 3 squad formation is walkable")
    void levelThreeSquadFormationIsWalkable() {
        TileMap map = new LevelLoader().loadTileMap("maps/map2.map");

        assertTrue(isClear(map, 9.5f, 4.5f), "Elric's Level 3 spawn is blocked");
        assertTrue(isClear(map, 10.5f, 4.5f), "Jane's Level 3 spawn is blocked");
        assertTrue(isClear(map, 8.5f, 4.5f), "villager slot 1 is blocked");
        assertTrue(isClear(map, 11.5f, 4.5f), "villager slot 2 is blocked");
        assertTrue(isClear(map, 8.5f, 3.5f), "villager slot 3 is blocked");
        assertTrue(isClear(map, 10.5f, 3.5f), "villager slot 4 is blocked");
    }

    @Test
    @DisplayName("both Level 4 player spawns are walkable")
    void levelFourPlayerSpawnsAreWalkable() {
        TileMap map = new LevelLoader().loadTileMap("maps/map2_part2.map");
        Checkpoint start = CheckpointRegistry.firstOf(4);

        assertTrue(isClear(map, start.spawnTileX(), start.spawnTileY()),
                "Elric's Level 4 spawn is blocked");
        assertTrue(isClear(map, start.spawnTileX() + 1.0f, start.spawnTileY()),
                "Jane's Level 4 spawn is blocked");
    }

    @Test
    @DisplayName("the Level 5 squad and zombies start on reachable open ground")
    void levelFiveSquadAndEnemySpawnsAreWalkable() {
        TileMap map = new LevelLoader().loadTileMap("maps/level3.map");
        Checkpoint start = CheckpointRegistry.firstOf(5);

        assertTrue(isClear(map, start.spawnTileX(), start.spawnTileY()), "Elric's Level 5 spawn is blocked");
        assertTrue(isClear(map, start.spawnTileX() + 1.0f, start.spawnTileY()), "Jane's Level 5 spawn is blocked");
        assertTrue(isClear(map, start.spawnTileX() - 1.0f, start.spawnTileY()), "NPC slot 1 is blocked");
        assertTrue(isClear(map, start.spawnTileX() - 2.0f, start.spawnTileY()), "NPC slot 2 is blocked");
        assertTrue(isClear(map, start.spawnTileX() - 1.0f, start.spawnTileY() - 1.0f), "NPC slot 3 is blocked");
        assertTrue(isClear(map, start.spawnTileX() + 1.0f, start.spawnTileY() - 1.0f), "NPC slot 4 is blocked");
        assertTrue(isClear(map, 30.5f, 20.5f), "big red zombie is blocked");
        assertTrue(isClear(map, 28.5f, 18.5f), "regular zombie 1 is blocked");
        assertTrue(isClear(map, 35.5f, 22.5f), "regular zombie 2 is blocked");
        assertTrue(isClear(map, 31.77f, 27.08f), "Level 5 endpoint is blocked");
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
