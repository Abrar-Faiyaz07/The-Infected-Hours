package com.infectedhour.core.screens;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.infectedhour.core.bridge.GameBridge;
import com.infectedhour.core.net.GameClient;

/**
 * Full-screen illustrated story panels (UI/UX doc §6). Advances only when
 * BOTH players press E (UX rule 5) with a 20s auto-ready fallback — mirrors
 * the LevelBriefingScreen ready-gate pattern.
 */
public class StoryPanelScreen implements Screen {

    public enum Sequence {
        INTRO, AFTER_LEVEL_1, AFTER_LEVEL_2, ENDING
    }

    private final Game game;
    private final GameClient client;
    private final GameBridge bridge;
    private final Sequence sequence;
    private final int justCompletedOrUpcomingLevel;

    private int currentPanelIndex = 0;
    private float typewriterElapsed = 0f;
    private static final float TYPEWRITER_CHARS_PER_SEC = 40f; // UI/UX doc §6
    private static final float AUTO_READY_FALLBACK_SECONDS = 20f;

    public StoryPanelScreen(Game game, GameClient client, GameBridge bridge, Sequence sequence, int levelContext) {
        this.game = game;
        this.client = client;
        this.bridge = bridge;
        this.sequence = sequence;
        this.justCompletedOrUpcomingLevel = levelContext;
    }

    @Override
    public void show() {
        // ============ TEAMMATE TASK: PANEL CONTENT ============
        // TODO(story): load 2-3 panels for this sequence.
        //  - Art: assets/story/<sequence>/panel_1.png ... (tinted screenshots
        //    with a dark vignette are fine for v1 — UI/UX doc par.6).
        //  - Text: hardcode a String[] per Sequence here, or load a simple
        //    JSON from assets/story/. The 4 sequences: INTRO, AFTER_LEVEL_1
        //    (Elric's family flashback), AFTER_LEVEL_2 (Jane's streets +
        //    the earlier outbreak truth), ENDING (redemption).
        // ======================================================
    }

    @Override
    public void render(float delta) {
        com.badlogic.gdx.Gdx.gl.glClearColor(0, 0, 0, 1);
        com.badlogic.gdx.Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        typewriterElapsed += delta;
        // ============ TEAMMATE TASK: PANEL RENDERING ============
        // TODO(story): UI/UX doc par.6 behavior:
        //  1. Draw the current panel full-screen; text box in panel-dark
        //     (#1C2536) at the bottom.
        //  2. Typewriter: visibleChars = (int)(typewriterElapsed *
        //     TYPEWRITER_CHARS_PER_SEC); draw text.substring(0, visibleChars).
        //  3. E while typing -> reveal full text instantly.
        //     E when full    -> mark THIS player ready to advance.
        //  4. Advance only when BOTH are ready (EventMessage sync) with the
        //     20s AUTO_READY_FALLBACK so nobody is held hostage (UX rule 5).
        // ========================================================

        // TODO(story): when the last panel of the sequence finishes:
        //  - ENDING -> credits screen (scroll the ASSETS_CREDITS.md content!)
        //    then bridge.notifyMatchEnded(new MatchOutcome("VICTORY", 3))
        //  - otherwise -> game.setScreen(new LevelBriefingScreen(...)) for
        //    justCompletedOrUpcomingLevel + 1.
    }

    @Override public void resize(int width, int height) { }
    @Override public void pause() { }
    @Override public void resume() { }
    @Override public void hide() { }
    @Override public void dispose() { }
}
