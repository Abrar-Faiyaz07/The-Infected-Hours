package com.infectedhour.core.systems;

import com.infectedhour.core.entities.Player;
import com.infectedhour.shared.network.InputCommand;

public class MovementSystem {

    private static final float BASE_SPEED = 4.0f;
    private static final float SPRINT_MULTIPLIER = 1.6f;

    /**
     * Updates player position based on incoming network input commands and delta time.
     */
    public void apply(Player player, InputCommand input, float delta) {
        if (player.isDowned()) {
            return;
        }

        float currentSpeed = BASE_SPEED;
        if (input.abilityPressed) {
            currentSpeed *= SPRINT_MULTIPLIER;
        }

        float moveX = input.moveX * currentSpeed * delta;
        float moveY = input.moveY * currentSpeed * delta;

        player.move(moveX, moveY);
    }
}