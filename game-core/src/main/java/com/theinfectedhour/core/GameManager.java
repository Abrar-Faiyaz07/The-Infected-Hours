package com.theinfectedhour.core;

/** Singleton top-level orchestrator that owns the lifecycle of all game systems. */
public final class GameManager {

    private static GameManager instance;

    private GameManager() {
    }

    /** Sole accessor for the single GameManager instance (Singleton — Architecture.md §6). */
    public static synchronized GameManager getInstance() {
        if (instance == null) {
            instance = new GameManager();
        }
        return instance;
    }

    public void update(float deltaTime) {
        // TODO: implement
    }
}
