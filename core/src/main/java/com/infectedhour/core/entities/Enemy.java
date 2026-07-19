package com.infectedhour.core.entities;

/**
 * Base infected enemy. Boss-specific behavior lives in systems/BossPhaseSystem
 * rather than a separate Boss subclass, so the boss reuses the same
 * entity/combat plumbing as regular enemies (TRD risk mitigation: "Boss
 * fight complexity — boss built on the same objective/enemy systems").
 */
public class Enemy implements Entity {

    private final String enemyId;
    private final String type; // e.g. "basic_infected", "mutation_swarm"
    private float x, y;
    private float hp;

    public Enemy(String enemyId, String type, float hp) {
        this.enemyId = enemyId;
        this.type = type;
        this.hp = hp;
    }

    public String getEnemyId() {
        return enemyId;
    }

    public String getType() {
        return type;
    }

    public float getHp() {
        return hp;
    }

    public boolean isDead() {
        return hp <= 0;
    }

    public void takeDamage(float amount) {
        hp = Math.max(0, hp - amount);
    }

    @Override
    public float getX() {
        return x;
    }

    @Override
    public float getY() {
        return y;
    }

    @Override
    public void update(float delta) {
        // Intentionally empty: enemy behavior is driven by systems/AISystem
        // on the HOST simulation tick — do NOT add movement logic here.
        // If you need per-enemy state (attack cooldown, stun timer), add
        // the FIELDS on this class and let AISystem read/write them.
    }
}
