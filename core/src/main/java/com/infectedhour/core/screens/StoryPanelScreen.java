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
import com.infectedhour.core.audio.SoundtrackCatalog;
import com.infectedhour.core.bridge.GameBridge;
import com.infectedhour.core.content.DialogueCatalog;
import com.infectedhour.core.net.GameClient;
import com.infectedhour.shared.constants.GameConstants;
import com.infectedhour.shared.network.MatchMode;

import java.util.List;

/**
 * Full-screen cinematic player for the campaign's generated images, subtitles,
 * and optional OGG narration.
 */
public class StoryPanelScreen implements Screen {

    public enum Sequence {
        INTRO(DialogueCatalog.Scene.INTRO),
        LEVEL_2_START(DialogueCatalog.Scene.LEVEL_2_START),
        LEVEL_3_START(DialogueCatalog.Scene.LEVEL_3_START),
        LEVEL_4_START(DialogueCatalog.Scene.LEVEL_4_START),
        LEVEL_5_START(DialogueCatalog.Scene.LEVEL_5_START),
        LEVEL_6_START(DialogueCatalog.Scene.LEVEL_6_START),
        ENDING(DialogueCatalog.Scene.ENDING);

        private final DialogueCatalog.Scene dialogueScene;

        Sequence(DialogueCatalog.Scene dialogueScene) {
            this.dialogueScene = dialogueScene;
        }

        public DialogueCatalog.Scene dialogueScene() {
            return dialogueScene;
        }

        public static Sequence afterCompletedLevel(int levelNumber) {
            return switch (levelNumber) {
                case 1 -> LEVEL_2_START;
                case 2 -> LEVEL_3_START;
                case 3 -> LEVEL_4_START;
                case 4 -> LEVEL_5_START;
                case 5 -> LEVEL_6_START;
                case 6 -> ENDING;
                default -> throw new IllegalArgumentException("No cinematic follows level " + levelNumber);
            };
        }

        /** Used by developer co-op when launching directly into a selected map. */
        public static Sequence beforeLevel(int levelNumber) {
            return switch (levelNumber) {
                case 1 -> INTRO;
                case 2 -> LEVEL_2_START;
                case 3 -> LEVEL_3_START;
                case 4 -> LEVEL_4_START;
                case 5 -> LEVEL_5_START;
                case 6 -> LEVEL_6_START;
                default -> throw new IllegalArgumentException("No cinematic precedes level " + levelNumber);
            };
        }
    }

    private static final String STORY_ADVANCE_EVENT = "STORY_ADVANCE";
    private static final String STORY_SKIP_EVENT = "STORY_SKIP";
    private static final float TYPEWRITER_CHARS_PER_SEC = 52f;
    private static final float AUTO_READY_FALLBACK_SECONDS = 20f;
    private static final float FADE_IN_SECONDS = 0.65f;

    private final InfectedHourGame game;
    private final GameClient client;
    private final GameBridge bridge;
    private final Sequence sequence;
    private final int levelContext;

    private SpriteBatch batch;
    private BitmapFont subtitleFont;
    private BitmapFont speakerFont;
    private BitmapFont titleFont;
    private Texture cinematicTexture;
    private Texture overlayPixel;
    private List<DialogueCatalog.Line> panels;

    private int currentPanelIndex;
    private float typewriterElapsed;
    private float panelElapsed;
    private boolean localAdvanceRequested;
    private volatile boolean partnerAdvanceRequested;
    private boolean transitioning;

    public StoryPanelScreen(InfectedHourGame game, GameClient client, GameBridge bridge,
                            Sequence sequence, int levelContext) {
        this.game = game;
        this.client = client;
        this.bridge = bridge;
        this.sequence = sequence;
        this.levelContext = levelContext;
    }

