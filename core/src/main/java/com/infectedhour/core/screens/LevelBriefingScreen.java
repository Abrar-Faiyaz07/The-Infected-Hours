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
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.infectedhour.core.InfectedHourGame;
import com.infectedhour.core.audio.SoundtrackCatalog;
import com.infectedhour.core.bridge.GameBridge;
import com.infectedhour.core.level.LevelDefinition;
import com.infectedhour.core.level.LevelLoader;
import com.infectedhour.core.net.GameClient;
import com.infectedhour.shared.constants.GameConstants;
import com.infectedhour.shared.network.CharacterType;
import com.infectedhour.shared.network.MatchMode;

/**
 * Tactical Mission Briefing & Deployment Gate.
 * Matches the exact AAA sci-fi layout provided in CHATGPT / briefing_level1.png.
 */
public class LevelBriefingScreen implements Screen {

    private final InfectedHourGame game;
    private final GameClient client;
    private final GameBridge bridge;
    private final int levelNumber;
    private final LevelDefinition definition;

    private SpriteBatch batch;
    private ShapeRenderer shapes;
    private BitmapFont font;
    private BitmapFont subFont;
    private BitmapFont titleFont;
    private Texture layoutTexture;
    private Texture portraitTexture;
    private Texture pixelTexture;

    private volatile boolean localReady = false;
    private volatile boolean partnerReadyReceived = false;
    private float elapsed = 0f;
    private float readyElapsed = 0f;
    private static final float AUTO_ADVANCE_SECONDS = 2.0f;

    public LevelBriefingScreen(InfectedHourGame game, GameClient client, GameBridge bridge, int levelNumber) {
        this.game = game;
        this.client = client;
        this.bridge = bridge;
        this.levelNumber = levelNumber;
        this.definition = new LevelLoader().loadDefinition(levelNumber);
    }

    @Override
    public void show() {
        game.getAudioDirector().playMusic(SoundtrackCatalog.forLevel(levelNumber));
        batch = new SpriteBatch();
        shapes = new ShapeRenderer();
        font = new BitmapFont();
        font.getData().setScale(1.1f);
        subFont = new BitmapFont();
        subFont.getData().setScale(0.95f);
        titleFont = new BitmapFont();
        titleFont.getData().setScale(2.1f);

        Pixmap p = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        p.setColor(Color.WHITE);
        p.fill();
        pixelTexture = new Texture(p);
        p.dispose();

        // Exact high-res UI layout image from user's CHATGPT folder
        if (Gdx.files.internal("briefing_level1.png").exists()) {
            try {
                layoutTexture = new Texture(Gdx.files.internal("briefing_level1.png"));
                layoutTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            } catch (Exception ignored) {}
        } else if (Gdx.files.internal("briefing_bg_mockup.png").exists()) {
            try {
                layoutTexture = new Texture(Gdx.files.internal("briefing_bg_mockup.png"));
                layoutTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            } catch (Exception ignored) {}
        }

        // Operative portrait for Jane override
        boolean isJane = client.getLocalCharacter() == CharacterType.JANE;
        String portraitPath = isJane ? "player 2/female_character_select_v3.jpg" : "male_character_select.jpg";
        if (isJane && !Gdx.files.internal(portraitPath).exists()) {
            portraitPath = Gdx.files.internal("female/female_character_select_v3.jpg").exists()
                    ? "female/female_character_select_v3.jpg"
                    : "female_character_select.jpg";
        }
        if (Gdx.files.internal(portraitPath).exists()) {
            try {
                portraitTexture = new Texture(Gdx.files.internal(portraitPath));
                portraitTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            } catch (Exception ignored) {}
        }

        client.setOnEvent(event -> {
            if (GameConstants.EVENT_READY.equals(event.type)) {
                partnerReadyReceived = true;
            }
        });
    }

