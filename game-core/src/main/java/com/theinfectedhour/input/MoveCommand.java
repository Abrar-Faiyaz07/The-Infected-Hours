package com.theinfectedhour.input;

import com.badlogic.gdx.math.Vector2;
import com.theinfectedhour.entities.player.Player;

/** Command that moves a player in a direction. */
public class MoveCommand implements GameCommand {

    private final Player target;
    private final Vector2 direction;

    public MoveCommand(Player target, Vector2 direction) {
        this.target = target;
        this.direction = direction;
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
