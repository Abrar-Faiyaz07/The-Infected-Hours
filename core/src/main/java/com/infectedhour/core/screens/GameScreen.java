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
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.infectedhour.core.InfectedHourGame;
import com.infectedhour.core.bridge.GameBridge;
import com.infectedhour.core.level.LevelDefinition;
import com.infectedhour.core.level.LevelLoader;
import com.infectedhour.core.level.TileMap;
import com.infectedhour.core.net.GameClient;
import com.infectedhour.shared.constants.GameConstants;
import com.infectedhour.shared.network.CharacterType;
import com.infectedhour.shared.network.InputCommand;
import com.infectedhour.shared.network.WorldSnapshot;

import java.util.HashMap;
import java.util.Map;

public class GameScreen implements Screen {

    private static final float PIXELS_PER_TILE = 48f;

    // --- map.png alignment (see core/src/main/resources/maps/level1.map) ---
    // The map art is drawn on a 41 px grid whose origin sits at image pixel
    // (14, 28). The world uses PIXELS_PER_TILE, so the texture is scaled and
    // shifted to make one art tile land exactly on one collision tile.
    // Without this the drawn walls and the collision grid drift apart, and
    // players stop at walls that are not where they look.
    private static final float MAP_ART_TILE_PX = 41.2667f;
    private static final float MAP_ART_ORIGIN_X_PX = 32f;
    private static final float MAP_ART_ORIGIN_Y_TOP_PX = 32f;
    private static final int MAP_ART_ROWS = 33;

    /**
     * Transparent padding below the character's feet inside a sprite frame.
     * Sprites are anchored by their feet because that is the entity's ground
     * point — the spot collision actually tests.
     */
    private static final float SPRITE_FEET_INSET_PX = 13f;

    // Virtual resolution for crisp pixel-art scaling
    private static final float VIRTUAL_WIDTH = 1280f;
    private static final float VIRTUAL_HEIGHT = 720f;

    private final InfectedHourGame game;
    private final GameClient client;
    private final GameBridge bridge;
    private final int levelNumber;

    /** Level collision grid, kept for the F1 debug overlay. */
    private TileMap tileMap;
    /** F1 draws the collision cells over the map, so misaligned geometry is visible rather than guessed at. */
    private boolean showCollisionOverlay = false;

    private SpriteBatch batch;
    private ShapeRenderer shapes;
    private BitmapFont font;

    private OrthographicCamera camera;
    private Viewport viewport;
    private Texture mapTexture;

    private Texture playerTexture;
    private TextureRegion[][] playerFrames;
    private int frameWidth, frameHeight;

    private Texture idleTexture;
    private TextureRegion[][] idleFrames;
    private int idleFrameWidth, idleFrameHeight;

    private Texture zombieTexture;
    private TextureRegion[][] zombieFrames;
    private int zombieFrameWidth, zombieFrameHeight;

    private float middleZombieX = 30f;
    private float middleZombieY = 20f;
    private boolean middleZombieChasing = false;

    private final Map<String, PlayerAnimState> animStates = new HashMap<>();
    private final ZombieAnimState zombieAnim = new ZombieAnimState();

    private float stamina = 100f;
    private final float maxStamina = 100f;

    private float biteCooldown = 1.0f;
    private boolean isBeingBitten = false;

    // ── INVENTORY VARIABLES ──
    private Texture inventoryTexture;
    private boolean isInventoryOpen = false;
    private final int INVENTORY_COLS = 4;
    private final int INVENTORY_ROWS = 4;
    private final float SLOT_SIZE = 50f;
    private final float SLOT_SPACING = 10f;
    private final float GRID_OFFSET_X = 60f;
    private final float GRID_OFFSET_Y = 80f;
    private boolean[] mockSlots;

    private static class PlayerAnimState {
        float lastX = -1f, lastY = -1f;
        int currentRow = 0;
        int currentColumn = 0;
        float stateTime = 0f;
    }

    private static class ZombieAnimState {
        int currentRow = 1;
        int currentColumn = 0;
        float stateTime = 0f;
    }

    private boolean paused = false;
    private String partnerBanner = null;
    private float partnerBannerSecondsLeft = 0f;