    @Override
    public void show() {
        batch = new SpriteBatch();
        subtitleFont = new BitmapFont();
        subtitleFont.getData().setScale(1.18f);
        speakerFont = new BitmapFont();
        speakerFont.getData().setScale(1.05f);
        titleFont = new BitmapFont();
        titleFont.getData().setScale(1.55f);
        panels = DialogueCatalog.lines(sequence.dialogueScene());

        Pixmap pixel = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixel.setColor(Color.WHITE);
        pixel.fill();
        overlayPixel = new Texture(pixel);
        pixel.dispose();

        client.setOnEvent(event -> {
            if (event.type == null) return;
            if (STORY_ADVANCE_EVENT.equals(event.type) && panelToken().equals(event.payload)) {
                partnerAdvanceRequested = true;
            } else if (STORY_SKIP_EVENT.equals(event.type) && sequence.name().equals(event.payload)) {
                Gdx.app.postRunnable(this::finishSequence);
            }
        });

        loadCurrentPanel();
    }

    @Override
    public void render(float delta) {
        game.stepSimulation(delta);
        typewriterElapsed += delta;
        panelElapsed += delta;

        DialogueCatalog.Line line = currentLine();
        String subtitle = sanitize(line.text());
        int visibleChars = Math.min(subtitle.length(), (int) (typewriterElapsed * TYPEWRITER_CHARS_PER_SEC));
        boolean fullyRevealed = visibleChars >= subtitle.length();

        if (Gdx.input.isKeyJustPressed(Input.Keys.S)) {
            client.sendEvent(STORY_SKIP_EVENT, sequence.name());
            finishSequence();
            return;
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.E)
                || Gdx.input.isKeyJustPressed(Input.Keys.SPACE)
                || Gdx.input.isKeyJustPressed(Input.Keys.ENTER)
                || Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
            if (!fullyRevealed) {
                typewriterElapsed = subtitle.length() / TYPEWRITER_CHARS_PER_SEC + 1f;
            } else if (!localAdvanceRequested) {
                localAdvanceRequested = true;
                client.sendEvent(STORY_ADVANCE_EVENT, panelToken());
            }
        }

        boolean partnerOk = partnerAdvanceRequested
                || panelElapsed >= AUTO_READY_FALLBACK_SECONDS
                || client.getMatchMode() == MatchMode.SOLO
                || (game.getSession() != null && game.getSession().debugSplitScreen())
                || (game.getServer() != null && game.getServer().getConnectedPlayerCount() <= 1);

        drawCinematic(line, subtitle.substring(0, visibleChars), fullyRevealed, partnerOk);

