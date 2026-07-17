package com.theinfectedhour.entities;

import com.theinfectedhour.entities.player.Player;

/** Contract for anything a player can interact with (crates, NPCs, sanitation points). */
public interface Interactable {

    void interact(Player source);
}
