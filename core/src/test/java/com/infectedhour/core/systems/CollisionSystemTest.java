package com.infectedhour.core.systems;

import com.infectedhour.core.entities.Enemy;
import com.infectedhour.core.entities.Player;
import com.infectedhour.core.level.TileMap;
import com.infectedhour.shared.constants.GameConstants;
import com.infectedhour.shared.network.CharacterType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CollisionSystemTest {

    private static final float TOLERANCE = 1e-4f;

    /** A 7x7 room: solid border, one solid pillar at tile (3, 3). */
    private static TileMap room() {
        return TileMap.fromRows(List.of(
                "#######",
                "#.....#",
                "#.....#",
                "#..#..#",
                "#.....#",
                "#.....#",
                "#######"));
    }

    private static Player playerAt(float x, float y) {
        Player player = new Player("p1", CharacterType.JANE);
        player.setPosition(x, y);
        return player;
    }

    private static Enemy enemyAt(float x, float y) {
        Enemy enemy = new Enemy("e1", "basic_infected", 30f);
        enemy.setPosition(x, y);
        return enemy;
    }

    @Test
    void anEntityStopsAtAWallInsteadOfPassingThrough() {
        CollisionSystem collision = new CollisionSystem(room());
        Player player = playerAt(1.5f, 1.5f);

        // Walk hard left into the border wall at x = 0.
        for (int i = 0; i < 60; i++) {
            collision.moveWithCollision(player, -0.1f, 0f);
        }

        assertTrue(player.getX() >= 1f - TOLERANCE,
                "collider radius should hold the player out of the x=0 wall, was " + player.getX());
        assertFalse(collision.overlapsBlockedTile(player.getX(), player.getY(), player.getCollisionRadius()),
                "a stopped player must never end up inside geometry");
    }

    @Test
    void movingDiagonallyIntoAWallSlidesAlongItRatherThanStopping() {
        // This is why the two axes are resolved separately: without it, brushing a
        // wall while moving diagonally would kill ALL movement and feel broken.
        CollisionSystem collision = new CollisionSystem(room());
        Player player = playerAt(1.5f, 3.0f);
        float startY = player.getY();

        collision.moveWithCollision(player, -0.5f, 0.2f);

        assertTrue(player.getX() >= 1f - TOLERANCE, "the blocked horizontal component should be dropped");
        assertTrue(player.getY() > startY, "the free vertical component should still apply (wall sliding)");
    }

    @Test
    void aFastMoveCannotTunnelThroughAThinWall() {
        // Jane's dash [STRETCH] would exceed a tile per tick; substepping is what
        // stops a single large displacement from skipping straight over a wall.
        CollisionSystem collision = new CollisionSystem(room());
        Player player = playerAt(3.5f, 1.5f);

        collision.moveWithCollision(player, 0f, 5f); // far past the pillar at (3,3) and the far wall

        assertTrue(player.getY() < 3f,
                "the pillar should have stopped the dash, but the player reached y=" + player.getY());
    }

    @Test
    void outOfBoundsCountsAsBlocked() {
        CollisionSystem collision = new CollisionSystem(TileMap.allWalkable(4, 4));

        assertTrue(collision.overlapsBlockedTile(-0.1f, 2f, 0.35f), "past the left edge is outside the world");
        assertTrue(collision.overlapsBlockedTile(4.2f, 2f, 0.35f), "past the right edge is outside the world");
        assertFalse(collision.overlapsBlockedTile(2f, 2f, 0.35f), "the middle of an open map is free");
    }

    @Test
    void restingFlushAgainstAWallDoesNotCountAsOverlapping() {
        // Touching must not read as overlapping, or a player parked against a
        // wall would look permanently stuck to every subsequent check.
        CollisionSystem collision = new CollisionSystem(room());
        float radius = GameConstants.PLAYER_COLLISION_RADIUS;

        assertFalse(collision.overlapsBlockedTile(1f + radius, 2.5f, radius),
                "exact contact with the x=1 wall face is contact, not penetration");
    }

    @Test
    void overlappingEntitiesArePushedApart() {
        CollisionSystem collision = new CollisionSystem(room());
        Player player = playerAt(2.5f, 2.5f);
        Enemy enemy = enemyAt(2.6f, 2.5f);

        assertTrue(collision.separate(player, enemy), "these two clearly overlap");

        float dx = enemy.getX() - player.getX();
        float dy = enemy.getY() - player.getY();
        float distance = (float) Math.sqrt(dx * dx + dy * dy);
        float minDistance = player.getCollisionRadius() + enemy.getCollisionRadius();

        assertTrue(distance >= minDistance - TOLERANCE,
                "after separation they should be at least " + minDistance + " apart, was " + distance);
    }

    @Test
    void entitiesThatAreNotTouchingAreLeftAlone() {
        CollisionSystem collision = new CollisionSystem(room());
        Player player = playerAt(1.5f, 1.5f);
        Enemy enemy = enemyAt(4.5f, 4.5f);

        assertFalse(collision.separate(player, enemy), "distant entities must not be nudged");
        assertEquals(1.5f, player.getX(), TOLERANCE);
        assertEquals(4.5f, enemy.getX(), TOLERANCE);
    }

    @Test
    void exactlyStackedEntitiesSeparateDeterministically() {
        // The host is authoritative and every client renders its output, so an
        // arbitrary tie-break must still be the SAME arbitrary tie-break each run.
        float firstRunX = separateStackedPairAndReturnPlayerX();
        float secondRunX = separateStackedPairAndReturnPlayerX();

        assertEquals(firstRunX, secondRunX, TOLERANCE, "identical inputs must produce identical output");
    }

    private float separateStackedPairAndReturnPlayerX() {
        CollisionSystem collision = new CollisionSystem(room());
        Player player = playerAt(2.5f, 2.5f);
        Enemy enemy = enemyAt(2.5f, 2.5f);
        collision.separate(player, enemy);
        return player.getX();
    }

    @Test
    void separationNeverPushesAnEntityIntoAWall() {
        // Two entities squeezed against a wall must not be resolved by shoving
        // one of them through it — separation routes through the tile check.
        CollisionSystem collision = new CollisionSystem(room());
        Player player = playerAt(1.4f, 2.5f);
        Enemy enemy = enemyAt(1.5f, 2.5f);

        collision.separateAll(List.of(player, enemy));

        assertFalse(collision.overlapsBlockedTile(player.getX(), player.getY(), player.getCollisionRadius()),
                "player was pushed into geometry at x=" + player.getX());
        assertFalse(collision.overlapsBlockedTile(enemy.getX(), enemy.getY(), enemy.getCollisionRadius()),
                "enemy was pushed into geometry at x=" + enemy.getX());
    }

    @Test
    void separateAllResolvesEveryPairInTheList() {
        CollisionSystem collision = new CollisionSystem(TileMap.allWalkable(20, 20));
        Player player = playerAt(10f, 10f);
        Enemy first = enemyAt(10.05f, 10f);
        Enemy second = enemyAt(10.1f, 10f);

        collision.separateAll(List.of(player, first, second));

        assertFalse(collision.separate(player, first), "player/first should already be resolved");
        assertFalse(collision.separate(first, second), "first/second should already be resolved");
    }

    @Test
    void malformedDisplacementsAreIgnoredRatherThanCorruptingPosition() {
        CollisionSystem collision = new CollisionSystem(room());
        Player player = playerAt(2.5f, 2.5f);

        collision.moveWithCollision(player, Float.NaN, 0f);
        collision.moveWithCollision(player, 0f, Float.POSITIVE_INFINITY);

        assertEquals(2.5f, player.getX(), TOLERANCE, "NaN input must not move the player");
        assertEquals(2.5f, player.getY(), TOLERANCE, "Infinite input must not move the player");
    }

    @Test
    void withNoTileMapOnlyEntitySeparationApplies() {
        // GameServer runs on an open field until a level pushes its grid in.
        CollisionSystem collision = new CollisionSystem();
        Player player = playerAt(0f, 0f);

        collision.moveWithCollision(player, -50f, -50f);

        // Loose tolerance on purpose: a 70-tile displacement is split into ~283
        // substeps, and summing that many floats drifts slightly. Normal movement
        // (~0.107 tiles/tick) never substeps, so this drift is not a gameplay concern.
        assertEquals(-50f, player.getX(), 0.01f, "no grid means no tile blocking");
    }
}
