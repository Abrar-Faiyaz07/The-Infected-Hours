package com.infectedhour.core.systems;

import com.infectedhour.core.entities.Player;
import com.infectedhour.core.level.TileMap;
import com.infectedhour.shared.constants.GameConstants;
import com.infectedhour.shared.network.CharacterType;
import com.infectedhour.shared.network.InputCommand;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MovementSystemTest {

    private static final float TOLERANCE = 1e-4f;
    private static final float ONE_TICK = 1f / GameConstants.SIMULATION_TICK_HZ;

    private static MovementSystem openFieldMovement() {
        return new MovementSystem(new CollisionSystem(TileMap.allWalkable(60, 40)));
    }

    private static Player playerAt(float x, float y) {
        Player player = new Player("p1", CharacterType.JANE);
        player.setPosition(x, y);
        return player;
    }

    private static InputCommand input(float moveX, float moveY) {
        InputCommand command = new InputCommand();
        command.moveX = moveX;
        command.moveY = moveY;
        return command;
    }

    private static float distanceMoved(Player player, float fromX, float fromY) {
        float dx = player.getX() - fromX;
        float dy = player.getY() - fromY;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    @Test
    void diagonalMovementIsNotFasterThanOrthogonalMovement() {
        // The bug this fixes: un-normalised diagonal input moved ~41% faster.
        MovementSystem movement = openFieldMovement();

        Player straight = playerAt(10f, 10f);
        Player diagonal = playerAt(10f, 10f);

        movement.apply(straight, input(1f, 0f), ONE_TICK);
        movement.apply(diagonal, input(1f, 1f), ONE_TICK);

        assertEquals(distanceMoved(straight, 10f, 10f), distanceMoved(diagonal, 10f, 10f), TOLERANCE,
                "diagonal and orthogonal input must cover the same distance per tick");
    }

    @Test
    void walkSpeedMatchesTheDocumentedTilesPerSecond() {
        MovementSystem movement = openFieldMovement();
        Player player = playerAt(10f, 10f);

        for (int tick = 0; tick < GameConstants.SIMULATION_TICK_HZ; tick++) {
            movement.apply(player, input(1f, 0f), ONE_TICK);
        }

        assertEquals(GameConstants.PLAYER_WALK_SPEED, player.getX() - 10f, 0.01f,
                "one second of input should cover PLAYER_WALK_SPEED tiles");
    }

    @Test
    void sprintingAppliesTheSprintMultiplier() {
        MovementSystem movement = openFieldMovement();
        Player walker = playerAt(10f, 10f);
        Player sprinter = playerAt(10f, 10f);

        InputCommand sprint = input(1f, 0f);
        sprint.abilityPressed = true;

        movement.apply(walker, input(1f, 0f), ONE_TICK);
        movement.apply(sprinter, sprint, ONE_TICK);

        float walked = walker.getX() - 10f;
        float sprinted = sprinter.getX() - 10f;
        assertEquals(GameConstants.PLAYER_SPRINT_MULTIPLIER, sprinted / walked, TOLERANCE);
    }

    @Test
    void partialInputStillProducesProportionallySlowerMovement() {
        // Only vectors LONGER than unit length are normalised, so analog input survives.
        MovementSystem movement = openFieldMovement();
        Player player = playerAt(10f, 10f);

        movement.apply(player, input(0.5f, 0f), ONE_TICK);

        float expected = GameConstants.PLAYER_WALK_SPEED * 0.5f * ONE_TICK;
        assertEquals(expected, player.getX() - 10f, TOLERANCE);
    }

    @Test
    void oversizedInputIsClampedSoAClientCannotTeleport() {
        // The host is authoritative and must not trust client-supplied vectors.
        MovementSystem movement = openFieldMovement();
        Player honest = playerAt(10f, 10f);
        Player cheater = playerAt(10f, 10f);

        movement.apply(honest, input(1f, 0f), ONE_TICK);
        movement.apply(cheater, input(1000f, 0f), ONE_TICK);

        assertEquals(honest.getX(), cheater.getX(), TOLERANCE,
                "a client sending an oversized vector must move no further than a legitimate one");
    }

    @Test
    void malformedInputIsIgnored() {
        MovementSystem movement = openFieldMovement();
        Player player = playerAt(10f, 10f);

        movement.apply(player, input(Float.NaN, Float.POSITIVE_INFINITY), ONE_TICK);

        assertEquals(10f, player.getX(), TOLERANCE);
        assertEquals(10f, player.getY(), TOLERANCE);
    }

    @Test
    void downedPlayersDoNotMove() {
        MovementSystem movement = openFieldMovement();
        Player player = playerAt(10f, 10f);
        player.applyDamage(999f); // drops the player into the revive window

        movement.apply(player, input(1f, 1f), ONE_TICK);

        assertEquals(10f, player.getX(), TOLERANCE, "a downed player must stay put (PRD §4 revive window)");
    }

    @Test
    void movementRespectsWallsFromTheCollisionSystem() {
        MovementSystem movement = new MovementSystem(new CollisionSystem(TileMap.fromRows(java.util.List.of(
                "#####",
                "#...#",
                "#...#",
                "#...#",
                "#####"))));
        Player player = playerAt(1.5f, 1.5f);

        for (int tick = 0; tick < 120; tick++) {
            movement.apply(player, input(-1f, 0f), ONE_TICK);
        }

        assertTrue(player.getX() >= 1f - TOLERANCE,
                "the wall should have stopped the player, but they reached x=" + player.getX());
    }

    @Test
    void zeroInputLeavesThePlayerExactlyWhereTheyWere() {
        MovementSystem movement = openFieldMovement();
        Player player = playerAt(10f, 10f);

        movement.apply(player, input(0f, 0f), ONE_TICK);

        assertEquals(10f, player.getX(), TOLERANCE);
        assertEquals(10f, player.getY(), TOLERANCE);
    }
}
