package com.infectedhour.core.systems;

import com.infectedhour.core.entities.Enemy;
import com.infectedhour.core.entities.Player;

import java.util.List;
import java.util.Objects;

/**
 * Basic enemy AI (chase nearest player, attack in range). Runs on the HOST
 * only, as part of the authoritative simulation tick.
 */
public class AISystem {

    private static final float AGGRO_RANGE = 6f;
    private static final float ATTACK_RANGE = 1f;

    /**
     * Shared with MovementSystem so enemies and players obey one collision rule.
     * Use {@code collisionSystem.moveWithCollision(enemy, dx, dy)} for every
     * enemy step — never {@code enemy.move(...)} directly, which skips walls.
     */
    private final CollisionSystem collisionSystem;

    public AISystem(CollisionSystem collisionSystem) {
        this.collisionSystem = Objects.requireNonNull(collisionSystem, "collisionSystem");
    }

    public void update(Enemy enemy, List<Player> players, float delta) {
        Player nearest = findNearestPlayer(enemy, players);
        if (nearest == null) return;

        float dx = nearest.getX() - enemy.getX();
        float dy = nearest.getY() - enemy.getY();
        float distSq = dx * dx + dy * dy;

        if (distSq <= ATTACK_RANGE * ATTACK_RANGE) {
            // ============== TEAMMATE TASK: ENEMY ATTACK ==============
            // TODO(ai): attack the player in range.
            //  1. Add an attack-cooldown field to Enemy (e.g. 1s between hits).
            //  2. When ready: combatSystem.applyEnemyHit(enemy, nearest, ~8f)
            //     (pass a CombatSystem into this method or the constructor).
            //  3. Reset the cooldown after the hit.
            // =========================================================
        } else if (distSq <= AGGRO_RANGE * AGGRO_RANGE) {
            // ============== TEAMMATE TASK: ENEMY CHASE ===============
            // TODO(ai): move toward the nearest player.
            //  1. Direction = (dx, dy) normalized by sqrt(distSq).
            //  2. Speed = GameConstants.ENEMY_WALK_SPEED (2 tiles/s) — slower
            //     than Jane (4 tiles/s) so she can always escape.
            //  3. Step with:
            //       collisionSystem.moveWithCollision(enemy, dirX * speed * delta,
            //                                                dirY * speed * delta);
            //     Collision is DONE — do not re-implement wall checks here, and
            //     never call enemy.move(...) directly (that skips walls).
            //     moveWithCollision returns true when a wall absorbed the step;
            //     that is your cue to add wall-following later if chasing looks
            //     dumb around corners. There is no pathfinding in v1.
            // =========================================================
        }
    }

    private Player findNearestPlayer(Enemy enemy, List<Player> players) {
        Player nearest = null;
        float nearestDistSq = Float.MAX_VALUE;
        for (Player p : players) {
            if (p.isDowned()) continue;
            float dx = p.getX() - enemy.getX();
            float dy = p.getY() - enemy.getY();
            float distSq = dx * dx + dy * dy;
            if (distSq < nearestDistSq) {
                nearestDistSq = distSq;
                nearest = p;
            }
        }
        return nearest;
    }
}
