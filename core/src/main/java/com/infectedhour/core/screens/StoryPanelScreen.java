package com.infectedhour.core.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Align;
import com.infectedhour.core.InfectedHourGame;
import com.infectedhour.core.bridge.GameBridge;
import com.infectedhour.core.net.GameClient;
import com.infectedhour.shared.constants.GameConstants;
import com.infectedhour.shared.network.MatchMode;

/**
 * Full-screen illustrated story panels (UI/UX doc §6). Advances only when
 * BOTH players press E (UX rule 5) with a 20s auto-ready fallback — mirrors
 * the LevelBriefingScreen ready-gate pattern, and for the same reason: a
 * co-op story beat that advances on one screen but not the other desyncs the
 * whole session.
 */
public class StoryPanelScreen implements Screen {

    public enum Sequence {
        INTRO, AFTER_LEVEL_1, AFTER_LEVEL_2, ENDING
    }

    private static final String STORY_ADVANCE_EVENT = "STORY_ADVANCE";

    private final InfectedHourGame game;
    private final GameClient client;
    private final GameBridge bridge;
    private final Sequence sequence;
    private final int justCompletedOrUpcomingLevel;

    private SpriteBatch batch;
    private BitmapFont font;
    private BitmapFont titleFont;

    private String[] panels;
    private int currentPanelIndex = 0;
    private float typewriterElapsed = 0f;
    private float panelElapsed = 0f;
    private boolean localAdvanceRequested = false;
    private volatile boolean partnerAdvanceRequested = false;

    private static final float TYPEWRITER_CHARS_PER_SEC = 40f; // UI/UX doc §6
    private static final float AUTO_READY_FALLBACK_SECONDS = 20f;

    public StoryPanelScreen(InfectedHourGame game, GameClient client, GameBridge bridge,
                            Sequence sequence, int levelContext) {
        this.game = game;
        this.client = client;
        this.bridge = bridge;
        this.sequence = sequence;
        this.justCompletedOrUpcomingLevel = levelContext;
    }

    @Override
    public void show() {
        batch = new SpriteBatch();
        font = new BitmapFont();
        titleFont = new BitmapFont();
        titleFont.getData().setScale(1.6f);
        panels = panelsFor(sequence);

        client.setOnEvent(event -> {
            if (STORY_ADVANCE_EVENT.equals(event.type)) {
                partnerAdvanceRequested = true;
            }
        });
    }

    @Override
    public void render(float delta) {
        game.stepSimulation(delta);

        typewriterElapsed += delta;
        panelElapsed += delta;

        String text = panels[currentPanelIndex];
        int visibleChars = Math.min(text.length(), (int) (typewriterElapsed * TYPEWRITER_CHARS_PER_SEC));
        boolean fullyRevealed = visibleChars >= text.length();

        if (Gdx.input.isKeyJustPressed(Input.Keys.E) || Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            if (!fullyRevealed) {
                typewriterElapsed = text.length() / TYPEWRITER_CHARS_PER_SEC + 1f; // reveal the rest instantly
            } else if (!localAdvanceRequested) {
                localAdvanceRequested = true;
                client.sendEvent(STORY_ADVANCE_EVENT, sequence.name() + ":" + currentPanelIndex);
            }
        }

        // Nobody is held hostage by a partner who walked away (UX rule 5).
        boolean partnerOk = partnerAdvanceRequested
                || panelElapsed >= AUTO_READY_FALLBACK_SECONDS
                || client.getMatchMode() == MatchMode.SOLO;

        Gdx.gl.glClearColor(0f, 0f, 0f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        float top = Gdx.graphics.getHeight() - 70f;
        batch.begin();
        titleFont.setColor(0.910f, 0.690f, 0.165f, 1f); // accent-gold
        titleFont.draw(batch, sequence.name().replace('_', ' '), 70f, top);

        font.setColor(Color.WHITE);
        font.draw(batch, text.substring(0, visibleChars), 70f, top - 60f,
                Gdx.graphics.getWidth() - 140f, Align.left, true);

        font.setColor(Color.GRAY);
        font.draw(batch, footerHint(fullyRevealed, partnerOk), 70f, 50f);
        batch.end();

        if (localAdvanceRequested && partnerOk) {
            nextPanel();
        }

        // ============ TEAMMATE TASK: PANEL ART ============
        // TODO(story): draw assets/story/<sequence>/panel_N.png full-screen
        // behind this text, with the copy in a panel-dark (#1C2536) box at the
        // bottom (UI/UX doc §6). The advance/sync logic above stays as-is.
        // ==================================================
    }

    private String footerHint(boolean fullyRevealed, boolean partnerOk) {
        if (!fullyRevealed) {
            return "[E] skip text";
        }
        if (!localAdvanceRequested) {
            return "[E] continue   (" + (currentPanelIndex + 1) + "/" + panels.length + ")";
        }
        return partnerOk ? "" : "Waiting for your partner…";
    }

    private void nextPanel() {
        localAdvanceRequested = false;
        partnerAdvanceRequested = false;
        typewriterElapsed = 0f;
        panelElapsed = 0f;
        currentPanelIndex++;

        if (currentPanelIndex < panels.length) {
            return;
        }
        if (sequence == Sequence.ENDING) {
            bridge.notifyMatchEnded(new GameBridge.MatchOutcome("VICTORY", GameConstants.BOSS_LEVEL_NUMBER));
            Gdx.app.exit();
            return;
        }
        if (sequence == Sequence.INTRO) {
            game.setScreen(new GameScreen(game, client, bridge, justCompletedOrUpcomingLevel));
        } else {
            game.setScreen(new LevelBriefingScreen(game, client, bridge, justCompletedOrUpcomingLevel + 1));
        }
    }

    /**
     * TEAMMATE TASK (story): replace this placeholder copy with the real script.
     * The four sequences are INTRO, AFTER_LEVEL_1 (Elric's family flashback),
     * AFTER_LEVEL_2 (Jane's streets and the truth about the earlier outbreak),
     * and ENDING (redemption).
     */
    private static String[] panelsFor(Sequence sequence) {
        return switch (sequence) {
            case INTRO -> new String[]{
                    "The outbreak reached Ashgrove before the warning did.",
                    "Elric, a field medic. Jane, a local scout. One hour before the district is sealed.",
                    "Contain it. Find the source. End it."
            };
            case AFTER_LEVEL_1 -> new String[]{
                    "The district holds — barely.",
                    "Elric checks a name off a list he has been carrying since the first outbreak."
            };
            case AFTER_LEVEL_2 -> new String[]{
                    "Jane knows these streets. She knows what was buried under them.",
                    "The source was never natural. Something down there is still growing."
            };
            case ENDING -> new String[]{
                    "The Virus Heart collapses. The air clears.",
                    "Ashgrove will remember the hour it nearly lost everything — and the two who stayed."
            };
        };
    }

    @Override public void resize(int width, int height) { }
    @Override public void pause() { }
    @Override public void resume() { }

    @Override
    public void hide() {
        client.setOnEvent(null);
    }

    @Override
    public void dispose() {
        if (batch != null) batch.dispose();
        if (font != null) font.dispose();
        if (titleFont != null) titleFont.dispose();
    }
}
