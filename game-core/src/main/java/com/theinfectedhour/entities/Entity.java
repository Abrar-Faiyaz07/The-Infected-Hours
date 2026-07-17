package com.theinfectedhour.entities;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;

/** Base class for every world object that has an identity and a position. */
public abstract class Entity implements Renderable, Updatable {

    protected String id;
    protected Vector2 position;

    @Override
    public void render(SpriteBatch batch) {
        // TODO: implement
    }

    @Override
    public void update(float deltaTime) {
        // TODO: implement
    }
}
