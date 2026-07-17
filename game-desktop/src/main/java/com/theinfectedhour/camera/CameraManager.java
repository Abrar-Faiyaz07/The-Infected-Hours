package com.theinfectedhour.camera;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.theinfectedhour.entities.Updatable;

/**
 * Follows the players and clamps the view to the ACTIVE MapSection's bounds —
 * locked sections are never shown (Resident-Evil-style reveal). FitViewport
 * letterboxes on resize so the aspect ratio never distorts across monitors.
 */
public class CameraManager implements Updatable {

    public static final float VIRTUAL_WIDTH = 1280f;
    public static final float VIRTUAL_HEIGHT = 720f;

    private final OrthographicCamera camera = new OrthographicCamera();
    private final Viewport viewport = new FitViewport(VIRTUAL_WIDTH, VIRTUAL_HEIGHT, camera);
    /** Camera clamp region; updated on SectionUnlockedEvent via NetworkManager/game glue. */
    private Rectangle sectionBounds;

    public OrthographicCamera getCamera() {
        return camera;
    }

    public Viewport getViewport() {
        return viewport;
    }

    /** Call from DesktopLauncher's resize() so libGDX recomputes letterboxing. */
    public void resize(int screenWidth, int screenHeight) {
        viewport.update(screenWidth, screenHeight);
    }

    /** New clamp region when a section unlocks or the players cross a gate. */
    public void setSectionBounds(Rectangle bounds) {
        this.sectionBounds = bounds;
    }

    @Override
    public void update(float deltaTime) {
        // TODO: lerp camera toward midpoint of both players, then clamp
        //       camera position so the view rectangle stays inside sectionBounds
    }
}
