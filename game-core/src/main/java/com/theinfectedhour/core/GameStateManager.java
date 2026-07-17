package com.theinfectedhour.core;

import com.theinfectedhour.core.state.GameState;

/** State-pattern context owning the current top-level state: menu, playing, paused, game over. */
public class GameStateManager {

    private GameState currentState;

    public void setState(GameState newState) {
        // TODO: implement (exit current state, enter new state)
    }

    public GameState getCurrentState() {
        return currentState;
    }

    public void update(float deltaTime) {
        // TODO: implement (delegate to current state)
    }
}
