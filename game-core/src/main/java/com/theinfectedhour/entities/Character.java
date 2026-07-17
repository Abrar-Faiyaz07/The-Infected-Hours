package com.theinfectedhour.entities;

/** Base for player- and AI-controlled actors: has health and composes an injected AbilitySet rather than subclassing behavior. */
public abstract class Character extends Entity implements Damageable {

    protected int health;
    protected AbilitySet abilitySet;

    protected Character(AbilitySet abilitySet) {
        this.abilitySet = abilitySet;
    }

    /** Delegates to the composed AbilitySet — abilities are injected, never inherited (Architecture.md §6). */
    public void useAbility() {
        // TODO: implement (delegate to abilitySet)
    }

    @Override
    public void takeDamage(int amount) {
        // TODO: implement
    }

    @Override
    public boolean isDead() {
        // TODO: implement
        return false;
    }
}
