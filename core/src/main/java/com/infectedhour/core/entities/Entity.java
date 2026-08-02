package com.infectedhour.core.entities;

/** Minimal contract shared by anything positioned/updated in the game world. */
public interface Entity {
    float getX();
    float getY();
    void update(float delta);
}
