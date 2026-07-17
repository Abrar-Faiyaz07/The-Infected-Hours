package com.theinfectedhour.world.objects;

import com.theinfectedhour.entities.Damageable;

/** A destructible obstacle that blocks movement until broken down. */
public class Barricade implements Damageable {

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
