package com.theinfectedhour.entities;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;

/** Contract for anything the render loop can draw. */
public interface Renderable {

    void render(SpriteBatch batch);
}
