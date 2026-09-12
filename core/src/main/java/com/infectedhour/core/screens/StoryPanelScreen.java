package com.infectedhour.core.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
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
    private Texture storyBackground;
    private Texture overlayPixel;

    private String[] panels;
    private String[] panelTitles;
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
        font.getData().setScale(1.15f);
        titleFont = new BitmapFont();
        titleFont.getData().setScale(2.05f);
        panels = panelsFor(sequence);
        panelTitles = panelTitlesFor(sequence);

        if (sequence == Sequence.INTRO && Gdx.files.internal("story_intro.png").exists()) {
            storyBackground = new Texture(Gdx.files.internal("story_intro.png"));
            storyBackground.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        }

        Pixmap pixel = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixel.setColor(Color.WHITE);
        pixel.fill();
        overlayPixel = new Texture(pixel);
        pixel.dispose();

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

        float screenWidth = Gdx.graphics.getWidth();
        float screenHeight = Gdx.graphics.getHeight();
        float marginX = Math.max(54f, screenWidth * 0.055f);
        float contentWidth = Math.min(700f, screenWidth * 0.40f);
        float top = screenHeight - Math.max(64f, screenHeight * 0.075f);

        batch.begin();
        if (storyBackground != null) {
            batch.setColor(Color.WHITE);
            batch.draw(storyBackground, 0f, 0f, screenWidth, screenHeight);
        }

        // Readability layers keep the generated scene visible while giving the
        // story copy a stable dark area at every supported resolution.
        batch.setColor(0.01f, 0.02f, 0.035f, 0.20f);
        batch.draw(overlayPixel, 0f, 0f, screenWidth, screenHeight);
        batch.setColor(0.01f, 0.02f, 0.035f, 0.82f);
        batch.draw(overlayPixel, 0f, 0f, Math.min(screenWidth * 0.54f, 930f), screenHeight);
        batch.setColor(0.910f, 0.690f, 0.165f, 1f);
        batch.draw(overlayPixel, marginX, top - 34f, 82f, 3f);
        batch.setColor(Color.WHITE);

        font.setColor(0.910f, 0.690f, 0.165f, 1f);
        font.draw(batch, sequenceLabel(sequence), marginX, top);

        titleFont.setColor(Color.WHITE);
        titleFont.draw(batch, panelTitles[currentPanelIndex], marginX, top - 62f,
                contentWidth, Align.left, true);

        font.setColor(0.88f, 0.90f, 0.92f, 1f);
        font.draw(batch, text.substring(0, visibleChars), marginX, top - 158f,
                contentWidth, Align.left, true);

        titleFont.setColor(0.910f, 0.690f, 0.165f, 1f); // accent-gold
        font.setColor(0.62f, 0.65f, 0.69f, 1f);
        font.draw(batch, footerHint(fullyRevealed, partnerOk), marginX, 58f);
        font.draw(batch, String.format("%02d / %02d", currentPanelIndex + 1, panels.length),
                marginX + contentWidth - 64f, 58f);
        batch.end();

        if (localAdvanceRequested && partnerOk) {
            nextPanel();
        }

    }

    private static String sequenceLabel(Sequence sequence) {
        return switch (sequence) {
            case INTRO -> "OPERATION ASHGROVE";
            case AFTER_LEVEL_1 -> "FIELD REPORT 01";
            case AFTER_LEVEL_2 -> "FIELD REPORT 02";
            case ENDING -> "FINAL REPORT";
        };
    }

    private static String[] panelTitlesFor(Sequence sequence) {
        return switch (sequence) {
            case INTRO -> new String[]{
                    "THE WARNING CAME TOO LATE",
                    "TWO RESPONDERS. ONE HOUR.",
                    "THE CONTAINMENT DIRECTIVE"
            };
            case AFTER_LEVEL_1 -> new String[]{
                    "BEYOND THE HOSPITAL",
                    "THE ROAD IS NOT EMPTY"
            };
            case AFTER_LEVEL_2 -> new String[]{
                    "THE TUNNEL BELOW ASHGROVE",
                    "THE HIDDEN LABORATORY"
            };
            case ENDING -> new String[]{
                    "THE HEART FALLS SILENT",
                    "ASHGROVE REMEMBERS"
            };
        };
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
            if (bridge.hasLauncher()) {
                bridge.requestReturnToLauncher(() -> Gdx.app.postRunnable(Gdx.app::exit));
            } else {
                game.setScreen(new MainMenuScreen(game, client, bridge));
            }
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
                    "By midnight, the infection had crossed the river. Every road out of Ashgrove was sealed before the warning reached its people.",
                    "Elric is a field medic who knows what an outbreak costs. Jane is the scout who knows every path through Ashgrove. They are the last team going in.",
                    "Find the survivors. Recover the samples. Trace the source. Destroy the Virus Heart before dawn."
            };
            case AFTER_LEVEL_1 -> new String[]{
                    "The hospital is behind them, but the road outside cuts through a village overrun by the infected.",
                    "Survivors are trapped between abandoned homes. Restoring the roadside relays may open the old service tunnel."
            };
            case AFTER_LEVEL_2 -> new String[]{
                    "The rescued villagers point toward a sealed passage beneath the road. The relay code unlocks it.",
                    "Below Ashgrove waits a hidden laboratory — and the organism that started the outbreak."
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
        if (storyBackground != null) storyBackground.dispose();
        if (overlayPixel != null) overlayPixel.dispose();
    }
}
