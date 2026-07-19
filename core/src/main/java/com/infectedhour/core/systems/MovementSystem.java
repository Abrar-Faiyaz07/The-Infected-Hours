package com.infectedhour.core.systems;

import com.infectedhour.core.entities.Player;
import com.infectedhour.shared.network.InputCommand;

/**
 * Applies InputCommand intent to a Player's position on the HOST simulation
 * only. Runs at SIMULATION_TICK_HZ (60). Character speed differences
 * (Jane faster + dash, PRD §4) live here.
 */
public class MovementSystem {

    private static final float ELRIC_SPEED = 3.0f;
    private static final float JANE_SPEED = 4.0f;
    private static final float JANE_DASH_MULTIPLIER = 2.5f;

    public void apply(Player player, InputCommand input, float delta) {
        float speed = switch (player.getCharacter()) {
            case ELRIC -> ELRIC_SPEED;
            case JANE -> input.abilityPressed ? JANE_SPEED * JANE_DASH_MULTIPLIER : JANE_SPEED;
        };
        // ===================== TEAMMATE TASK: MOVEMENT =====================
        // TODO(movement): normalize input + tile collision before applying.
        //  1. Normalize (moveX, moveY) so diagonal movement isn't ~41% faster:
        //       float len = (float) Math.hypot(input.moveX, input.moveY);
        //       if (len > 1) divide both components by len.
        //  2. Collision: get the walkable-tile grid from LevelLoader (Tiled
        //     collision layer). Compute the tile the player WOULD land on;
        //     if that tile is blocked, cancel only that axis (wall-sliding).
        //  3. Jane's dash: only allow when Player.abilityCooldownRemaining <= 0;
        //     after dashing, set the cooldown (~3s feels right).
        // DOCS: docs/02_TRD1.md par.4 (collision), docs/01_PRD1.md par.4 (characters)
        // ===================================================================
        player.move(input.moveX * speed * delta, input.moveY * speed * delta);
    }
}
