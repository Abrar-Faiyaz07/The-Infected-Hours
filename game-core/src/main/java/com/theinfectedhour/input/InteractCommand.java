package com.theinfectedhour.input;

import com.theinfectedhour.entities.Interactable;
import com.theinfectedhour.entities.player.Player;

/** Command that makes a player interact with a nearby Interactable. */
public class InteractCommand implements GameCommand {

    private final Player source;
    private final Interactable target;

    public InteractCommand(Player source, Interactable target) {
        this.source = source;
        this.target = target;
    }

    @Override
    public void execute() {
        // TODO: implement
    }

    @Override
    public void undo() {
        // TODO: implement
    }
}
