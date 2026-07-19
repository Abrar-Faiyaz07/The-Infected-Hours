package com.infectedhour.core.systems;

import com.infectedhour.core.entities.Enemy;
import com.infectedhour.core.entities.Player;

import java.util.List;

/**
 * Basic enemy AI (chase nearest player, attack in range). Runs on the HOST
 * only, as part of the authoritative simulation tick.
 */
public class AISystem {

    private static final float AGGRO_RANGE = 6f;
    private static final float ATTACK_RANGE = 1f;

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
            //  2. Move at an enemy-speed constant (~2f * delta) — slower
            //     than Jane (4f) so she can always escape.
            //  3. Check the walkable-tile grid before stepping (same
            //     collision rule as MovementSystem).
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
