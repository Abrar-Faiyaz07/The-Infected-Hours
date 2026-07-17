package com.theinfectedhour.core.state;

import com.theinfectedhour.core.GameStateManager;

/** State-pattern contract for one top-level game state managed by GameStateManager. */
public interface GameState {

    void onEnter(GameStateManager manager);

    void update(float deltaTime);

    void onExit();
}