    /**
     * Draws the level art so its tile grid coincides with the collision grid.
     *
     * <p>The texture is scaled by {@code PIXELS_PER_TILE / MAP_ART_TILE_PX} and
     * shifted so that the art's grid origin lands on world (0, 0). Drawing it at
     * plain (0, 0) unscaled would put a 41 px art tile under a 48 px world tile
     * and the two would drift a whole tile apart within ~7 tiles.
     */
    /**
     * Debug overlay (F1): fills every blocked collision cell in translucent red.
     *
     * <p>Worth keeping. Collision bugs in a traced map are nearly impossible to
     * diagnose from behaviour alone — "I got stuck here" could be a misaligned
     * grid, a misclassified tile, or a sprite drawn at the wrong offset. With
     * this on, all three are obvious at a glance.
     */
    private void drawCollisionOverlay() {
        if (tileMap == null) {
            return;
        }
        float cell = PIXELS_PER_TILE * tileMap.getCellSize();

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapes.setProjectionMatrix(camera.combined);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(1f, 0f, 0f, 0.35f);

        // Only the cells on screen — the full grid is 135x99 and redrawing all
        // of it every frame would cost more than the overlay is worth.
        float halfW = camera.viewportWidth * 0.5f + cell;
        float halfH = camera.viewportHeight * 0.5f + cell;
        int minCellX = Math.max(0, tileMap.toCell((camera.position.x - halfW) / PIXELS_PER_TILE));
        int maxCellX = Math.min(tileMap.getCollisionWidth() - 1,
                tileMap.toCell((camera.position.x + halfW) / PIXELS_PER_TILE));
        int minCellY = Math.max(0, tileMap.toCell((camera.position.y - halfH) / PIXELS_PER_TILE));
        int maxCellY = Math.min(tileMap.getCollisionHeight() - 1,
                tileMap.toCell((camera.position.y + halfH) / PIXELS_PER_TILE));

        for (int cellY = minCellY; cellY <= maxCellY; cellY++) {
            for (int cellX = minCellX; cellX <= maxCellX; cellX++) {
                if (!tileMap.isCellWalkable(cellX, cellY)) {
                    shapes.rect(cellX * cell, cellY * cell, cell, cell);
                }
            }
        }
        shapes.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    private void drawMapAlignedToCollisionGrid() {
        float scale = PIXELS_PER_TILE / MAP_ART_TILE_PX;

        // World y is up, image rows count down, so the grid's bottom edge is
        // measured from the bottom of the texture.
        float gridBottomFromTexBottomPx =
                mapTexture.getHeight() - (MAP_ART_ORIGIN_Y_TOP_PX + MAP_ART_ROWS * MAP_ART_TILE_PX);

        batch.draw(mapTexture,
                -MAP_ART_ORIGIN_X_PX * scale,
                -gridBottomFromTexBottomPx * scale,
                mapTexture.getWidth() * scale,
                mapTexture.getHeight() * scale);
    }

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
        // Use FitViewport to maintain crisp pixel art scaling across all resolutions and fullscreen
        viewport = new FitViewport(VIRTUAL_WIDTH, VIRTUAL_HEIGHT, camera);
        viewport.apply();

        LevelLoader levelLoader = new LevelLoader();
        LevelDefinition def = levelLoader.loadDefinition(levelNumber);
        tileMap = levelLoader.loadMap(def);

        // Only the host simulates, so only the host needs the collision grid —
        // clients render interpolated snapshots and never resolve collision.
        if (game.isHost()) {
            game.getServer().loadTileMap(tileMap);
        }

        mapTexture = new Texture(Gdx.files.internal("map.png"));
        mapTexture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);

        playerTexture = new Texture(Gdx.files.internal("player.png"));
        playerTexture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        frameWidth = playerTexture.getWidth() / 8;
        frameHeight = playerTexture.getHeight() / 4;
        playerFrames = TextureRegion.split(playerTexture, frameWidth, frameHeight);

        idleTexture = new Texture(Gdx.files.internal("player_idle.png"));
        idleTexture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        idleFrameWidth = idleTexture.getWidth() / 8;
        idleFrameHeight = idleTexture.getHeight() / 4;
        idleFrames = TextureRegion.split(idleTexture, idleFrameWidth, idleFrameHeight);

