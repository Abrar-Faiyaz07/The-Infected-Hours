package com.theinfectedhour.entities;

import com.badlogic.gdx.math.Vector2;

/** Contract for anything that can be moved by a direction vector. */
public interface Movable {

    void move(Vector2 direction);
}
