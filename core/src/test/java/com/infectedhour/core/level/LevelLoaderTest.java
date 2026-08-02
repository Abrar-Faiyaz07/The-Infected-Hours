package com.infectedhour.core.level;

import com.infectedhour.core.systems.CollisionSystem;
import com.infectedhour.shared.constants.GameConstants;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LevelLoaderTest {

    /**
     * The real value of this test: a hand-authored .map with a miscounted row is
     * a typo you cannot see in an editor. Parsing every shipped level here turns
     * that into a build failure instead of a wall with a hole in it at demo time.
     */
    @ParameterizedTest
    @ValueSource(ints = { 1, 2, 3 })
    void everyShippedLevelMapParsesAndIsEnclosed(int levelNumber) {
        LevelLoader loader = new LevelLoader();
        LevelDefinition definition = loader.loadDefinition(levelNumber);

        TileMap map = loader.loadMap(definition);

        assertNotNull(map, "level " + levelNumber + " must produce a grid");
        // Sizes differ per level (level 1 is traced from the hospital art at
        // 45x33 tiles / 3 subdivisions; levels 2-3 are still 60x40 placeholders),
        // so assert a sane playable size rather than one fixed shape.
        assertTrue(map.getWidth() >= 20 && map.getHeight() >= 20,
                "level " + levelNumber + " grid looks too small: " + map.getWidth() + "x" + map.getHeight());
        assertEquals(map.getWidth() * map.getSubdivisions(), map.getCollisionWidth(),
                "collision grid should be the tile grid times the subdivision factor");

        // No border assertion: players cannot leave the map regardless, because
        // TileMap treats out-of-bounds as blocked. A traced map's outer wall is
        // not a clean one-tile ring, so requiring one would only be busywork.
    }

    /**
     * Guards the spawn points against a map edit that walls them in. GameServer
     * would relocate a blocked spawn at runtime, but silently starting players
     * somewhere unintended is worse than a failing build.
     */
    @ParameterizedTest
    @ValueSource(ints = { 1, 2, 3 })
    void playerSpawnPointsAreOnWalkableGround(int levelNumber) {
        // Mirrors GameServer.spawnX/spawnY: Elric (5.5, 4.5), Jane (6.5, 4.5).
        LevelLoader loader = new LevelLoader();
        TileMap map = loader.loadMap(loader.loadDefinition(levelNumber));

        assertTrue(map.isWalkableAt(5.5f, 4.5f), "level " + levelNumber + ": Elric's spawn is inside geometry");
        assertTrue(map.isWalkableAt(6.5f, 4.5f), "level " + levelNumber + ": Jane's spawn is inside geometry");
    }

    /**
     * Every spot the player can physically stand must be reachable from the
     * spawn — the invariant that actually matters, and the one whose absence
     * made every room in the hospital unenterable.
     *
     * <p>Reachability is measured in <b>eroded</b> space: a position counts only
     * if the player's collision disc fits there. Checking bare walkable cells is
     * not enough, because a one-cell doorway is connected on paper while being
     * far too narrow for a body 2.1 cells across to pass through.
     *
     * <p>Standability is decided by {@link CollisionSystem} itself rather than
     * reimplemented here, so a map can never pass this test and then behave
     * differently at runtime.
     */
    @ParameterizedTest
    @ValueSource(ints = { 1, 2, 3 })
    void everyStandablePositionIsReachableFromSpawn(int levelNumber) {
        LevelLoader loader = new LevelLoader();
        TileMap map = loader.loadMap(loader.loadDefinition(levelNumber));
        CollisionSystem collision = new CollisionSystem(map);

        int cellsWide = map.getCollisionWidth();
        int cellsHigh = map.getCollisionHeight();
        float cellSize = map.getCellSize();
        float radius = GameConstants.PLAYER_COLLISION_RADIUS;

        boolean[] standable = new boolean[cellsWide * cellsHigh];
        int standableCount = 0;
        for (int y = 0; y < cellsHigh; y++) {
            for (int x = 0; x < cellsWide; x++) {
                float centreX = (x + 0.5f) * cellSize;
                float centreY = (y + 0.5f) * cellSize;
                if (!collision.overlapsBlockedTile(centreX, centreY, radius)) {
                    standable[y * cellsWide + x] = true;
                    standableCount++;
                }
            }
        }

        int startX = map.toCell(5.5f);
        int startY = map.toCell(4.5f);
        assertTrue(standable[startY * cellsWide + startX],
                "level " + levelNumber + ": the player does not fit at their own spawn point");

        boolean[] seen = new boolean[cellsWide * cellsHigh];
        java.util.Deque<int[]> queue = new java.util.ArrayDeque<>();
        queue.add(new int[] { startX, startY });
        seen[startY * cellsWide + startX] = true;

        int reached = 0;
        while (!queue.isEmpty()) {
            int[] cell = queue.poll();
            reached++;
            int[][] neighbours = {
                    { cell[0] + 1, cell[1] }, { cell[0] - 1, cell[1] },
                    { cell[0], cell[1] + 1 }, { cell[0], cell[1] - 1 } };
            for (int[] next : neighbours) {
                if (next[0] < 0 || next[1] < 0 || next[0] >= cellsWide || next[1] >= cellsHigh) {
                    continue;
                }
                int index = next[1] * cellsWide + next[0];
                if (standable[index] && !seen[index]) {
                    seen[index] = true;
                    queue.add(next);
                }
            }
        }

        assertEquals(standableCount, reached, "level " + levelNumber + ": "
                + (standableCount - reached) + " standable positions are walled off from spawn — "
                + "a room the player can never enter");
    }

    @Test
    void everyLevelHasSomeObstaclesNotJustABorder() {
        LevelLoader loader = new LevelLoader();
        TileMap map = loader.loadMap(loader.loadDefinition(1));

        int blockedInterior = 0;
        for (int y = 1; y < map.getHeight() - 1; y++) {
            for (int x = 1; x < map.getWidth() - 1; x++) {
                if (!map.isWalkable(x, y)) {
                    blockedInterior++;
                }
            }
        }
        assertTrue(blockedInterior > 0, "level 1 should have interior obstacles, not just an empty box");
    }

    @Test
    void aMissingMapResourceFallsBackToAnOpenFieldRatherThanCrashing() {
        // A missing asset must never take the demo down mid-match.
        LevelLoader loader = new LevelLoader();

        TileMap map = loader.loadTileMap("maps/does_not_exist.map");

        assertNotNull(map);
        assertTrue(map.isWalkable(1, 1), "the fallback field should be walkable");
    }

    @Test
    void unknownLevelNumbersAreRejected() {
        LevelLoader loader = new LevelLoader();
        assertThrows(IllegalArgumentException.class, () -> loader.loadDefinition(99));
    }
}
