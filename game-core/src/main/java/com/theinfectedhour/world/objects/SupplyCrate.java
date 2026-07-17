package com.theinfectedhour.world.objects;

import com.theinfectedhour.entities.Interactable;
import com.theinfectedhour.entities.player.Player;

/** A lootable crate — Interactable only, deliberately NOT Damageable (interface segregation, Architecture.md §5). */
public class SupplyCrate implements Interactable {

    @Override
    public void interact(Player source) {
        // TODO: implement
    }
}
