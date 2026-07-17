package com.theinfectedhour.graphics;

import com.badlogic.gdx.graphics.g2d.ParticleEffect;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.theinfectedhour.entities.Updatable;
import com.theinfectedhour.world.Level;

/**
 * Owns the render pipeline: the shared SpriteBatch, the Tiled map renderer,
 * particle effects (contamination mist, sanitation sparks), and draw order
 * (map layers below entities, HUD on top).
 */
public class GraphicsManager implements Updatable {

    private final SpriteBatch batch = new SpriteBatch();
    /** Created when a Level loads; renders the .tmx layers authored in Tiled. */
    private OrthogonalTiledMapRenderer mapRenderer;

    public SpriteBatch getBatch() {
        return batch;
    }

    /** Swap the map renderer when LevelManager loads a new Level. */
    public void setLevel(Level level) {
        // TODO: dispose old renderer; mapRenderer = new OrthogonalTiledMapRenderer(level.getTiledMap(), batch)
    }

    /** Load a particle effect authored in the libGDX Particle Editor (.p file). */
    public ParticleEffect loadParticleEffect(String effectPath) {
        // TODO: effect.load(Gdx.files.internal(effectPath), Gdx.files.internal("textures"))
        return null;
    }

    @Override
    public void update(float deltaTime) {
        // TODO: mapRenderer.setView(camera); mapRenderer.render();
        //       then batch entity sprites and active particle effects
    }

    public void dispose() {
        // TODO: dispose batch, mapRenderer, loaded effects
    }
}