    @Override
    public void render(float delta) {
        elapsed += delta;
        game.stepSimulation(delta);

        boolean isSinglePlayer = (client.getMatchMode() == MatchMode.SOLO)
                || (game.getSession() != null && game.getSession().debugSplitScreen())
                || (game.getServer() != null && game.getServer().getConnectedPlayerCount() <= 1);

        boolean partnerGateOpen = partnerReadyReceived || isSinglePlayer || readyElapsed >= AUTO_ADVANCE_SECONDS;

        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE) || Gdx.input.isKeyJustPressed(Input.Keys.Q)) {
            bridge.requestReturnToLauncher(() -> Gdx.app.exit());
            return;
        }

        float width = Gdx.graphics.getWidth();
        float height = Gdx.graphics.getHeight();

        // Interactive button bounds
        float btnX = width * 0.605f;
        float btnY = height * 0.095f;
        float btnW = width * 0.355f;
        float btnH = height * 0.090f;

        float mouseX = Gdx.input.getX();
        float mouseY = height - Gdx.input.getY();
        boolean hoverBtn = (mouseX >= btnX && mouseX <= btnX + btnW && mouseY >= btnY && mouseY <= btnY + btnH);

        if (Gdx.input.isKeyJustPressed(Input.Keys.E)
                || Gdx.input.isKeyJustPressed(Input.Keys.ENTER)
                || Gdx.input.isKeyJustPressed(Input.Keys.SPACE)
                || (hoverBtn && Gdx.input.isButtonJustPressed(Input.Buttons.LEFT))) {
            if (!localReady) {
                localReady = true;
                readyElapsed = 0f;
                client.sendEvent(GameConstants.EVENT_READY, String.valueOf(levelNumber));
                if (isSinglePlayer) {
                    advance();
                    return;
                }
            } else {
                advance();
                return;
            }
        }

        if (localReady) {
            readyElapsed += delta;
            if (partnerGateOpen) {
                advance();
                return;
            }
        }

        Gdx.gl.glClearColor(0.015f, 0.02f, 0.035f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        batch.begin();

        if (layoutTexture != null) {
            // Draw exact 16:9 pixel-perfect layout from the user
            batch.draw(layoutTexture, 0f, 0f, width, height);

            // If playing as Jane, overlay the operative dossier section with Jane's portrait and stats
            boolean isJane = client.getLocalCharacter() == CharacterType.JANE;
            if (isJane) {
                float dossierX = width * 0.612f;
                float dossierY = height * 0.220f;
                float dossierW = width * 0.340f;
                float dossierH = height * 0.660f;

                // Translucent background to cover Elric graphic cleanly
                batch.setColor(0.03f, 0.05f, 0.08f, 0.95f);
                batch.draw(pixelTexture, dossierX, dossierY, dossierW, dossierH);
                batch.setColor(Color.WHITE);

                // Jane's tactical portrait
                float portSize = Math.min(dossierW * 0.72f, dossierH * 0.48f);
                float portX = dossierX + (dossierW - portSize) / 2f;
                float portY = dossierY + dossierH - portSize - 40f;
                if (portraitTexture != null) {
                    batch.draw(portraitTexture, portX, portY, portSize, portSize);
                }

                // Jane's Dossier Info
                font.setColor(0.910f, 0.690f, 0.165f, 1f); // Accent gold
                font.draw(batch, "JANE - TACTICAL SCOUT", dossierX + 25f, portY - 20f);

                subFont.setColor(0.85f, 0.90f, 0.95f, 1f);
                subFont.draw(batch, "Role: High Agility & Objective Runner", dossierX + 25f, portY - 48f);
                subFont.draw(batch, "Affiliation: Undercover Government Agent", dossierX + 25f, portY - 72f);
                subFont.draw(batch, "Gear: Tactical Katana + Medkit Kit", dossierX + 25f, portY - 96f);
            }

            // Interactive Pulsing Ready Button
            float pulse = (float) Math.sin(elapsed * 4.0f) * 0.25f + 0.75f;
            if (localReady) {
                // Glow green when ready
                batch.setColor(0.05f, 0.40f, 0.15f, 0.90f);
                batch.draw(pixelTexture, btnX, btnY, btnW, btnH);
                batch.setColor(Color.WHITE);

                font.setColor(Color.LIME);
                font.draw(batch, ">>   [ READY FOR INSERTION ]   <<", btnX + (btnW / 2f) - 130f, btnY + btnH - 18f);

                subFont.setColor(Color.WHITE);
                subFont.draw(batch, partnerLine(isSinglePlayer, partnerGateOpen), btnX + (btnW / 2f) - 140f, btnY + 22f);
            } else if (hoverBtn) {
                // Subtle bright highlight when hovering
                batch.setColor(0.910f, 0.690f, 0.165f, 0.18f * pulse);
                batch.draw(pixelTexture, btnX, btnY, btnW, btnH);
                batch.setColor(Color.WHITE);
            }

        } else {
            // Fallback Vector Rendering if image is missing
            drawFallbackVectorUI(width, height, btnX, btnY, btnW, btnH, partnerGateOpen);
        }

        batch.end();

        if (localReady && partnerGateOpen) {
            advance();
        }
    }

    private void drawFallbackVectorUI(float width, float height, float btnX, float btnY, float btnW, float btnH, boolean partnerGateOpen) {
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapes.begin(ShapeRenderer.ShapeType.Filled);

        float leftColW = width * 0.55f;
        float marginX = 40f;
        float cardTop = height - 120f;
        float cardH = height - 200f;

        // Top Banner
        shapes.setColor(0.06f, 0.08f, 0.12f, 0.95f);
        shapes.rect(0f, height - 90f, width, 90f);
        shapes.setColor(0.910f, 0.690f, 0.165f, 1f);
        shapes.rect(0f, height - 94f, width, 4f);

        // Cards
        shapes.setColor(0.04f, 0.06f, 0.09f, 0.92f);
        shapes.rect(marginX, 75f, leftColW, cardH);
        shapes.rect(btnX, 75f, btnW, cardH);

        shapes.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);

        titleFont.setColor(0.910f, 0.690f, 0.165f, 1f);
        titleFont.draw(batch, "OPERATION ASHGROVE // LEVEL " + levelNumber + " - " + definition.name().toUpperCase(), marginX, height - 32f);

        font.setColor(Color.WHITE);
        font.draw(batch, localReady ? "[ READY - LAUNCHING MISSION ]" : "[ PRESS E OR ENTER TO DEPLOY ]", btnX + 30f, btnY + 40f);
    }

    private String partnerLine(boolean isSinglePlayer, boolean partnerGateOpen) {
        if (isSinglePlayer) {
            return "Solo Deployment - Ready when you are";
        }
        if (partnerReadyReceived) {
            return "Partner: READY";
        }
        if (partnerGateOpen) {
            return "Launching mission...";
        }
        int dots = ((int) (elapsed * 2f)) % 4;
        return "Click or [SPACE] to Force Deploy | Waiting" + ".".repeat(dots);
    }

    private void advance() {
        if (levelNumber == 1) {
            game.setScreen(new StoryPanelScreen(game, client, bridge, StoryPanelScreen.Sequence.INTRO, levelNumber));
        } else if (levelNumber == 6) {
            CharacterType pref = (game.getSession() != null) ? game.getSession().preferredCharacter() : CharacterType.ELRIC;
            game.setScreen(new BossScreen(game, client, bridge, pref));
        } else {
            game.setScreen(new GameScreen(game, client, bridge, levelNumber));
        }
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
        if (shapes != null) shapes.dispose();
        if (font != null) font.dispose();
        if (subFont != null) subFont.dispose();
        if (titleFont != null) titleFont.dispose();
        if (layoutTexture != null) layoutTexture.dispose();
        if (portraitTexture != null) portraitTexture.dispose();
        if (pixelTexture != null) pixelTexture.dispose();
    }
}
