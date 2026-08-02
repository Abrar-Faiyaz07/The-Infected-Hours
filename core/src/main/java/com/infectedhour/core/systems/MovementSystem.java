package com.infectedhour.core.systems;

import com.infectedhour.core.entities.Player;
import com.infectedhour.shared.constants.GameConstants;
import com.infectedhour.shared.network.InputCommand;

import java.util.Objects;

/**
 * Turns per-player {@link InputCommand}s into collision-resolved movement.
 * Runs on the HOST only, inside the authoritative simulation tick.
 */
public class MovementSystem {

    private final CollisionSystem collisionSystem;

    public MovementSystem(CollisionSystem collisionSystem) {
        this.collisionSystem = Objects.requireNonNull(collisionSystem, "collisionSystem");
    }

    /**
     * Applies one tick of movement for {@code player}.
     *
     * <p>The input vector is sanitised and normalised here rather than on the
     * client: the host is authoritative and must not trust anything a client
     * sends. A client could otherwise send {@code moveX = 1000} and teleport, and
     * a client that forgets to normalise would move ~41% faster diagonally than
     * orthogonally.
     *
     * <p>Vectors shorter than unit length are left alone, so partial/analog input
     * still produces proportionally slower movement.
     */
    public void apply(Player player, InputCommand input, float delta) {
        if (player.isDowned() || input == null || delta <= 0f) {
            return;
        }

        float moveX = sanitiseAxis(input.moveX);
        float moveY = sanitiseAxis(input.moveY);

        float magnitudeSq = moveX * moveX + moveY * moveY;
        if (magnitudeSq <= 0f) {
            return;
        }
        if (magnitudeSq > 1f) {
            float magnitude = (float) Math.sqrt(magnitudeSq);
            moveX /= magnitude;
            moveY /= magnitude;
        }

        float speed = GameConstants.PLAYER_WALK_SPEED;
        if (input.abilityPressed) {
            speed *= GameConstants.PLAYER_SPRINT_MULTIPLIER;
        }

        collisionSystem.moveWithCollision(player, moveX * speed * delta, moveY * speed * delta);
    }

    /** Clamps to [-1, 1] and drops NaN/Infinity, so malformed input can never move a player. */
    private static float sanitiseAxis(float axis) {
        if (Float.isNaN(axis) || Float.isInfinite(axis)) {
            return 0f;
        }
        return Math.max(-1f, Math.min(1f, axis));
    }
}
