package com.infectedhour.core.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;
import com.infectedhour.core.InfectedHourGame;
import com.infectedhour.core.bridge.GameBridge;
import com.infectedhour.core.level.LevelDefinition;
import com.infectedhour.core.net.GameClient;
import com.infectedhour.core.systems.BossPhaseSystem;

/** Playable skeleton for Level 3's hidden-laboratory boss encounter. */
public class BossScreen implements Screen {

    private static final float WIDTH = 1280f;
    private static final float HEIGHT = 720f;

    private final InfectedHourGame game;
    private final GameClient client;
    private final GameBridge bridge;
    private final BossPhaseSystem boss = new BossPhaseSystem();

    private SpriteBatch batch;
    private ShapeRenderer shapes;
    private BitmapFont font;
    private BitmapFont titleFont;
    private Matrix4 projection;
    private Texture mapTexture;
    private boolean titleCardDismissed;
    private int injectedSamples;
    private float coreHoldSeconds;
    private float victorySeconds;
    private boolean endingStarted;

    public BossScreen(InfectedHourGame game, GameClient client, GameBridge bridge) {
        this.game = game;
        this.client = client;
        this.bridge = bridge;
    }

    @Override
    public void show() {
        batch = new SpriteBatch();
        shapes = new ShapeRenderer();
        font = new BitmapFont();
        titleFont = new BitmapFont();
        titleFont.getData().setScale(2.6f);
        projection = new Matrix4().setToOrtho2D(0f, 0f, WIDTH, HEIGHT);
        if (Gdx.files.internal("map3.png").exists()) {
            try {
                mapTexture = new Texture(Gdx.files.internal("map3.png"));
                mapTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            } catch (Exception ignored) { }
        }
        if (game.isHost() && game.getServer() != null) {
            game.getServer().configureLevel(LevelDefinition.level3Boss());
        }
    }

    @Override
    public void render(float delta) {
        game.stepSimulation(delta);
        Gdx.gl.glClearColor(0.025f, 0.035f, 0.045f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        if (!titleCardDismissed) {
            drawTitleCard();
            if (Gdx.input.isKeyJustPressed(Input.Keys.E)
                    || Gdx.input.isKeyJustPressed(Input.Keys.SPACE)
                    || Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
                titleCardDismissed = true;
            }
            return;
        }

        if (mapTexture != null) {
            batch.setProjectionMatrix(projection);
            batch.begin();
            batch.setColor(0.40f, 0.45f, 0.48f, 1f);
            batch.draw(mapTexture, 0f, 0f, WIDTH, HEIGHT);
            batch.setColor(Color.WHITE);
            batch.end();
        }

        updateEncounter(delta);
        drawLaboratory();
        drawBoss();
        drawInterface();
    }

    private void updateEncounter(float delta) {
        switch (boss.getCurrentPhase()) {
            case SHIELD -> {
                if (Gdx.input.isKeyJustPressed(Input.Keys.E)) {
                    injectedSamples = Math.min(3, injectedSamples + 1);
                    if (injectedSamples == 3) boss.onShieldWeakened();
                }
            }
            case EXPOSURE -> {
                float damage = Gdx.input.isKeyJustPressed(Input.Keys.SPACE) ? 0.10f : 0f;
                boss.tickExposure(delta, damage);
                if (boss.getCurrentPhase() == BossPhaseSystem.Phase.SHIELD) injectedSamples = 0;
            }
            case CORE_DESTRUCTION -> {
                if (Gdx.input.isKeyPressed(Input.Keys.E)) {
                    coreHoldSeconds = Math.min(1.25f, coreHoldSeconds + delta);
                } else {
                    coreHoldSeconds = Math.max(0f, coreHoldSeconds - delta * 2f);
                }
                if (coreHoldSeconds >= 1.2f) {
                    boss.resolveCoreDestructionAttempt(true, true);
                }
            }
            case DEFEATED -> {
                victorySeconds += delta;
                if (victorySeconds >= 1.5f && !endingStarted) {
                    endingStarted = true;
                    game.setScreen(new StoryPanelScreen(
                            game, client, bridge, StoryPanelScreen.Sequence.ENDING, 3));
                }
            }
        }
    }

    private void drawTitleCard() {
        shapes.setProjectionMatrix(projection);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0.02f, 0.025f, 0.035f, 1f);
        shapes.rect(0f, 0f, WIDTH, HEIGHT);
        shapes.setColor(0.35f, 0.035f, 0.055f, 0.75f);
        shapes.rect(0f, 292f, WIDTH, 138f);
        shapes.end();

        batch.setProjectionMatrix(projection);
        batch.begin();
        font.setColor(0.62f, 0.66f, 0.70f, 1f);
        font.draw(batch, "LEVEL 3  /  HIDDEN LABORATORY", 500f, 470f);
        titleFont.setColor(0.91f, 0.20f, 0.24f, 1f);
        titleFont.draw(batch, "THE VIRUS HEART", 420f, 380f);
        font.setColor(Color.WHITE);
        font.draw(batch, "The source of the Ashgrove outbreak is awake.", 476f, 315f);
        font.setColor(0.91f, 0.69f, 0.16f, 1f);
        font.draw(batch, "[E] ENTER THE CONTAINMENT CHAMBER", 485f, 220f);
        batch.end();
    }

