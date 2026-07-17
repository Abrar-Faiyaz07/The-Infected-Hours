package com.theinfectedhour.entities;

/** Contract for anything ticked once per frame by the game loop. */
public interface Updatable {

    void update(float deltaTime);
}
