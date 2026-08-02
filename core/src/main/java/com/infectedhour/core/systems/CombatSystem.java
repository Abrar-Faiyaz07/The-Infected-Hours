package com.infectedhour.core.systems;

import com.infectedhour.core.entities.Enemy;
import com.infectedhour.core.entities.Player;

/**
 * Damage resolution between players and enemies. Elric's heal ability
 * (PRD §4) is handled here rather than in Player itself, keeping
 * "combat rules" in one place instead of spread across entity classes.
 */
public class CombatSystem {

    private static final float PLAYER_BASE_DAMAGE = 10f;
    private static final float ELRIC_HEAL_AMOUNT = 25f;

    public void attack(Player attacker, Enemy target) {
        target.takeDamage(PLAYER_BASE_DAMAGE);
    }

    public void applyEnemyHit(Enemy attacker, Player target, float damage) {
        target.applyDamage(damage);
    }

    /**
     * Elric's heal ability, self or partner (PRD par.4).
     * TEAMMATE TASK (combat):
     *  1. Check elric.abilityCooldownRemaining <= 0 before healing; if on
     *     cooldown do nothing (HUD should grey out the ability icon).
     *  2. After a successful heal, set the cooldown (suggest 8-10 seconds).
     *  3. Range check: only allow healing the partner within ~2 tiles.
     */
    public void elricHeal(Player elric, Player target) {
        target.heal(ELRIC_HEAL_AMOUNT);
    }

    public void revive(Player reviver, Player downedPlayer) {
        if (downedPlayer.isDowned()) {
            downedPlayer.revive();
        }
    }
}
