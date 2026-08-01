package com.infectedhour.core.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.infectedhour.core.InfectedHourGame;
import com.infectedhour.core.bridge.GameBridge;
import com.infectedhour.core.level.LevelDefinition;
import com.infectedhour.core.level.LevelLoader;
import com.infectedhour.core.net.GameClient;
import com.infectedhour.shared.constants.GameConstants;
import com.infectedhour.shared.network.CharacterType;
import com.infectedhour.shared.network.InputCommand;
import com.infectedhour.shared.network.WorldSnapshot;

import java.util.HashMap;
import java.util.Map;

public class GameScreen implements Screen {

    private static final float PIXELS_PER_TILE = 48f;

    private final InfectedHourGame game;
    private final GameClient client;
    private final GameBridge bridge;
    private final int levelNumber;

    private SpriteBatch batch;
    private ShapeRenderer shapes;
    private BitmapFont font;

    // Map & Camera
    private OrthographicCamera camera;
    private Texture mapTexture;

    // Walk Sprites
    private Texture playerTexture;
    private TextureRegion[][] playerFrames;
    private int frameWidth;
    private int frameHeight;

    // Idle Sprites
    private Texture idleTexture;
    private TextureRegion[][] idleFrames;
    private int idleFrameWidth;
    private int idleFrameHeight;

    // Animation State Tracker
    private final Map<String, PlayerAnimState> animStates = new HashMap<>();

    private static class PlayerAnimState {
        float lastX = -1f, lastY = -1f;
        int currentRow = 0; // 0: Down, 1: Left, 2: Right, 3: Up
        int currentColumn = 0;
        float stateTime = 0f;
    }

    private boolean paused = false;
    private String partnerBanner = null;
    private float partnerBannerSecondsLeft = 0f;

    public GameScreen(InfectedHourGame game, GameClient client, GameBridge bridge, int levelNumber) {
        this.game = game;
        this.client = client;
        this.bridge = bridge;
        this.levelNumber = levelNumber;
    }