        if (localAdvanceRequested && partnerOk) {
            nextPanel();
        }
    }

    private void drawCinematic(DialogueCatalog.Line line, String visibleSubtitle,
                               boolean fullyRevealed, boolean partnerOk) {
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        float width = Gdx.graphics.getWidth();
        float height = Gdx.graphics.getHeight();
        float fade = Math.min(1f, panelElapsed / FADE_IN_SECONDS);
        float zoom = 1f + Math.min(0.035f, panelElapsed * 0.0015f);

        batch.begin();
        if (cinematicTexture != null) {
            float drawWidth = width * zoom;
            float drawHeight = height * zoom;
            batch.setColor(1f, 1f, 1f, fade);
            batch.draw(cinematicTexture, (width - drawWidth) / 2f, (height - drawHeight) / 2f,
                    drawWidth, drawHeight);
        }

        batch.setColor(0.01f, 0.015f, 0.025f, 0.18f * fade);
        batch.draw(overlayPixel, 0f, 0f, width, height);

        // Jane lies close to the lower edge in the Map 1 discovery artwork.
        // Use a compact panel only for that shot so her face remains visible.
        boolean compactJaneDiscoveryPanel = "intro-03".equals(line.id());
        float subtitleHeight = compactJaneDiscoveryPanel
                ? Math.max(170f, height * 0.18f)
                : Math.max(235f, height * 0.285f);
        batch.setColor(0.008f, 0.012f, 0.022f, 0.88f * fade);
        batch.draw(overlayPixel, 0f, 0f, width, subtitleHeight);
        batch.setColor(0.32f, 0.68f, 0.86f, 0.9f * fade);
        batch.draw(overlayPixel, 0f, subtitleHeight - 2f, width, 2f);

        float margin = Math.max(48f, width * 0.075f);
        float textWidth = width - margin * 2f;

        titleFont.setColor(0.94f, 0.97f, 1f, fade);
        titleFont.draw(batch, line.title(), margin, height - 38f, textWidth, Align.left, false);

        speakerFont.setColor(0.96f, 0.72f, 0.22f, fade);
        speakerFont.draw(batch, line.speaker(), margin, subtitleHeight - 28f);

        subtitleFont.setColor(0.95f, 0.96f, 0.98f, fade);
        subtitleFont.draw(batch, visibleSubtitle, margin, subtitleHeight - 58f,
                textWidth, Align.left, true);

        speakerFont.setColor(0.70f, 0.78f, 0.86f, fade);
        String prompt = !fullyRevealed
                ? "[E] REVEAL SUBTITLE"
                : (!localAdvanceRequested
                ? "[E] CONTINUE"
                : (partnerOk ? "CONTINUING..." : "WAITING FOR PARTNER..."));
        speakerFont.draw(batch, prompt + "     [S] SKIP CINEMATIC", margin, 28f);
        speakerFont.draw(batch,
                String.format("%02d / %02d", currentPanelIndex + 1, panels.size()),
                width - margin - 72f, 28f);

        batch.setColor(Color.WHITE);
        batch.end();
    }

    private void nextPanel() {
        game.getAudioDirector().stopVoice();
        game.getAudioDirector().playEffect(SoundtrackCatalog.Effect.STORY_ADVANCE);
        currentPanelIndex++;
        if (currentPanelIndex >= panels.size()) {
            finishSequence();
            return;
        }
        loadCurrentPanel();
    }

    private void loadCurrentPanel() {
        localAdvanceRequested = false;
        partnerAdvanceRequested = false;
        typewriterElapsed = 0f;
        panelElapsed = 0f;

        if (cinematicTexture != null) {
            cinematicTexture.dispose();
            cinematicTexture = null;
        }

        DialogueCatalog.Line line = currentLine();
        String imagePath = line.imageAsset();
        try {
            if (Gdx.files.internal(imagePath).exists()) {
                cinematicTexture = new Texture(Gdx.files.internal(imagePath));
            } else if (Gdx.files.internal("story_intro.png").exists()) {
                Gdx.app.log("StoryPanelScreen", "Cinematic image missing: " + imagePath);
                cinematicTexture = new Texture(Gdx.files.internal("story_intro.png"));
            }
            if (cinematicTexture != null) {
                cinematicTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            }
        } catch (RuntimeException error) {
            Gdx.app.log("StoryPanelScreen", "Could not load cinematic image: " + imagePath);
        }

        game.getAudioDirector().playVoice(line);
    }

    private DialogueCatalog.Line currentLine() {
        return panels.get(currentPanelIndex);
    }

    private String panelToken() {
        return sequence.name() + ":" + currentPanelIndex;
    }

    private void finishSequence() {
        if (transitioning) return;
        transitioning = true;
        game.getAudioDirector().stopVoice();

        if (sequence == Sequence.ENDING) {
            bridge.notifyMatchEnded(new GameBridge.MatchOutcome("VICTORY", GameConstants.BOSS_LEVEL_NUMBER));
            if (bridge.hasLauncher()) {
                bridge.requestReturnToLauncher(() -> Gdx.app.postRunnable(Gdx.app::exit));
            } else {
                game.setScreen(new MainMenuScreen(game, client, bridge));
            }
        } else if (sequence == Sequence.INTRO) {
            game.setScreen(new GameScreen(game, client, bridge, levelContext));
        } else {
            game.setScreen(new LevelBriefingScreen(game, client, bridge, levelContext + 1));
        }
    }

    private static String sanitize(String text) {
        if (text == null) return "";
        return text.replace("—", " -- ")
                .replace("–", " - ")
                .replace("…", "...")
                .replace("“", "\"")
                .replace("”", "\"")
                .replace("‘", "'")
                .replace("’", "'");
    }

    @Override public void resize(int width, int height) { }
    @Override public void pause() { }
    @Override public void resume() { }

    @Override
    public void hide() {
        client.setOnEvent(null);
        game.getAudioDirector().stopVoice();
    }

    @Override
    public void dispose() {
        if (batch != null) batch.dispose();
        if (subtitleFont != null) subtitleFont.dispose();
        if (speakerFont != null) speakerFont.dispose();
        if (titleFont != null) titleFont.dispose();
        if (cinematicTexture != null) cinematicTexture.dispose();
        if (overlayPixel != null) overlayPixel.dispose();
    }
}
