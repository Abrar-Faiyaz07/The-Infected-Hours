package com.theinfectedhour.entities;

/** Contract for anything that can take damage and die. */
public interface Damageable {

    void takeDamage(int amount);

    boolean isDead();
}