    @Override
    public void show() {
        batch = new SpriteBatch();
        shapes = new ShapeRenderer();
        font = new BitmapFont();

        camera = new OrthographicCamera();
        camera.setToOrtho(false, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        // Setup fake level data for the server
        LevelLoader levelLoader = new LevelLoader();
        LevelDefinition def = levelLoader.loadDefinition(levelNumber);
        levelLoader.loadMap(def);

        if (game.isHost()) {
            game.getServer().setMapWidthInTiles(levelLoader.getMapWidthInTiles());
        }

        // Load your map.png directly
        mapTexture = new Texture(Gdx.files.internal("map.png"));

        // Setup player WALKING sprites (8 columns, 4 rows)
        playerTexture = new Texture(Gdx.files.internal("player.png"));
        int walkCols = 8;
        int walkRows = 4;
        frameWidth = playerTexture.getWidth() / walkCols;
        frameHeight = playerTexture.getHeight() / walkRows;
        playerFrames = TextureRegion.split(playerTexture, frameWidth, frameHeight);

        // Setup player IDLE sprites (Assuming 8 columns, 4 rows just like walking)
        idleTexture = new Texture(Gdx.files.internal("player_idle.png"));
        int idleCols = 8;
        int idleRows = 4;
        idleFrameWidth = idleTexture.getWidth() / idleCols;
        idleFrameHeight = idleTexture.getHeight() / idleRows;
        idleFrames = TextureRegion.split(idleTexture, idleFrameWidth, idleFrameHeight);

        client.setOnEvent(event -> {
            if (event.type == null) return;
            switch (event.type) {
                case GameConstants.EVENT_PARTNER_DISCONNECTED -> showBanner("Partner disconnected — waiting…");
                case GameConstants.EVENT_PARTNER_RECONNECTED -> showBanner("Partner reconnected");
                case GameConstants.EVENT_CONVERTED_TO_SOLO -> showBanner("Continuing solo");
                default -> { }
            }
        });
    }

    @Override
    public void render(float delta) {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            paused = !paused;
            client.sendEvent(paused ? GameConstants.EVENT_PAUSE : GameConstants.EVENT_RESUME, "");
        }

        game.stepSimulation(delta);

        Gdx.gl.glClearColor(0.055f, 0.078f, 0.125f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        if (!paused) {
            client.sendInputIfDue(readLocalInput(), delta);
        }

        WorldSnapshot snapshot = client.getInterpolatedSnapshot(System.currentTimeMillis());

        if (snapshot != null) {
            WorldSnapshot.PlayerState me = client.findLocalPlayer(snapshot);
            if (me != null) {
                camera.position.set(me.x * PIXELS_PER_TILE, me.y * PIXELS_PER_TILE, 0);
                camera.update();
            }

            batch.setProjectionMatrix(camera.combined);
            batch.begin();

            // 1. Draw the map background starting at coordinates 0,0
            batch.draw(mapTexture, 0, 0);

            // 2. Draw Entities with Delta
            drawWorld(snapshot, delta);

            batch.end();

            // 3. Draw enemies (shapes need to be drawn outside of batch)
            shapes.setProjectionMatrix(camera.combined);
            shapes.begin(ShapeRenderer.ShapeType.Filled);
            if (snapshot.enemies != null) {
                shapes.setColor(0.482f, 0.310f, 0.651f, 1f);
                for (WorldSnapshot.EnemyState enemy : snapshot.enemies) {
                    shapes.rect(enemy.x * PIXELS_PER_TILE, enemy.y * PIXELS_PER_TILE,
                            PIXELS_PER_TILE * 0.8f, PIXELS_PER_TILE * 0.8f);
                }
            }
            shapes.end();
        }

        drawHud(snapshot, delta);

        if (paused) drawPauseOverlay();
    }

    private void drawWorld(WorldSnapshot snapshot, float delta) {
        if (snapshot.players != null) {
            for (WorldSnapshot.PlayerState player : snapshot.players) {

                PlayerAnimState anim = animStates.computeIfAbsent(player.playerId, k -> new PlayerAnimState());

                if (anim.lastX == -1f) {
                    anim.lastX = player.x;
                    anim.lastY = player.y;
                }

                float dx = player.x - anim.lastX;
                float dy = player.y - anim.lastY;

                boolean moving = Math.abs(dx) > 0.001f || Math.abs(dy) > 0.001f;
                TextureRegion currentFrame;

                // Determine which half of the sprite sheet to use (Elric vs Jane)
                int characterOffset = (player.character == CharacterType.ELRIC) ? 0 : 4;

                if (moving) {
                    // Determine facing direction (Row)
                    // If moving horizontally AT ALL, prioritize Left/Right to prevent diagonal flickering
                    if (Math.abs(dx) > 0.005f) {
                        anim.currentRow = dx > 0 ? 2 : 1; // 2: Right, 1: Left
                    } else {
                        anim.currentRow = dy > 0 ? 3 : 0; // 3: Up, 0: Down
                    }

                    // Advance WALKING animation frame every 150ms
                    anim.stateTime += delta;
                    if (anim.stateTime > 0.15f) {
                        anim.currentColumn = (anim.currentColumn + 1) % 4;
                        anim.stateTime = 0f;
                    }

                    currentFrame = playerFrames[anim.currentRow][anim.currentColumn + characterOffset];
                } else {
                    // ANIMATED IDLE: Slowly bounce between frame 0 and frame 1 every 500ms
                    anim.stateTime += delta;
                    if (anim.stateTime > 0.5f) {
                        anim.currentColumn = (anim.currentColumn == 0) ? 1 : 0;
                        anim.stateTime = 0f;
                    }

                    currentFrame = idleFrames[anim.currentRow][anim.currentColumn + characterOffset];
                }

                anim.lastX = player.x;
                anim.lastY = player.y;

                float drawX = (player.x * PIXELS_PER_TILE) - (frameWidth / 2f);
                float drawY = (player.y * PIXELS_PER_TILE) - (frameHeight / 2f);

                batch.draw(currentFrame, drawX, drawY);
            }
        }
    }

    private void drawHud(WorldSnapshot snapshot, float delta) {
        float top = Gdx.graphics.getHeight() - 20f;
        if (partnerBannerSecondsLeft > 0f) partnerBannerSecondsLeft -= delta;

        batch.begin();
        font.setColor(Color.WHITE);
        font.draw(batch, "LEVEL " + levelNumber + "   |   " + client.getMatchMode()
                + "   |   " + (game.isHost() ? "HOST" : "CLIENT"), 20f, top);

        if (snapshot == null) {
            font.setColor(Color.LIGHT_GRAY);
            font.draw(batch, "Waiting for the first snapshot from the host…", 20f, top - 24f);
        } else {
            font.draw(batch, String.format("global contamination %.1f%%   tick %d   players %d",
                            snapshot.globalContaminationPct, snapshot.serverTick,
                            snapshot.players == null ? 0 : snapshot.players.size()),
                    20f, top - 24f);

            WorldSnapshot.PlayerState me = client.findLocalPlayer(snapshot);
            if (me != null) {
                font.draw(batch, String.format("you: %s   hp %.0f   contamination %.0f%%%s",
                                me.character, me.hp, me.personalContaminationPct,
                                me.downed ? "   DOWNED " + me.reviveSecondsRemaining + "s" : ""),
                        20f, top - 48f);
            }
        }

        if (partnerBannerSecondsLeft > 0f && partnerBanner != null) {
            font.setColor(0.910f, 0.353f, 0.310f, 1f);
            font.draw(batch, partnerBanner, 20f, top - 80f);
        }

        font.setColor(Color.GRAY);
        font.draw(batch, "WASD move   E interact   SPACE attack   SHIFT ability   ESC pause", 20f, 30f);
        batch.end();
    }

    private void drawPauseOverlay() {
        Gdx.gl.glEnable(GL20.GL_BLEND);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0f, 0f, 0f, 0.6f);
        shapes.rect(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        shapes.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);

        batch.begin();
        font.setColor(Color.WHITE);
        font.draw(batch, "PAUSED — ESC to resume",
                Gdx.graphics.getWidth() / 2f - 90f, Gdx.graphics.getHeight() / 2f);
        batch.end();
    }

    private InputCommand readLocalInput() {
        InputCommand input = new InputCommand();
        input.moveX = (Gdx.input.isKeyPressed(Input.Keys.D) ? 1 : 0) - (Gdx.input.isKeyPressed(Input.Keys.A) ? 1 : 0);
        input.moveY = (Gdx.input.isKeyPressed(Input.Keys.W) ? 1 : 0) - (Gdx.input.isKeyPressed(Input.Keys.S) ? 1 : 0);
        input.attackPressed = Gdx.input.isKeyJustPressed(Input.Keys.SPACE);
        input.interactHeld = Gdx.input.isKeyPressed(Input.Keys.E);
        input.interactPressed = Gdx.input.isKeyJustPressed(Input.Keys.E);
        input.abilityPressed = Gdx.input.isKeyJustPressed(Input.Keys.SHIFT_LEFT);
        input.dropPressed = Gdx.input.isKeyJustPressed(Input.Keys.Q);
        return input;
    }

    private void showBanner(String message) {
        partnerBanner = message;
        partnerBannerSecondsLeft = 5f;
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
        if (mapTexture != null) mapTexture.dispose();
        if (playerTexture != null) playerTexture.dispose();
        if (idleTexture != null) idleTexture.dispose();
    }
}