    private void drawLaboratory() {
        shapes.setProjectionMatrix(projection);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0.045f, 0.065f, 0.078f, 1f);
        shapes.rect(70f, 70f, WIDTH - 140f, HEIGHT - 140f);
        shapes.setColor(0.09f, 0.16f, 0.17f, 1f);
        shapes.rect(105f, 105f, WIDTH - 210f, HEIGHT - 210f);
        shapes.setColor(0.12f, 0.22f, 0.22f, 1f);
        for (int x = 125; x < 1160; x += 80) shapes.rect(x, 120f, 3f, 480f);
        for (int y = 120; y < 610; y += 70) shapes.rect(120f, y, 1040f, 3f);
        shapes.setColor(0.20f, 0.42f, 0.40f, 0.75f);
        shapes.rect(110f, 330f, 260f, 18f);
        shapes.rect(910f, 330f, 260f, 18f);
        shapes.end();
    }

    private void drawBoss() {
        float hp = boss.getCoreHpPct();
        shapes.setProjectionMatrix(projection);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0.16f, 0.02f, 0.055f, 1f);
        shapes.circle(WIDTH / 2f, HEIGHT / 2f, 126f, 64);
        shapes.setColor(0.42f + (1f - hp) * 0.25f, 0.04f, 0.10f, 1f);
        shapes.circle(WIDTH / 2f, HEIGHT / 2f, 82f, 64);
        shapes.setColor(0.92f, 0.16f, 0.24f, 0.85f);
        shapes.circle(WIDTH / 2f, HEIGHT / 2f, 38f + hp * 18f, 48);

        if (boss.getCurrentPhase() == BossPhaseSystem.Phase.SHIELD) {
            for (int i = 0; i < 3; i++) {
                float x = 430f + i * 210f;
                shapes.setColor(i < injectedSamples
                        ? new Color(0.25f, 0.80f, 0.48f, 1f)
                        : new Color(0.10f, 0.42f, 0.46f, 1f));
                shapes.circle(x, 170f, 28f, 32);
            }
        }
        shapes.end();
    }

    private void drawInterface() {
        shapes.setProjectionMatrix(projection);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0.08f, 0.09f, 0.11f, 1f);
        shapes.rect(390f, 645f, 500f, 20f);
        shapes.setColor(0.88f, 0.12f, 0.18f, 1f);
        shapes.rect(390f, 645f, 500f * boss.getCoreHpPct(), 20f);
        if (boss.getCurrentPhase() == BossPhaseSystem.Phase.CORE_DESTRUCTION) {
            shapes.setColor(0.08f, 0.09f, 0.11f, 1f);
            shapes.rect(440f, 150f, 400f, 18f);
            shapes.setColor(0.91f, 0.69f, 0.16f, 1f);
            shapes.rect(440f, 150f, 400f * Math.min(1f, coreHoldSeconds / 1.2f), 18f);
        }
        shapes.end();

        batch.setProjectionMatrix(projection);
        batch.begin();
        font.setColor(Color.WHITE);
        font.draw(batch, "VIRUS HEART  " + Math.round(boss.getCoreHpPct() * 100f) + "%", 555f, 690f);
        font.setColor(0.91f, 0.69f, 0.16f, 1f);
        switch (boss.getCurrentPhase()) {
            case SHIELD -> font.draw(batch,
                    "PHASE 1 - Press [E] to inject cure samples  (" + injectedSamples + "/3)", 420f, 90f);
            case EXPOSURE -> font.draw(batch,
                    "PHASE 2 - CORE EXPOSED! Press [SPACE] repeatedly to attack", 400f, 90f);
            case CORE_DESTRUCTION -> font.draw(batch,
                    "FINAL PHASE - Hold [E] to overload the infected core", 425f, 90f);
            case DEFEATED -> {
                titleFont.setColor(0.25f, 0.90f, 0.52f, 1f);
                titleFont.draw(batch, "CONTAINMENT RESTORED", 400f, 380f);
            }
        }
        batch.end();
    }

    @Override public void resize(int width, int height) { }
    @Override public void pause() { }
    @Override public void resume() { }
    @Override public void hide() { }

    @Override
    public void dispose() {
        if (batch != null) batch.dispose();
        if (shapes != null) shapes.dispose();
        if (font != null) font.dispose();
        if (titleFont != null) titleFont.dispose();
        if (mapTexture != null) mapTexture.dispose();
    }
}
