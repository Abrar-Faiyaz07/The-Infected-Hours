package com.theinfectedhour.inventory;

import com.theinfectedhour.entities.player.Player;

/** Base for anything that can sit in an inventory and be used by a player. */
public abstract class Item {

    protected String id;
    protected String name;

    public abstract void use(Player p);
}
