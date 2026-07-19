package com.infectedhour.core.screens;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.infectedhour.core.bridge.GameBridge;
import com.infectedhour.core.net.GameClient;

/**
 * "Mission list with icons, controls reminder, Both players ready sync gate"
 * (UI/UX doc §2, screen 10). Gate advances to StoryPanelScreen (level 1 only)
 * or straight into GameScreen once both players signal ready.
 */
public class LevelBriefingScreen implements Screen {

    private final Game game;
    private final GameClient client;
    private final GameBridge bridge;
    private final int levelNumber;

    private boolean localReady = false;
    // TODO(screens): set this to true when EventMessage("READY","") arrives
    // from the partner (register a listener on GameClient). Solo host: set
    // it true immediately so the gate doesn't block single-player testing.
    private boolean partnerReady = false;

    public LevelBriefingScreen(Game game, GameClient client, GameBridge bridge, int levelNumber) {
        this.game = game;
        this.client = client;
        this.bridge = bridge;
        this.levelNumber = levelNumber;
    }

    @Override
    public void show() {
        // ============== TEAMMATE TASK: BRIEFING UI ==============
        // TODO(screens): build the Scene2D stage (UI/UX doc par.2 screen 10):
        //  1. Stage + FitViewport(1280, 720); Gdx.input.setInputProcessor(stage)
        //  2. Table with: level-name title (accent-gold #E8B02A), mission list
        //     with icons from LevelLoader.loadDefinition(levelNumber)
        //     .objectives(), controls reminder (WASD/E/Space/Shift), READY btn.
        //  3. READY click -> localReady = true and send
        //     EventMessage("READY", "") via client; when the partner's READY
        //     arrives -> partnerReady = true (solo host: set it immediately).
        //  4. While waiting: "Waiting for partner..." with animated dots
        //     (UX rule 1: never block silently on network waits).
        // ========================================================
    }

    @Override
    public void render(float delta) {
        com.badlogic.gdx.Gdx.gl.glClearColor(0.05f, 0.06f, 0.08f, 1); // bg-night #0E1420
        com.badlogic.gdx.Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        if (localReady && partnerReady) {
            if (levelNumber == 1) {
                game.setScreen(new StoryPanelScreen(game, client, bridge, StoryPanelScreen.Sequence.INTRO, levelNumber));
            } else if (levelNumber == com.infectedhour.shared.constants.GameConstants.BOSS_LEVEL_NUMBER) {
                game.setScreen(new BossScreen(game, client, bridge));
            } else {
                game.setScreen(new GameScreen(game, client, bridge, levelNumber));
            }
        }
    }

    @Override public void resize(int width, int height) { }
    @Override public void pause() { }
    @Override public void resume() { }
    @Override public void hide() { }
    @Override public void dispose() { }
}
