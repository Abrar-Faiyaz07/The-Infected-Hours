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
import com.infectedhour.core.systems.CollisionSystem;
import com.infectedhour.shared.constants.GameConstants;
import com.infectedhour.shared.dto.SaveSlotDto;
import com.infectedhour.shared.network.CharacterType;
import com.infectedhour.shared.network.InputCommand;
import com.infectedhour.shared.network.WorldSnapshot;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GameScreen implements Screen {

    private static final float PIXELS_PER_TILE = 48f;

    // --- map.png alignment (see core/src/main/resources/maps/level1.map) ---
    private static final float MAP_ART_TILE_PX = 41.2667f;
    private static final float MAP_ART_ORIGIN_X_PX = 32f;
    private static final float MAP_ART_ORIGIN_Y_TOP_PX = 32f;
    private static final int MAP_ART_ROWS = 33;

    private static final float SPRITE_FEET_INSET_PX = 13f;

    // Virtual resolution for crisp pixel-art scaling
    private static final float VIRTUAL_WIDTH = 1280f;
    private static final float VIRTUAL_HEIGHT = 720f;

    private final InfectedHourGame game;
    private final GameClient client;
    private final GameBridge bridge;
    private final int levelNumber;

    private TileMap tileMap;
    private boolean showCollisionOverlay = false;

    private SpriteBatch batch;
    private ShapeRenderer shapes;
    private BitmapFont font;

    private OrthographicCamera camera;
    private Viewport viewport;
    private Texture mapTexture;

    // ── COLLISION SYSTEM ──
    private CollisionSystem collisionSystem;

    private Texture playerTexture;
    private TextureRegion[][] playerFrames;
    private int frameWidth, frameHeight;

    private Texture idleTexture;
    private TextureRegion[][] idleFrames;
    private int idleFrameWidth, idleFrameHeight;

    private Texture zombieTexture;
    private TextureRegion[][] zombieFrames;
    private int zombieFrameWidth, zombieFrameHeight;

    // ── ZOMBIE PLACED IN THE MIDDLE ──
    private float middleZombieX = 22f;
    private float middleZombieY = 16f;
    private boolean middleZombieChasing = false;

    private final Map<String, PlayerAnimState> animStates = new HashMap<>();
    private final ZombieAnimState zombieAnim = new ZombieAnimState();

    private float stamina = 100f;
    private final float maxStamina = 100f;

    private float biteCooldown = 1.0f;
    private boolean isBeingBitten = false;

    private Texture inventoryTexture;
    private boolean isInventoryOpen = false;
    private final int INVENTORY_COLS = 4;
    private final int INVENTORY_ROWS = 4;
    private final float SLOT_SIZE = 50f;
    private final float SLOT_SPACING = 10f;
    private final float GRID_OFFSET_X = 60f;
    private final float GRID_OFFSET_Y = 80f;
    private boolean[] mockSlots;

    private boolean isSaveOverlayOpen = false;
    private List<SaveSlotDto> overlaySlots = null;

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
        viewport = new FitViewport(VIRTUAL_WIDTH, VIRTUAL_HEIGHT, camera);
        viewport.apply();

        LevelLoader levelLoader = new LevelLoader();
        LevelDefinition def = levelLoader.loadDefinition(levelNumber);
        tileMap = levelLoader.loadMap(def);

        // ── INITIALIZE COLLISION SYSTEM ──
        this.collisionSystem = new CollisionSystem(tileMap);

        if (game.isHost()) {
            game.getServer().loadTileMap(tileMap);
            game.getServer().setMapWidthInTiles(levelLoader.getMapWidthInTiles());
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

        inventoryTexture = new Texture(Gdx.files.internal("inventory.png"));
        inventoryTexture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        mockSlots = new boolean[INVENTORY_COLS * INVENTORY_ROWS];

        client.setOnEvent(event -> {
            if (event.type == null) return;
            switch (event.type) {
                case GameConstants.EVENT_PARTNER_DISCONNECTED -> showBanner("Partner disconnected — waiting…");
                case GameConstants.EVENT_PARTNER_RECONNECTED -> showBanner("Partner reconnected");
                case GameConstants.EVENT_CONVERTED_TO_SOLO -> showBanner("Continuing solo");
                case GameConstants.EVENT_GAME_SAVED -> showBanner("Game Saved to Slot " + event.payload + "!");
                default -> { }
            }
        });
    }

    // ── COLLISION CHECK METHOD ──
    private boolean isWalkable(float x, float y, float radius) {
        if (collisionSystem == null) return true;
        return !collisionSystem.overlapsBlockedTile(x, y, radius);
    }

    @Override
    public void render(float delta) {
        WorldSnapshot snapshot = client.getInterpolatedSnapshot(System.currentTimeMillis());
        WorldSnapshot.PlayerState me = null;

        if (snapshot != null) {
            me = client.findLocalPlayer(snapshot);
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
                    bridge.notifyMatchEnded(new GameBridge.MatchOutcome("DEFEAT", levelNumber));
                    com.badlogic.gdx.Gdx.app.exit();
                }
                return;
            }
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            if (isSaveOverlayOpen) {
                isSaveOverlayOpen = false;
            } else {
                paused = !paused;
                client.sendEvent(paused ? GameConstants.EVENT_PAUSE : GameConstants.EVENT_RESUME, "");
            }
        }

        boolean ctrlPressed = Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT) || Gdx.input.isKeyPressed(Input.Keys.CONTROL_RIGHT);
        if (ctrlPressed && Gdx.input.isKeyJustPressed(Input.Keys.S)) {
            isSaveOverlayOpen = !isSaveOverlayOpen;
            if (isSaveOverlayOpen) {
                overlaySlots = bridge.getSaveSlots();
            }
        }

        if (isSaveOverlayOpen) {
            renderSaveOverlay();
            return;
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.I)) {
            isInventoryOpen = !isInventoryOpen;
        }

        if (isInventoryOpen && Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
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
            client.sendInputIfDue(readLocalInput(me, delta), delta);
        }

        boolean isSprinting = Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT);
        if (isSprinting && stamina > 0f) {
            stamina = Math.max(0f, stamina - (45f * delta));
        } else if (!isSprinting && stamina < maxStamina) {
            stamina = Math.min(maxStamina, stamina + (30f * delta));
        }

        if (snapshot != null) {
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

                // ── BITE DAMAGE ENABLED ──
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

                    // ── ZOMBIE REAL WALL COLLISION & SLIDE LOGIC ──
                    float zombieRadius = 0.25f; // Exactly the same as the player radius!
                    float nextX = middleZombieX + moveX;
                    float nextY = middleZombieY + moveY;

                    if (isWalkable(nextX, middleZombieY, zombieRadius)) {
                        middleZombieX = nextX;
                    }
                    if (isWalkable(middleZombieX, nextY, zombieRadius)) {
                        middleZombieY = nextY;
                    }

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

                // ── ZOMBIE VISUAL CENTER (zombieFrameHeight / 2f) ──
                float midDrawX = Math.round((middleZombieX * PIXELS_PER_TILE) - (zombieFrameWidth / 2f));
                float midDrawY = Math.round((middleZombieY * PIXELS_PER_TILE) - (zombieFrameHeight / 2f));
                batch.draw(zombieFrame, midDrawX, midDrawY);
            }

            if (isInventoryOpen) {
                Matrix4 hudMatrix = new Matrix4().setToOrtho2D(0, 0, VIRTUAL_WIDTH, VIRTUAL_HEIGHT);
                batch.setProjectionMatrix(hudMatrix);

                int invX = Math.round((VIRTUAL_WIDTH - inventoryTexture.getWidth()) / 2f);
                int invY = Math.round((VIRTUAL_HEIGHT - inventoryTexture.getHeight()) / 2f);

                batch.draw(inventoryTexture, invX, invY);
            }

            batch.end();

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
            font.draw(batch, "Waiting for the first snapshot from the host…", 20f, top - 18f);
        }

        if (partnerBannerSecondsLeft > 0f && partnerBanner != null) {
            font.setColor(0.910f, 0.353f, 0.310f, 1f);
            font.draw(batch, partnerBanner, 20f, top - 90f);
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
                float barY = top - 48f;
                float barWidth = 150f;
                float barHeight = 14f;

                shapes.setProjectionMatrix(hudMatrix);
                shapes.begin(ShapeRenderer.ShapeType.Filled);

                shapes.setColor(0.2f, 0.2f, 0.2f, 0.8f);
                shapes.rect(barX, barY, barWidth, barHeight);

                shapes.setColor(0.15f, 0.8f, 0.3f, 1f);
                shapes.rect(barX, barY, barWidth * hpPercent, barHeight);

                shapes.setColor(0.1f, 0.1f, 0.1f, 1f);
                shapes.rect(barX + (barWidth / 3f), barY, 2f, barHeight);
                shapes.rect(barX + (barWidth * 2f / 3f), barY, 2f, barHeight);

                float sprintBarY = barY - 18f;
                float sprintPercent = stamina / maxStamina;

                shapes.setColor(0.2f, 0.2f, 0.2f, 0.8f);
                shapes.rect(barX, sprintBarY, barWidth, barHeight);

                shapes.setColor(0.95f, 0.8f, 0.15f, 1f);
                shapes.rect(barX, sprintBarY, barWidth * sprintPercent, barHeight);

                shapes.setColor(0.1f, 0.1f, 0.1f, 1f);
                shapes.rect(barX + (barWidth / 3f), sprintBarY, 2f, barHeight);
                shapes.rect(barX + (barWidth * 2f / 3f), sprintBarY, 2f, barHeight);

                shapes.end();
            }
        }
    }

    private void drawPauseOverlay() {
        Matrix4 hudMatrix = new Matrix4().setToOrtho2D(0, 0, VIRTUAL_WIDTH, VIRTUAL_HEIGHT);

        Gdx.gl.glEnable(GL20.GL_BLEND);
        shapes.setProjectionMatrix(hudMatrix);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0.04f, 0.06f, 0.1f, 0.85f);
        shapes.rect(0, 0, VIRTUAL_WIDTH, VIRTUAL_HEIGHT);

        float panelW = 420f;
        float panelH = 240f;
        float panelX = (VIRTUAL_WIDTH - panelW) / 2f;
        float panelY = (VIRTUAL_HEIGHT - panelH) / 2f;

        shapes.setColor(0.08f, 0.12f, 0.2f, 0.95f);
        shapes.rect(panelX, panelY, panelW, panelH);
        shapes.end();

        shapes.begin(ShapeRenderer.ShapeType.Line);
        shapes.setColor(0.91f, 0.69f, 0.16f, 1f);
        shapes.rect(panelX, panelY, panelW, panelH);
        shapes.end();

        com.badlogic.gdx.math.Vector3 mouseCoords = new com.badlogic.gdx.math.Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
        viewport.unproject(mouseCoords);
        float mx = mouseCoords.x;
        float my = mouseCoords.y;

        boolean mouseJustPressed = Gdx.input.isButtonJustPressed(Input.Buttons.LEFT);

        float btnW = 320f;
        float btnH = 45f;
        float btnX = (VIRTUAL_WIDTH - btnW) / 2f;
        float btn1Y = panelY + 120f;

        boolean btn1Hovered = mx >= btnX && mx <= btnX + btnW && my >= btn1Y && my <= btn1Y + btnH;
        if (btn1Hovered && mouseJustPressed) {
            paused = false;
            client.sendEvent(GameConstants.EVENT_RESUME, "");
        }

        shapes.begin(ShapeRenderer.ShapeType.Filled);
        if (btn1Hovered) {
            shapes.setColor(0.25f, 0.35f, 0.5f, 1f);
        } else {
            shapes.setColor(0.15f, 0.2f, 0.3f, 1f);
        }
        shapes.rect(btnX, btn1Y, btnW, btnH);
        shapes.end();

        shapes.begin(ShapeRenderer.ShapeType.Line);
        if (btn1Hovered) {
            shapes.setColor(0.91f, 0.69f, 0.16f, 1f);
        } else {
            shapes.setColor(0.4f, 0.5f, 0.65f, 1f);
        }
        shapes.rect(btnX, btn1Y, btnW, btnH);
        shapes.end();

        float btn2Y = panelY + 50f;
        boolean btn2Hovered = mx >= btnX && mx <= btnX + btnW && my >= btn2Y && my <= btn2Y + btnH;
        if ((btn2Hovered && mouseJustPressed) || Gdx.input.isKeyJustPressed(Input.Keys.Q) || Gdx.input.isKeyJustPressed(Input.Keys.M)) {
            bridge.onGameWindowClosed();
            Gdx.app.exit();
            return;
        }

        shapes.begin(ShapeRenderer.ShapeType.Filled);
        if (btn2Hovered) {
            shapes.setColor(0.5f, 0.18f, 0.18f, 1f);
        } else {
            shapes.setColor(0.28f, 0.12f, 0.12f, 1f);
        }
        shapes.rect(btnX, btn2Y, btnW, btnH);
        shapes.end();

        shapes.begin(ShapeRenderer.ShapeType.Line);
        if (btn2Hovered) {
            shapes.setColor(1f, 0.4f, 0.4f, 1f);
        } else {
            shapes.setColor(0.65f, 0.25f, 0.25f, 1f);
        }
        shapes.rect(btnX, btn2Y, btnW, btnH);
        shapes.end();

        Gdx.gl.glDisable(GL20.GL_BLEND);

        batch.setProjectionMatrix(hudMatrix);
        batch.begin();

        font.setColor(new Color(0.91f, 0.69f, 0.16f, 1f));
        font.draw(batch, "GAME PAUSED", VIRTUAL_WIDTH / 2f - 60f, panelY + panelH - 25f);

        font.setColor(btn1Hovered ? Color.WHITE : Color.LIGHT_GRAY);
        font.draw(batch, "Resume Game (ESC)", btnX + 70f, btn1Y + 28f);

        font.setColor(btn2Hovered ? Color.WHITE : new Color(0.95f, 0.6f, 0.6f, 1f));
        font.draw(batch, "Exit to Main Menu (Q)", btnX + 60f, btn2Y + 28f);

        batch.end();
    }

    private InputCommand readLocalInput(WorldSnapshot.PlayerState me, float delta) {
        InputCommand input = new InputCommand();
        float proposedMoveX = (Gdx.input.isKeyPressed(Input.Keys.D) ? 1 : 0) - (Gdx.input.isKeyPressed(Input.Keys.A) ? 1 : 0);
        float proposedMoveY = (Gdx.input.isKeyPressed(Input.Keys.W) ? 1 : 0) - (Gdx.input.isKeyPressed(Input.Keys.S) ? 1 : 0);

        if (me != null) {
            float playerSpeed = 4.0f * delta;
            float playerRadius = 0.25f;

            boolean isCurrentlyStuck = !isWalkable(me.x, me.y, playerRadius);

            if (!isCurrentlyStuck) {
                if (!isWalkable(me.x + (proposedMoveX * playerSpeed), me.y, playerRadius)) {
                    proposedMoveX = 0;
                }
                if (!isWalkable(me.x, me.y + (proposedMoveY * playerSpeed), playerRadius)) {
                    proposedMoveY = 0;
                }
            }
        }

        input.moveX = proposedMoveX;
        input.moveY = proposedMoveY;
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
        viewport.update(width, height, true);
    }

    @Override public void pause() { }
    @Override public void resume() { }
    @Override public void hide() { client.setOnEvent(null); }

    private void renderSaveOverlay() {
        if (overlaySlots == null || overlaySlots.size() < GameConstants.SAVE_SLOT_COUNT) {
            overlaySlots = bridge.getSaveSlots();
        }

        Matrix4 hudMatrix = new Matrix4().setToOrtho2D(0, 0, VIRTUAL_WIDTH, VIRTUAL_HEIGHT);

        Gdx.gl.glEnable(GL20.GL_BLEND);
        shapes.setProjectionMatrix(hudMatrix);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0.04f, 0.06f, 0.1f, 0.92f);
        shapes.rect(0, 0, VIRTUAL_WIDTH, VIRTUAL_HEIGHT);

        int cols = 3;
        float cardW = 340f;
        float cardH = 145f;
        float gapX = 20f;
        float gapY = 16f;

        float gridTotalWidth = (cols * cardW) + ((cols - 1) * gapX);
        float startX = (VIRTUAL_WIDTH - gridTotalWidth) / 2f;
        float startY = VIRTUAL_HEIGHT - 120f;

        com.badlogic.gdx.math.Vector3 mouseCoords = new com.badlogic.gdx.math.Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
        viewport.unproject(mouseCoords);
        float mx = mouseCoords.x;
        float my = mouseCoords.y;

        int clickedSlot = -1;
        boolean mouseJustPressed = Gdx.input.isButtonJustPressed(Input.Buttons.LEFT);

        for (int i = 0; i < GameConstants.SAVE_SLOT_COUNT; i++) {
            int c = i % cols;
            int r = i / cols;

            float x = startX + c * (cardW + gapX);
            float y = startY - (r + 1) * cardH - r * gapY;

            boolean isHovered = mx >= x && mx <= x + cardW && my >= y && my <= y + cardH;
            if (isHovered && mouseJustPressed) {
                clickedSlot = i + 1;
            }

            SaveSlotDto slot = (overlaySlots != null && i < overlaySlots.size()) ? overlaySlots.get(i) : SaveSlotDto.empty(i + 1);

            if (isHovered) {
                shapes.setColor(0.18f, 0.24f, 0.35f, 1f);
            } else if (slot != null && slot.occupied()) {
                shapes.setColor(0.1f, 0.14f, 0.22f, 1f);
            } else {
                shapes.setColor(0.07f, 0.1f, 0.15f, 1f);
            }
            shapes.rect(x, y, cardW, cardH);

            shapes.end();
            shapes.begin(ShapeRenderer.ShapeType.Line);
            if (isHovered) {
                shapes.setColor(0.91f, 0.69f, 0.16f, 1f);
            } else if (slot != null && slot.occupied()) {
                shapes.setColor(0.3f, 0.45f, 0.65f, 0.8f);
            } else {
                shapes.setColor(0.2f, 0.25f, 0.35f, 0.5f);
            }
            shapes.rect(x, y, cardW, cardH);
            shapes.end();
            shapes.begin(ShapeRenderer.ShapeType.Filled);
        }

        float btnW = 200f;
        float btnH = 40f;
        float btnX = (VIRTUAL_WIDTH - btnW) / 2f;
        float btnY = 30f;

        boolean btnHovered = mx >= btnX && mx <= btnX + btnW && my >= btnY && my <= btnY + btnH;
        if (btnHovered && mouseJustPressed) {
            isSaveOverlayOpen = false;
        }

        if (btnHovered) {
            shapes.setColor(0.25f, 0.32f, 0.45f, 1f);
        } else {
            shapes.setColor(0.12f, 0.16f, 0.25f, 1f);
        }
        shapes.rect(btnX, btnY, btnW, btnH);
        shapes.end();

        shapes.begin(ShapeRenderer.ShapeType.Line);
        if (btnHovered) {
            shapes.setColor(0.91f, 0.69f, 0.16f, 1f);
        } else {
            shapes.setColor(0.4f, 0.45f, 0.55f, 1f);
        }
        shapes.rect(btnX, btnY, btnW, btnH);
        shapes.end();

        Gdx.gl.glDisable(GL20.GL_BLEND);

        // ── FIXED THE TYPO HERE ON LINE 844 ──
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_1) || Gdx.input.isKeyJustPressed(Input.Keys.NUMPAD_1)) clickedSlot = 1;
        else if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_2) || Gdx.input.isKeyJustPressed(Input.Keys.NUMPAD_2)) clickedSlot = 2;
        else if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_3) || Gdx.input.isKeyJustPressed(Input.Keys.NUMPAD_3)) clickedSlot = 3;
        else if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_4) || Gdx.input.isKeyJustPressed(Input.Keys.NUMPAD_4)) clickedSlot = 4;
        else if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_5) || Gdx.input.isKeyJustPressed(Input.Keys.NUMPAD_5)) clickedSlot = 5;
        else if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_6) || Gdx.input.isKeyJustPressed(Input.Keys.NUMPAD_6)) clickedSlot = 6;
        else if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_7) || Gdx.input.isKeyJustPressed(Input.Keys.NUMPAD_7)) clickedSlot = 7;
        else if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_8) || Gdx.input.isKeyJustPressed(Input.Keys.NUMPAD_8)) clickedSlot = 8;
        else if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_9) || Gdx.input.isKeyJustPressed(Input.Keys.NUMPAD_9)) clickedSlot = 9;

        if (clickedSlot != -1) {
            if (game.isHost() && game.getServer() != null) {
                SaveSlotDto slotDto = game.getServer().captureSave(clickedSlot);
                if (slotDto != null) {
                    bridge.requestSave(clickedSlot, slotDto);
                    overlaySlots = bridge.getSaveSlots();
                    showBanner("Game Saved to Slot " + clickedSlot + "!");
                    isSaveOverlayOpen = false;
                }
            } else {
                showBanner("Only Host can save");
            }
        }

        batch.setProjectionMatrix(hudMatrix);
        batch.begin();

        font.setColor(new Color(0.91f, 0.69f, 0.16f, 1f));
        font.draw(batch, "SAVE GAME", startX, VIRTUAL_HEIGHT - 35f);
        font.setColor(Color.LIGHT_GRAY);
        font.draw(batch, "Select a slot to save progress. Click or press keys [1-9]. (Ctrl+S / ESC to close)", startX, VIRTUAL_HEIGHT - 65f);

        for (int i = 0; i < GameConstants.SAVE_SLOT_COUNT; i++) {
            int c = i % cols;
            int r = i / cols;

            float x = startX + c * (cardW + gapX);
            float y = startY - (r + 1) * cardH - r * gapY;

            SaveSlotDto slot = (overlaySlots != null && i < overlaySlots.size()) ? overlaySlots.get(i) : SaveSlotDto.empty(i + 1);

            font.setColor(new Color(0.91f, 0.69f, 0.16f, 1f));
            font.draw(batch, "SLOT " + (i + 1), x + 14f, y + cardH - 14f);

            if (slot == null || !slot.occupied()) {
                font.setColor(Color.GRAY);
                font.draw(batch, "— Empty —", x + 14f, y + cardH - 55f);
                font.setColor(Color.LIGHT_GRAY);
                font.draw(batch, "Click or Press [" + (i + 1) + "] to Save", x + 14f, y + 28f);
            } else {
                font.setColor(Color.WHITE);
                font.draw(batch, "Level " + slot.levelNumber() + (slot.levelName() == null ? "" : " — " + slot.levelName()), x + 14f, y + cardH - 42f);

                font.setColor(Color.LIGHT_GRAY);
                font.draw(batch, slot.checkpointName() == null ? "Checkpoint: —" : slot.checkpointName(), x + 14f, y + cardH - 65f);

                font.draw(batch, String.format("%s   HP %.0f", slot.formattedPlaytime(), slot.playerHp()), x + 14f, y + cardH - 88f);

                font.setColor(Color.DARK_GRAY);
                font.draw(batch, "Click/Press [" + (i + 1) + "] to Overwrite", x + 14f, y + 24f);
            }
        }

        font.setColor(btnHovered ? Color.WHITE : Color.LIGHT_GRAY);
        font.draw(batch, "Close (ESC)", btnX + 60f, btnY + 26f);

        batch.end();
    }

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