        zombieTexture = new Texture(Gdx.files.internal("zombie.png"));
        zombieTexture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        zombieFrameWidth = zombieTexture.getWidth() / 8;
        zombieFrameHeight = zombieTexture.getHeight() / 4;
        zombieFrames = TextureRegion.split(zombieTexture, zombieFrameWidth, zombieFrameHeight);

        // ── LOAD & FILTER INVENTORY BACKGROUND TEXTURE ──
        inventoryTexture = new Texture(Gdx.files.internal("inventory.png"));
        inventoryTexture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        mockSlots = new boolean[INVENTORY_COLS * INVENTORY_ROWS];

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
        WorldSnapshot snapshot = client.getInterpolatedSnapshot(System.currentTimeMillis());

        if (snapshot != null) {
            WorldSnapshot.PlayerState me = client.findLocalPlayer(snapshot);
            if (me != null && (me.downed || me.hp <= 0f)) {
                Gdx.gl.glClearColor(0f, 0f, 0f, 1f);
                Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

                Matrix4 hudMatrix = new Matrix4().setToOrtho2D(0, 0, VIRTUAL_WIDTH, VIRTUAL_HEIGHT);
                batch.setProjectionMatrix(hudMatrix);
                batch.begin();
                font.setColor(Color.WHITE);
                font.draw(batch, "You are dead",
                        VIRTUAL_WIDTH / 2f - 40f, VIRTUAL_HEIGHT / 2f);
                font.draw(batch, "Press any key to return to Main Menu",
                        VIRTUAL_WIDTH / 2f - 115f, VIRTUAL_HEIGHT / 2f - 30f);
                batch.end();

                if (Gdx.input.isKeyJustPressed(Input.Keys.ANY_KEY) || Gdx.input.isButtonPressed(Input.Buttons.LEFT)) {
                    if (game.isHost() && game.getServer() != null) {
                        game.getServer().stop();
                    }
                    game.setScreen(new BossScreen(game, client, bridge));
                }
                return;
            }
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            paused = !paused;
            client.sendEvent(paused ? GameConstants.EVENT_PAUSE : GameConstants.EVENT_RESUME, "");
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.F1)) {
            showCollisionOverlay = !showCollisionOverlay;
        }

        // ── TOGGLE INVENTORY WITH 'I' KEY ──
        if (Gdx.input.isKeyJustPressed(Input.Keys.I)) {
            isInventoryOpen = !isInventoryOpen;
        }

        // ── HANDLE MOUSE CLICKS INSIDE INVENTORY SLOTS (Scaled to Virtual Viewport) ──
        if (isInventoryOpen && Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
            // Translate physical mouse screen coordinates into virtual world/viewport coordinates
            com.badlogic.gdx.math.Vector3 mouseCoords = new com.badlogic.gdx.math.Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
            viewport.unproject(mouseCoords);

            float mouseX = mouseCoords.x;
            float mouseY = mouseCoords.y;

            float invX = Math.round((VIRTUAL_WIDTH - inventoryTexture.getWidth()) / 2f);
            float invY = Math.round((VIRTUAL_HEIGHT - inventoryTexture.getHeight()) / 2f);

            for (int i = 0; i < mockSlots.length; i++) {
                int col = i % INVENTORY_COLS;
                int row = i / INVENTORY_COLS;
                int invertedRow = (INVENTORY_ROWS - 1) - row;

                float slotX = invX + GRID_OFFSET_X + (col * (SLOT_SIZE + SLOT_SPACING));
                float slotY = invY + GRID_OFFSET_Y + (invertedRow * (SLOT_SIZE + SLOT_SPACING));

                if (mouseX >= slotX && mouseX <= slotX + SLOT_SIZE &&
                        mouseY >= slotY && mouseY <= slotY + SLOT_SIZE) {
                    System.out.println("Clicked inventory slot: " + i);
                    break;
                }
            }
        }

        game.stepSimulation(delta);

        Gdx.gl.glClearColor(0.055f, 0.078f, 0.125f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        if (!paused) {
            client.sendInputIfDue(readLocalInput(), delta);
        }

        boolean isSprinting = Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT);
        if (isSprinting && stamina > 0f) {
            stamina = Math.max(0f, stamina - (45f * delta));
        } else if (!isSprinting && stamina < maxStamina) {
            stamina = Math.min(maxStamina, stamina + (30f * delta));
        }

        if (snapshot != null) {
            WorldSnapshot.PlayerState me = client.findLocalPlayer(snapshot);
            if (me != null) {
                camera.position.set(
                        Math.round(me.x * PIXELS_PER_TILE),
                        Math.round(me.y * PIXELS_PER_TILE),
                        0
                );
                camera.update();
            }

            batch.setProjectionMatrix(camera.combined);
            batch.begin();
            drawMapAlignedToCollisionGrid();

            drawWorld(snapshot, delta);

            if (me != null) {
                float aggroRadiusTiles = 5.0f;
                float loseRadiusTiles = 8.0f;
                float zombieSpeed = 2.0f;

                float midDistX = me.x - middleZombieX;
                float midDistY = me.y - middleZombieY;
                float midDistance = (float) Math.sqrt(midDistX * midDistX + midDistY * midDistY);

                if (midDistance <= 0.8f && !me.downed && me.hp > 0f) {
                    isBeingBitten = true;
                    biteCooldown -= delta;
                    if (biteCooldown <= 0f) {
                        client.sendEvent("ZOMBIE_BITE_DAMAGE", "33.4");
                        biteCooldown = 1.0f;
                    }
                } else {
                    isBeingBitten = false;
                    biteCooldown = 1.0f;
                }

                if (!middleZombieChasing && midDistance <= aggroRadiusTiles) {
                    middleZombieChasing = true;
                } else if (middleZombieChasing && midDistance >= loseRadiusTiles) {
                    middleZombieChasing = false;
                }

                TextureRegion zombieFrame;

                if (middleZombieChasing && midDistance > 0.5f) {
                    float moveX = (midDistX / midDistance) * zombieSpeed * delta;
                    float moveY = (midDistY / midDistance) * zombieSpeed * delta;

                    middleZombieX += moveX;
                    middleZombieY += moveY;

                    if (Math.abs(midDistX) > Math.abs(midDistY)) {
                        zombieAnim.currentRow = midDistX > 0 ? 3 : 2;
                    } else {
                        zombieAnim.currentRow = midDistY > 0 ? 0 : 1;
                    }

                    zombieAnim.stateTime += delta;
                    if (zombieAnim.stateTime > 0.15f) {
                        zombieAnim.currentColumn = (zombieAnim.currentColumn + 1) % 8;
                        zombieAnim.stateTime = 0f;
                    }

                    zombieFrame = zombieFrames[zombieAnim.currentRow][zombieAnim.currentColumn];
                } else {
                    zombieAnim.stateTime += delta;
                    if (zombieAnim.stateTime > 0.5f) {
                        zombieAnim.currentColumn = (zombieAnim.currentColumn + 1) % 8;
                        zombieAnim.stateTime = 0f;
                    }

                    zombieFrame = zombieFrames[zombieAnim.currentRow][zombieAnim.currentColumn];
                }

                float midDrawX = Math.round((middleZombieX * PIXELS_PER_TILE) - (zombieFrameWidth / 2f));
                float midDrawY = Math.round((middleZombieY * PIXELS_PER_TILE) - (zombieFrameHeight / 2f));
                batch.draw(zombieFrame, midDrawX, midDrawY);
            }

            // ── DRAW INVENTORY OVERLAY ON TOP ──
            if (isInventoryOpen) {
                Matrix4 hudMatrix = new Matrix4().setToOrtho2D(0, 0, VIRTUAL_WIDTH, VIRTUAL_HEIGHT);
                batch.setProjectionMatrix(hudMatrix);

                int invX = Math.round((VIRTUAL_WIDTH - inventoryTexture.getWidth()) / 2f);
                int invY = Math.round((VIRTUAL_HEIGHT - inventoryTexture.getHeight()) / 2f);

                batch.draw(inventoryTexture, invX, invY);
            }

            batch.end();

            // After the world, before the HUD: the overlay belongs in world
            // space so it lines up with the map, but must not tint the HUD.
            if (showCollisionOverlay) {
                drawCollisionOverlay();
            }
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

                int characterOffset = (player.character == CharacterType.ELRIC) ? 0 : 4;

                if (moving) {
                    if (Math.abs(dx) > 0.005f) {
                        anim.currentRow = dx > 0 ? 2 : 1;
                    } else {
                        anim.currentRow = dy > 0 ? 3 : 0;
                    }

                    anim.stateTime += delta;
                    if (anim.stateTime > 0.15f) {
                        anim.currentColumn = (anim.currentColumn + 1) % 4;
                        anim.stateTime = 0f;
                    }
                    currentFrame = playerFrames[anim.currentRow][anim.currentColumn + characterOffset];
                } else {
                    anim.stateTime += delta;
                    if (anim.stateTime > 0.5f) {
                        anim.currentColumn = (anim.currentColumn == 0) ? 1 : 0;
                        anim.stateTime = 0f;
                    }
                    currentFrame = idleFrames[anim.currentRow][anim.currentColumn + characterOffset];
                }

                anim.lastX = player.x;
                anim.lastY = player.y;

                // Anchor the sprite by its FEET, not its centre. An entity's
                // position is its ground point and is what collision tests, so
                // centring a 100px-tall sprite on a 48px tile drew the feet a
                // whole tile below the collider: the player appeared to walk
                // through low walls and to stop at invisible ones.
                float drawX = Math.round((player.x * PIXELS_PER_TILE) - (frameWidth / 2f));
                float drawY = Math.round((player.y * PIXELS_PER_TILE) - SPRITE_FEET_INSET_PX);

                batch.draw(currentFrame, drawX, drawY);
            }
        }
    }

    private void drawHud(WorldSnapshot snapshot, float delta) {
        if (partnerBannerSecondsLeft > 0f) partnerBannerSecondsLeft -= delta;

        Matrix4 hudMatrix = new Matrix4().setToOrtho2D(0, 0, VIRTUAL_WIDTH, VIRTUAL_HEIGHT);
        float top = VIRTUAL_HEIGHT - 20f;

        batch.setProjectionMatrix(hudMatrix);
        batch.begin();
        font.setColor(Color.WHITE);
        font.draw(batch, "LEVEL " + levelNumber + "   |   " + client.getMatchMode()
                + "   |   " + (game.isHost() ? "HOST" : "CLIENT"), 20f, top);

        // With the collision overlay on, report exactly which grid cell the
        // player occupies. Without this, "I am stuck here" can only be relayed
        // as a screenshot someone then has to locate on the map by eye — with
        // it, the cell can be read off and fixed directly in the .map file.
        if (showCollisionOverlay && snapshot != null && tileMap != null) {
            WorldSnapshot.PlayerState here = client.findLocalPlayer(snapshot);
            if (here != null) {
                int cellX = tileMap.toCell(here.x);
                int cellYWorld = tileMap.toCell(here.y);
                int cellYFile = tileMap.getCollisionHeight() - 1 - cellYWorld;
                font.setColor(Color.YELLOW);
                font.draw(batch, String.format(
                                "F1 collision overlay   tile %.2f,%.2f   cell %d,%d   map-file row %d col %d",
                                here.x, here.y, cellX, cellYWorld, cellYFile, cellX),
                        20f, top - 48f);
                font.setColor(Color.WHITE);
            }
        }

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
                font.draw(batch, String.format("you: %s   contamination %.0f%%%s",
                                me.character, me.personalContaminationPct,
                                me.downed ? "   DOWNED " + me.reviveSecondsRemaining + "s" : ""),
                        20f, top - 75f);
            }
        }

        if (partnerBannerSecondsLeft > 0f && partnerBanner != null) {
            font.setColor(0.910f, 0.353f, 0.310f, 1f);
            font.draw(batch, partnerBanner, 20f, top - 105f);
        }

        if (isBeingBitten) {
            font.setColor(Color.RED);
            font.draw(batch, "BEING BITTEN!", VIRTUAL_WIDTH / 2f - 60f, VIRTUAL_HEIGHT / 2f + 60f);
        }

        font.setColor(Color.GRAY);
        font.draw(batch, "WASD move   E interact   SPACE attack   SHIFT sprint   ESC pause   I inventory", 20f, 30f);
        batch.end();

        if (snapshot != null) {
            WorldSnapshot.PlayerState me = client.findLocalPlayer(snapshot);
            if (me != null) {
                float maxHp = 100f;
                float currentHp = Math.max(0f, Math.min(me.hp, maxHp));
                float hpPercent = currentHp / maxHp;

                float barX = 20f;
                float barY = top - 62f;
                float barWidth = 150f;
                float barHeight = 16f;

                shapes.setProjectionMatrix(hudMatrix);
                shapes.begin(ShapeRenderer.ShapeType.Filled);

                shapes.setColor(0.2f, 0.2f, 0.2f, 0.8f);
                shapes.rect(barX, barY, barWidth, barHeight);

                shapes.setColor(0.15f, 0.8f, 0.3f, 1f);
                shapes.rect(barX, barY, barWidth * hpPercent, barHeight);

                shapes.setColor(0.1f, 0.1f, 0.1f, 1f);
                shapes.rect(barX + (barWidth / 3f), barY, 2f, barHeight);
                shapes.rect(barX + (barWidth * 2f / 3f), barY, 2f, barHeight);

                float sprintBarY = barY - 20f;
                float sprintPercent = stamina / maxStamina;

                shapes.setColor(0.2f, 0.2f, 0.2f, 0.8f);
                shapes.rect(barX, sprintBarY, barWidth, barHeight);

                shapes.setColor(0.15f, 0.5f, 0.9f, 1f);
                shapes.rect(barX, sprintBarY, barWidth * sprintPercent, barHeight);

                shapes.setColor(0.1f, 0.1f, 0.1f, 1f);
                shapes.rect(barX + (barWidth / 3f), sprintBarY, 2f, barHeight);
                shapes.rect(barX + (barWidth * 2f / 3f), sprintBarY, 2f, barHeight);

                shapes.end();
            }
        }
    }

    private void drawPauseOverlay() {
        Gdx.gl.glEnable(GL20.GL_BLEND);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0f, 0f, 0f, 0.6f);
        shapes.rect(0, 0, VIRTUAL_WIDTH, VIRTUAL_HEIGHT);
        shapes.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);

        batch.begin();
        font.setColor(Color.WHITE);
        font.draw(batch, "PAUSED — ESC to resume",
                VIRTUAL_WIDTH / 2f - 90f, VIRTUAL_HEIGHT / 2f);
        batch.end();
    }

    private InputCommand readLocalInput() {
        InputCommand input = new InputCommand();
        input.moveX = (Gdx.input.isKeyPressed(Input.Keys.D) ? 1 : 0) - (Gdx.input.isKeyPressed(Input.Keys.A) ? 1 : 0);
        input.moveY = (Gdx.input.isKeyPressed(Input.Keys.W) ? 1 : 0) - (Gdx.input.isKeyPressed(Input.Keys.S) ? 1 : 0);
        input.attackPressed = Gdx.input.isKeyJustPressed(Input.Keys.SPACE);
        input.interactHeld = Gdx.input.isKeyPressed(Input.Keys.E);
        input.interactPressed = Gdx.input.isKeyJustPressed(Input.Keys.E);
        input.abilityPressed = Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT);
        input.dropPressed = Gdx.input.isKeyJustPressed(Input.Keys.Q);

        return input;
    }

    private void showBanner(String message) {
        partnerBanner = message;
        partnerBannerSecondsLeft = 5f;
    }

    @Override
    public void resize(int width, int height) {
        // Crucial: Update the viewport whenever the window changes size or enters fullscreen
        viewport.update(width, height, true);
    }

    @Override public void pause() { }
    @Override public void resume() { }
    @Override public void hide() { client.setOnEvent(null); }

    @Override
    public void dispose() {
        if (batch != null) batch.dispose();
        if (shapes != null) shapes.dispose();
        if (font != null) font.dispose();
        if (mapTexture != null) mapTexture.dispose();
        if (playerTexture != null) playerTexture.dispose();
        if (idleTexture != null) idleTexture.dispose();
        if (zombieTexture != null) zombieTexture.dispose();
        if (inventoryTexture != null) inventoryTexture.dispose();
    }
}