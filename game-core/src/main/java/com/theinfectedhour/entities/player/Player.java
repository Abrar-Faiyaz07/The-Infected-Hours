package com.theinfectedhour.entities.player;

import com.theinfectedhour.entities.AbilitySet;
import com.theinfectedhour.entities.Character;
import com.theinfectedhour.inventory.Inventory;

/** A human-controlled character with a role and an inventory. */
public class Player extends Character {

    private final PlayerRole role;
    private Inventory inventory;

    public Player(PlayerRole role, AbilitySet abilitySet) {
        super(abilitySet);
        this.role = role;
    }

    public PlayerRole getRole() {
        return role;
    }
}
