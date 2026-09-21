package com.infectedhour.core.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.infectedhour.core.InfectedHourGame;
import com.infectedhour.core.bridge.GameBridge;
import com.infectedhour.core.level.CampaignLevelPlan;
import com.infectedhour.core.level.LevelDefinition;
import com.infectedhour.core.level.LevelExit;
import com.infectedhour.core.level.LevelLoader;
import com.infectedhour.core.level.TileMap;
import com.infectedhour.core.net.GameClient;
import com.infectedhour.core.systems.CollisionSystem;
import com.infectedhour.core.state.CampaignSquadState;
import com.infectedhour.shared.constants.GameConstants;
import com.infectedhour.shared.dto.SaveSlotDto;
import com.infectedhour.shared.level.Checkpoint;
import com.infectedhour.shared.level.CheckpointRegistry;
import com.infectedhour.shared.network.CharacterType;
import com.infectedhour.shared.network.InputCommand;
import com.infectedhour.shared.network.WorldSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

public class GameScreen implements Screen {

    private static final float PIXELS_PER_TILE = 48f;

    private static final float MAP_ART_TILE_PX = 41.2667f;
    private static final float MAP_ART_ORIGIN_X_PX = 32f;
    private static final float MAP_ART_ORIGIN_Y_TOP_PX = 32f;
    private static final int MAP_ART_ROWS = 33;

    private static final float SPRITE_FEET_INSET_PX = 13f;

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
    private BitmapFont pauseFont;

    // Level Music
    private Music levelMusic;
    private static final float LEVEL_MUSIC_VOLUME = 0.55f;

    // One-shot combat sound effects
    private Sound macheteSound; // successful melee hit
    private Sound swingSound;   // melee miss / no damage
    private Sound bombSound;    // bomb impact / explosion
    private static final float MACHETE_SOUND_VOLUME = 1.0f;
    private static final float SWING_SOUND_VOLUME = 1.0f;
    private static final float BOMB_SOUND_VOLUME = 1.0f;

    private OrthographicCamera camera;
    private Viewport viewport;
    private Texture mapTexture;
    private Texture markerTexture;
    private List<CampaignLevelPlan.Feature> levelFeatures = List.of();
    private final Set<String> completedFeatureIds = new HashSet<>();
    private boolean zombieObjectiveSent = false;

    private CollisionSystem collisionSystem;

    // Standard Textures
    private Texture playerTexture;
    private TextureRegion[][] playerFrames;
    private int frameWidth, frameHeight;

    private Texture idleTexture;
    private TextureRegion[][] idleFrames;
    private int idleFrameWidth, idleFrameHeight;

    // Female Player (Jane) / Player 2 Textures & Frames
    private Texture p2Texture;
    private TextureRegion[][] p2Frames;
    private Texture p2IdleTexture;
    private TextureRegion[][] p2IdleFrames;
    private Texture p2SprintTexture;
    private TextureRegion[][] p2SprintFrames;
    private Texture p2MeleeTexture;
    private TextureRegion[][] p2MeleeFrames;
    private Texture p2MeleeHitTexture;
    private TextureRegion[][] p2MeleeHitFrames;
    private int p2MeleeHitFrameWidth, p2MeleeHitFrameHeight;
    private Texture p2IdleMeleeTexture;
    private TextureRegion[][] p2IdleMeleeFrames;
    private Texture p2BombTexture;
    private TextureRegion[][] p2BombFrames;
    private Texture p2BombIdleTexture;
    private TextureRegion[][] p2BombIdleFrames;
    private Texture femalePlayerTexture;
    private TextureRegion[][] femaleFrames;
    private int femaleFrameWidth, femaleFrameHeight;
    private static final float FEMALE_SPRITE_SCALE = 0.82f;
    private static final float FEMALE_MELEE_SCALE = FEMALE_SPRITE_SCALE * 1.1f;

    // Sprint Texture
    private Texture playerSprintTexture;
    private TextureRegion[][] playerSprintFrames;

    // Melee Textures
    private Texture playerMeleeTexture;
    private TextureRegion[][] playerMeleeFrames;

    private Texture idleMeleeTexture;
    private TextureRegion[][] idleMeleeFrames;

    // Player Bomb Textures
    private Texture playerBombTexture;
    private TextureRegion[][] playerBombFrames;
    private Texture playerBombIdleTexture;
    private TextureRegion[][] playerBombIdleFrames;
    private Texture playerBombThrowTexture;
    private TextureRegion[][] playerBombThrowFrames;
    private int pbFrameWidth, pbFrameHeight;

    // Ground Items & Effects
    private Texture meleeTexture;
    private TextureRegion[][] meleeGroundFrames;
    private int meleeFrameWidth;
    private float groundItemStateTime = 0f;

    private Texture bloodTexture;
    private final List<Vector2> bloodPools = new ArrayList<>();

    // Hit Animation Textures
    private Texture meleeHitTexture;
    private TextureRegion[][] meleeHitFrames;
    private int meleeHitFrameWidth, meleeHitFrameHeight;

    // Bomb Texture & Tracking
    private Texture bombTexture;
    private TextureRegion[][] bombFrames;
    private int bombFrameWidth;
    private float bombCooldown = 0f;
    private final List<ActiveBomb> activeBombs = new ArrayList<>();

    // Bomb Explosion Tracking
    private Texture bombEffectTexture;
    private TextureRegion[][] bombEffectFrames;
    private int bombEffectFrameWidth;
    private final List<ActiveExplosion> activeExplosions = new ArrayList<>();

    // Healing Ability
    private float healCooldown = 0f;
    private static final float MAX_HEAL_COOLDOWN = 12.0f;
    private float healEffectTimer = 0f;
    private float healFloatingTextTimer = 0f;
    private Texture healIconTexture;
    private Texture healEffectTexture;

    // Level 1 ground pickup art
    // herb.png = 1 row x 2 animated frames
    private Texture herbTexture;
    private TextureRegion[] herbFrames;
    private int herbFrameWidth;

    // medic.png = single image
    private Texture medicTexture;

    private static final float HERB_FRAME_DURATION = 0.30f;

    // Ground pickup draw sizes: +75% from the previous 20px size.
    private static final float HERB_DRAW_SIZE = 35f;
    private static final float MEDIC_DRAW_SIZE = 35f;

    // HUD Damage Screen Tracking
    private Texture damagedScreen1Texture;
    private Texture damagedScreen2Texture;

    // Immunity Timer Tracking (Set to Infinite Time as requested)
    private Texture timerTexture;
    private TextureRegion[] timerFrames;
    private int timerFrameWidth;
    private float maxImmunityTime = 999999f;
    private float currentImmunityTime = 999999f;

    private float mockBombX = -1f;
    private float mockBombY = -1f;

    // Inventory Textures & Logic
    private Texture inventoryTexture;
    private Texture meleeInventoryTexture;
    private boolean isInventoryOpen = false;

    private boolean hasMachete = false;
    private boolean isMacheteEquipped = false;

    private boolean hasBomb = false;
    private boolean isBombEquipped = false;

    private Texture zombieTexture;
    private TextureRegion[][] zombieFrames;
    private int zombieFrameWidth, zombieFrameHeight;

    private Texture zombieIdleTexture;
    private TextureRegion[][] zombieIdleFrames;

    // Zombie Bite Texture
    private Texture zombieBiteTexture;
    private TextureRegion[][] zombieBiteFrames;
    private int zombieBiteFrameWidth, zombieBiteFrameHeight;

    private float middleZombieX = 22f;
    private float middleZombieY = 16f;
    private boolean middleZombieChasing = false;
    private boolean middleZombieBiting = false;

    private float middleZombieMaxHp = 25f;
    private float middleZombieHp = 25f;
    private boolean isZombieDead = false;

    private final Map<String, PlayerAnimState> animStates = new HashMap<>();
    private final ZombieAnimState zombieAnim = new ZombieAnimState();

    private float stamina = 100f;
    private float p2Stamina = 100f;
    private final float maxStamina = 100f;

    private float biteCooldown = 0.8f;
    private boolean isBeingBitten = false;

    // Level 1 Key & Boss Mechanics
    private boolean hasStairsKey = false;
    private boolean isAmbushActive = false;
    private boolean isAmbushDefeated = false;
    private Texture keyTexture;
    private float keyX = 24.5f;
    private float keyY = 26.5f;

    // Level 3 ambulance extraction
    private boolean hasAmbulanceKey = false;
    private float ambulanceKeyX = -1f;
    private float ambulanceKeyY = -1f;

    // Level 4 three-part restoration puzzle
    private static final int LEVEL_4_PUZZLE_PART_COUNT = 3;
    private final List<Vector2> level4PuzzleParts = new ArrayList<>();
    private int level4PuzzlePartsCollected = 0;

    // Level 5 laboratory gate
    private boolean hasLabPasskey = false;

    // Economy & Objective Overlay Toggle
    private int coins = 0;
    private boolean showObjectivesOverlay = false;

    // Level 1 Herb Parts & Mixed Herb Revive Kit
    private static final float HERB_1_X = 3.5f, HERB_1_Y = 6.5f;
    private static final float HERB_2_X = 41.0f, HERB_2_Y = 14.0f;
    private static final float MEDKIT_X = 4.5f, MEDKIT_Y = 6.5f;
    private boolean herb1Collected = false;
    private boolean herb2Collected = false;
    private int herbPartsCollected = 0;
    private boolean hasReviveKit = false;
    private boolean hasPickedFloorMedkit = false;

    private static final float JANE_PHARMACY_X = 42.0f, JANE_PHARMACY_Y = 5.5f;

    // Single-frame unconscious Jane sprite shown only before revival.
    // File: assets/player 2/jane_knocked.png
    private Texture janeKnockedTexture;
    private static final float JANE_KNOCKED_SCALE = 0.25f;
    private boolean isJaneRevived = false;
    private float janeX = 40.0f, janeY = 8.0f;
    private float janeHp = GameConstants.PLAYER_MAX_HP, janeMaxHp = GameConstants.PLAYER_MAX_HP;
    private float janeAttackCooldown = 0f;
    private float janeMedkitTimer = 60.0f;
    private int janeMedkitsProduced = 0;

    // Level 1 Villagers
    // All villager sheets are 4 rows x 8 columns:
    // row 0 = DOWN, row 1 = LEFT, row 2 = RIGHT, row 3 = UP.
    //
    // Asset mapping:
    // Dr. Ramirez (villager id v1) uses v1 / v1_idle
    // Nurse Claire (villager id v2) uses v2 / v2_idle
    private Texture villagerV1WalkTexture;
    private TextureRegion[][] villagerV1WalkFrames;
    private int villagerV1WalkFrameWidth, villagerV1WalkFrameHeight;

    private Texture villagerV1IdleTexture;
    private TextureRegion[][] villagerV1IdleFrames;
    private int villagerV1IdleFrameWidth, villagerV1IdleFrameHeight;

    private Texture villagerV2WalkTexture;
    private TextureRegion[][] villagerV2WalkFrames;
    private int villagerV2WalkFrameWidth, villagerV2WalkFrameHeight;

    private Texture villagerV2IdleTexture;
    private TextureRegion[][] villagerV2IdleFrames;
    private int villagerV2IdleFrameWidth, villagerV2IdleFrameHeight;

    private static final float VILLAGER_SCALE = 0.80f;

    // Per-sheet size adjustments:
    // Per-sheet size adjustments.
    // v2.png walking frames are 30% larger.
    // v1_idle.png idle frames are 30% larger.
    // v2_idle.png idle frames are 30% larger.
    private static final float V2_WALK_SCALE_MULTIPLIER = 1.30f;
    private static final float V1_IDLE_SCALE_MULTIPLIER = 1.30f;
    private static final float V2_IDLE_SCALE_MULTIPLIER = 1.30f;

    private static final float VILLAGER_WALK_FRAME_DURATION = 0.12f;
    private static final float VILLAGER_IDLE_FRAME_DURATION = 0.18f;

    private static class LevelVillager {
        final String id;
        final String name;
        float x, y;
        float hp = 100f;
        final float maxHp = 100f;
        boolean isRescued = false;
        boolean isDead = false;
        boolean isStaying = false;
        String leaderId = null;
        float biteCooldown = 0.8f;
        float stateTime = 0f;
        int facing = 0; // 0=DOWN, 1=LEFT, 2=RIGHT, 3=UP
        boolean moving = false;

        LevelVillager(String id, String name, float x, float y) {
            this.id = id;
            this.name = name;
            this.x = x;
            this.y = y;
        }
    }
    private final List<LevelVillager> levelVillagers = new ArrayList<>();

    private static class AmbushZombie {
        float x, y;
        float hp = 25f;
        float maxHp = 25f;
        boolean isBoss = false;
        boolean dead = false;
        float stateTime = 0f;
        boolean biting = false;
        float biteCooldown = 0.8f;
        TextureRegion currentFrame = null;
        // Co-op mirroring: animation state (0 idle, 1 walk, 2 bite) + facing row, and the host's latest position
        int animState = 0;
        int dirRow = 0;
        boolean hasNetState = false;
        float netX, netY;

        AmbushZombie(float x, float y, boolean isBoss) {
            this.x = x;
            this.y = y;
            this.isBoss = isBoss;
            this.maxHp = isBoss ? 100f : 25f;
            this.hp = this.maxHp;
        }
    }
    private final List<AmbushZombie> ambushZombies = new ArrayList<>();

    // Co-op zombie kill sync: a kill on either screen is relayed so the zombie dies on both.
    // IDs are stable across clients: "middle" for the patrol zombie, "ambush:<index>" for horde zombies
    // (horde lists are always built in the same fixed order).
    private static final String EVENT_ZOMBIE_KILLED = "ZOMBIE_KILLED";
    private static final String MIDDLE_ZOMBIE_ID = "middle";
    private final Set<String> killedZombieIds = new HashSet<>();

    // Co-op zombie position sync: only the host runs zombie AI (movement + bites) and sends the state;
    // the other player's screen mirrors it so zombies are in the same place on both screens.
    private static final String EVENT_ZOMBIE_STATE = "ZOMBIE_STATE";
    private static final String EVENT_STAIRS_KEY_TAKEN = "STAIRS_KEY_TAKEN";
    private static final float ZOMBIE_SYNC_INTERVAL = 1f / 15f;
    private float zombieSyncTimer = 0f;
    private int middleZombieAnimState = 0; // 0 idle, 1 walk, 2 bite
    private boolean hasMiddleZombieNetState = false;
    private float middleZombieNetX, middleZombieNetY;

    // Minimap Radar
    private boolean isMinimapOpen = true;
    private float minimapStateTime = 0f;
    private TextureRegion minimapRegion;

    // Save Overlay & State
    private boolean isSaveOverlayOpen = false;
    private List<SaveSlotDto> overlaySlots = null;

    private boolean paused = false;
    private boolean pauseObjectivesOpen = false;
    private boolean pauseControlsOpen = false;
    private boolean returningToLauncher = false;
    private volatile boolean levelTransitionInProgress = false;
    private boolean isDualViewDebugMode = false;
    private boolean showAxisDebug = false;
    private String partnerBanner = null;
    private float partnerBannerSecondsLeft = 0f;

    private static class ActiveBomb {
        float startX, startY;
        float targetX, targetY;
        float timeElapsed = 0f;
        float totalDuration = 0.8f;
    }

    private static class ActiveExplosion {
        float x, y;
        float timeElapsed = 0f;
        float totalDuration = 0.4f;
    }

    private static class PlayerAnimState {
        float lastX = -1f, lastY = -1f;
        int currentRow = 0;
        int currentColumn = 0;
        float stateTime = 0f;

        boolean isAttacking = false;
        float attackTime = 0f;
        boolean damageApplied = false;

        boolean moving = false;
        TextureRegion currentFrame = null;
        float currentDrawWidth = 0f;
        float currentDrawHeight = 0f;
    }
    private final PlayerAnimState aiJaneAnim = new PlayerAnimState();

    private static class ZombieAnimState {
        int currentRow = 1;
        int currentColumn = 0;
        float stateTime = 0f;
    }

    public GameScreen(InfectedHourGame game, GameClient client, GameBridge bridge, int levelNumber) {
        this.game = game;
        this.client = client;
        this.bridge = bridge;
        this.levelNumber = levelNumber;
    }

    private Texture loadTextureSafely(String internalPath) {
        try {
            if (Gdx.files.internal(internalPath).exists()) {
                Texture tex = new Texture(Gdx.files.internal(internalPath));
                tex.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
                return tex;
            }
        } catch (Exception e) {
            Gdx.app.error("GameScreen", "Optional texture missing (" + internalPath + "): " + e.getMessage());
        }
        return null;
    }

    private Texture createColorTexture(int width, int height, Color color) {
        Pixmap pixmap = new Pixmap(width, height, Pixmap.Format.RGBA8888);
        pixmap.setColor(color);
        pixmap.fill();
        Texture tex = new Texture(pixmap);
        pixmap.dispose();
        return tex;
    }

    private void drawCollisionOverlay() {
        if (tileMap == null) return;
        float cell = PIXELS_PER_TILE * tileMap.getCellSize();

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapes.setProjectionMatrix(camera.combined);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(1f, 0f, 0f, 0.35f);

        float halfW = camera.viewportWidth * 0.5f + cell;
        float halfH = camera.viewportHeight * 0.5f + cell;
        int minCellX = Math.max(0, tileMap.toCell((camera.position.x - halfW) / PIXELS_PER_TILE));
        int maxCellX = Math.min(tileMap.getCollisionWidth() - 1, tileMap.toCell((camera.position.x + halfW) / PIXELS_PER_TILE));
        int minCellY = Math.max(0, tileMap.toCell((camera.position.y - halfH) / PIXELS_PER_TILE));
        int maxCellY = Math.min(tileMap.getCollisionHeight() - 1, tileMap.toCell((camera.position.y + halfH) / PIXELS_PER_TILE));

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

    /**
     * Follows a world position while keeping the camera viewport completely
     * inside the playable tile-map bounds.
     *
     * The player can still move all the way to the edge; only the camera stops.
     */
    private void updateCameraClamped(float targetWorldX, float targetWorldY) {
        if (camera == null) return;

        // Fallback if the map has not been initialized yet.
        if (tileMap == null) {
            camera.position.set(
                    Math.round(targetWorldX),
                    Math.round(targetWorldY),
                    0f
            );
            camera.update();
            return;
        }

        float mapWidthPx = tileMap.getWidth() * PIXELS_PER_TILE;
        float mapHeightPx = tileMap.getHeight() * PIXELS_PER_TILE;

        float halfViewWidth = (camera.viewportWidth * camera.zoom) * 0.5f;
        float halfViewHeight = (camera.viewportHeight * camera.zoom) * 0.5f;

        float cameraX;
        float cameraY;

        // If a map dimension is smaller than the camera viewport,
        // keep that dimension centered instead of trying to clamp it.
        if (mapWidthPx <= halfViewWidth * 2f) {
            cameraX = mapWidthPx * 0.5f;
        } else {
            cameraX = Math.max(
                    halfViewWidth,
                    Math.min(targetWorldX, mapWidthPx - halfViewWidth)
            );
        }

        if (mapHeightPx <= halfViewHeight * 2f) {
            cameraY = mapHeightPx * 0.5f;
        } else {
            cameraY = Math.max(
                    halfViewHeight,
                    Math.min(targetWorldY, mapHeightPx - halfViewHeight)
            );
        }

        camera.position.set(
                Math.round(cameraX),
                Math.round(cameraY),
                0f
        );
        camera.update();
    }

    private void drawMapAlignedToCollisionGrid() {
        float scale = PIXELS_PER_TILE / MAP_ART_TILE_PX;
        float gridBottomFromTexBottomPx = mapTexture.getHeight() - (MAP_ART_ORIGIN_Y_TOP_PX + MAP_ART_ROWS * MAP_ART_TILE_PX);

        batch.draw(mapTexture,
                -MAP_ART_ORIGIN_X_PX * scale,
                -gridBottomFromTexBottomPx * scale,
                mapTexture.getWidth() * scale,
                mapTexture.getHeight() * scale);
    }

    private void drawLevelMap() {
        if (levelNumber != 1) {
            batch.draw(mapTexture, 0f, 0f,
                    tileMap.getWidth() * PIXELS_PER_TILE,
                    tileMap.getHeight() * PIXELS_PER_TILE);
            return;
        }
        float scale = PIXELS_PER_TILE / MAP_ART_TILE_PX;
        float gridBottomFromTexBottomPx = mapTexture.getHeight() - (MAP_ART_ORIGIN_Y_TOP_PX + MAP_ART_ROWS * MAP_ART_TILE_PX);

        batch.draw(mapTexture,
                -MAP_ART_ORIGIN_X_PX * scale,
                -gridBottomFromTexBottomPx * scale,
                mapTexture.getWidth() * scale,
                mapTexture.getHeight() * scale);
    }

    private Texture createRoadsideVillageTexture() {
        final int pixelsPerTile = 16;
        int width = tileMap.getWidth() * pixelsPerTile;
        int height = tileMap.getHeight() * pixelsPerTile;

        Pixmap pixmap = new Pixmap(width, height, Pixmap.Format.RGBA8888);
        pixmap.setColor(0.055f, 0.12f, 0.08f, 1f);
        pixmap.fill();

        for (int y = 0; y < tileMap.getHeight(); y++) {
            for (int x = 0; x < tileMap.getWidth(); x++) {
                int px = x * pixelsPerTile;
                int py = (tileMap.getHeight() - 1 - y) * pixelsPerTile;
                boolean road = (x >= 26 && x <= 33) || (y >= 18 && y <= 22);

                if (!tileMap.isWalkable(x, y)) {
                    pixmap.setColor(((x + y) & 1) == 0
                            ? new Color(0.18f, 0.14f, 0.12f, 1f)
                            : new Color(0.23f, 0.17f, 0.13f, 1f));
                    pixmap.fillRectangle(px, py, pixelsPerTile, pixelsPerTile);
                    pixmap.setColor(0.38f, 0.25f, 0.12f, 1f);
                    pixmap.drawRectangle(px, py, pixelsPerTile, pixelsPerTile);
                } else if (road) {
                    pixmap.setColor(0.22f, 0.22f, 0.20f, 1f);
                    pixmap.fillRectangle(px, py, pixelsPerTile, pixelsPerTile);
                    if (((x + y) % 4) == 0) {
                        pixmap.setColor(0.55f, 0.46f, 0.24f, 0.55f);
                        pixmap.fillRectangle(px + 6, py + 6, 4, 4);
                    }
                } else if (((x * 13 + y * 7) % 11) == 0) {
                    pixmap.setColor(0.08f, 0.19f, 0.10f, 1f);
                    pixmap.fillCircle(px + 8, py + 8, 3);
                }
            }
        }

        pixmap.setColor(0.70f, 0.58f, 0.24f, 0.75f);
        int roadY = (tileMap.getHeight() - 1 - 20) * pixelsPerTile + 7;
        for (int x = 1; x < tileMap.getWidth() - 1; x += 4) {
            pixmap.fillRectangle(x * pixelsPerTile, roadY, pixelsPerTile * 2, 2);
        }

        Texture texture = new Texture(pixmap);
        texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        pixmap.dispose();
        return texture;
    }

    private void loadMacheteSound() {
        if (macheteSound != null) return;

        String[] candidates = {
                "music/machete.mp3",
                "machete.mp3"
        };

        for (String path : candidates) {
            try {
                if (Gdx.files.internal(path).exists()) {
                    macheteSound = Gdx.audio.newSound(Gdx.files.internal(path));
                    Gdx.app.log("GameScreen", "Loaded melee SFX: " + path);
                    return;
                }
            } catch (Exception e) {
                Gdx.app.error("GameScreen", "Failed loading melee SFX: " + path, e);
            }
        }

        Gdx.app.error(
                "GameScreen",
                "machete.mp3 not found. Expected assets/music/machete.mp3"
        );
    }

    private void playMacheteSound() {
        if (macheteSound != null) {
            macheteSound.play(MACHETE_SOUND_VOLUME);
        }
    }

    private void loadSwingSound() {
        if (swingSound != null) return;

        String[] candidates = {
                "music/swing.mp3",
                "swing.mp3"
        };

        for (String path : candidates) {
            try {
                if (Gdx.files.internal(path).exists()) {
                    swingSound = Gdx.audio.newSound(Gdx.files.internal(path));
                    Gdx.app.log("GameScreen", "Loaded swing SFX: " + path);
                    return;
                }
            } catch (Exception e) {
                Gdx.app.error("GameScreen", "Failed loading swing SFX: " + path, e);
            }
        }

        Gdx.app.error(
                "GameScreen",
                "swing.mp3 not found. Expected assets/music/swing.mp3"
        );
    }

    private void playSwingSound() {
        if (swingSound != null) {
            swingSound.play(SWING_SOUND_VOLUME);
        }
    }

    private void loadBombSound() {
        if (bombSound != null) return;

        String[] candidates = {
                "music/bomb.mp3",
                "bomb.mp3"
        };

        for (String path : candidates) {
            try {
                if (Gdx.files.internal(path).exists()) {
                    bombSound = Gdx.audio.newSound(Gdx.files.internal(path));
                    Gdx.app.log("GameScreen", "Loaded bomb SFX: " + path);
                    return;
                }
            } catch (Exception e) {
                Gdx.app.error("GameScreen", "Failed loading bomb SFX: " + path, e);
            }
        }

        Gdx.app.error(
                "GameScreen",
                "bomb.mp3 not found. Expected assets/music/bomb.mp3"
        );
    }

    private void playBombSound() {
        if (bombSound != null) {
            bombSound.play(BOMB_SOUND_VOLUME);
        }
    }

    private void startLevelMusic() {
        stopLevelMusic();

        String musicPath;
        if (levelNumber == 1 || levelNumber == 2) {
            musicPath = "music/lvl1-2.mp3";
        } else if (levelNumber == 3) {
            musicPath = "music/lvl3.mp3";
        } else {
            return;
        }

        try {
            if (!Gdx.files.internal(musicPath).exists()) {
                Gdx.app.error("GameScreen", "Level music not found: " + musicPath);
                return;
            }

            levelMusic = Gdx.audio.newMusic(Gdx.files.internal(musicPath));
            levelMusic.setLooping(true);
            float masterMusic = bridge != null ? bridge.getMusicVolume() : 1.0f;
            levelMusic.setVolume(LEVEL_MUSIC_VOLUME * masterMusic);
            levelMusic.play();

            Gdx.app.log(
                    "GameScreen",
                    "Playing level music: " + musicPath + " for level " + levelNumber
            );
        } catch (Exception e) {
            Gdx.app.error(
                    "GameScreen",
                    "Failed to load/play level music: " + musicPath,
                    e
            );
            levelMusic = null;
        }
    }

    private void stopLevelMusic() {
        if (levelMusic != null) {
            try {
                levelMusic.stop();
                levelMusic.dispose();
            } catch (Exception ignored) {
            }
            levelMusic = null;
        }
    }

    @Override
    public void show() {
        startLevelMusic();
        loadMacheteSound();
        loadSwingSound();
        loadBombSound();

        batch = new SpriteBatch();
        shapes = new ShapeRenderer();
        font = new BitmapFont();
        pauseFont = new BitmapFont();
        pauseFont.getData().setScale(1.25f);
        pauseFont.getRegion().getTexture().setFilter(
                Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);

        camera = new OrthographicCamera();
        viewport = new FitViewport(VIRTUAL_WIDTH, VIRTUAL_HEIGHT, camera);
        viewport.apply();

        LevelLoader levelLoader = new LevelLoader();
        LevelDefinition def = levelLoader.loadDefinition(levelNumber);
        tileMap = levelLoader.loadMap(def);
        levelFeatures = CampaignLevelPlan.featuresFor(levelNumber);

        if (game.getSession() != null && game.getSession().debugSplitScreen()) {
            isDualViewDebugMode = true;
        }
        if (game.getSession() != null && game.getSession().showCollisionOverlay()) {
            showCollisionOverlay = true;
        }

        if (levelNumber == 1) {
            middleZombieX = 22.0f;
            middleZombieY = 16.0f;
            middleZombieHp = middleZombieMaxHp;
            isZombieDead = false;

            levelVillagers.clear();
            levelVillagers.add(new LevelVillager("v1", "Dr. Ramirez", 18.5f, 28.5f));
            levelVillagers.add(new LevelVillager("v2", "Nurse Claire", 35.5f, 28.5f));

            ambushZombies.clear();
            isAmbushActive = false;
            isAmbushDefeated = false;
            hasStairsKey = false;
            keyX = 24.5f;
            keyY = 26.5f;
            herb1Collected = false;
            herb2Collected = false;
            herbPartsCollected = 0;
            hasReviveKit = false;
            hasPickedFloorMedkit = false;
            isJaneRevived = false;
            janeX = 40.0f;
            janeY = 8.0f;
            janeHp = GameConstants.PLAYER_MAX_HP;
            janeMedkitTimer = 60.0f;
            coins = CampaignSquadState.coins;
            hasBomb = CampaignSquadState.hasBomb;
            isBombEquipped = CampaignSquadState.isBombEquipped;
            hasMachete = true;
            isMacheteEquipped = true;
            CampaignSquadState.hasMachete = true;
            CampaignSquadState.isMacheteEquipped = true;
            isJaneRevived = false;
            CampaignSquadState.isJaneRevived = false;
            showObjectivesOverlay = false;
            currentImmunityTime = 999999f;
            maxImmunityTime = 999999f;
            client.sendEvent("PLAYER_HEAL", "100");
        } else if (levelNumber == 2) {
            // Carry over coins and inventory from Level 1
            coins = CampaignSquadState.coins;
            hasBomb = CampaignSquadState.hasBomb;
            isBombEquipped = CampaignSquadState.isBombEquipped;
            hasMachete = CampaignSquadState.hasMachete;
            isMacheteEquipped = CampaignSquadState.isMacheteEquipped;

            // Jane ally continuity (revived with Katana)
            isJaneRevived = true;
            janeX = 3.5f;
            janeY = 37.22f;
            janeHp = CampaignSquadState.janeHp > 0f ? CampaignSquadState.janeHp : GameConstants.PLAYER_MAX_HP;

            // Rescued hospital villagers carry over as active followers
            levelVillagers.clear();
            boolean hasV1 = CampaignSquadState.rescuedVillagers.isEmpty() || CampaignSquadState.rescuedVillagers.stream().anyMatch(info -> "v1".equals(info.id()));
            boolean hasV2 = CampaignSquadState.rescuedVillagers.isEmpty() || CampaignSquadState.rescuedVillagers.stream().anyMatch(info -> "v2".equals(info.id()));
            if (hasV1) {
                LevelVillager v1 = new LevelVillager("v1", "Dr. Ramirez", 6.0f, 35.5f);
                v1.isRescued = true;
                levelVillagers.add(v1);
            }
            if (hasV2) {
                LevelVillager v2 = new LevelVillager("v2", "Nurse Claire", 3.5f, 35.5f);
                v2.isRescued = true;
                levelVillagers.add(v2);
            }

            // Level 2 stranded villagers awaiting rescue
            boolean hasV3 = CampaignSquadState.rescuedVillagers.stream().anyMatch(info -> "v3".equals(info.id()));
            LevelVillager v3 = new LevelVillager("v3", "Survivor Arthur", 16.96f, 32.86f);
            v3.isRescued = hasV3;
            levelVillagers.add(v3);

            boolean hasV4 = CampaignSquadState.rescuedVillagers.stream().anyMatch(info -> "v4".equals(info.id()));
            LevelVillager v4 = new LevelVillager("v4", "Survivor Maya", 51.0f, 33.65f);
            v4.isRescued = hasV4;
            levelVillagers.add(v4);

            levelFeatures.stream()
                    .filter(feature -> feature.type() == CampaignLevelPlan.FeatureType.ZOMBIE_ENCOUNTER)
                    .findFirst()
                    .ifPresent(feature -> {
                        middleZombieX = feature.tileX();
                        middleZombieY = feature.tileY();
                    });
        } else if (levelNumber >= 3) {
            coins = CampaignSquadState.coins;
            hasBomb = CampaignSquadState.hasBomb;
            isBombEquipped = CampaignSquadState.isBombEquipped;
            hasMachete = CampaignSquadState.hasMachete;
            isMacheteEquipped = CampaignSquadState.isMacheteEquipped;

            isJaneRevived = true;
            CampaignSquadState.isJaneRevived = true;
            Checkpoint levelStart = CheckpointRegistry.firstOf(levelNumber);
            boolean squadStartsAtCheckpoint = levelNumber >= 3 && levelNumber <= 6;
            janeX = squadStartsAtCheckpoint ? levelStart.spawnTileX() + 1.0f : 28.0f;
            janeY = squadStartsAtCheckpoint ? levelStart.spawnTileY() : 10.0f;
            janeHp = CampaignSquadState.janeHp > 0f ? CampaignSquadState.janeHp : GameConstants.PLAYER_MAX_HP;

            levelVillagers.clear();
            if (levelNumber == 3) {
                // Villagers accompany the squad through Level 3 only. Their
                // final outcome is recorded when this map is completed.
                float[][] offsets = {{-1.0f, 0.0f}, {2.0f, 0.0f}, {-1.0f, -1.0f}, {1.0f, -1.0f}};
                List<CampaignSquadState.RescuedVillagerInfo> arrivingVillagers = CampaignSquadState.rescuedVillagers;
                boolean directLevelDebug = game.getSession() != null
                        && game.getSession().startingLevelOverride() == levelNumber;
                if (arrivingVillagers.isEmpty() && directLevelDebug) {
                    // A direct level-select run has no earlier maps from which to
                    // build a roster, so populate all four rescueable villagers
                    // to make the selected level's squad start testable in isolation.
                    arrivingVillagers = List.of(
                            new CampaignSquadState.RescuedVillagerInfo("v1", "Dr. Ramirez", 100f),
                            new CampaignSquadState.RescuedVillagerInfo("v2", "Nurse Claire", 100f),
                            new CampaignSquadState.RescuedVillagerInfo("v3", "Survivor Arthur", 100f),
                            new CampaignSquadState.RescuedVillagerInfo("v4", "Survivor Maya", 100f));
                }
                int slot = 0;
                for (CampaignSquadState.RescuedVillagerInfo info : arrivingVillagers) {
                    if (info.hp() <= 0f) continue;
                    float[] offset = offsets[Math.min(slot, offsets.length - 1)];
                    LevelVillager villager = new LevelVillager(info.id(), info.name(),
                            levelStart.spawnTileX() + offset[0], levelStart.spawnTileY() + offset[1]);
                    villager.hp = Math.min(villager.maxHp, info.hp());
                    villager.isRescued = true;
                    levelVillagers.add(villager);
                    slot++;
                }
            }

            isZombieDead = true;
            isAmbushActive = true;
            isAmbushDefeated = false;
            ambushZombies.clear();

            if (levelNumber == 6) {
                AmbushZombie virusHeart = new AmbushZombie(30.5f, 20.5f, true);
                virusHeart.maxHp = 150f;
                virusHeart.hp = 150f;
                ambushZombies.add(virusHeart);
                ambushZombies.add(new AmbushZombie(24.5f, 18.5f, false));
                ambushZombies.add(new AmbushZombie(36.5f, 22.5f, false));
                ambushZombies.add(new AmbushZombie(28.5f, 26.5f, false));
                ambushZombies.add(new AmbushZombie(32.5f, 14.5f, false));
            } else if (levelNumber == 5) {
                AmbushZombie redBrute = new AmbushZombie(30.5f, 20.5f, true);
                redBrute.maxHp = 100f;
                redBrute.hp = 100f;
                ambushZombies.add(redBrute);
                // Both regular zombies are on the connected walkable corridor.
                ambushZombies.add(new AmbushZombie(28.5f, 18.5f, false));
                ambushZombies.add(new AmbushZombie(35.5f, 22.5f, false));
            } else {
                AmbushZombie sectorBoss = new AmbushZombie(30.5f, 20.5f, true);
                sectorBoss.maxHp = 60f;
                sectorBoss.hp = 60f;
                ambushZombies.add(sectorBoss);
                ambushZombies.add(new AmbushZombie(25.5f, 18.5f, false));
                ambushZombies.add(new AmbushZombie(35.5f, 22.5f, false));
            }
        }

        this.collisionSystem = new CollisionSystem(tileMap);

        if (levelNumber == 3) {
            hasAmbulanceKey = false;
            placeAmbulanceKeyAtRandomWalkableLocation();
        } else if (levelNumber == 4) {
            level4PuzzlePartsCollected = 0;
            placeLevelFourPuzzleParts();
        } else if (levelNumber == 5) {
            hasLabPasskey = false;
        }

        if (game.isHost()) {
            game.getServer().loadTileMap(tileMap);
            game.getServer().setMapWidthInTiles(levelLoader.getMapWidthInTiles());
            game.getServer().configureLevel(def);
        }

        if (levelNumber == 1) {
            mapTexture = loadTextureSafely("map1.png");
            if (mapTexture == null) {
                mapTexture = loadTextureSafely("map.png");
            }
            if (mapTexture != null) {
                mapTexture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
            }
        } else if (levelNumber == 2) {
            mapTexture = loadTextureSafely("map1_part2.png");
            if (mapTexture == null) {
                mapTexture = loadTextureSafely("map1__part2.png");
            }
            if (mapTexture != null) {
                mapTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            }
        } else if (levelNumber == 3) {
            mapTexture = loadTextureSafely("map2.png");
            if (mapTexture != null) {
                mapTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            }
        } else if (levelNumber == 4) {
            mapTexture = loadTextureSafely("map2_part2.png");
            if (mapTexture != null) {
                mapTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            }
        } else if (levelNumber == 5) {
            mapTexture = loadTextureSafely("map3.png");
            if (mapTexture != null) {
                mapTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            }
        } else if (levelNumber == 6) {
            mapTexture = loadTextureSafely("map_final.png");
            if (mapTexture != null) {
                mapTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            }
        }

        if (mapTexture == null) {
            mapTexture = createRoadsideVillageTexture();
        }
        markerTexture = createColorTexture(1, 1, Color.WHITE);
        keyTexture = loadTextureSafely("key.png");

        if (levelNumber == 1 && mapTexture != null) {
            int srcW = Math.min((int) (45 * MAP_ART_TILE_PX), mapTexture.getWidth() - 32);
            int srcH = Math.min((int) (33 * MAP_ART_TILE_PX), mapTexture.getHeight() - 32);
            minimapRegion = new TextureRegion(mapTexture, 32, 32, srcW, srcH);
        } else if (mapTexture != null) {
            minimapRegion = new TextureRegion(mapTexture);
        }

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

        // Villager spritesheets: assets/Villager/
        // All sheets: 4 rows x 8 columns.
        // Row order: DOWN, LEFT, RIGHT, UP.
        villagerV1WalkTexture = loadTextureSafely("Villager/v1.png");
        villagerV1IdleTexture = loadTextureSafely("Villager/v1_idle.png");
        villagerV2WalkTexture = loadTextureSafely("Villager/v2.png");
        villagerV2IdleTexture = loadTextureSafely("Villager/v2_idle.png");

        if (villagerV1WalkTexture == null || villagerV1IdleTexture == null) {
            Gdx.app.error(
                    "GameScreen",
                    "V1 SPRITES MISSING: expected assets/Villager/v1.png and assets/Villager/v1_idle.png."
            );
        }

        if (villagerV2WalkTexture == null || villagerV2IdleTexture == null) {
            Gdx.app.error(
                    "GameScreen",
                    "V2 SPRITES MISSING: expected assets/Villager/v2.png and assets/Villager/v2_idle.png."
            );
        }

        if (villagerV1WalkTexture != null) {
            villagerV1WalkTexture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
            if (villagerV1WalkTexture.getWidth() % 8 != 0 || villagerV1WalkTexture.getHeight() % 4 != 0) {
                Gdx.app.error(
                        "GameScreen",
                        "Villager/v1.png must be a true 4x8 sheet. Size found: "
                                + villagerV1WalkTexture.getWidth() + "x" + villagerV1WalkTexture.getHeight()
                );
            }
            villagerV1WalkFrameWidth = villagerV1WalkTexture.getWidth() / 8;
            villagerV1WalkFrameHeight = villagerV1WalkTexture.getHeight() / 4;
            villagerV1WalkFrames = TextureRegion.split(
                    villagerV1WalkTexture,
                    villagerV1WalkFrameWidth,
                    villagerV1WalkFrameHeight
            );
        }

        if (villagerV1IdleTexture != null) {
            villagerV1IdleTexture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
            if (villagerV1IdleTexture.getWidth() % 8 != 0 || villagerV1IdleTexture.getHeight() % 4 != 0) {
                Gdx.app.error(
                        "GameScreen",
                        "Villager/v1_idle.png must be a true 4x8 sheet. Size found: "
                                + villagerV1IdleTexture.getWidth() + "x" + villagerV1IdleTexture.getHeight()
                );
            }
            villagerV1IdleFrameWidth = villagerV1IdleTexture.getWidth() / 8;
            villagerV1IdleFrameHeight = villagerV1IdleTexture.getHeight() / 4;
            villagerV1IdleFrames = TextureRegion.split(
                    villagerV1IdleTexture,
                    villagerV1IdleFrameWidth,
                    villagerV1IdleFrameHeight
            );
        }

        if (villagerV2WalkTexture != null) {
            villagerV2WalkTexture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
            if (villagerV2WalkTexture.getWidth() % 8 != 0 || villagerV2WalkTexture.getHeight() % 4 != 0) {
                Gdx.app.error(
                        "GameScreen",
                        "Villager/v2.png must be a true 4x8 sheet. Size found: "
                                + villagerV2WalkTexture.getWidth() + "x" + villagerV2WalkTexture.getHeight()
                );
            }
            villagerV2WalkFrameWidth = villagerV2WalkTexture.getWidth() / 8;
            villagerV2WalkFrameHeight = villagerV2WalkTexture.getHeight() / 4;
            villagerV2WalkFrames = TextureRegion.split(
                    villagerV2WalkTexture,
                    villagerV2WalkFrameWidth,
                    villagerV2WalkFrameHeight
            );
        }

        if (villagerV2IdleTexture != null) {
            villagerV2IdleTexture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
            if (villagerV2IdleTexture.getWidth() % 8 != 0 || villagerV2IdleTexture.getHeight() % 4 != 0) {
                Gdx.app.error(
                        "GameScreen",
                        "Villager/v2_idle.png must be a true 4x8 sheet. Size found: "
                                + villagerV2IdleTexture.getWidth() + "x" + villagerV2IdleTexture.getHeight()
                );
            }
            villagerV2IdleFrameWidth = villagerV2IdleTexture.getWidth() / 8;
            villagerV2IdleFrameHeight = villagerV2IdleTexture.getHeight() / 4;
            villagerV2IdleFrames = TextureRegion.split(
                    villagerV2IdleTexture,
                    villagerV2IdleFrameWidth,
                    villagerV2IdleFrameHeight
            );
        }

        // Jane unconscious sprite (single frame).
        janeKnockedTexture = loadTextureSafely("player 2/jane_knocked.png");
        if (janeKnockedTexture != null) {
            janeKnockedTexture.setFilter(
                    Texture.TextureFilter.Nearest,
                    Texture.TextureFilter.Nearest
            );
        } else {
            Gdx.app.error(
                    "GameScreen",
                    "Jane knocked sprite missing: assets/player 2/jane_knocked.png"
            );
        }

        // Player 2 / Jane sprite sheets from player 2/ directory
        p2Texture = loadTextureSafely("player 2/player.png");
        if (p2Texture == null) p2Texture = loadTextureSafely("player 2/map_player.png");
        if (p2Texture != null) {
            p2Texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
            p2Frames = TextureRegion.split(p2Texture, p2Texture.getWidth() / 8, p2Texture.getHeight() / 4);
        }

        p2IdleTexture = loadTextureSafely("player 2/player_idle_final.png");
        if (p2IdleTexture == null) p2IdleTexture = loadTextureSafely("player 2/player_idle.png");
        if (p2IdleTexture != null) {
            p2IdleTexture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
            p2IdleFrames = TextureRegion.split(p2IdleTexture, p2IdleTexture.getWidth() / 8, p2IdleTexture.getHeight() / 4);
        }

        p2SprintTexture = loadTextureSafely("player 2/sprint.png");
        if (p2SprintTexture != null) {
            p2SprintTexture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
            p2SprintFrames = TextureRegion.split(p2SprintTexture, p2SprintTexture.getWidth() / 8, p2SprintTexture.getHeight() / 4);
        }

        p2MeleeTexture = loadTextureSafely("player 2/player_melee.png");
        if (p2MeleeTexture != null) {
            p2MeleeTexture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
            p2MeleeFrames = TextureRegion.split(p2MeleeTexture, p2MeleeTexture.getWidth() / 8, p2MeleeTexture.getHeight() / 4);
        }

        p2MeleeHitTexture = loadTextureSafely("player 2/melee_hit.png");
        if (p2MeleeHitTexture != null) {
            p2MeleeHitTexture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
            p2MeleeHitFrameWidth = p2MeleeHitTexture.getWidth() / 2;
            p2MeleeHitFrameHeight = p2MeleeHitTexture.getHeight() / 4;
            p2MeleeHitFrames = TextureRegion.split(p2MeleeHitTexture, p2MeleeHitFrameWidth, p2MeleeHitFrameHeight);
        }

        p2IdleMeleeTexture = loadTextureSafely("player 2/player_idle_melee.png");
        if (p2IdleMeleeTexture != null) {
            p2IdleMeleeTexture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
            p2IdleMeleeFrames = TextureRegion.split(p2IdleMeleeTexture, p2IdleMeleeTexture.getWidth() / 8, p2IdleMeleeTexture.getHeight() / 4);
        }

        p2BombTexture = loadTextureSafely("player 2/player_bomb.png");
        if (p2BombTexture != null) {
            p2BombTexture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
            p2BombFrames = TextureRegion.split(p2BombTexture, p2BombTexture.getWidth() / 8, p2BombTexture.getHeight() / 4);
        }

        p2BombIdleTexture = loadTextureSafely("player 2/player_bomb_idle.png");
        if (p2BombIdleTexture != null) {
            p2BombIdleTexture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
            p2BombIdleFrames = TextureRegion.split(p2BombIdleTexture, p2BombIdleTexture.getWidth() / 8, p2BombIdleTexture.getHeight() / 4);
        }

        femalePlayerTexture = loadTextureSafely("player 2/female_sprite_sheet_v3.png");
        if (femalePlayerTexture == null) femalePlayerTexture = loadTextureSafely("player 2/map_player.png");
        if (femalePlayerTexture == null) femalePlayerTexture = loadTextureSafely("female/female_sprite_sheet_v2.png");
        if (femalePlayerTexture != null) {
            femalePlayerTexture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
            femaleFrameWidth = femalePlayerTexture.getWidth() / 8;
            femaleFrameHeight = femalePlayerTexture.getHeight() / 4;
            femaleFrames = TextureRegion.split(femalePlayerTexture, femaleFrameWidth, femaleFrameHeight);
        }

        playerSprintTexture = loadTextureSafely("sprint.png");
        if (playerSprintTexture != null) {
            playerSprintFrames = TextureRegion.split(playerSprintTexture, playerSprintTexture.getWidth() / 8, playerSprintTexture.getHeight() / 4);
        } else {
            playerSprintFrames = playerFrames;
        }

        playerMeleeTexture = loadTextureSafely("player_melee.png");
        if (playerMeleeTexture != null) {
            playerMeleeFrames = TextureRegion.split(playerMeleeTexture, frameWidth, frameHeight);
        } else {
            playerMeleeFrames = playerFrames;
        }

        idleMeleeTexture = loadTextureSafely("player_idle_melee.png");
        if (idleMeleeTexture != null) {
            idleMeleeFrames = TextureRegion.split(idleMeleeTexture, idleFrameWidth, idleFrameHeight);
        } else {
            idleMeleeFrames = idleFrames;
        }

        playerBombTexture = loadTextureSafely("player_bomb.png");
        if (playerBombTexture != null) {
            pbFrameWidth = playerBombTexture.getWidth() / 8;
            pbFrameHeight = playerBombTexture.getHeight() / 4;
            playerBombFrames = TextureRegion.split(playerBombTexture, pbFrameWidth, pbFrameHeight);
        } else {
            pbFrameWidth = frameWidth;
            pbFrameHeight = frameHeight;
            playerBombFrames = playerFrames;
        }

        playerBombIdleTexture = loadTextureSafely("player_bomb_idle.png");
        if (playerBombIdleTexture != null) {
            int pbIdleFrameWidth = playerBombIdleTexture.getWidth() / 8;
            int pbIdleFrameHeight = playerBombIdleTexture.getHeight() / 4;
            playerBombIdleFrames = TextureRegion.split(playerBombIdleTexture, pbIdleFrameWidth, pbIdleFrameHeight);
        } else {
            playerBombIdleFrames = idleFrames;
        }

        playerBombThrowTexture = loadTextureSafely("bomb_throw.png");
        if (playerBombThrowTexture != null) {
            int pbtFrameWidth = playerBombThrowTexture.getWidth() / 2;
            int pbtFrameHeight = playerBombThrowTexture.getHeight() / 4;
            playerBombThrowFrames = TextureRegion.split(playerBombThrowTexture, pbtFrameWidth, pbtFrameHeight);
        } else {
            playerBombThrowFrames = playerFrames;
        }

        bombTexture = loadTextureSafely("bomb.png");
        if (bombTexture == null) {
            bombTexture = createColorTexture(16 * 8, 16, new Color(0.25f, 0.25f, 0.3f, 1f));
        }
        bombFrameWidth = bombTexture.getWidth() / 8;
        bombFrames = TextureRegion.split(bombTexture, bombFrameWidth, bombTexture.getHeight());

        bombEffectTexture = loadTextureSafely("bomb_effect.png");
        if (bombEffectTexture == null) {
            bombEffectTexture = createColorTexture(32 * 8, 32, new Color(1f, 0.6f, 0.1f, 0.85f));
        }
        bombEffectFrameWidth = bombEffectTexture.getWidth() / 8;
        bombEffectFrames = TextureRegion.split(bombEffectTexture, bombEffectFrameWidth, bombEffectTexture.getHeight());

        damagedScreen1Texture = loadTextureSafely("damaged_screen1.png");
        damagedScreen2Texture = loadTextureSafely("damaged_screen2.png");
        healIconTexture = loadTextureSafely("heal_icon.png");
        healEffectTexture = loadTextureSafely("heal_effect.png");

        // Herb pickup: 2-frame horizontal spritesheet.
        herbTexture = loadTextureSafely("herb.png");
        if (herbTexture != null) {
            herbTexture.setFilter(
                    Texture.TextureFilter.Nearest,
                    Texture.TextureFilter.Nearest
            );

            if (herbTexture.getWidth() % 2 != 0) {
                Gdx.app.error(
                        "GameScreen",
                        "herb.png must contain 2 equal horizontal frames. Width found: "
                                + herbTexture.getWidth()
                );
            }

            herbFrameWidth = herbTexture.getWidth() / 2;
            TextureRegion[][] herbSplit = TextureRegion.split(
                    herbTexture,
                    herbFrameWidth,
                    herbTexture.getHeight()
            );

            if (herbSplit.length > 0 && herbSplit[0].length >= 2) {
                herbFrames = new TextureRegion[]{
                        herbSplit[0][0],
                        herbSplit[0][1]
                };
            }
        } else {
            Gdx.app.error("GameScreen", "Missing ground pickup sprite: assets/herb.png");
        }

        // Floor medkit pickup: single image.
        medicTexture = loadTextureSafely("medic.png");
        if (medicTexture != null) {
            medicTexture.setFilter(
                    Texture.TextureFilter.Nearest,
                    Texture.TextureFilter.Nearest
            );
        } else {
            Gdx.app.error("GameScreen", "Missing ground pickup sprite: assets/medic.png");
        }

        timerTexture = loadTextureSafely("timer.png");
        if (timerTexture != null) {
            timerFrameWidth = timerTexture.getWidth() / 8;
            TextureRegion[][] tSplit = TextureRegion.split(timerTexture, timerFrameWidth, timerTexture.getHeight());
            timerFrames = tSplit[0];
        }

        zombieTexture = new Texture(Gdx.files.internal("zombie.png"));
        zombieTexture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        zombieFrameWidth = zombieTexture.getWidth() / 8;
        zombieFrameHeight = zombieTexture.getHeight() / 4;
        zombieFrames = TextureRegion.split(zombieTexture, zombieFrameWidth, zombieFrameHeight);

        for (int r = 0; r < zombieFrames.length; r++) {
            for (int c = 0; c < zombieFrames[r].length; c++) {
                zombieFrames[r][c].setRegionHeight(Math.max(1, zombieFrameHeight - 2));
            }
        }

        zombieIdleTexture = loadTextureSafely("zombie_idle.png");
        if (zombieIdleTexture != null) {
            int zIdleFrameH = zombieIdleTexture.getHeight() / 4;
            zombieIdleFrames = TextureRegion.split(zombieIdleTexture, zombieIdleTexture.getWidth() / 8, zIdleFrameH);
            for (int r = 0; r < zombieIdleFrames.length; r++) {
                for (int c = 0; c < zombieIdleFrames[r].length; c++) {
                    zombieIdleFrames[r][c].setRegionHeight(Math.max(1, zIdleFrameH - 2));
                }
            }
        } else {
            zombieIdleFrames = zombieFrames;
        }

        zombieBiteTexture = loadTextureSafely("zombie_bite.png");
        if (zombieBiteTexture != null) {
            int bCols = 4;
            int bRows = 2;
            if (zombieBiteTexture.getHeight() > zombieBiteTexture.getWidth()) {
                bCols = 2;
                bRows = 4;
            }
            zombieBiteFrameWidth = zombieBiteTexture.getWidth() / bCols;
            zombieBiteFrameHeight = zombieBiteTexture.getHeight() / bRows;
            zombieBiteFrames = TextureRegion.split(zombieBiteTexture, zombieBiteFrameWidth, zombieBiteFrameHeight);
        } else {
            zombieBiteFrameWidth = zombieFrameWidth;
            zombieBiteFrameHeight = zombieFrameHeight;
            zombieBiteFrames = zombieFrames;
        }

        meleeTexture = loadTextureSafely("melee.png");
        if (meleeTexture == null) {
            meleeTexture = createColorTexture(24 * 7, 24, new Color(0.85f, 0.85f, 0.9f, 1f));
        }
        meleeFrameWidth = meleeTexture.getWidth() / 7;
        meleeGroundFrames = TextureRegion.split(meleeTexture, meleeFrameWidth, meleeTexture.getHeight());

        meleeHitTexture = loadTextureSafely("melee_hit.png");
        if (meleeHitTexture == null) {
            meleeHitTexture = createColorTexture(32 * 4, 32 * 2, new Color(1f, 1f, 1f, 0.9f));
        }
        int mCols = 4;
        int mRows = 2;
        if (meleeHitTexture.getHeight() > meleeHitTexture.getWidth()) {
            mCols = 2;
            mRows = 4;
        }
        meleeHitFrameWidth = meleeHitTexture.getWidth() / mCols;
        meleeHitFrameHeight = meleeHitTexture.getHeight() / mRows;
        meleeHitFrames = TextureRegion.split(meleeHitTexture, meleeHitFrameWidth, meleeHitFrameHeight);

        bloodTexture = loadTextureSafely("blood.png");
        if (bloodTexture == null) {
            bloodTexture = createColorTexture(24, 24, new Color(0.65f, 0.08f, 0.08f, 0.75f));
        }

        inventoryTexture = new Texture(Gdx.files.internal("inventory.png"));
        inventoryTexture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);

        meleeInventoryTexture = loadTextureSafely("melee_inventory.png");
        if (meleeInventoryTexture == null) {
            meleeInventoryTexture = createColorTexture(36, 36, new Color(0.7f, 0.75f, 0.85f, 1f));
        }

        client.setOnEvent(event -> {
            if (event.type == null) return;
            switch (event.type) {
                case GameConstants.EVENT_PARTNER_DISCONNECTED -> showBanner("Partner disconnected — waiting…");
                case GameConstants.EVENT_PARTNER_RECONNECTED -> showBanner("Partner reconnected");
                case GameConstants.EVENT_CONVERTED_TO_SOLO -> showBanner("Continuing solo");
                case GameConstants.EVENT_GAME_SAVED -> showBanner("Game Saved to Slot " + event.payload + "!");
                case GameConstants.EVENT_OBJECTIVE_PROGRESS -> Gdx.app.postRunnable(() -> {
                    completedFeatureIds.add(event.payload);
                    CampaignLevelPlan.findFeature(levelNumber, event.payload)
                            .ifPresent(feature -> showBanner("Objective updated: " + feature.label()));
                });
                case GameConstants.EVENT_PAUSE -> Gdx.app.postRunnable(() -> paused = true);
                case GameConstants.EVENT_RESUME -> Gdx.app.postRunnable(() -> {
                    paused = false;
                    pauseObjectivesOpen = false;
                    pauseControlsOpen = false;
                });
                case "REVIVE_JANE" -> Gdx.app.postRunnable(() -> {
                    isJaneRevived = true;
                    CampaignSquadState.isJaneRevived = true;
                    showBanner("JANE REVIVED! Player 2 controls unlocked! Fight together!");
                });
                case EVENT_ZOMBIE_KILLED -> Gdx.app.postRunnable(() -> applyRemoteZombieKill(event.payload));
                case EVENT_ZOMBIE_STATE -> {
                    if (!isZombieAuthority()) Gdx.app.postRunnable(() -> applyZombieState(event.payload));
                }
                case EVENT_STAIRS_KEY_TAKEN -> Gdx.app.postRunnable(this::onStairsKeyTaken);
                default -> { }
            }
        });

        client.setOnLevelTransition(transition -> {
            if (transition.nextLevelNumber != levelNumber + 1 || levelTransitionInProgress) {
                return;
            }
            levelTransitionInProgress = true;
            CampaignSquadState.coins = coins;
            CampaignSquadState.hasMachete = hasMachete;
            CampaignSquadState.isMacheteEquipped = isMacheteEquipped;
            CampaignSquadState.hasBomb = hasBomb;
            CampaignSquadState.isBombEquipped = isBombEquipped;
            CampaignSquadState.isJaneRevived = isJaneRevived;
            CampaignSquadState.janeHp = janeHp;
            if (levelNumber == 3) {
                int livingRescued = (int) levelVillagers.stream()
                        .filter(v -> v.isRescued && !v.isDead)
                        .count();
                CampaignSquadState.finalizeSurvivorReport(livingRescued);
            } else if (levelNumber < 3) {
                CampaignSquadState.rescuedVillagers.clear();
                for (LevelVillager v : levelVillagers) {
                    if (v.isRescued && !v.isDead) {
                        CampaignSquadState.rescuedVillagers.add(
                                new CampaignSquadState.RescuedVillagerInfo(v.id, v.name, v.hp));
                    }
                }
            } else {
                CampaignSquadState.rescuedVillagers.clear();
            }
            Gdx.app.postRunnable(() -> {
                StoryPanelScreen.Sequence cinematic = StoryPanelScreen.Sequence.afterCompletedLevel(levelNumber);
                game.setScreen(new StoryPanelScreen(game, client, bridge, cinematic, levelNumber));
            });
        });
    }

    private boolean isWalkable(float x, float y, float radius) {
        if (collisionSystem == null) return true;
        return !collisionSystem.overlapsBlockedTile(x, y, radius);
    }

    /** Picks a different searchable, reachable floor tile for each Level 3 run. */
    private void placeAmbulanceKeyAtRandomWalkableLocation() {
        List<Vector2> candidates = new ArrayList<>();
        Checkpoint start = CheckpointRegistry.firstOf(3);
        LevelExit extraction = LevelExit.forLevel(3).orElseThrow();

        for (int tileY = 1; tileY < tileMap.getHeight() - 1; tileY++) {
            for (int tileX = 1; tileX < tileMap.getWidth() - 1; tileX++) {
                float x = tileX + 0.5f;
                float y = tileY + 0.5f;
                if (!isWalkable(x, y, GameConstants.ENEMY_COLLISION_RADIUS)) continue;
                if (Math.hypot(x - start.spawnTileX(), y - start.spawnTileY()) < 8.0f) continue;
                if (Math.hypot(x - extraction.tileX(), y - extraction.tileY()) < 6.0f) continue;
                candidates.add(new Vector2(x, y));
            }
        }

        if (candidates.isEmpty()) {
            ambulanceKeyX = 30.5f;
            ambulanceKeyY = 20.5f;
            return;
        }

        Vector2 chosen = candidates.get(ThreadLocalRandom.current().nextInt(candidates.size()));
        ambulanceKeyX = chosen.x;
        ambulanceKeyY = chosen.y;
    }

    /** Places three separated puzzle parts only on floor reachable from the Level 4 entrance. */
    private void placeLevelFourPuzzleParts() {
        level4PuzzleParts.clear();
        Checkpoint start = CheckpointRegistry.firstOf(4);
        LevelExit exit = LevelExit.forLevel(4).orElseThrow();
        int width = tileMap.getWidth();
        int height = tileMap.getHeight();
        boolean[][] visited = new boolean[height][width];
        ArrayDeque<int[]> frontier = new ArrayDeque<>();
        int startX = TileMap.toTile(start.spawnTileX());
        int startY = TileMap.toTile(start.spawnTileY());
        frontier.add(new int[]{startX, startY});
        visited[startY][startX] = true;
        List<Vector2> candidates = new ArrayList<>();
        int[][] directions = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

        while (!frontier.isEmpty()) {
            int[] cell = frontier.removeFirst();
            float x = cell[0] + 0.5f;
            float y = cell[1] + 0.5f;
            if (Math.hypot(x - start.spawnTileX(), y - start.spawnTileY()) >= 6.0f
                    && Math.hypot(x - exit.tileX(), y - exit.tileY()) >= 5.0f) {
                candidates.add(new Vector2(x, y));
            }
            for (int[] direction : directions) {
                int nx = cell[0] + direction[0];
                int ny = cell[1] + direction[1];
                if (nx < 0 || ny < 0 || nx >= width || ny >= height || visited[ny][nx]) continue;
                if (!isWalkable(nx + 0.5f, ny + 0.5f, GameConstants.PLAYER_COLLISION_RADIUS)) continue;
                visited[ny][nx] = true;
                frontier.addLast(new int[]{nx, ny});
            }
        }

        while (level4PuzzleParts.size() < LEVEL_4_PUZZLE_PART_COUNT && !candidates.isEmpty()) {
            Vector2 chosen = candidates.remove(ThreadLocalRandom.current().nextInt(candidates.size()));
            level4PuzzleParts.add(chosen);
            int stillNeeded = LEVEL_4_PUZZLE_PART_COUNT - level4PuzzleParts.size();
            List<Vector2> separated = new ArrayList<>();
            for (Vector2 candidate : candidates) {
                if (candidate.dst(chosen) >= 6.0f) separated.add(candidate);
            }
            if (separated.size() >= stillNeeded) candidates = separated;
        }
    }

    private boolean hasLineOfSight(float x1, float y1, float x2, float y2) {
        float dist = (float) Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2));
        int steps = Math.max(1, (int) Math.ceil(dist / 0.2f));
        for (int i = 0; i <= steps; i++) {
            float t = (float) i / steps;
            float cx = x1 + (x2 - x1) * t;
            float cy = y1 + (y2 - y1) * t;
            if (!isWalkable(cx, cy, 0.1f)) {
                return false;
            }
        }
        return true;
    }

    private void handleLevelFeatureInteraction(WorldSnapshot.PlayerState player) {
        if (levelNumber != 2 || !Gdx.input.isKeyJustPressed(Input.Keys.E)) return;
        levelFeatures.stream()
                .filter(feature -> feature.type() != CampaignLevelPlan.FeatureType.ZOMBIE_ENCOUNTER)
                .filter(feature -> !completedFeatureIds.contains(feature.actionId()))
                .filter(feature -> feature.contains(player.x, player.y))
                .findFirst()
                .ifPresent(this::completeFeature);
    }

    private void completeFeature(CampaignLevelPlan.Feature feature) {
        if (!completedFeatureIds.add(feature.actionId())) return;
        client.sendEvent(GameConstants.EVENT_OBJECTIVE_PROGRESS, feature.actionId());
        showBanner(feature.label() + " complete");
        if (feature.type() == CampaignLevelPlan.FeatureType.SURVIVOR) {
            for (LevelVillager v : levelVillagers) {
                if (!v.isRescued && Math.hypot(v.x - feature.tileX(), v.y - feature.tileY()) <= 2.2f) {
                    v.isRescued = true;
                    CampaignSquadState.rescuedVillagers.add(new CampaignSquadState.RescuedVillagerInfo(v.id, v.name, v.hp));
                    showBanner(v.name + " joined squad! Protect them from infected bites.");
                    break;
                }
            }
        }
    }

    private CampaignLevelPlan.Feature nearbyIncompleteFeature(WorldSnapshot.PlayerState player) {
        if (player == null || levelNumber != 2) return null;
        return levelFeatures.stream()
                .filter(feature -> feature.type() != CampaignLevelPlan.FeatureType.ZOMBIE_ENCOUNTER)
                .filter(feature -> !completedFeatureIds.contains(feature.actionId()))
                .filter(feature -> feature.contains(player.x, player.y))
                .findFirst()
                .orElse(null);
    }

    @Override
    public void render(float delta) {
        WorldSnapshot snapshot = client.getInterpolatedSnapshot(System.currentTimeMillis());
        WorldSnapshot.PlayerState me = null;

        if (!paused) {
            if (bombCooldown > 0f) {
                bombCooldown -= delta;
            }

            if (healCooldown > 0f) {
                healCooldown = Math.max(0f, healCooldown - delta);
            }
            if (healEffectTimer > 0f) {
                healEffectTimer = Math.max(0f, healEffectTimer - delta);
            }
            if (healFloatingTextTimer > 0f) {
                healFloatingTextTimer = Math.max(0f, healFloatingTextTimer - delta);
            }
        }

        // Infinite Time: immunity does not deplete
        currentImmunityTime = 999999f;

        if (snapshot != null) {
            me = client.findLocalPlayer(snapshot);
        }

        boolean healPressed = Gdx.input.isKeyJustPressed(Input.Keys.H);
        if (healPressed && !paused && !isInventoryOpen && me != null && !me.downed && me.hp > 0f) {
            boolean isJane = (me.character == CharacterType.JANE);
            float healAmt = isJane ? 50f : 35f;

            LevelVillager woundedVillager = null;
            float nearestDist = 2.2f;
            for (LevelVillager v : levelVillagers) {
                if (v.isRescued && !v.isDead && v.hp < v.maxHp) {
                    float d = (float) Math.hypot(me.x - v.x, me.y - v.y);
                    if (d < nearestDist) {
                        nearestDist = d;
                        woundedVillager = v;
                    }
                }
            }

            if (woundedVillager != null) {
                if (healCooldown <= 0f) {
                    healCooldown = MAX_HEAL_COOLDOWN;
                    healEffectTimer = 1.5f;
                    woundedVillager.hp = Math.min(woundedVillager.maxHp, woundedVillager.hp + healAmt);
                    showBanner("Treated " + woundedVillager.name + "! (+" + (int) healAmt + " HP)");
                } else {
                    showBanner("First Aid on cooldown (" + String.format("%.1f", healCooldown) + "s)");
                }
            } else if (healCooldown <= 0f) {
                if (me.hp < GameConstants.PLAYER_MAX_HP) {
                    healCooldown = MAX_HEAL_COOLDOWN;
                    healEffectTimer = 1.5f;
                    healFloatingTextTimer = 1.5f;
                    client.sendEvent("PLAYER_HEAL", String.valueOf(healAmt));
                    showBanner("Used First Aid! (+" + (int) healAmt + " HP)");
                } else {
                    showBanner("Health is already full (" + (int) GameConstants.PLAYER_MAX_HP + " HP)!");
                }
            } else {
                showBanner("First Aid on cooldown (" + String.format("%.1f", healCooldown) + "s)");
            }
        }

        // P2 (Jane) Healing in Dual-View Mode [NUMPAD 9 / NUM 9]
        boolean p2HealPressed = Gdx.input.isKeyJustPressed(Input.Keys.NUMPAD_9) || Gdx.input.isKeyJustPressed(Input.Keys.NUM_9);
        if (p2HealPressed && isDualViewDebugMode && !paused && !isInventoryOpen && snapshot != null && snapshot.players != null && snapshot.players.size() > 1) {
            WorldSnapshot.PlayerState p2 = snapshot.players.get(1);
            if (p2 != null && !p2.downed && p2.hp > 0f && !(levelNumber == 1 && !isJaneRevived)) {
                LevelVillager woundedVillager = null;
                float nearestDist = 2.2f;
                for (LevelVillager v : levelVillagers) {
                    if (v.isRescued && !v.isDead && v.hp < v.maxHp) {
                        float d = (float) Math.hypot(p2.x - v.x, p2.y - v.y);
                        if (d < nearestDist) {
                            nearestDist = d;
                            woundedVillager = v;
                        }
                    }
                }
                if (woundedVillager != null) {
                    woundedVillager.hp = Math.min(woundedVillager.maxHp, woundedVillager.hp + 50f);
                    showBanner("Jane treated " + woundedVillager.name + "! (+50 HP)");
                } else {
                    showBanner("Jane: Field Medic ready (+50 HP)");
                }
            }
        }

        if (snapshot != null) {
            boolean isMultiplayerOrCoop = isDualViewDebugMode || (snapshot.players != null && snapshot.players.size() > 1);
            boolean allDead = true;
            if (isMultiplayerOrCoop && snapshot.players != null) {
                for (WorldSnapshot.PlayerState p : snapshot.players) {
                    if (levelNumber == 1 && !isJaneRevived && p.character == CharacterType.JANE) continue;
                    if (!p.downed && p.hp > 0f) {
                        allDead = false;
                        break;
                    }
                }
            } else {
                allDead = (me != null && (me.downed || me.hp <= 0f));
            }

            if (allDead) {
                Gdx.gl.glClearColor(0f, 0f, 0f, 1f);
                Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

                Matrix4 hudMatrix = new Matrix4().setToOrtho2D(0, 0, VIRTUAL_WIDTH, VIRTUAL_HEIGHT);
                batch.setProjectionMatrix(hudMatrix);
                batch.begin();
                font.setColor(Color.WHITE);
                font.draw(batch, "You are dead", VIRTUAL_WIDTH / 2f - 40f, VIRTUAL_HEIGHT / 2f);
                font.draw(batch, "Press any key to return to Main Menu", VIRTUAL_WIDTH / 2f - 115f, VIRTUAL_HEIGHT / 2f - 30f);
                batch.end();

                if (!returningToLauncher && (Gdx.input.isKeyJustPressed(Input.Keys.ANY_KEY) || Gdx.input.isButtonPressed(Input.Buttons.LEFT))) {
                    returningToLauncher = true;
                    bridge.notifyMatchEnded(new GameBridge.MatchOutcome("DEFEAT", levelNumber));
                    if (bridge.hasLauncher()) {
                        bridge.requestReturnToLauncher(() -> Gdx.app.postRunnable(Gdx.app::exit));
                    } else {
                        game.setScreen(new MainMenuScreen(game, client, bridge));
                    }
                }
                return;
            }

            if (me != null && "MELEE".equals(me.equippedWeapon) && !hasMachete) {
                hasMachete = true;
                if (!isBombEquipped) isMacheteEquipped = true;
                CampaignSquadState.hasMachete = true;
                CampaignSquadState.isMacheteEquipped = isMacheteEquipped;
            }
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.F11)) {
            com.infectedhour.core.display.DisplayManager.toggleDisplayMode();
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.F12)) {
            takeInGameScreenshot();
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            if (isSaveOverlayOpen) {
                isSaveOverlayOpen = false;
            } else if (isInventoryOpen) {
                isInventoryOpen = false;
            } else if (paused && (pauseObjectivesOpen || pauseControlsOpen)) {
                pauseObjectivesOpen = false;
                pauseControlsOpen = false;
            } else {
                paused = !paused;
                if (!paused) {
                    pauseObjectivesOpen = false;
                    pauseControlsOpen = false;
                }
                client.sendEvent(paused ? GameConstants.EVENT_PAUSE : GameConstants.EVENT_RESUME, "");
            }
        }

        boolean ctrlPressed = Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT) || Gdx.input.isKeyPressed(Input.Keys.CONTROL_RIGHT);
        if (ctrlPressed && Gdx.input.isKeyJustPressed(Input.Keys.S)) {
            isSaveOverlayOpen = !isSaveOverlayOpen;
            if (isSaveOverlayOpen) overlaySlots = bridge.getSaveSlots();
        }

        if (isSaveOverlayOpen) {
            renderSaveOverlay();
            return;
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.I)) {
            isInventoryOpen = !isInventoryOpen;
        }

        if (!paused && Gdx.input.isKeyJustPressed(Input.Keys.M)) {
            isMinimapOpen = !isMinimapOpen;
        }

        if (!paused && Gdx.input.isKeyJustPressed(Input.Keys.O)) {
            showObjectivesOverlay = !showObjectivesOverlay;
        }

        if (!paused && !isInventoryOpen && me != null) {
            handleLevelFeatureInteraction(me);
        }

        if (!paused && !isInventoryOpen && !levelTransitionInProgress
                && me != null && isNearLevelExit(me)
                && Gdx.input.isKeyJustPressed(Input.Keys.E)) {
            if (levelNumber == 1 && !hasStairsKey) {
                showBanner("The stairs are locked! Find the Staff Room key first!");
            } else if (levelNumber == 1 && !isAmbushDefeated) {
                long remaining = ambushZombies.stream().filter(z -> !z.dead).count();
                boolean bossAlive = ambushZombies.stream().anyMatch(z -> z.isBoss && !z.dead);
                if (bossAlive) {
                    showBanner("Defeat the Mutated Boss & corridor swarm first!");
                } else {
                    showBanner("Defeat the remaining " + remaining + " hostiles first!");
                }
            } else if (levelNumber == 3 && !isAmbushDefeated) {
                long remaining = ambushZombies.stream().filter(z -> !z.dead).count();
                showBanner("Clear all zombies before returning to the ambulance! (" + remaining + " remaining)");
            } else if (levelNumber == 3 && !hasAmbulanceKey) {
                showBanner("The ambulance is locked! Search the map for its key.");
            } else if (levelNumber == 4 && level4PuzzlePartsCollected < LEVEL_4_PUZZLE_PART_COUNT) {
                showBanner("The endpoint is locked! Find all 3 puzzle parts first ("
                        + level4PuzzlePartsCollected + "/3).");
            } else if (levelNumber == 5 && !hasLabPasskey) {
                showBanner("The laboratory gate is locked! Defeat the big red zombie for its passkey card.");
            } else if (areLevelObjectivesComplete(snapshot)) {
                if (levelNumber == 1) {
                    long livingRescued = levelVillagers.stream().filter(v -> !v.isDead && v.isRescued).count();
                    int villagerBonus = (int) (livingRescued * 5);
                    coins += villagerBonus;
                    showBanner("LEVEL 1 CLEARED! Rescued " + livingRescued + "/2 staff (+" + villagerBonus + " Coins)! Total Coins: " + coins);
                }
                String nextName = levelNumber == 1 ? "Hospital Floor 2" : (levelNumber == 2 ? "Roadside Village" : "Level " + (levelNumber + 1));
                client.sendEvent(GameConstants.EVENT_LEVEL_EXIT_REQUEST, String.valueOf(levelNumber));
            } else {
                showBanner("Complete remaining objectives before proceeding!");
            }
        }

        // Herb Part 1 Pickup:
        if (levelNumber == 1 && !herb1Collected && me != null) {
            float dx = me.x - HERB_1_X;
            float dy = me.y - HERB_1_Y;
            if (Math.hypot(dx, dy) <= 1.5f && Gdx.input.isKeyJustPressed(Input.Keys.E)) {
                herb1Collected = true;
                herbPartsCollected++;
                if (herbPartsCollected == 2) {
                    hasReviveKit = true;
                    showBanner("Crafted Mixed Herb Revive Kit! Head to Pharmacy to revive Jane.");
                } else {
                    showBanner("Picked up Herb Part (1/2)! Find the second part.");
                }
            }
        }

        // Consultation Room Medkit Pickup:
        if (levelNumber == 1 && !hasPickedFloorMedkit && me != null) {
            float dx = me.x - MEDKIT_X;
            float dy = me.y - MEDKIT_Y;
            if (Math.hypot(dx, dy) <= 1.5f && Gdx.input.isKeyJustPressed(Input.Keys.E)) {
                hasPickedFloorMedkit = true;
                healCooldown = 0f;
                showBanner("Picked up Field Medkit! First Aid ability is ready.");
            }
        }

        // Herb Part 2 Pickup:
        if (levelNumber == 1 && !herb2Collected && me != null) {
            float dx = me.x - HERB_2_X;
            float dy = me.y - HERB_2_Y;
            if (Math.hypot(dx, dy) <= 1.5f && Gdx.input.isKeyJustPressed(Input.Keys.E)) {
                herb2Collected = true;
                herbPartsCollected++;
                if (herbPartsCollected == 2) {
                    hasReviveKit = true;
                    showBanner("Crafted Mixed Herb Revive Kit! Head to Pharmacy to revive Jane.");
                } else {
                    showBanner("Picked up Herb Part (1/2)! Find the second part.");
                }
            }
        }

        // Senseless Jane in Pharmacy Interaction:
        if (levelNumber == 1 && !isJaneRevived && me != null) {
            float dx = me.x - JANE_PHARMACY_X;
            float dy = me.y - JANE_PHARMACY_Y;
            if (Math.hypot(dx, dy) <= 1.8f && Gdx.input.isKeyJustPressed(Input.Keys.E)) {
                if (hasReviveKit) {
                    hasReviveKit = false;
                    isJaneRevived = true;
                    CampaignSquadState.isJaneRevived = true;
                    CampaignSquadState.janeHp = janeHp;
                    coins += 3;
                    CampaignSquadState.coins = coins;
                    client.sendEvent("REVIVE_JANE", "");
                    showBanner("Jane Revived! (+3 Coins) Player 2 controls unlocked! Fight together!");
                } else {
                    showBanner("Jane is unconscious! Need Mixed Herb Revive Kit to wake her.");
                }
            }
        }

        // Downed Partner Revive Interaction (Co-op / Dual-View)
        if (snapshot != null && snapshot.players != null && me != null && !me.downed && me.hp > 0f) {
            for (WorldSnapshot.PlayerState otherPlayer : snapshot.players) {
                if (otherPlayer.playerId.equals(me.playerId)) continue;
                if (levelNumber == 1 && !isJaneRevived && otherPlayer.character == CharacterType.JANE) continue;
                if (otherPlayer.downed) {
                    float dist = (float) Math.hypot(me.x - otherPlayer.x, me.y - otherPlayer.y);
                    if (dist <= 1.8f && Gdx.input.isKeyJustPressed(Input.Keys.E)) {
                        client.sendEvent("PLAYER_REVIVE", otherPlayer.playerId);
                        showBanner("Revived " + (otherPlayer.character != null ? otherPlayer.character.name() : "partner") + "!");
                    }
                }
            }
        }

        // Villagers Interaction (All levels):
        WorldSnapshot.PlayerState p2 = (snapshot != null && snapshot.players != null && snapshot.players.size() > 1) ? snapshot.players.get(1) : null;
        for (LevelVillager v : levelVillagers) {
            if (!v.isDead && !v.isRescued) {
                // Elric / Local Player
                if (me != null) {
                    float dx = me.x - v.x;
                    float dy = me.y - v.y;
                    if (Math.hypot(dx, dy) <= 1.8f && Gdx.input.isKeyJustPressed(Input.Keys.E)) {
                        v.isRescued = true;
                        v.leaderId = me.playerId;
                        showBanner(v.name + " joined squad! Protect them from infected bites.");
                        CampaignSquadState.rescuedVillagers.add(new CampaignSquadState.RescuedVillagerInfo(v.id, v.name, v.hp));
                        if (levelNumber == 2) {
                            levelFeatures.stream()
                                    .filter(f -> f.type() == CampaignLevelPlan.FeatureType.SURVIVOR && !completedFeatureIds.contains(f.actionId()))
                                    .filter(f -> Math.hypot(f.tileX() - v.x, f.tileY() - v.y) <= 2.2f)
                                    .findFirst()
                                    .ifPresent(this::completeFeature);
                        }
                    }
                }
                // Jane (P2 in local co-op)
                if (p2 != null && !(levelNumber == 1 && !isJaneRevived)) {
                    float dx2 = p2.x - v.x;
                    float dy2 = p2.y - v.y;
                    boolean p2Interact = Gdx.input.isKeyJustPressed(Input.Keys.NUMPAD_3)
                            || Gdx.input.isKeyJustPressed(Input.Keys.NUM_3)
                            || Gdx.input.isKeyJustPressed(Input.Keys.PERIOD)
                            || (isDualViewDebugMode && Gdx.input.isKeyJustPressed(Input.Keys.E));
                    if (Math.hypot(dx2, dy2) <= 1.8f && p2Interact) {
                        v.isRescued = true;
                        v.leaderId = p2.playerId;
                        showBanner(v.name + " joined Jane's squad! Protect them from infected bites.");
                        CampaignSquadState.rescuedVillagers.add(new CampaignSquadState.RescuedVillagerInfo(v.id, v.name, v.hp));
                        if (levelNumber == 2) {
                            levelFeatures.stream()
                                    .filter(f -> f.type() == CampaignLevelPlan.FeatureType.SURVIVOR && !completedFeatureIds.contains(f.actionId()))
                                    .filter(f -> Math.hypot(f.tileX() - v.x, f.tileY() - v.y) <= 2.2f)
                                    .findFirst()
                                    .ifPresent(this::completeFeature);
                        }
                    }
                }
            }
        }

        // Staff Room Key Pickup & Ambush Spawn:
        if (levelNumber == 1 && !hasStairsKey && me != null) {
            float distX = me.x - keyX;
            float distY = me.y - keyY;
            if (Math.hypot(distX, distY) <= 1.5f) {
                if (Gdx.input.isKeyJustPressed(Input.Keys.E)) {
                    onStairsKeyTaken();
                    // Co-op: the key is shared and the horde spawns on the partner's screen too
                    if (client != null) client.sendEvent(EVENT_STAIRS_KEY_TAKEN, "");
                }
            }
        }

        // Level 3 Ambulance Key Pickup (either co-op player may collect it).
        if (levelNumber == 3 && !hasAmbulanceKey) {
            WorldSnapshot.PlayerState keyCollector = null;
            if (me != null && Math.hypot(me.x - ambulanceKeyX, me.y - ambulanceKeyY) <= 1.5f) {
                keyCollector = me;
            } else if (p2 != null && Math.hypot(p2.x - ambulanceKeyX, p2.y - ambulanceKeyY) <= 1.5f) {
                keyCollector = p2;
            }
            boolean keyInteract = Gdx.input.isKeyJustPressed(Input.Keys.E)
                    || Gdx.input.isKeyJustPressed(Input.Keys.NUMPAD_3)
                    || Gdx.input.isKeyJustPressed(Input.Keys.NUM_3)
                    || Gdx.input.isKeyJustPressed(Input.Keys.PERIOD);
            if (keyCollector != null && keyInteract) {
                hasAmbulanceKey = true;
                showBanner("AMBULANCE KEY FOUND! Clear all zombies, then return to Jane at the ambulance.");
            }
        }

        // Level 4 puzzle parts may be collected by either co-op player.
        if (levelNumber == 4 && !level4PuzzleParts.isEmpty()) {
            Vector2 nearbyPart = null;
            for (Vector2 part : level4PuzzleParts) {
                boolean elricNear = me != null && Math.hypot(me.x - part.x, me.y - part.y) <= 1.5f;
                boolean janeNear = p2 != null && Math.hypot(p2.x - part.x, p2.y - part.y) <= 1.5f;
                if (elricNear || janeNear) {
                    nearbyPart = part;
                    break;
                }
            }
            boolean partInteract = Gdx.input.isKeyJustPressed(Input.Keys.E)
                    || Gdx.input.isKeyJustPressed(Input.Keys.NUMPAD_3)
                    || Gdx.input.isKeyJustPressed(Input.Keys.NUM_3)
                    || Gdx.input.isKeyJustPressed(Input.Keys.PERIOD);
            if (nearbyPart != null && partInteract) {
                level4PuzzleParts.remove(nearbyPart);
                level4PuzzlePartsCollected++;
                showBanner(level4PuzzlePartsCollected == LEVEL_4_PUZZLE_PART_COUNT
                        ? "PUZZLE SOLVED! Return to the endpoint to clear Level 4."
                        : "Puzzle part collected (" + level4PuzzlePartsCollected + "/3).");
            }
        }

        if (!hasBomb && mockBombX != -1f && me != null) {
            float distX = me.x - mockBombX;
            float distY = me.y - mockBombY;
            if (Math.sqrt(distX * distX + distY * distY) <= 1.5f) {
                if (Gdx.input.isKeyJustPressed(Input.Keys.E)) {
                    hasBomb = true;
                    CampaignSquadState.hasBomb = true;
                    showBanner("Picked up Grenade!");
                }
            }
        }

        if (!paused && !isInventoryOpen && me != null) {
            boolean attackCmd = Gdx.input.isKeyJustPressed(Input.Keys.SPACE) || Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)
                    || Gdx.input.isKeyJustPressed(Input.Keys.NUM_1) || Gdx.input.isKeyJustPressed(Input.Keys.NUMPAD_1);
            if (attackCmd) {
                PlayerAnimState anim = animStates.computeIfAbsent(me.playerId, k -> new PlayerAnimState());

                if (isMacheteEquipped && !anim.isAttacking) {
                    anim.isAttacking = true;
                    anim.attackTime = 0f;
                    anim.damageApplied = false;
                } else if (isBombEquipped && bombCooldown <= 0f) {
                    anim.isAttacking = true;
                    anim.attackTime = 0f;

                    ActiveBomb b = new ActiveBomb();
                    b.startX = me.x;
                    b.startY = me.y;

                    float throwDistance = 6.0f;
                    if (anim.currentRow == 3) { b.targetX = me.x; b.targetY = me.y + throwDistance; }
                    else if (anim.currentRow == 0) { b.targetX = me.x; b.targetY = me.y - throwDistance; }
                    else if (anim.currentRow == 1) { b.targetX = me.x - throwDistance; b.targetY = me.y; }
                    else if (anim.currentRow == 2) { b.targetX = me.x + throwDistance; b.targetY = me.y; }

                    activeBombs.add(b);
                    bombCooldown = 1.0f;
                }
            }
        }

        if (!paused) {
            checkZombiePatrolObjective();
            game.stepSimulation(delta);
        }

        Gdx.gl.glClearColor(0.055f, 0.078f, 0.125f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);        if (!paused) client.sendInputIfDue(readLocalInput(me, delta), delta);

        if (!paused && isDualViewDebugMode && game.getServer() != null) {
            float p2MoveX = 0f;
            float p2MoveY = 0f;
            if (Gdx.input.isKeyPressed(Input.Keys.NUMPAD_6) || Gdx.input.isKeyPressed(Input.Keys.NUM_6) || Gdx.input.isKeyPressed(Input.Keys.RIGHT)) p2MoveX += 1f;
            if (Gdx.input.isKeyPressed(Input.Keys.NUMPAD_4) || Gdx.input.isKeyPressed(Input.Keys.NUM_4) || Gdx.input.isKeyPressed(Input.Keys.LEFT)) p2MoveX -= 1f;
            if (Gdx.input.isKeyPressed(Input.Keys.NUMPAD_8) || Gdx.input.isKeyPressed(Input.Keys.NUMPAD_5) || Gdx.input.isKeyPressed(Input.Keys.NUM_8) || Gdx.input.isKeyPressed(Input.Keys.NUM_5) || Gdx.input.isKeyPressed(Input.Keys.UP)) p2MoveY += 1f;
            if (Gdx.input.isKeyPressed(Input.Keys.NUMPAD_2) || Gdx.input.isKeyPressed(Input.Keys.NUM_2) || Gdx.input.isKeyPressed(Input.Keys.DOWN)) p2MoveY -= 1f;

            p2 = (snapshot != null && snapshot.players != null && snapshot.players.size() > 1) ? snapshot.players.get(1) : null;
            PlayerAnimState p2Anim = p2 != null ? animStates.get(p2.playerId) : null;
            if (p2Anim != null && p2Anim.isAttacking) {
                p2MoveX = 0f;
                p2MoveY = 0f;
            } else if (p2 != null) {
                float playerSpeed = 4.0f * delta;
                float playerRadius = 0.25f;
                boolean isCurrentlyStuck = !isWalkable(p2.x, p2.y, playerRadius);
                if (!isCurrentlyStuck) {
                    if (!isWalkable(p2.x + (p2MoveX * playerSpeed), p2.y, playerRadius)) p2MoveX = 0;
                    if (!isWalkable(p2.x, p2.y + (p2MoveY * playerSpeed), playerRadius)) p2MoveY = 0;
                }
            }

            boolean p2Moving = p2MoveX != 0f || p2MoveY != 0f;
            boolean p2WantsToSprint = (Gdx.input.isKeyPressed(Input.Keys.SHIFT_RIGHT) || Gdx.input.isKeyPressed(Input.Keys.NUMPAD_ENTER)) && p2Moving;
            if (p2WantsToSprint && p2Stamina > 0f) {
                p2Stamina = Math.max(0f, p2Stamina - (45f * delta));
            } else if (!p2WantsToSprint && p2Stamina < maxStamina) {
                p2Stamina = Math.min(maxStamina, p2Stamina + (30f * delta));
            }

            InputCommand p2Input = new InputCommand();
            p2Input.moveX = p2MoveX;
            p2Input.moveY = p2MoveY;
            p2Input.attackPressed = Gdx.input.isKeyJustPressed(Input.Keys.NUMPAD_0)
                    || Gdx.input.isKeyJustPressed(Input.Keys.INSERT)
                    || Gdx.input.isKeyJustPressed(Input.Keys.NUM_0)
                    || Gdx.input.isKeyJustPressed(Input.Keys.NUMPAD_1)
                    || Gdx.input.isKeyJustPressed(Input.Keys.NUM_1)
                    || Gdx.input.isKeyJustPressed(Input.Keys.CONTROL_RIGHT);
            p2Input.interactPressed = Gdx.input.isKeyJustPressed(Input.Keys.NUMPAD_3)
                    || Gdx.input.isKeyJustPressed(Input.Keys.NUM_3)
                    || Gdx.input.isKeyJustPressed(Input.Keys.PERIOD);
            p2Input.interactHeld = Gdx.input.isKeyPressed(Input.Keys.NUMPAD_3)
                    || Gdx.input.isKeyPressed(Input.Keys.NUM_3)
                    || Gdx.input.isKeyPressed(Input.Keys.PERIOD);
            p2Input.abilityPressed = p2WantsToSprint && p2Stamina > 0f;

            if (levelNumber == 1 && !isJaneRevived) {
                p2Input = new InputCommand();
            }

            game.getServer().injectLocalP2Input(p2Input);

            if (p2Input.attackPressed && p2 != null && !(levelNumber == 1 && !isJaneRevived)) {
                if (p2Anim == null) p2Anim = animStates.computeIfAbsent(p2.playerId, k -> new PlayerAnimState());
                p2Anim.isAttacking = true;
                p2Anim.attackTime = 0f;
                p2Anim.damageApplied = false;
            }

            if (p2Input.interactPressed && p2 != null && !(levelNumber == 1 && !isJaneRevived)) {
                WorldSnapshot.PlayerState p1 = snapshot.players.get(0);
                if (p1 != null && p1.downed && !p2.downed) {
                    float dist = (float) Math.hypot(p2.x - p1.x, p2.y - p1.y);
                    if (dist <= 1.8f) {
                        client.sendEvent("PLAYER_REVIVE", p1.playerId);
                        showBanner("Jane revived Elric!");
                    }
                }
                for (LevelVillager v : levelVillagers) {
                    if (!v.isDead && !v.isRescued) {
                        float dist = (float) Math.hypot(p2.x - v.x, p2.y - v.y);
                        if (dist <= 1.8f) {
                            v.isRescued = true;
                            v.leaderId = p2.playerId;
                            showBanner(v.name + " joined Jane's squad! Protect them from infected bites.");
                            CampaignSquadState.rescuedVillagers.add(new CampaignSquadState.RescuedVillagerInfo(v.id, v.name, v.hp));
                            if (levelNumber == 2) {
                                levelFeatures.stream()
                                        .filter(f -> f.type() == CampaignLevelPlan.FeatureType.SURVIVOR && !completedFeatureIds.contains(f.actionId()))
                                        .filter(f -> Math.hypot(f.tileX() - v.x, f.tileY() - v.y) <= 2.2f)
                                        .findFirst()
                                        .ifPresent(this::completeFeature);
                            }
                            break;
                        }
                    }
                }
            }
        }

        if (!paused) {
            boolean movementKeysPressed = Gdx.input.isKeyPressed(Input.Keys.W) || Gdx.input.isKeyPressed(Input.Keys.A) ||
                    Gdx.input.isKeyPressed(Input.Keys.S) || Gdx.input.isKeyPressed(Input.Keys.D);
            boolean wantsToSprint = Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT) && movementKeysPressed;

            if (wantsToSprint && stamina > 0f) {
                stamina = Math.max(0f, stamina - (45f * delta));
            } else if (!wantsToSprint && stamina < maxStamina) {
                stamina = Math.min(maxStamina, stamina + (30f * delta));
            }
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.F3)) {
            isDualViewDebugMode = !isDualViewDebugMode;
            if (isDualViewDebugMode && game.isHost() && game.getServer() != null) {
                game.getServer().enableLocalCoopDummy(CharacterType.JANE);
            }
            showBanner(isDualViewDebugMode ? "Dual View Debug Mode: ON (Split Screen)" : "Dual View Debug Mode: OFF");
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.J)) {
            showAxisDebug = !showAxisDebug;
            showBanner(showAxisDebug ? "Coordinates Display: ON [J]" : "Coordinates Display: OFF [J]");
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.X)) {
            WorldSnapshot.PlayerState commander = me;
            if (isDualViewDebugMode && p2 != null) {
                float distMe = Float.MAX_VALUE;
                float distP2 = Float.MAX_VALUE;
                for (LevelVillager v : levelVillagers) {
                    if (v.isRescued && !v.isDead) {
                        if (me != null) distMe = Math.min(distMe, (float) Math.hypot(me.x - v.x, me.y - v.y));
                        distP2 = Math.min(distP2, (float) Math.hypot(p2.x - v.x, p2.y - v.y));
                    }
                }
                if (distP2 < distMe) {
                    commander = p2;
                }
            }
            handleVillagerCommandToggle(commander);
        }
        if (isDualViewDebugMode && (Gdx.input.isKeyJustPressed(Input.Keys.NUMPAD_7) || Gdx.input.isKeyJustPressed(Input.Keys.NUM_7) || Gdx.input.isKeyJustPressed(Input.Keys.COMMA))) {
            if (p2 != null) {
                handleVillagerCommandToggle(p2);
            }
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.C) || Gdx.input.isKeyJustPressed(Input.Keys.F1)) {
            showCollisionOverlay = !showCollisionOverlay;
            showBanner("Collision Overlay: " + (showCollisionOverlay ? "ON" : "OFF") + " [C / F1]");
        }

        if (snapshot != null) {
            TextureRegion zombieFrame;
            if (!paused) {
                isBeingBitten = false;
                if (isZombieAuthority()) {
                    zombieFrame = updateMiddleZombie(snapshot, me, delta);
                    updateAmbushZombies(snapshot, me, delta);
                    sendZombieStateIfDue(snapshot, delta);
                } else {
                    zombieFrame = updateMirroredZombies(me, delta);
                }
                updateAlliesAndVillagers(delta, me, snapshot);
                updatePlayerAnimations(snapshot, delta, me);
            } else {
                zombieFrame = currentMiddleZombieFrame();
            }

            if (isDualViewDebugMode && snapshot.players != null && !snapshot.players.isEmpty()) {
                WorldSnapshot.PlayerState p1 = snapshot.players.get(0);
                p2 = snapshot.players.size() > 1 ? snapshot.players.get(1) : p1;

                int screenW = Gdx.graphics.getWidth();
                int screenH = Gdx.graphics.getHeight();
                int halfW = screenW / 2;

                Gdx.gl.glViewport(0, 0, halfW, screenH);
                updateCameraClamped(
                        p1.x * PIXELS_PER_TILE,
                        p1.y * PIXELS_PER_TILE
                );

                batch.setProjectionMatrix(camera.combined);
                batch.begin();
                drawLevelMap();
                drawWorld(snapshot, paused ? 0f : delta, zombieFrame, me);
                batch.end();
                if (showCollisionOverlay) drawCollisionOverlay();

                Gdx.gl.glViewport(halfW, 0, halfW, screenH);
                updateCameraClamped(
                        p2.x * PIXELS_PER_TILE,
                        p2.y * PIXELS_PER_TILE
                );

                batch.setProjectionMatrix(camera.combined);
                batch.begin();
                drawLevelMap();
                drawWorld(snapshot, paused ? 0f : delta, zombieFrame, me);
                batch.end();
                if (showCollisionOverlay) drawCollisionOverlay();

                Gdx.gl.glViewport(0, 0, screenW, screenH);

                Matrix4 hudMatrix = new Matrix4().setToOrtho2D(0, 0, VIRTUAL_WIDTH, VIRTUAL_HEIGHT);
                shapes.setProjectionMatrix(hudMatrix);
                shapes.begin(ShapeRenderer.ShapeType.Filled);
                shapes.setColor(0.95f, 0.8f, 0.15f, 1f);
                shapes.rect(VIRTUAL_WIDTH / 2f - 2f, 0, 4f, VIRTUAL_HEIGHT);
                shapes.end();

                batch.setProjectionMatrix(hudMatrix);
                batch.begin();
                font.setColor(Color.GOLD);
                font.draw(batch, "P1: " + (p1.character != null ? p1.character.name() : "ELRIC") + " [WASD | SPACE attack | E interact]", 20f, VIRTUAL_HEIGHT - 40f);
                if (levelNumber == 1 && !isJaneRevived) {
                    font.setColor(Color.PINK);
                    font.draw(batch, "P2: JANE [UNCONSCIOUS IN LAB - Awaiting Revive Kit]", VIRTUAL_WIDTH / 2f + 20f, VIRTUAL_HEIGHT - 40f);
                    font.setColor(Color.YELLOW);
                    font.draw(batch, "[UNCONSCIOUS IN LAB]\nTrapped until Elric crafts Mixed Herb Revive Kit!", VIRTUAL_WIDTH * 0.75f - 180f, VIRTUAL_HEIGHT / 2f);
                } else {
                    font.draw(batch, "P2: " + (p2.character != null ? p2.character.name() : "JANE") + " [NUM 8-4-5-6 | R-SHIFT sprint | NUM 0 attack | NUM 3 interact | NUM 7 order | NUM 9 heal]", VIRTUAL_WIDTH / 2f + 20f, VIRTUAL_HEIGHT - 40f);
                }
                batch.end();
            } else {
                if (me != null) {
                    updateCameraClamped(
                            me.x * PIXELS_PER_TILE,
                            me.y * PIXELS_PER_TILE
                    );
                }

                batch.setProjectionMatrix(camera.combined);
                batch.begin();
                drawLevelMap();
                drawWorld(snapshot, paused ? 0f : delta, zombieFrame, me);
                batch.end();
                if (showCollisionOverlay) drawCollisionOverlay();
            }

            Gdx.gl.glEnable(GL20.GL_BLEND);
            shapes.setProjectionMatrix(camera.combined);
            shapes.begin(ShapeRenderer.ShapeType.Filled);

            if (snapshot.players != null) {
                for (WorldSnapshot.PlayerState player : snapshot.players) {
                    if (!player.downed && player.hp > 0) {
                        float barW = 24f;
                        float barH = 3f;
                        float px = Math.round((player.x * PIXELS_PER_TILE) - (barW / 2f) + 8f);
                        float py = Math.round((player.y * PIXELS_PER_TILE) + (frameHeight / 2f) + 22f);

                        shapes.setColor(Color.BLACK);
                        shapes.rect(px - 1f, py - 1f, barW + 2f, barH + 2f);

                        shapes.setColor(Color.GREEN);
                        shapes.rect(px, py, barW * (player.hp / GameConstants.PLAYER_MAX_HP), barH);
                    }
                }
            }

            if (!isZombieDead && me != null) {
                float barW = 24f;
                float barH = 3f;
                float zx = Math.round((middleZombieX * PIXELS_PER_TILE) - (barW / 2f));
                float zy = Math.round((middleZombieY * PIXELS_PER_TILE) + (zombieFrameHeight / 2f) + 20f);

                shapes.setColor(Color.BLACK);
                shapes.rect(zx - 1f, zy - 1f, barW + 2f, barH + 2f);

                shapes.setColor(Color.YELLOW);
                shapes.rect(zx, zy, barW * (middleZombieHp / middleZombieMaxHp), barH);
            }

            // Villager health bars (All Levels)
            for (LevelVillager v : levelVillagers) {
                if (v.isDead) continue;
                float barW = 24f;
                float barH = 3f;
                float vx = Math.round((v.x * PIXELS_PER_TILE) - (barW / 2f));
                float vy = Math.round((v.y * PIXELS_PER_TILE) + (frameHeight / 2f) + 18f);

                shapes.setColor(Color.BLACK);
                shapes.rect(vx - 1f, vy - 1f, barW + 2f, barH + 2f);

                shapes.setColor(v.isRescued ? Color.CYAN : Color.LIGHT_GRAY);
                shapes.rect(vx, vy, barW * (v.hp / v.maxHp), barH);
            }


            // Jane ally health bar (All Levels where revived)
            if (isJaneRevived) {
                float barW = 24f;
                float barH = 3f;
                float jx = Math.round((janeX * PIXELS_PER_TILE) - (barW / 2f));
                float jy = Math.round((janeY * PIXELS_PER_TILE) + (frameHeight / 2f) + 18f);

                shapes.setColor(Color.BLACK);
                shapes.rect(jx - 1f, jy - 1f, barW + 2f, barH + 2f);

                shapes.setColor(Color.LIME);
                shapes.rect(jx, jy, barW * (janeHp / janeMaxHp), barH);
            }

            if (isAmbushActive) {
                for (AmbushZombie az : ambushZombies) {
                    if (az.dead) continue;
                    if (az.isBoss) {
                        float barW = 38f;
                        float barH = 5f;
                        float zx = Math.round((az.x * PIXELS_PER_TILE) - (barW / 2f) + 8f);
                        float zy = Math.round((az.y * PIXELS_PER_TILE) + (zombieFrameHeight * 1.45f / 2f) + 24f);

                        shapes.setColor(Color.BLACK);
                        shapes.rect(zx - 1f, zy - 1f, barW + 2f, barH + 2f);

                        shapes.setColor(Color.MAGENTA);
                        shapes.rect(zx, zy, barW * Math.max(0f, az.hp / az.maxHp), barH);
                    } else {
                        float barW = 24f;
                        float barH = 3f;
                        float zx = Math.round((az.x * PIXELS_PER_TILE) - (barW / 2f) + 8f);
                        float zy = Math.round((az.y * PIXELS_PER_TILE) + (zombieFrameHeight / 2f) + 22f);

                        shapes.setColor(Color.BLACK);
                        shapes.rect(zx - 1f, zy - 1f, barW + 2f, barH + 2f);

                        shapes.setColor(Color.FIREBRICK);
                        shapes.rect(zx, zy, barW * Math.max(0f, az.hp / az.maxHp), barH);
                    }
                }
            }
            shapes.end();
            Gdx.gl.glDisable(GL20.GL_BLEND);

            if (isInventoryOpen) {
                drawInventoryOverlay();
            }
        }

        drawHud(snapshot, delta);
        if (paused) drawPauseOverlay(snapshot);
    }

    private void handleVillagerCommandToggle(WorldSnapshot.PlayerState commander) {
        if (commander == null) return;
        List<LevelVillager> activeRescued = new ArrayList<>();
        for (LevelVillager v : levelVillagers) {
            if (v.isRescued && !v.isDead) {
                activeRescued.add(v);
            }
        }
        if (activeRescued.isEmpty()) {
            showBanner("No rescued villagers are currently following!");
            return;
        }

        LevelVillager nearest = null;
        float minDist = 2.2f;
        for (LevelVillager v : activeRescued) {
            float d = (float) Math.hypot(commander.x - v.x, commander.y - v.y);
            if (d < minDist) {
                minDist = d;
                nearest = v;
            }
        }

        boolean isJaneCmd = (commander.character == CharacterType.JANE || (commander.playerId != null && commander.playerId.contains("p2")));
        String prefix = isJaneCmd ? "Jane ordered " : "";
        String hotkeyHint = isJaneCmd ? " [X / NUM 7]" : " [X]";

        if (nearest != null) {
            nearest.isStaying = !nearest.isStaying;
            if (nearest.isStaying) {
                showBanner(prefix + nearest.name + " ordered to HOLD POSITION!" + hotkeyHint);
            } else {
                showBanner(prefix + nearest.name + " ordered to FOLLOW SQUAD!" + hotkeyHint);
            }
        } else {
            boolean anyFollowing = false;
            for (LevelVillager v : activeRescued) {
                if (!v.isStaying) {
                    anyFollowing = true;
                    break;
                }
            }
            boolean newState = anyFollowing;
            for (LevelVillager v : activeRescued) {
                v.isStaying = newState;
            }
            if (newState) {
                showBanner(prefix + "All rescued villagers ordered to HOLD POSITION!" + hotkeyHint);
            } else {
                showBanner(prefix + "All rescued villagers ordered to FOLLOW SQUAD!" + hotkeyHint);
            }
        }
    }

    private void updateAlliesAndVillagers(float delta, WorldSnapshot.PlayerState me, WorldSnapshot snapshot) {
        if (me == null) return;

        // 2. Villager Escort Following & Bites from Ambush/Middle Zombies (All levels)
        for (LevelVillager v : levelVillagers) {
            if (v.isDead) continue;

            // Keep the idle animation running even before the villager is rescued.
            v.stateTime += delta;
            v.moving = false;

            // Stranded villagers stay in place and play v1_idle.png.
            if (!v.isRescued) continue;

            WorldSnapshot.PlayerState followTarget = null;
            if (snapshot != null && snapshot.players != null) {
                if (v.leaderId != null) {
                    for (WorldSnapshot.PlayerState p : snapshot.players) {
                        if (v.leaderId.equals(p.playerId) && !p.downed && p.hp > 0f) {
                            followTarget = p;
                            break;
                        }
                    }
                }
                if (followTarget == null) {
                    float closestD = Float.MAX_VALUE;
                    for (WorldSnapshot.PlayerState p : snapshot.players) {
                        if (!p.downed && p.hp > 0f) {
                            float d = (float) Math.hypot(p.x - v.x, p.y - v.y);
                            if (d < closestD) {
                                closestD = d;
                                followTarget = p;
                            }
                        }
                    }
                }
            }
            if (followTarget == null) followTarget = me;

            float distXP = followTarget.x - v.x;
            float distYP = followTarget.y - v.y;
            float distP = (float) Math.hypot(distXP, distYP);

            if (!v.isStaying && distP > 1.8f) {
                // Pick the spritesheet row from the direction the villager is travelling.
                if (Math.abs(distXP) > Math.abs(distYP)) {
                    v.facing = distXP > 0f ? 2 : 1; // RIGHT : LEFT
                } else {
                    v.facing = distYP > 0f ? 3 : 0; // UP : DOWN
                }
                float speed = 2.0f;
                float moveX = (distXP / distP) * speed * delta;
                float moveY = (distYP / distP) * speed * delta;

                boolean moved = false;
                if (isWalkable(v.x + moveX, v.y, 0.25f)) {
                    v.x += moveX;
                    moved = true;
                }
                if (isWalkable(v.x, v.y + moveY, 0.25f)) {
                    v.y += moveY;
                    moved = true;
                }
                v.moving = moved;
            }

            if (isAmbushActive) {
                for (AmbushZombie az : ambushZombies) {
                    if (az.dead) continue;
                    float d = (float) Math.hypot(az.x - v.x, az.y - v.y);
                    if (d <= 0.8f) {
                        v.biteCooldown -= delta;
                        if (v.biteCooldown <= 0f) {
                            v.biteCooldown = 1.0f;
                            v.hp -= az.isBoss ? 25f : 15f;
                            if (v.hp <= 0f) {
                                v.isDead = true;
                                bloodPools.add(new Vector2(v.x, v.y));
                                showBanner(v.name + " was killed by the horde!");
                            }
                        }
                    }
                }
            }
        }

        // 3. Revived Jane Ally (Katana + Medkit Synthesis)
        if (isJaneRevived) {
            janeMedkitTimer -= delta;
            if (janeMedkitTimer <= 0f) {
                janeMedkitTimer = 60.0f;
                janeMedkitsProduced++;
                healCooldown = 0f;
                showBanner("Jane synthesized a Field Medkit! First Aid is ready (+50 HP capacity)!");
            }

            // In dual-view debug co-op, Jane is player-controlled so skip AI movement and auto-attacks
            if (!isDualViewDebugMode) {
                float jdx = me.x - janeX;
                float jdy = me.y - janeY;
                float jdist = (float) Math.hypot(jdx, jdy);

                boolean jMoving = false;
                if (!aiJaneAnim.isAttacking && jdist > 1.5f) {
                    float jSpeed = 2.4f;
                    float jMoveX = (jdx / jdist) * jSpeed * delta;
                    float jMoveY = (jdy / jdist) * jSpeed * delta;
                    if (isWalkable(janeX + jMoveX, janeY, 0.25f)) janeX += jMoveX;
                    if (isWalkable(janeX, janeY + jMoveY, 0.25f)) janeY += jMoveY;
                    jMoving = true;
                    if (Math.abs(jdx) > Math.abs(jdy)) {
                        aiJaneAnim.currentRow = jdx > 0 ? 2 : 1;
                    } else {
                        aiJaneAnim.currentRow = jdy > 0 ? 3 : 0;
                    }
                }

                if (janeAttackCooldown > 0f) {
                    janeAttackCooldown -= delta;
                } else if (!aiJaneAnim.isAttacking) {
                    float nearestDist = 2.2f;
                    Runnable attackAction = null;
                    float atkWX = 0f, atkWY = 0f;

                    if (!isZombieDead) {
                        float d = (float) Math.hypot(middleZombieX - janeX, middleZombieY - janeY);
                        if (d <= nearestDist) {
                            nearestDist = d;
                            atkWX = middleZombieX - janeX;
                            atkWY = middleZombieY - janeY;
                            attackAction = () -> {
                                middleZombieHp -= 30f;
                                float kbX = Math.signum(middleZombieX - janeX) * 0.5f;
                                float kbY = Math.signum(middleZombieY - janeY) * 0.5f;
                                if (isWalkable(middleZombieX + kbX * 0.5f, middleZombieY, 0.25f) && isWalkable(middleZombieX + kbX, middleZombieY, 0.25f)) middleZombieX += kbX;
                                else if (isWalkable(middleZombieX + kbX * 0.5f, middleZombieY, 0.25f)) middleZombieX += kbX * 0.5f;
                                if (isWalkable(middleZombieX, middleZombieY + kbY * 0.5f, 0.25f) && isWalkable(middleZombieX, middleZombieY + kbY, 0.25f)) middleZombieY += kbY;
                                else if (isWalkable(middleZombieX, middleZombieY + kbY * 0.5f, 0.25f)) middleZombieY += kbY * 0.5f;
                                if (middleZombieHp <= 0f) {
                                    isZombieDead = true;
                                    coins += 1;
                                    bloodPools.add(new Vector2(middleZombieX, middleZombieY));
                                    checkZombiePatrolObjective();
                                    broadcastZombieKill(MIDDLE_ZOMBIE_ID);
                                }
                            };
                        }
                    }

                    if (isAmbushActive) {
                        for (AmbushZombie az : ambushZombies) {
                            if (az.dead) continue;
                            float d = (float) Math.hypot(az.x - janeX, az.y - janeY);
                            if (d <= nearestDist) {
                                nearestDist = d;
                                atkWX = az.x - janeX;
                                atkWY = az.y - janeY;
                                final AmbushZombie targetAz = az;
                                attackAction = () -> {
                                    targetAz.hp -= 30f;
                                    if (!targetAz.isBoss) {
                                        float kbX = Math.signum(targetAz.x - janeX) * 0.5f;
                                        float kbY = Math.signum(targetAz.y - janeY) * 0.5f;
                                        if (isWalkable(targetAz.x + kbX * 0.5f, targetAz.y, 0.25f) && isWalkable(targetAz.x + kbX, targetAz.y, 0.25f)) targetAz.x += kbX;
                                        else if (isWalkable(targetAz.x + kbX * 0.5f, targetAz.y, 0.25f)) targetAz.x += kbX * 0.5f;
                                        if (isWalkable(targetAz.x, targetAz.y + kbY * 0.5f, 0.25f) && isWalkable(targetAz.x, targetAz.y + kbY, 0.25f)) targetAz.y += kbY;
                                        else if (isWalkable(targetAz.x, targetAz.y + kbY * 0.5f, 0.25f)) targetAz.y += kbY * 0.5f;
                                    }
                                    if (targetAz.hp <= 0f) {
                                        targetAz.dead = true;
                                        grantLabPasskeyIfBoss(targetAz);
                                        coins += 1;
                                        bloodPools.add(new Vector2(targetAz.x, targetAz.y));
                                        broadcastAmbushZombieKill(targetAz);
                                        checkAmbushCompletion();
                                    }
                                };
                            }
                        }
                    }

                    if (attackAction != null) {
                        janeAttackCooldown = 0.7f;
                        if (Math.abs(atkWX) > Math.abs(atkWY)) {
                            aiJaneAnim.currentRow = atkWX > 0 ? 2 : 1;
                        } else {
                            aiJaneAnim.currentRow = atkWY > 0 ? 3 : 0;
                        }
                        aiJaneAnim.isAttacking = true;
                        aiJaneAnim.attackTime = 0f;

                        // attackAction only exists when Jane has a valid living target
                        // inside her melee range, so the hit sound plays after damage.
                        attackAction.run();
                        playMacheteSound();
                    }
                }

                // AI Jane animation state
                if (aiJaneAnim.isAttacking) {
                    aiJaneAnim.attackTime += delta;
                    if (p2MeleeHitFrames != null) {
                        float attackSpeed = 0.22f;
                        int attackFrame = (int) (aiJaneAnim.attackTime / attackSpeed);
                        if (attackFrame >= 2) {
                            aiJaneAnim.isAttacking = false;
                        } else {
                            int safeRow = aiJaneAnim.currentRow % p2MeleeHitFrames.length;
                            int safeCol = attackFrame % p2MeleeHitFrames[0].length;
                            aiJaneAnim.currentFrame = p2MeleeHitFrames[safeRow][safeCol];
                        }
                    } else {
                        float frameDuration = 0.045f;
                        int attackCol = (int) (aiJaneAnim.attackTime / frameDuration);
                        if (attackCol >= 8) {
                            aiJaneAnim.isAttacking = false;
                        } else if (p2MeleeFrames != null) {
                            int safeRow = aiJaneAnim.currentRow % p2MeleeFrames.length;
                            int safeCol = attackCol % p2MeleeFrames[0].length;
                            aiJaneAnim.currentFrame = p2MeleeFrames[safeRow][safeCol];
                        }
                    }
                } else if (jMoving && p2MeleeFrames != null) {
                    aiJaneAnim.stateTime += delta;
                    if (aiJaneAnim.stateTime > 0.15f) {
                        aiJaneAnim.currentColumn = (aiJaneAnim.currentColumn + 1) % 8;
                        aiJaneAnim.stateTime = 0f;
                    }
                    int safeRow = aiJaneAnim.currentRow % p2MeleeFrames.length;
                    int safeCol = aiJaneAnim.currentColumn % p2MeleeFrames[0].length;
                    aiJaneAnim.currentFrame = p2MeleeFrames[safeRow][safeCol];
                } else {
                    aiJaneAnim.stateTime += delta;
                    TextureRegion[][] idleSrc = (p2IdleMeleeFrames != null) ? p2IdleMeleeFrames : p2IdleFrames;
                    if (idleSrc != null) {
                        if (aiJaneAnim.stateTime > 0.5f) {
                            aiJaneAnim.currentColumn = (aiJaneAnim.currentColumn + 1) % idleSrc[0].length;
                            aiJaneAnim.stateTime = 0f;
                        }
                        int safeRow = aiJaneAnim.currentRow % idleSrc.length;
                        int safeCol = aiJaneAnim.currentColumn % idleSrc[0].length;
                        aiJaneAnim.currentFrame = idleSrc[safeRow][safeCol];
                    }
                }
            }
        }
    }

    private static class ZombieTarget {
        final float x, y;
        final String playerId;
        final boolean isLocal;
        final boolean isAiJane;
        final LevelVillager villager;
        final float distance;
        final boolean hasLOS;

        ZombieTarget(float x, float y, String playerId, boolean isLocal, boolean isAiJane, LevelVillager villager, float distance, boolean hasLOS) {
            this.x = x;
            this.y = y;
            this.playerId = playerId;
            this.isLocal = isLocal;
            this.isAiJane = isAiJane;
            this.villager = villager;
            this.distance = distance;
            this.hasLOS = hasLOS;
        }
    }

    private ZombieTarget findClosestZombieTarget(float zx, float zy, WorldSnapshot snapshot, WorldSnapshot.PlayerState me) {
        ZombieTarget bestTarget = null;
        float bestDist = Float.MAX_VALUE;

        // 1. Check all connected / local players in snapshot
        if (snapshot != null && snapshot.players != null) {
            for (WorldSnapshot.PlayerState p : snapshot.players) {
                if (p == null || p.downed || p.hp <= 0f) continue;
                // Jane unconscious in pharmacy before revive cannot be targeted
                if (levelNumber == 1 && !isJaneRevived && p.character == CharacterType.JANE) continue;

                float dist = (float) Math.hypot(p.x - zx, p.y - zy);
                boolean los = hasLineOfSight(zx, zy, p.x, p.y);
                boolean isLocal = me != null && p.playerId.equals(me.playerId);

                if (bestTarget == null || (los && !bestTarget.hasLOS) || (los == bestTarget.hasLOS && dist < bestDist)) {
                    bestTarget = new ZombieTarget(p.x, p.y, p.playerId, isLocal, false, null, dist, los);
                    bestDist = dist;
                }
            }
        }

        // 2. Check AI Jane if in single-player and revived
        boolean hasPlayerJane = isDualViewDebugMode;
        if (snapshot != null && snapshot.players != null) {
            for (WorldSnapshot.PlayerState p : snapshot.players) {
                if (p.character == CharacterType.JANE) {
                    hasPlayerJane = true;
                    break;
                }
            }
        }
        if (!hasPlayerJane && isJaneRevived && janeHp > 0f) {
            float dist = (float) Math.hypot(janeX - zx, janeY - zy);
            boolean los = hasLineOfSight(zx, zy, janeX, janeY);
            if (bestTarget == null || (los && !bestTarget.hasLOS) || (los == bestTarget.hasLOS && dist < bestDist)) {
                bestTarget = new ZombieTarget(janeX, janeY, null, false, true, null, dist, los);
                bestDist = dist;
            }
        }

        // 3. Check living villagers (priority based on proximity and LOS)
        for (LevelVillager v : levelVillagers) {
            if (v == null || v.isDead || v.hp <= 0f) continue;
            boolean los = hasLineOfSight(zx, zy, v.x, v.y);
            if (!v.isRescued && !los) continue;

            float dist = (float) Math.hypot(v.x - zx, v.y - zy);
            if (bestTarget == null || (los && !bestTarget.hasLOS) || (los == bestTarget.hasLOS && dist < bestDist)) {
                bestTarget = new ZombieTarget(v.x, v.y, null, false, false, v, dist, los);
                bestDist = dist;
            }
        }

        return bestTarget;
    }

    private TextureRegion currentMiddleZombieFrame() {
        if (isZombieDead) return null;

        TextureRegion[][] frames;
        if (middleZombieBiting && zombieBiteFrames != null) {
            frames = zombieBiteFrames;
        } else if (middleZombieChasing && zombieFrames != null) {
            frames = zombieFrames;
        } else {
            frames = zombieIdleFrames != null ? zombieIdleFrames : zombieFrames;
        }
        if (frames == null || frames.length == 0 || frames[0].length == 0) return null;

        int safeRow = Math.floorMod(zombieAnim.currentRow, frames.length);
        int safeCol = Math.floorMod(zombieAnim.currentColumn, frames[safeRow].length);
        return frames[safeRow][safeCol];
    }

    private TextureRegion updateMiddleZombie(WorldSnapshot snapshot, WorldSnapshot.PlayerState me, float delta) {
        if (isZombieDead) {
            middleZombieBiting = false;
            return null;
        }

        ZombieTarget target = findClosestZombieTarget(middleZombieX, middleZombieY, snapshot, me);
        if (target == null) {
            middleZombieBiting = false;
            middleZombieChasing = false;
            middleZombieAnimState = 0;
            zombieAnim.stateTime += delta;
            if (zombieAnim.stateTime > 0.5f) {
                zombieAnim.currentColumn = (zombieAnim.currentColumn == 0) ? 1 : 0;
                zombieAnim.stateTime = 0f;
            }
            int safeRow = zombieAnim.currentRow % zombieIdleFrames.length;
            int safeCol = zombieAnim.currentColumn % zombieIdleFrames[0].length;
            return zombieIdleFrames[safeRow][safeCol];
        }

        float aggroRadiusTiles = 5.0f;
        float loseRadiusTiles = 8.0f;
        float zombieSpeed = 2.0f;

        float midDistX = target.x - middleZombieX;
        float midDistY = target.y - middleZombieY;
        float midDistance = target.distance;
        boolean hasLOS = target.hasLOS;

        if (midDistance <= 0.8f && hasLOS) {
            middleZombieBiting = true;
            if (target.isLocal) {
                isBeingBitten = true;
            }
            biteCooldown -= delta;
            if (biteCooldown <= 0f) {
                float biteDmg = 33.4f;
                if (target.isLocal) {
                    client.sendEvent("ZOMBIE_BITE_DAMAGE", String.valueOf(biteDmg));
                } else if (target.isAiJane) {
                    janeHp = Math.max(0f, janeHp - biteDmg);
                    CampaignSquadState.janeHp = janeHp;
                    if (janeHp <= 0f) {
                        bloodPools.add(new Vector2(janeX, janeY));
                        showBanner("Jane collapsed from zombie bites!");
                    }
                } else if (target.villager != null) {
                    target.villager.hp = Math.max(0f, target.villager.hp - biteDmg);
                    if (target.villager.hp <= 0f && !target.villager.isDead) {
                        target.villager.isDead = true;
                        bloodPools.add(new Vector2(target.villager.x, target.villager.y));
                        showBanner(target.villager.name + " was killed by the infected!");
                    }
                } else if (target.playerId != null) {
                    client.sendEvent("ZOMBIE_BITE_DAMAGE", biteDmg + ":" + target.playerId);
                }
                biteCooldown = 0.8f;
            }
        } else {
            middleZombieBiting = false;
            biteCooldown = 0.8f;
        }

        if (!middleZombieChasing && midDistance <= aggroRadiusTiles && hasLOS) {
            middleZombieChasing = true;
        } else if (middleZombieChasing && (midDistance >= loseRadiusTiles || !hasLOS)) {
            middleZombieChasing = false;
        }

        boolean zombieMoving = middleZombieChasing && midDistance > 0.5f;
        middleZombieAnimState = middleZombieBiting ? 2 : (zombieMoving ? 1 : 0);

        if (middleZombieBiting) {
            if (Math.abs(midDistX) > Math.abs(midDistY)) zombieAnim.currentRow = midDistX > 0 ? 2 : 1;
            else zombieAnim.currentRow = midDistY > 0 ? 3 : 0;

            float biteProgress = 0.8f - biteCooldown;
            if (biteProgress < 0f) biteProgress = 0f;
            int biteFrame = (int) (biteProgress / 0.4f);
            if (biteFrame >= 2) biteFrame = 1;

            int safeRow = zombieAnim.currentRow % zombieBiteFrames.length;
            int safeCol = biteFrame % zombieBiteFrames[0].length;
            return zombieBiteFrames[safeRow][safeCol];
        } else if (zombieMoving) {
            float zombieRadius = 0.25f;
            if (!isWalkable(middleZombieX, middleZombieY, zombieRadius)) {
                float[] dxs = {0.1f, -0.1f, 0f, 0f, 0.25f, -0.25f, 0f, 0f};
                float[] dys = {0f, 0f, 0.1f, -0.1f, 0f, 0f, 0.25f, -0.25f};
                for (int i = 0; i < dxs.length; i++) {
                    if (isWalkable(middleZombieX + dxs[i], middleZombieY + dys[i], zombieRadius)) {
                        middleZombieX += dxs[i];
                        middleZombieY += dys[i];
                        break;
                    }
                }
            }

            float moveX = (midDistX / midDistance) * zombieSpeed * delta;
            float moveY = (midDistY / midDistance) * zombieSpeed * delta;

            float nextX = middleZombieX + moveX;
            float nextY = middleZombieY + moveY;

            boolean movedX = false;
            boolean movedY = false;
            if (isWalkable(nextX, middleZombieY, zombieRadius)) { middleZombieX = nextX; movedX = true; }
            if (isWalkable(middleZombieX, nextY, zombieRadius)) { middleZombieY = nextY; movedY = true; }
            if (!movedX && !movedY) {
                if (isWalkable(nextX, middleZombieY, 0.15f)) middleZombieX = nextX;
                else if (isWalkable(middleZombieX, nextY, 0.15f)) middleZombieY = nextY;
            }

            if (Math.abs(midDistX) > Math.abs(midDistY)) zombieAnim.currentRow = midDistX > 0 ? 2 : 1;
            else zombieAnim.currentRow = midDistY > 0 ? 3 : 0;

            zombieAnim.stateTime += delta;
            if (zombieAnim.stateTime > 0.15f) {
                zombieAnim.currentColumn = (zombieAnim.currentColumn + 1) % 8;
                zombieAnim.stateTime = 0f;
            }
            int safeRow = zombieAnim.currentRow % zombieFrames.length;
            int safeCol = zombieAnim.currentColumn % zombieFrames[0].length;
            return zombieFrames[safeRow][safeCol];
        } else {
            zombieAnim.stateTime += delta;
            if (zombieAnim.stateTime > 0.5f) {
                zombieAnim.currentColumn = (zombieAnim.currentColumn == 0) ? 1 : 0;
                zombieAnim.stateTime = 0f;
            }
            int safeRow = zombieAnim.currentRow % zombieIdleFrames.length;
            int safeCol = zombieAnim.currentColumn % zombieIdleFrames[0].length;
            return zombieIdleFrames[safeRow][safeCol];
        }
    }

    private void grantLabPasskeyIfBoss(AmbushZombie zombie) {
        if (levelNumber == 5 && zombie.isBoss && !hasLabPasskey) {
            hasLabPasskey = true;
            showBanner("BIG RED ZOMBIE DEFEATED! Lab Passkey Card acquired — return to the facility gate.");
        }
    }

    /** Level 1: the staff room key was picked up (here or by the partner) — take the key and spawn the horde once. */
    private void onStairsKeyTaken() {
        if (levelNumber != 1 || hasStairsKey) return;
        hasStairsKey = true;
        isAmbushActive = true;
        ambushZombies.clear();
        // Boss zombie guarding the door corridor (clear walkway at 42.0, 28.0):
        ambushZombies.add(new AmbushZombie(42.0f, 28.0f, true));
        // 3 regular zombies swarming (verified walkable positions):
        ambushZombies.add(new AmbushZombie(36.0f, 26.5f, false));
        ambushZombies.add(new AmbushZombie(42.0f, 25.0f, false));
        ambushZombies.add(new AmbushZombie(36.5f, 28.5f, false));
        applyPendingAmbushKills(); // partner may already have killed some of these
        showBanner("DOOR BREACHED! MUTATED BOSS & ZOMBIE HORDE EMERGE!");
    }

    /** Only the host runs zombie AI; the other co-op player mirrors the host's zombies. */
    private boolean isZombieAuthority() {
        return game.isHost();
    }

    private static float round2(float v) {
        return Math.round(v * 100f) / 100f;
    }

    /** Host: sends every living zombie's position + animation state to the partner (~15 times/s). */
    private void sendZombieStateIfDue(WorldSnapshot snapshot, float delta) {
        if (client == null || snapshot == null || snapshot.players == null || snapshot.players.size() < 2) return;
        zombieSyncTimer += delta;
        if (zombieSyncTimer < ZOMBIE_SYNC_INTERVAL) return;
        zombieSyncTimer = 0f;

        StringBuilder sb = new StringBuilder("L").append(levelNumber);
        if (!isZombieDead) {
            sb.append("|m,").append(round2(middleZombieX)).append(',').append(round2(middleZombieY))
              .append(',').append(zombieAnim.currentRow).append(',').append(middleZombieAnimState);
        }
        if (isAmbushActive) {
            for (int i = 0; i < ambushZombies.size(); i++) {
                AmbushZombie az = ambushZombies.get(i);
                if (az.dead) continue;
                sb.append('|').append(i).append(',').append(round2(az.x)).append(',').append(round2(az.y))
                  .append(',').append(az.dirRow).append(',').append(az.animState);
            }
        }
        client.sendEvent(EVENT_ZOMBIE_STATE, sb.toString());
    }

    /** Partner: stores the host's latest zombie positions/animation (applied smoothly in updateMirroredZombies). */
    private void applyZombieState(String payload) {
        if (payload == null) return;
        String[] parts = payload.split("\\|");
        if (parts.length == 0 || !parts[0].equals("L" + levelNumber)) return; // stale packet from another level
        for (int p = 1; p < parts.length; p++) {
            String[] f = parts[p].split(",");
            if (f.length < 5) continue;
            try {
                float x = Float.parseFloat(f[1]);
                float y = Float.parseFloat(f[2]);
                int row = Integer.parseInt(f[3]);
                int state = Integer.parseInt(f[4]);
                if (f[0].equals("m")) {
                    if (isZombieDead) continue;
                    if (!hasMiddleZombieNetState) {
                        middleZombieX = x;
                        middleZombieY = y;
                    }
                    middleZombieNetX = x;
                    middleZombieNetY = y;
                    hasMiddleZombieNetState = true;
                    zombieAnim.currentRow = row;
                    middleZombieAnimState = state;
                } else {
                    int index = Integer.parseInt(f[0]);
                    if (index < 0 || index >= ambushZombies.size()) continue;
                    AmbushZombie az = ambushZombies.get(index);
                    if (az.dead) continue;
                    if (!az.hasNetState) {
                        az.x = x;
                        az.y = y;
                    }
                    az.netX = x;
                    az.netY = y;
                    az.hasNetState = true;
                    az.dirRow = row;
                    az.animState = state;
                }
            } catch (NumberFormatException ignored) {
            }
        }
    }

    /** Partner: glides zombies toward the host's positions and animates them; no AI or bite damage here. */
    private TextureRegion updateMirroredZombies(WorldSnapshot.PlayerState me, float delta) {
        float follow = Math.min(1f, delta * 12f);
        TextureRegion middleFrame = null;

        if (!isZombieDead) {
            if (hasMiddleZombieNetState) {
                middleZombieX += (middleZombieNetX - middleZombieX) * follow;
                middleZombieY += (middleZombieNetY - middleZombieY) * follow;
            }
            middleZombieBiting = middleZombieAnimState == 2;
            middleZombieChasing = middleZombieAnimState >= 1;
            zombieAnim.stateTime += delta;
            if (middleZombieAnimState == 1) {
                if (zombieAnim.stateTime > 0.15f) {
                    zombieAnim.currentColumn = (zombieAnim.currentColumn + 1) % 8;
                    zombieAnim.stateTime = 0f;
                }
            } else if (zombieAnim.stateTime > (middleZombieAnimState == 2 ? 0.4f : 0.5f)) {
                zombieAnim.currentColumn = (zombieAnim.currentColumn == 0) ? 1 : 0;
                zombieAnim.stateTime = 0f;
            }
            if (middleZombieBiting && me != null && Math.hypot(me.x - middleZombieX, me.y - middleZombieY) <= 0.9f) {
                isBeingBitten = true;
            }
            middleFrame = currentMiddleZombieFrame();
        } else {
            middleZombieBiting = false;
        }

        if (isAmbushActive) {
            for (AmbushZombie az : ambushZombies) {
                if (az.dead) {
                    az.currentFrame = null;
                    continue;
                }
                az.stateTime += delta;
                if (az.hasNetState) {
                    az.x += (az.netX - az.x) * follow;
                    az.y += (az.netY - az.y) * follow;
                }
                az.biting = az.animState == 2;
                if (az.biting && me != null && Math.hypot(me.x - az.x, me.y - az.y) <= (az.isBoss ? 1.1f : 0.9f)) {
                    isBeingBitten = true;
                }

                TextureRegion[][] frames;
                int col;
                if (az.animState == 2 && zombieBiteFrames != null) {
                    frames = zombieBiteFrames;
                    col = ((int) (az.stateTime / 0.4f)) % 2;
                } else if (az.animState == 1 && zombieFrames != null) {
                    frames = zombieFrames;
                    col = ((int) (az.stateTime / 0.15f)) % 8;
                } else {
                    frames = zombieIdleFrames != null ? zombieIdleFrames : zombieFrames;
                    col = ((int) (az.stateTime / 0.5f)) % 2;
                }
                if (frames == null || frames.length == 0 || frames[0].length == 0) {
                    az.currentFrame = null;
                    continue;
                }
                int safeRow = Math.floorMod(az.dirRow, frames.length);
                az.currentFrame = frames[safeRow][Math.floorMod(col, frames[safeRow].length)];
            }
        }
        return middleFrame;
    }

    /** Tells the partner's screen that this zombie died here. */
    private void broadcastZombieKill(String zombieId) {
        killedZombieIds.add(zombieId);
        if (client != null) client.sendEvent(EVENT_ZOMBIE_KILLED, zombieId);
    }

    private void broadcastAmbushZombieKill(AmbushZombie zombie) {
        int index = ambushZombies.indexOf(zombie);
        if (index >= 0) broadcastZombieKill("ambush:" + index);
    }

    /** Partner killed a zombie on their screen: kill the same zombie here (no-op if already dead). */
    private void applyRemoteZombieKill(String zombieId) {
        if (zombieId == null || !killedZombieIds.add(zombieId)) return;
        applyZombieKillIfPresent(zombieId);
    }

    /** Kills the zombie with this ID if it currently exists on this screen. */
    private void applyZombieKillIfPresent(String zombieId) {
        if (MIDDLE_ZOMBIE_ID.equals(zombieId)) {
            if (!isZombieDead) {
                middleZombieHp = 0f;
                isZombieDead = true;
                isBeingBitten = false;
                coins += 1;
                bloodPools.add(new Vector2(middleZombieX, middleZombieY));
                checkZombiePatrolObjective();
            }
            return;
        }
        if (zombieId.startsWith("ambush:")) {
            int index;
            try {
                index = Integer.parseInt(zombieId.substring("ambush:".length()));
            } catch (NumberFormatException e) {
                return;
            }
            if (index < 0 || index >= ambushZombies.size()) return; // Not spawned here yet; applied on spawn
            AmbushZombie az = ambushZombies.get(index);
            if (!az.dead) {
                az.hp = 0f;
                az.dead = true;
                grantLabPasskeyIfBoss(az);
                coins += 1;
                bloodPools.add(new Vector2(az.x, az.y));
                checkAmbushCompletion();
            }
        }
    }

    /** Applies kills the partner made before this screen spawned the horde. */
    private void applyPendingAmbushKills() {
        for (String id : killedZombieIds) {
            if (id.startsWith("ambush:")) applyZombieKillIfPresent(id);
        }
    }

    private void checkAmbushCompletion() {
        if (!isAmbushActive || isAmbushDefeated) return;
        long remaining = ambushZombies.stream().filter(z -> !z.dead).count();
        if (remaining == 0) {
            isAmbushDefeated = true;
            if (levelNumber == 6) {
                showBanner("FINAL MUTATION DESTROYED! THE ANTIDOTE IS SECURE!");
                Gdx.app.postRunnable(() -> {
                    game.setScreen(new StoryPanelScreen(game, client, bridge, StoryPanelScreen.Sequence.ENDING, 6));
                });
            } else if (levelNumber == 3) {
                coins += 5;
                showBanner(hasAmbulanceKey
                        ? "ALL ZOMBIES CLEARED! Return to Jane at the ambulance. (+5 Coins)"
                        : "ALL ZOMBIES CLEARED! Find the ambulance key, then return to Jane. (+5 Coins)");
            } else {
                coins += 5;
                String dest = levelNumber == 1 ? "Hospital Floor 2" : "the next area";
                showBanner("MUTATED BOSS & HORDE DEFEATED! (+5 Coins) The passage to " + dest + " is unlocked!");
            }
        }
    }

    private void updateAmbushZombies(WorldSnapshot snapshot, WorldSnapshot.PlayerState me, float delta) {
        if (!isAmbushActive) return;

        for (AmbushZombie az : ambushZombies) {
            if (az.dead) {
                az.currentFrame = null;
                continue;
            }

            az.stateTime += delta;
            ZombieTarget target = findClosestZombieTarget(az.x, az.y, snapshot, me);
            if (target == null) {
                az.biting = false;
                az.animState = 0;
                az.dirRow = 0;
                az.currentFrame = (zombieIdleFrames != null) ? zombieIdleFrames[0][0] : null;
                continue;
            }

            float distXP = target.x - az.x;
            float distYP = target.y - az.y;
            float distP = target.distance;
            boolean hasLOS = target.hasLOS;

            float biteDist = az.isBoss ? 1.0f : 0.8f;
            if (distP <= biteDist && hasLOS) {
                az.biting = true;
                if (target.isLocal) {
                    isBeingBitten = true;
                }
                az.biteCooldown -= delta;
                if (az.biteCooldown <= 0f) {
                    float biteDmg = az.isBoss ? 35.0f : 20.0f;
                    if (target.isLocal) {
                        client.sendEvent("ZOMBIE_BITE_DAMAGE", String.valueOf(biteDmg));
                    } else if (target.isAiJane) {
                        janeHp = Math.max(0f, janeHp - biteDmg);
                        CampaignSquadState.janeHp = janeHp;
                        if (janeHp <= 0f) {
                            bloodPools.add(new Vector2(janeX, janeY));
                            showBanner("Jane was downed by the horde!");
                        }
                    } else if (target.villager != null) {
                        target.villager.hp = Math.max(0f, target.villager.hp - biteDmg);
                        if (target.villager.hp <= 0f && !target.villager.isDead) {
                            target.villager.isDead = true;
                            bloodPools.add(new Vector2(target.villager.x, target.villager.y));
                            showBanner(target.villager.name + " was killed by the horde!");
                        }
                    } else if (target.playerId != null) {
                        client.sendEvent("ZOMBIE_BITE_DAMAGE", biteDmg + ":" + target.playerId);
                    }
                    az.biteCooldown = az.isBoss ? 1.0f : 0.8f;
                }
            } else {
                az.biting = false;
                az.biteCooldown = az.isBoss ? 1.0f : 0.8f;
            }

            int dirRow;
            if (Math.abs(distXP) > Math.abs(distYP)) dirRow = distXP > 0 ? 2 : 1;
            else dirRow = distYP > 0 ? 3 : 0;
            az.dirRow = dirRow;
            az.animState = az.biting ? 2 : ((distP > 0.5f && distP <= 14.0f) ? 1 : 0);

            if (az.biting && zombieBiteFrames != null) {
                float biteProgress = (az.isBoss ? 1.0f : 0.8f) - az.biteCooldown;
                if (biteProgress < 0f) biteProgress = 0f;
                int biteCol = (int) (biteProgress / 0.4f);
                if (biteCol >= 2) biteCol = 1;
                int safeRow = dirRow % zombieBiteFrames.length;
                int safeCol = biteCol % zombieBiteFrames[0].length;
                az.currentFrame = zombieBiteFrames[safeRow][safeCol];
                continue;
            }

            if (distP > 0.5f && distP <= 14.0f) {
                float radius = 0.25f;
                if (!isWalkable(az.x, az.y, radius)) {
                    float[] dxs = {0.1f, -0.1f, 0f, 0f, 0.25f, -0.25f, 0f, 0f};
                    float[] dys = {0f, 0f, 0.1f, -0.1f, 0f, 0f, 0.25f, -0.25f};
                    for (int i = 0; i < dxs.length; i++) {
                        if (isWalkable(az.x + dxs[i], az.y + dys[i], radius)) {
                            az.x += dxs[i];
                            az.y += dys[i];
                            break;
                        }
                    }
                }

                float speed = az.isBoss ? 1.4f : 1.7f;
                float moveX = (distXP / distP) * speed * delta;
                float moveY = (distYP / distP) * speed * delta;
                float nextX = az.x + moveX;
                float nextY = az.y + moveY;
                boolean movedX = false;
                boolean movedY = false;
                if (isWalkable(nextX, az.y, radius)) { az.x = nextX; movedX = true; }
                if (isWalkable(az.x, nextY, radius)) { az.y = nextY; movedY = true; }
                if (!movedX && !movedY) {
                    if (isWalkable(nextX, az.y, 0.15f)) az.x = nextX;
                    else if (isWalkable(az.x, nextY, 0.15f)) az.y = nextY;
                }

                if (zombieFrames != null) {
                    int walkCol = ((int) (az.stateTime / 0.15f)) % 8;
                    int safeRow = dirRow % zombieFrames.length;
                    int safeCol = walkCol % zombieFrames[0].length;
                    az.currentFrame = zombieFrames[safeRow][safeCol];
                    continue;
                }
            } else if (zombieIdleFrames != null) {
                int idleCol = ((int) (az.stateTime / 0.5f)) % 2;
                int safeRow = dirRow % zombieIdleFrames.length;
                int safeCol = idleCol % zombieIdleFrames[0].length;
                az.currentFrame = zombieIdleFrames[safeRow][safeCol];
                continue;
            }

            az.currentFrame = zombieFrames != null ? zombieFrames[0][0] : null;
        }
    }

    private void drawWorld(WorldSnapshot snapshot, float delta, TextureRegion zombieFrame, WorldSnapshot.PlayerState me) {
        drawCampaignFeatures();

        batch.end();

        Gdx.gl.glEnable(GL20.GL_STENCIL_TEST);
        Gdx.gl.glClear(GL20.GL_STENCIL_BUFFER_BIT);

        Gdx.gl.glColorMask(false, false, false, false);
        Gdx.gl.glDepthMask(false);
        Gdx.gl.glStencilFunc(GL20.GL_ALWAYS, 1, 0xFF);
        Gdx.gl.glStencilOp(GL20.GL_REPLACE, GL20.GL_REPLACE, GL20.GL_REPLACE);

        shapes.setProjectionMatrix(camera.combined);
        shapes.begin(ShapeRenderer.ShapeType.Filled);

        float cell = PIXELS_PER_TILE * (tileMap != null ? tileMap.getCellSize() : 1f);
        float halfW = camera.viewportWidth * 0.5f + cell;
        float halfH = camera.viewportHeight * 0.5f + cell;
        int minCellX = Math.max(0, tileMap.toCell((camera.position.x - halfW) / PIXELS_PER_TILE));
        int maxCellX = Math.min(tileMap.getCollisionWidth() - 1, tileMap.toCell((camera.position.x + halfW) / PIXELS_PER_TILE));
        int minCellY = Math.max(0, tileMap.toCell((camera.position.y - halfH) / PIXELS_PER_TILE));
        int maxCellY = Math.min(tileMap.getCollisionHeight() - 1, tileMap.toCell((camera.position.y + halfH) / PIXELS_PER_TILE));

        for (int cellY = minCellY; cellY <= maxCellY; cellY++) {
            for (int cellX = minCellX; cellX <= maxCellX; cellX++) {
                if (tileMap.isCellWalkable(cellX, cellY)) {
                    shapes.rect(cellX * cell, cellY * cell, cell, cell);
                }
            }
        }
        shapes.end();

        Gdx.gl.glColorMask(true, true, true, true);
        Gdx.gl.glDepthMask(true);
        Gdx.gl.glStencilFunc(GL20.GL_EQUAL, 1, 0xFF);
        Gdx.gl.glStencilOp(GL20.GL_KEEP, GL20.GL_KEEP, GL20.GL_KEEP);

        batch.begin();

        if (bloodTexture != null) {
            float bloodScale = 0.3f;
            float bw = bloodTexture.getWidth() * bloodScale;
            float bh = bloodTexture.getHeight() * bloodScale;
            for (Vector2 pool : bloodPools) {
                float px = Math.round((pool.x * PIXELS_PER_TILE) - (bw / 2f));
                float py = Math.round((pool.y * PIXELS_PER_TILE) - (bh / 2f));
                batch.draw(bloodTexture, px, py, bw, bh);
            }
        }

        if (snapshot.items != null) {
            for (WorldSnapshot.ItemState item : snapshot.items) {
                if (mockBombX == -1f) {
                    mockBombX = item.x + 1.0f;
                    mockBombY = item.y;
                }
            }

            if (!hasBomb && mockBombX != -1f) {
                float bScale = 0.1f;
                float scaledW = bombFrameWidth * bScale;
                float scaledH = bombTexture.getHeight() * bScale;
                float drawBX = Math.round((mockBombX * PIXELS_PER_TILE) - (scaledW / 2f));
                float drawBY = Math.round((mockBombY * PIXELS_PER_TILE) - (scaledH / 2f));
                batch.draw(bombFrames[0][0], drawBX, drawBY, scaledW, scaledH);
            }

            groundItemStateTime += delta;
            int currentMacheteFrame = (int) (groundItemStateTime / 0.35f) % 7;
            TextureRegion currentGroundFrame = meleeGroundFrames[0][currentMacheteFrame % meleeGroundFrames[0].length];

            float itemScale = 0.35f;
            float displayWidth = meleeFrameWidth * itemScale;
            float displayHeight = meleeTexture.getHeight() * itemScale;

            for (WorldSnapshot.ItemState item : snapshot.items) {
                float drawX = Math.round((item.x * PIXELS_PER_TILE) - (displayWidth / 2f));
                float drawY = Math.round((item.y * PIXELS_PER_TILE) - (displayHeight / 2f));
                batch.draw(currentGroundFrame, drawX, drawY, displayWidth, displayHeight);
            }
        }

        // Draw Stairs Key in Level 1
        if (levelNumber == 1 && !hasStairsKey && keyTexture != null) {
            float kSize = 24f;
            float bob = (float) Math.sin(groundItemStateTime * 4.0f) * 3.0f;
            float kDrawX = Math.round((keyX * PIXELS_PER_TILE) - (kSize / 2f));
            float kDrawY = Math.round((keyY * PIXELS_PER_TILE) - (kSize / 2f) + bob);
            batch.draw(keyTexture, kDrawX, kDrawY, kSize, kSize);
        }

        // The Level 3 ambulance key is deliberately shown only in the world,
        // not on the minimap, so players must search the map for it.
        if (levelNumber == 3 && !hasAmbulanceKey && keyTexture != null) {
            float kSize = 24f;
            float bob = (float) Math.sin(groundItemStateTime * 4.0f) * 3.0f;
            float kDrawX = Math.round((ambulanceKeyX * PIXELS_PER_TILE) - (kSize / 2f));
            float kDrawY = Math.round((ambulanceKeyY * PIXELS_PER_TILE) - (kSize / 2f) + bob);
            batch.draw(keyTexture, kDrawX, kDrawY, kSize, kSize);
        }

        if (levelNumber == 4 && markerTexture != null) {
            float bob = (float) Math.sin(groundItemStateTime * 4.0f) * 3.0f;
            Color[] colors = {Color.GOLD, Color.CYAN, Color.LIME};
            for (int i = 0; i < level4PuzzleParts.size(); i++) {
                Vector2 part = level4PuzzleParts.get(i);
                float size = 18f;
                float px = Math.round(part.x * PIXELS_PER_TILE - size / 2f);
                float py = Math.round(part.y * PIXELS_PER_TILE - size / 2f + bob);
                batch.setColor(colors[i % colors.length]);
                batch.draw(markerTexture, px, py, size, size);
            }
            batch.setColor(Color.WHITE);
        }

        // Draw Herbs and Floor Medkit in Level 1
        if (levelNumber == 1) {
            float bob = (float) Math.sin(groundItemStateTime * 4.0f) * 3.0f;

            TextureRegion currentHerbFrame = null;
            if (herbFrames != null && herbFrames.length >= 2) {
                int herbFrameIndex =
                        ((int) (groundItemStateTime / HERB_FRAME_DURATION)) % 2;
                currentHerbFrame = herbFrames[herbFrameIndex];
            }

            if (!herb1Collected) {
                float herbSize = HERB_DRAW_SIZE;
                float hx = Math.round((HERB_1_X * PIXELS_PER_TILE) - (herbSize / 2f));
                float hy = Math.round((HERB_1_Y * PIXELS_PER_TILE) - (herbSize / 2f) + bob);

                batch.setColor(Color.WHITE);
                if (currentHerbFrame != null) {
                    batch.draw(currentHerbFrame, hx, hy, herbSize, herbSize);
                } else if (markerTexture != null) {
                    batch.draw(markerTexture, hx, hy, 16f, 16f);
                }
            }

            if (!hasPickedFloorMedkit) {
                float medicSize = MEDIC_DRAW_SIZE;
                float mx = Math.round((MEDKIT_X * PIXELS_PER_TILE) - (medicSize / 2f));
                float my = Math.round((MEDKIT_Y * PIXELS_PER_TILE) - (medicSize / 2f) + bob);

                batch.setColor(Color.WHITE);
                if (medicTexture != null) {
                    batch.draw(medicTexture, mx, my, medicSize, medicSize);
                } else if (markerTexture != null) {
                    batch.draw(markerTexture, mx, my, 16f, 16f);
                }
            }

            if (!herb2Collected) {
                float herbSize = HERB_DRAW_SIZE;
                float hx = Math.round((HERB_2_X * PIXELS_PER_TILE) - (herbSize / 2f));
                float hy = Math.round((HERB_2_Y * PIXELS_PER_TILE) - (herbSize / 2f) + bob);

                batch.setColor(Color.WHITE);
                if (currentHerbFrame != null) {
                    batch.draw(currentHerbFrame, hx, hy, herbSize, herbSize);
                } else if (markerTexture != null) {
                    batch.draw(markerTexture, hx, hy, 16f, 16f);
                }
            }

            batch.setColor(Color.WHITE);
        }

        batch.end();
        Gdx.gl.glDisable(GL20.GL_STENCIL_TEST);
        batch.begin();

        for (int i = activeBombs.size() - 1; i >= 0; i--) {
            ActiveBomb b = activeBombs.get(i);
            b.timeElapsed += delta;
            float t = b.timeElapsed / b.totalDuration;

            float currX = b.startX + (b.targetX - b.startX) * t;
            float currY = b.startY + (b.targetY - b.startY) * t;

            boolean hitWall = false;
            if (!isWalkable(currX, currY, 0.05f)) {
                hitWall = true;
            }

            if (t >= 1.0f || hitWall) {
                float expX = hitWall ? currX : b.targetX;
                float expY = hitWall ? currY : b.targetY;

                // Play once at the exact impact/explosion moment:
                // - hitWall == true: bomb collided before landing
                // - t >= 1.0f: bomb completed its arc and hit the ground
                playBombSound();

                ActiveExplosion exp = new ActiveExplosion();
                exp.x = expX;
                exp.y = expY;
                activeExplosions.add(exp);

                float blastRadius = 1.5f;

                if (!isZombieDead) {
                    float distZ = (float) Math.sqrt(Math.pow(middleZombieX - exp.x, 2) + Math.pow(middleZombieY - exp.y, 2));
                    float dmgZ = 0f;
                    if (distZ <= 0.5f) dmgZ = 300f;
                    else if (distZ <= 1.0f) dmgZ = 200f;
                    else if (distZ <= 1.5f) dmgZ = 100f;

                    if (dmgZ > 0f) {
                        middleZombieHp -= dmgZ;
                        if (middleZombieHp <= 0f) {
                            isZombieDead = true;
                            isBeingBitten = false;
                            bloodPools.add(new Vector2(middleZombieX, middleZombieY));
                            checkZombiePatrolObjective();
                            broadcastZombieKill(MIDDLE_ZOMBIE_ID);
                        }
                    }
                }

                if (isAmbushActive) {
                    for (AmbushZombie az : ambushZombies) {
                        if (az.dead) continue;
                        float distAZ = (float) Math.sqrt(Math.pow(az.x - exp.x, 2) + Math.pow(az.y - exp.y, 2));
                        float dmgAZ = 0f;
                        if (distAZ <= 0.5f) dmgAZ = 300f;
                        else if (distAZ <= 1.0f) dmgAZ = 200f;
                        else if (distAZ <= 1.5f) dmgAZ = 100f;

                        if (dmgAZ > 0f) {
                            az.hp -= dmgAZ;
                            if (az.hp <= 0f) {
                                az.dead = true;
                                grantLabPasskeyIfBoss(az);
                                bloodPools.add(new Vector2(az.x, az.y));
                                broadcastAmbushZombieKill(az);
                            }
                        }
                    }
                    checkAmbushCompletion();
                }

                if (snapshot != null && snapshot.players != null) {
                    for (WorldSnapshot.PlayerState p : snapshot.players) {
                        if (p != null && !p.downed && p.hp > 0f) {
                            if (levelNumber == 1 && !isJaneRevived && p.character == CharacterType.JANE) continue;
                            float distP = (float) Math.hypot(p.x - exp.x, p.y - exp.y);
                            float dmgP = 0f;
                            if (distP <= 0.5f) dmgP = 300f;
                            else if (distP <= 1.0f) dmgP = 200f;
                            else if (distP <= 1.5f) dmgP = 100f;

                            if (dmgP > 0f) {
                                if (me != null && p.playerId.equals(me.playerId)) {
                                    client.sendEvent("ZOMBIE_BITE_DAMAGE", String.valueOf(dmgP));
                                } else {
                                    client.sendEvent("ZOMBIE_BITE_DAMAGE", dmgP + ":" + p.playerId);
                                }
                            }
                        }
                    }
                }
                boolean hasPlayerJaneForBomb = isDualViewDebugMode;
                if (snapshot != null && snapshot.players != null) {
                    for (WorldSnapshot.PlayerState p : snapshot.players) {
                        if (p.character == CharacterType.JANE) {
                            hasPlayerJaneForBomb = true;
                            break;
                        }
                    }
                }
                if (!hasPlayerJaneForBomb && isJaneRevived && janeHp > 0f) {
                    float distJ = (float) Math.hypot(janeX - exp.x, janeY - exp.y);
                    float dmgJ = 0f;
                    if (distJ <= 0.5f) dmgJ = 300f;
                    else if (distJ <= 1.0f) dmgJ = 200f;
                    else if (distJ <= 1.5f) dmgJ = 100f;
                    if (dmgJ > 0f) {
                        janeHp = Math.max(0f, janeHp - dmgJ);
                        CampaignSquadState.janeHp = janeHp;
                        if (janeHp <= 0f) {
                            bloodPools.add(new Vector2(janeX, janeY));
                            showBanner("Jane was caught in the blast!");
                        }
                    }
                }

                activeBombs.remove(i);
                continue;
            }

            float maxArcHeightTiles = 2.0f;
            float arcHeightPx = 4.0f * maxArcHeightTiles * t * (1.0f - t) * PIXELS_PER_TILE;

            int frameIdx = (int) ((b.timeElapsed / 0.05f) % 8);

            float bScale = 0.2f;
            float scaledW = bombFrameWidth * bScale;
            float scaledH = bombTexture.getHeight() * bScale;

            float bombDrawX = Math.round((currX * PIXELS_PER_TILE) - (scaledW / 2f));
            float bombDrawY = Math.round((currY * PIXELS_PER_TILE) - (scaledH / 2f) + arcHeightPx);

            batch.draw(bombFrames[0][frameIdx], bombDrawX, bombDrawY, scaledW, scaledH);
        }

        for (int i = activeExplosions.size() - 1; i >= 0; i--) {
            ActiveExplosion exp = activeExplosions.get(i);
            exp.timeElapsed += delta;

            if (exp.timeElapsed >= exp.totalDuration) {
                activeExplosions.remove(i);
                continue;
            }

            int frameIdx = (int) ((exp.timeElapsed / exp.totalDuration) * 8);
            if (frameIdx >= 8) frameIdx = 7;

            float expScale = 1.0f;
            float expW = bombEffectFrameWidth * expScale;
            float expH = bombEffectTexture.getHeight() * expScale;

            float drawX = Math.round((exp.x * PIXELS_PER_TILE) - (expW / 2f));
            float drawY = Math.round((exp.y * PIXELS_PER_TILE) - (expH / 2f));

            batch.draw(bombEffectFrames[0][frameIdx], drawX, drawY, expW, expH);
        }

        // 5. Middle Zombie
        if (!isZombieDead && zombieFrame != null) {
            float currentZDrawWidth = middleZombieBiting ? zombieBiteFrameWidth : zombieFrameWidth;
            float currentZDrawHeight = middleZombieBiting ? zombieBiteFrameHeight : zombieFrameHeight;
            float midDrawX = Math.round((middleZombieX * PIXELS_PER_TILE) - (currentZDrawWidth / 2f));
            float midDrawY = Math.round((middleZombieY * PIXELS_PER_TILE) - SPRITE_FEET_INSET_PX);
            batch.draw(zombieFrame, midDrawX, midDrawY, currentZDrawWidth, currentZDrawHeight);
        }

        // 5b. Ambush Zombies & Boss
        if (isAmbushActive) {
            for (AmbushZombie az : ambushZombies) {
                if (az.dead) continue;
                TextureRegion azFrame = az.currentFrame;
                if (azFrame != null) {
                    float baseW = az.biting ? zombieBiteFrameWidth : zombieFrameWidth;
                    float baseH = az.biting ? zombieBiteFrameHeight : zombieFrameHeight;
                    float scale = az.isBoss ? 1.45f : 1.0f;
                    float azDrawWidth = baseW * scale;
                    float azDrawHeight = baseH * scale;
                    float azDrawX = Math.round((az.x * PIXELS_PER_TILE) - (azDrawWidth / 2f));
                    float azDrawY = Math.round((az.y * PIXELS_PER_TILE) - SPRITE_FEET_INSET_PX);

                    if (az.isBoss) {
                        batch.setColor(1.0f, 0.45f, 0.45f, 1.0f);
                    }
                    batch.draw(azFrame, azDrawX, azDrawY, azDrawWidth, azDrawHeight);
                    if (az.isBoss) {
                        batch.setColor(Color.WHITE);
                    }
                }
            }
        }

        // 5d. Rescued / Stranded Villagers (Follow squad across all levels)
        // Dr. Ramirez (v1) uses v1 sprites, Nurse Claire (v2) uses v2 sprites.
        for (LevelVillager v : levelVillagers) {
            if (v.isDead) continue;
            // Stranded (not yet rescued) villagers are visible from the start of every level, not only level 1

            boolean isV2 = "v2".equals(v.id);

            // Normal mapping:
            // v1 id -> v1/v1_idle assets
            // v2 id -> v2/v2_idle assets
            TextureRegion[][] walkFramesForVillager = isV2
                    ? villagerV2WalkFrames
                    : villagerV1WalkFrames;
            TextureRegion[][] idleFramesForVillager = isV2
                    ? villagerV2IdleFrames
                    : villagerV1IdleFrames;

            int walkFrameWidthForVillager = isV2
                    ? villagerV2WalkFrameWidth
                    : villagerV1WalkFrameWidth;
            int walkFrameHeightForVillager = isV2
                    ? villagerV2WalkFrameHeight
                    : villagerV1WalkFrameHeight;

            int idleFrameWidthForVillager = isV2
                    ? villagerV2IdleFrameWidth
                    : villagerV1IdleFrameWidth;
            int idleFrameHeightForVillager = isV2
                    ? villagerV2IdleFrameHeight
                    : villagerV1IdleFrameHeight;

            TextureRegion villagerFrame;
            int baseFrameWidth;
            int baseFrameHeight;
            boolean usingIdleFrame = false;

            int row = Math.max(0, Math.min(3, v.facing));

            if (v.moving && walkFramesForVillager != null) {
                int frame = ((int) (v.stateTime / VILLAGER_WALK_FRAME_DURATION)) % 8;
                villagerFrame = walkFramesForVillager[row][frame];
                baseFrameWidth = walkFrameWidthForVillager;
                baseFrameHeight = walkFrameHeightForVillager;
            } else if (idleFramesForVillager != null) {
                int frame = ((int) (v.stateTime / VILLAGER_IDLE_FRAME_DURATION)) % 8;
                villagerFrame = idleFramesForVillager[row][frame];
                baseFrameWidth = idleFrameWidthForVillager;
                baseFrameHeight = idleFrameHeightForVillager;
                usingIdleFrame = true;
            } else if (walkFramesForVillager != null) {
                // If an idle sheet is missing, keep the character visible
                // using the first walking frame for its current facing.
                villagerFrame = walkFramesForVillager[row][0];
                baseFrameWidth = walkFrameWidthForVillager;
                baseFrameHeight = walkFrameHeightForVillager;
            } else {
                // Never fall back to player sprites.
                continue;
            }

            float drawScale = VILLAGER_SCALE;

            // v2.png walking frames: +30%
            if (isV2 && !usingIdleFrame) {
                drawScale *= V2_WALK_SCALE_MULTIPLIER;
            }

            // v1_idle.png idle frames: +30%
            if (!isV2 && usingIdleFrame) {
                drawScale *= V1_IDLE_SCALE_MULTIPLIER;
            }

            // v2_idle.png idle frames: +30%
            if (isV2 && usingIdleFrame) {
                drawScale *= V2_IDLE_SCALE_MULTIPLIER;
            }

            float drawWidth = baseFrameWidth * drawScale;
            float drawHeight = baseFrameHeight * drawScale;

            // Keep the sprite centered on the same world X and anchored at the feet.
            float vx = Math.round((v.x * PIXELS_PER_TILE) - (drawWidth / 2f));
            float vy = Math.round((v.y * PIXELS_PER_TILE) - SPRITE_FEET_INSET_PX);

            if (!v.isRescued) {
                batch.setColor(1.0f, 0.95f, 0.70f, 1.0f);
            } else {
                batch.setColor(Color.WHITE);
            }
            batch.draw(villagerFrame, vx, vy, drawWidth, drawHeight);
            batch.setColor(Color.WHITE);

            if (!v.isRescued) {
                font.setColor(Color.YELLOW);
                font.draw(batch, v.name, Math.round(v.x * PIXELS_PER_TILE - 28f), Math.round(vy + drawHeight + 12f));
                font.setColor(Color.WHITE);
            } else if (v.isStaying) {
                font.setColor(Color.ORANGE);
                font.draw(batch, "[HOLD]", Math.round(v.x * PIXELS_PER_TILE - 18f), Math.round(vy + drawHeight + 12f));
                font.setColor(Color.WHITE);
            }
        }

        // 5e. Senseless Jane in Pharmacy / Revived Jane Ally (All levels)
        // In dual-view debug co-op or network co-op, Jane is player 2 (rendered in section 6)
        boolean hasPlayerJane = isDualViewDebugMode;
        if (snapshot.players != null) {
            for (WorldSnapshot.PlayerState p : snapshot.players) {
                if (p.character == CharacterType.JANE) {
                    hasPlayerJane = true;
                    break;
                }
            }
        }
        if (!hasPlayerJane) {
            TextureRegion jFrame = aiJaneAnim.currentFrame != null ? aiJaneAnim.currentFrame :
                    ((p2IdleMeleeFrames != null) ? p2IdleMeleeFrames[0][0]
                     : ((p2IdleFrames != null) ? p2IdleFrames[0][0]
                        : ((femaleFrames != null) ? femaleFrames[0][0] : idleFrames[0][0])));
            if (!isJaneRevived) {
                if (levelNumber == 1 && janeKnockedTexture != null) {
                    float knockedWidth = janeKnockedTexture.getWidth() * JANE_KNOCKED_SCALE;
                    float knockedHeight = janeKnockedTexture.getHeight() * JANE_KNOCKED_SCALE;

                    // Center the lying sprite on Jane's pharmacy world position.
                    float jx = Math.round((JANE_PHARMACY_X * PIXELS_PER_TILE) - (knockedWidth / 2f));
                    float jy = Math.round((JANE_PHARMACY_Y * PIXELS_PER_TILE) - (knockedHeight / 2f));

                    batch.setColor(Color.WHITE);
                    batch.draw(
                            janeKnockedTexture,
                            jx,
                            jy,
                            knockedWidth,
                            knockedHeight
                    );
                }
            } else {
                float jScale = FEMALE_MELEE_SCALE;
                float jWidth = (jFrame != null) ? jFrame.getRegionWidth() * jScale : (frameWidth * 0.8f * jScale);
                float jHeight = (jFrame != null) ? jFrame.getRegionHeight() * jScale : (frameHeight * 0.8f * jScale);
                if (aiJaneAnim.isAttacking && p2MeleeHitFrames != null) {
                    jWidth = p2MeleeHitFrameWidth * 0.8f * jScale;
                    jHeight = p2MeleeHitFrameHeight * 0.8f * jScale;
                }
                float jx = Math.round((janeX * PIXELS_PER_TILE) - (jWidth / 2f));
                float jy = Math.round((janeY * PIXELS_PER_TILE) - SPRITE_FEET_INSET_PX);
                batch.draw(jFrame, jx, jy, jWidth, jHeight);
            }
        }

        // 6. Players
        if (snapshot.players != null) {
            for (WorldSnapshot.PlayerState player : snapshot.players) {
                PlayerAnimState anim = animStates.computeIfAbsent(player.playerId, k -> new PlayerAnimState());
                TextureRegion currentFrame = anim.currentFrame;
                if (currentFrame == null) continue;

                boolean isFemale = (player.character == CharacterType.JANE);
                boolean isUnconsciousJane = isFemale && levelNumber == 1 && !isJaneRevived;
                boolean isLocalPlayer = me != null && player.playerId.equals(me.playerId);

                float drawX = Math.round((player.x * PIXELS_PER_TILE) - (anim.currentDrawWidth / 2f));
                float drawY = Math.round((player.y * PIXELS_PER_TILE) - SPRITE_FEET_INSET_PX);

                if (isUnconsciousJane && janeKnockedTexture != null) {
                    float knockedWidth = janeKnockedTexture.getWidth() * JANE_KNOCKED_SCALE;
                    float knockedHeight = janeKnockedTexture.getHeight() * JANE_KNOCKED_SCALE;

                    // Before revival, always show the dedicated knocked Jane art,
                    // even if Jane exists as Player 2 in the world snapshot.
                    float knockedX = Math.round((JANE_PHARMACY_X * PIXELS_PER_TILE) - (knockedWidth / 2f));
                    float knockedY = Math.round((JANE_PHARMACY_Y * PIXELS_PER_TILE) - (knockedHeight / 2f));

                    batch.setColor(Color.WHITE);
                    batch.draw(
                            janeKnockedTexture,
                            knockedX,
                            knockedY,
                            knockedWidth,
                            knockedHeight
                    );
                } else {
                    batch.draw(currentFrame, drawX, drawY, anim.currentDrawWidth, anim.currentDrawHeight);
                }

                // Healing Aura & Floating Text
                if (isLocalPlayer && healEffectTimer > 0f && healEffectTexture != null) {
                    float effSize = 72f + (float) Math.sin((1.5f - healEffectTimer) * 8.0f) * 10f;
                    float effX = Math.round((player.x * PIXELS_PER_TILE) - (effSize / 2f));
                    float effY = Math.round((player.y * PIXELS_PER_TILE) - (effSize / 2f) + 12f);
                    float alpha = Math.min(1.0f, healEffectTimer / 0.5f);
                    batch.setColor(0.3f, 1.0f, 0.5f, alpha * 0.85f);
                    batch.draw(healEffectTexture, effX, effY, effSize, effSize);
                    batch.setColor(Color.WHITE);
                }

                if (isLocalPlayer && healFloatingTextTimer > 0f) {
                    float lift = (1.5f - healFloatingTextTimer) * 28f;
                    float tx = Math.round((player.x * PIXELS_PER_TILE) - 22f);
                    float ty = Math.round((player.y * PIXELS_PER_TILE) + 42f + lift);
                    font.setColor(0.2f, 1.0f, 0.4f, Math.min(1f, healFloatingTextTimer / 0.4f));
                    font.draw(batch, "+35 HP", tx, ty);
                    font.setColor(Color.WHITE);
                }
            }
        }
    }

    private boolean applyMeleeAttackDamage(WorldSnapshot.PlayerState player, PlayerAnimState anim, boolean isJaneMelee) {
        float playerDamage = isJaneMelee ? 30f : 50f;
        boolean didDamage = false;

        if (!isZombieDead) {
            float distX = middleZombieX - player.x;
            float distY = middleZombieY - player.y;
            float distanceToZombie = (float) Math.sqrt(distX * distX + distY * distY);

            if (distanceToZombie <= 2.2f) {
                boolean validHit = false;
                if (anim.currentRow == 3 && distY > 0 && Math.abs(distX) <= 1.5f) validHit = true;
                else if (anim.currentRow == 0 && distY < 0 && Math.abs(distX) <= 1.5f) validHit = true;
                else if (anim.currentRow == 2 && distX > 0 && Math.abs(distY) <= 1.5f) validHit = true;
                else if (anim.currentRow == 1 && distX < 0 && Math.abs(distY) <= 1.5f) validHit = true;

                if (validHit) {
                    middleZombieHp -= playerDamage;
                    didDamage = true;
                    if (isJaneMelee) {
                        float kbX = Math.signum(distX) * 0.5f;
                        float kbY = Math.signum(distY) * 0.5f;
                        if (isWalkable(middleZombieX + kbX * 0.5f, middleZombieY, 0.25f) && isWalkable(middleZombieX + kbX, middleZombieY, 0.25f)) middleZombieX += kbX;
                        else if (isWalkable(middleZombieX + kbX * 0.5f, middleZombieY, 0.25f)) middleZombieX += kbX * 0.5f;
                        if (isWalkable(middleZombieX, middleZombieY + kbY * 0.5f, 0.25f) && isWalkable(middleZombieX, middleZombieY + kbY, 0.25f)) middleZombieY += kbY;
                        else if (isWalkable(middleZombieX, middleZombieY + kbY * 0.5f, 0.25f)) middleZombieY += kbY * 0.5f;
                    }
                    if (middleZombieHp <= 0f) {
                        isZombieDead = true;
                        isBeingBitten = false;
                        coins += 1;
                        bloodPools.add(new Vector2(middleZombieX, middleZombieY));
                        checkZombiePatrolObjective();
                        broadcastZombieKill(MIDDLE_ZOMBIE_ID);
                    }
                }
            }
        }

        if (isAmbushActive) {
            for (AmbushZombie az : ambushZombies) {
                if (az.dead) continue;
                float azDistX = az.x - player.x;
                float azDistY = az.y - player.y;
                float azDistance = (float) Math.sqrt(azDistX * azDistX + azDistY * azDistY);

                float hitReach = az.isBoss ? 2.5f : 2.2f;
                if (azDistance <= hitReach) {
                    boolean azHit = false;
                    float bound = 1.5f;
                    if (anim.currentRow == 3 && azDistY > 0 && Math.abs(azDistX) <= bound) azHit = true;
                    else if (anim.currentRow == 0 && azDistY < 0 && Math.abs(azDistX) <= bound) azHit = true;
                    else if (anim.currentRow == 2 && azDistX > 0 && Math.abs(azDistY) <= bound) azHit = true;
                    else if (anim.currentRow == 1 && azDistX < 0 && Math.abs(azDistY) <= bound) azHit = true;

                    if (azHit) {
                        az.hp -= playerDamage;
                        didDamage = true;
                        if (isJaneMelee && !az.isBoss) {
                            float kbX = Math.signum(azDistX) * 0.5f;
                            float kbY = Math.signum(azDistY) * 0.5f;
                            if (isWalkable(az.x + kbX * 0.5f, az.y, 0.25f) && isWalkable(az.x + kbX, az.y, 0.25f)) az.x += kbX;
                            else if (isWalkable(az.x + kbX * 0.5f, az.y, 0.25f)) az.x += kbX * 0.5f;
                            if (isWalkable(az.x, az.y + kbY * 0.5f, 0.25f) && isWalkable(az.x, az.y + kbY, 0.25f)) az.y += kbY;
                            else if (isWalkable(az.x, az.y + kbY * 0.5f, 0.25f)) az.y += kbY * 0.5f;
                        }
                        if (az.hp <= 0f) {
                            az.dead = true;
                            grantLabPasskeyIfBoss(az);
                            coins += 1;
                            bloodPools.add(new Vector2(az.x, az.y));
                            broadcastAmbushZombieKill(az);
                        }
                    }
                }
            }
            checkAmbushCompletion();
        }

        return didDamage;
    }

    private void updatePlayerAnimations(WorldSnapshot snapshot, float delta, WorldSnapshot.PlayerState me) {
        if (snapshot == null || snapshot.players == null) return;

        for (WorldSnapshot.PlayerState player : snapshot.players) {
            PlayerAnimState anim = animStates.computeIfAbsent(player.playerId, k -> new PlayerAnimState());

            if (anim.lastX == -1f) {
                anim.lastX = player.x;
                anim.lastY = player.y;
            }

            float dx = player.x - anim.lastX;
            float dy = player.y - anim.lastY;
            boolean moving = Math.abs(dx) > 0.001f || Math.abs(dy) > 0.001f;
            anim.moving = moving;
            anim.lastX = player.x;
            anim.lastY = player.y;

            boolean isFemale = (player.character == CharacterType.JANE);
            boolean isUnconsciousJane = isFemale && levelNumber == 1 && !isJaneRevived;
            if (isUnconsciousJane) {
                moving = false;
                anim.moving = false;
                anim.isAttacking = false;
            }

            boolean isLocalPlayer = me != null && player.playerId.equals(me.playerId);
            boolean showMacheteSprite = (isLocalPlayer ? isMacheteEquipped : "MELEE".equals(player.equippedWeapon))
                    || (isDualViewDebugMode && isFemale);
            boolean showBombSprite = isLocalPlayer && isBombEquipped;

            boolean localSprint = isLocalPlayer && Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT) && stamina > 0f && moving;
            boolean p2Sprint = isDualViewDebugMode && isFemale && (Gdx.input.isKeyPressed(Input.Keys.SHIFT_RIGHT) || Gdx.input.isKeyPressed(Input.Keys.NUMPAD_ENTER)) && p2Stamina > 0f && moving;
            boolean remoteSprint = !isLocalPlayer && !isDualViewDebugMode && (Math.abs(dx) > 0.08f || Math.abs(dy) > 0.08f);
            boolean isSprintingAnim = localSprint || p2Sprint || remoteSprint;

            TextureRegion[][] activeRunFrames = isFemale
                    ? (p2Frames != null ? p2Frames : (femaleFrames != null ? femaleFrames : playerFrames))
                    : playerFrames;
            TextureRegion[][] activeIdleFrames = isFemale
                    ? (p2IdleFrames != null ? p2IdleFrames : (femaleFrames != null ? femaleFrames : idleFrames))
                    : idleFrames;

            if (showMacheteSprite) {
                if (isFemale && p2IdleMeleeFrames != null) {
                    activeIdleFrames = p2IdleMeleeFrames;
                } else if (!isFemale) {
                    activeIdleFrames = idleMeleeFrames;
                }
            } else if (showBombSprite) {
                if (isFemale && p2BombIdleFrames != null) {
                    activeIdleFrames = p2BombIdleFrames;
                } else {
                    activeIdleFrames = playerBombIdleFrames;
                }
            }

            if (isSprintingAnim) {
                if (isFemale && p2SprintFrames != null) {
                    activeRunFrames = p2SprintFrames;
                } else if (!isFemale) {
                    activeRunFrames = playerSprintFrames;
                }
            } else if (showMacheteSprite) {
                if (isFemale && p2MeleeFrames != null) {
                    activeRunFrames = p2MeleeFrames;
                } else if (!isFemale) {
                    activeRunFrames = playerMeleeFrames;
                }
            } else if (showBombSprite) {
                if (isFemale && p2BombFrames != null) {
                    activeRunFrames = p2BombFrames;
                } else {
                    activeRunFrames = playerBombFrames;
                }
            }

            int animFrames = (activeRunFrames != null && activeRunFrames[0].length >= 8) ? 8 : 4;
            int idleAnimFrames = (activeIdleFrames != null && activeIdleFrames[0].length >= 8) ? 8 : 2;
            int activeOffset = 0;

            anim.currentColumn = anim.currentColumn % animFrames;

            if (moving) {
                if (Math.abs(dx) > 0.005f) {
                    anim.currentRow = dx > 0 ? 2 : 1;
                } else if (Math.abs(dy) > 0.005f) {
                    anim.currentRow = dy > 0 ? 3 : 0;
                }

                anim.stateTime += delta;
                float runSpeed = isSprintingAnim ? 0.07f : 0.15f;
                if (anim.stateTime > runSpeed) {
                    anim.currentColumn = (anim.currentColumn + 1) % animFrames;
                    anim.stateTime = 0f;
                }

                int safeRow = anim.currentRow % activeRunFrames.length;
                int safeCol = (anim.currentColumn + activeOffset) % activeRunFrames[0].length;
                anim.currentFrame = activeRunFrames[safeRow][safeCol];
            } else {
                anim.stateTime += delta;
                float idleSpeed = showBombSprite ? 0.15f : 0.5f;
                if (anim.stateTime > idleSpeed) {
                    anim.currentColumn = (anim.currentColumn + 1) % idleAnimFrames;
                    anim.stateTime = 0f;
                }

                int safeRow = anim.currentRow % activeIdleFrames.length;
                int safeCol = (anim.currentColumn + activeOffset) % activeIdleFrames[0].length;
                anim.currentFrame = activeIdleFrames[safeRow][safeCol];
            }

            if (isUnconsciousJane) {
                anim.currentFrame = (p2IdleFrames != null) ? p2IdleFrames[0][0]
                        : ((femaleFrames != null) ? femaleFrames[0][0] : idleFrames[0][0]);
            }

            if (anim.currentFrame != null) {
                anim.currentDrawWidth = anim.currentFrame.getRegionWidth();
                anim.currentDrawHeight = anim.currentFrame.getRegionHeight();
            }

            if (isFemale) {
                float fScale = showMacheteSprite ? FEMALE_MELEE_SCALE : FEMALE_SPRITE_SCALE;
                anim.currentDrawWidth *= fScale;
                anim.currentDrawHeight *= fScale;
            }

            if (showBombSprite && !isSprintingAnim) {
                anim.currentDrawWidth *= 0.9f;
                anim.currentDrawHeight *= 0.9f;
            }

            if (anim.isAttacking && showMacheteSprite) {
                anim.attackTime += delta;
                if (isFemale && p2MeleeHitFrames != null) {
                    // Jane uses player 2/melee_hit.png (2 frames per row, 4 rows)
                    float attackSpeed = 0.22f;
                    int attackFrame = (int) (anim.attackTime / attackSpeed);

                    if (attackFrame >= 1 && !anim.damageApplied && (isLocalPlayer || isDualViewDebugMode)) {
                        anim.damageApplied = true;
                        if (applyMeleeAttackDamage(player, anim, true)) {
                            playMacheteSound();
                        } else {
                            playSwingSound();
                        }
                    }

                    if (attackFrame >= 2) {
                        anim.isAttacking = false;
                    } else {
                        int safeRow = anim.currentRow % p2MeleeHitFrames.length;
                        int safeCol = attackFrame % p2MeleeHitFrames[0].length;
                        anim.currentFrame = p2MeleeHitFrames[safeRow][safeCol];
                        anim.currentDrawWidth = p2MeleeHitFrameWidth * 0.8f * FEMALE_MELEE_SCALE;
                        anim.currentDrawHeight = p2MeleeHitFrameHeight * 0.8f * FEMALE_MELEE_SCALE;
                    }
                } else if (isFemale && p2MeleeFrames != null) {
                    // Fallback to player 2/player_melee.png (8 frames per row)
                    float frameDuration = 0.045f;
                    int attackCol = (int) (anim.attackTime / frameDuration);

                    if (attackCol >= 2 && !anim.damageApplied && (isLocalPlayer || isDualViewDebugMode)) {
                        anim.damageApplied = true;
                        if (applyMeleeAttackDamage(player, anim, true)) {
                            playMacheteSound();
                        } else {
                            playSwingSound();
                        }
                    }

                    if (attackCol >= 8) {
                        anim.isAttacking = false;
                    } else {
                        int safeRow = anim.currentRow % p2MeleeFrames.length;
                        int safeCol = attackCol % p2MeleeFrames[0].length;
                        anim.currentFrame = p2MeleeFrames[safeRow][safeCol];
                        anim.currentDrawWidth = p2MeleeFrames[safeRow][safeCol].getRegionWidth() * FEMALE_MELEE_SCALE;
                        anim.currentDrawHeight = p2MeleeFrames[safeRow][safeCol].getRegionHeight() * FEMALE_MELEE_SCALE;
                    }
                } else {
                    // Elric uses meleeHitFrames (melee_hit.png)
                    float attackSpeed = 0.25f;
                    int attackFrame = (int) (anim.attackTime / attackSpeed);

                    if (attackFrame >= 1 && !anim.damageApplied && (isLocalPlayer || isDualViewDebugMode)) {
                        anim.damageApplied = true;
                        if (applyMeleeAttackDamage(player, anim, false)) {
                            playMacheteSound();
                        } else {
                            playSwingSound();
                        }
                    }

                    if (attackFrame >= 2) {
                        anim.isAttacking = false;
                    } else {
                        int safeRow = anim.currentRow % meleeHitFrames.length;
                        int safeCol = attackFrame % meleeHitFrames[0].length;
                        anim.currentFrame = meleeHitFrames[safeRow][safeCol];
                        anim.currentDrawWidth = meleeHitFrameWidth * 0.8f;
                        anim.currentDrawHeight = meleeHitFrameHeight * 0.8f;
                    }
                }
            } else if (anim.isAttacking && showBombSprite) {
                anim.attackTime += delta;
                float attackSpeed = 0.125f;
                int attackFrame = (int) (anim.attackTime / attackSpeed);

                if (attackFrame >= 2) {
                    anim.isAttacking = false;
                } else if (playerBombThrowFrames != null) {
                    int safeRow = anim.currentRow % playerBombThrowFrames.length;
                    int safeCol = attackFrame % playerBombThrowFrames[0].length;
                    anim.currentFrame = playerBombThrowFrames[safeRow][safeCol];
                    anim.currentDrawWidth = anim.currentFrame.getRegionWidth() * 0.6f;
                    anim.currentDrawHeight = anim.currentFrame.getRegionHeight() * 0.6f;
                    if (isFemale) {
                        anim.currentDrawWidth *= FEMALE_SPRITE_SCALE;
                        anim.currentDrawHeight *= FEMALE_SPRITE_SCALE;
                    }
                }
            }
        }
    }

    private void drawCampaignFeatures() {
        if (levelNumber != 2 || markerTexture == null) return;
        for (CampaignLevelPlan.Feature feature : levelFeatures) {
            if (completedFeatureIds.contains(feature.actionId())) continue;

            Color tint = switch (feature.type()) {
                case ZOMBIE_ENCOUNTER -> new Color(0.82f, 0.18f, 0.16f, 0.60f);
                case SURVIVOR -> new Color(0.20f, 0.78f, 0.67f, 0.82f);
                case POWER_RELAY -> new Color(0.94f, 0.70f, 0.15f, 0.82f);
            };
            float size = feature.type() == CampaignLevelPlan.FeatureType.ZOMBIE_ENCOUNTER ? 54f : 30f;
            float x = feature.tileX() * PIXELS_PER_TILE - size / 2f;
            float y = feature.tileY() * PIXELS_PER_TILE - size / 2f;
            batch.setColor(tint);
            batch.draw(markerTexture, x, y, size, size);
            batch.setColor(Color.WHITE);
        }
    }

    private void drawInventoryOverlay() {
        Matrix4 hudMatrix = new Matrix4().setToOrtho2D(0, 0, VIRTUAL_WIDTH, VIRTUAL_HEIGHT);
        batch.setProjectionMatrix(hudMatrix);
        batch.begin();

        float invX = Math.round((VIRTUAL_WIDTH - inventoryTexture.getWidth()) / 2f);
        float invY = Math.round((VIRTUAL_HEIGHT - inventoryTexture.getHeight()) / 2f);
        batch.draw(inventoryTexture, invX, invY);

        batch.end();

        Gdx.gl.glEnable(GL20.GL_BLEND);
        shapes.setProjectionMatrix(hudMatrix);

        float rowW = inventoryTexture.getWidth() - 40f;
        float rowH = 50f;
        float rowSpacing = 5f;
        float rowX = invX + 20f;
        float startY = invY + inventoryTexture.getHeight() - 115f;

        float mouseX = (Gdx.input.getX() - viewport.getScreenX()) * (VIRTUAL_WIDTH / viewport.getScreenWidth());
        float mouseY = (Gdx.graphics.getHeight() - Gdx.input.getY() - viewport.getScreenY()) * (VIRTUAL_HEIGHT / viewport.getScreenHeight());
        boolean justPressed = Gdx.input.isButtonJustPressed(Input.Buttons.LEFT);
        boolean isPressed = Gdx.input.isButtonPressed(Input.Buttons.LEFT);

        int itemIndex = 0;
        shapes.begin(ShapeRenderer.ShapeType.Filled);

        if (hasMachete) {
            float rowY = startY - (itemIndex * (rowH + rowSpacing));
            float btnW = 80f;
            float btnH = 30f;
            float btnX = rowX + rowW - btnW - 10f;
            float btnY = rowY + (rowH - btnH) / 2f;

            boolean rowHovered = mouseX >= rowX && mouseX <= rowX + rowW && mouseY >= rowY && mouseY <= rowY + rowH;
            boolean btnHovered = mouseX >= btnX && mouseX <= btnX + btnW && mouseY >= btnY && mouseY <= btnY + btnH;

            if (btnHovered && justPressed) {
                isMacheteEquipped = !isMacheteEquipped;
                if (isMacheteEquipped) isBombEquipped = false;
                CampaignSquadState.isMacheteEquipped = isMacheteEquipped;
                CampaignSquadState.isBombEquipped = isBombEquipped;
            }

            shapes.setColor(rowHovered ? new Color(1f, 1f, 1f, 0.08f) : new Color(1f, 1f, 1f, 0.02f));
            shapes.rect(rowX, rowY, rowW, rowH);

            if (isMacheteEquipped) {
                shapes.setColor((btnHovered && isPressed) ? new Color(0.5f, 0.2f, 0.2f, 1f) : (btnHovered ? new Color(0.7f, 0.3f, 0.3f, 1f) : new Color(0.6f, 0.25f, 0.25f, 1f)));
            } else {
                shapes.setColor((btnHovered && isPressed) ? new Color(0.15f, 0.45f, 0.15f, 1f) : (btnHovered ? new Color(0.25f, 0.65f, 0.25f, 1f) : new Color(0.2f, 0.55f, 0.2f, 1f)));
            }
            shapes.rect(btnX, btnY, btnW, btnH);

            itemIndex++;
        }

        if (hasBomb) {
            float rowY = startY - (itemIndex * (rowH + rowSpacing));
            float btnW = 80f;
            float btnH = 30f;
            float btnX = rowX + rowW - btnW - 10f;
            float btnY = rowY + (rowH - btnH) / 2f;

            boolean rowHovered = mouseX >= rowX && mouseX <= rowX + rowW && mouseY >= rowY && mouseY <= rowY + rowH;
            boolean btnHovered = mouseX >= btnX && mouseX <= btnX + btnW && mouseY >= btnY && mouseY <= btnY + btnH;

            if (btnHovered && justPressed) {
                isBombEquipped = !isBombEquipped;
                if (isBombEquipped) isMacheteEquipped = false;
                CampaignSquadState.isBombEquipped = isBombEquipped;
                CampaignSquadState.isMacheteEquipped = isMacheteEquipped;
            }

            shapes.setColor(rowHovered ? new Color(1f, 1f, 1f, 0.08f) : new Color(1f, 1f, 1f, 0.02f));
            shapes.rect(rowX, rowY, rowW, rowH);

            if (isBombEquipped) {
                shapes.setColor((btnHovered && isPressed) ? new Color(0.5f, 0.2f, 0.2f, 1f) : (btnHovered ? new Color(0.7f, 0.3f, 0.3f, 1f) : new Color(0.6f, 0.25f, 0.25f, 1f)));
            } else {
                shapes.setColor((btnHovered && isPressed) ? new Color(0.15f, 0.45f, 0.15f, 1f) : (btnHovered ? new Color(0.25f, 0.65f, 0.25f, 1f) : new Color(0.2f, 0.55f, 0.2f, 1f)));
            }
            shapes.rect(btnX, btnY, btnW, btnH);

            itemIndex++;
        }

        shapes.end();

        shapes.begin(ShapeRenderer.ShapeType.Line);
        shapes.setColor(new Color(1f, 1f, 1f, 0.15f));

        int lineIndex = 0;
        if (hasMachete) {
            float rY = startY - (lineIndex * (rowH + rowSpacing));
            shapes.line(rowX, rY, rowX + rowW, rY);
            shapes.setColor(Color.DARK_GRAY);
            shapes.rect(rowX + rowW - 90f, rY + 10f, 80f, 30f);
            shapes.setColor(new Color(1f, 1f, 1f, 0.15f));
            lineIndex++;
        }
        if (hasBomb) {
            float rY = startY - (lineIndex * (rowH + rowSpacing));
            shapes.line(rowX, rY, rowX + rowW, rY);
            shapes.setColor(Color.DARK_GRAY);
            shapes.rect(rowX + rowW - 90f, rY + 10f, 80f, 30f);
            shapes.setColor(new Color(1f, 1f, 1f, 0.15f));
            lineIndex++;
        }
        shapes.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);

        batch.begin();
        itemIndex = 0;

        if (hasMachete) {
            float rowY = startY - (itemIndex * (rowH + rowSpacing));
            float btnX = rowX + rowW - 90f;
            float btnY = rowY + (rowH - 30f) / 2f;

            float iconSize = 36f;
            float iconX = rowX + 15f;
            float iconY = rowY + (rowH - iconSize) / 2f;

            batch.draw(meleeInventoryTexture, iconX, iconY, iconSize, iconSize);

            font.setColor(Color.WHITE);
            font.draw(batch, "Machete", iconX + iconSize + 20f, rowY + (rowH / 2f) + 5f);

            String btnText = isMacheteEquipped ? "Unequip" : "Equip";
            float textOffset = isMacheteEquipped ? 12f : 20f;
            font.draw(batch, btnText, btnX + textOffset, btnY + 20f);

            itemIndex++;
        }

        if (hasBomb) {
            float rowY = startY - (itemIndex * (rowH + rowSpacing));
            float btnX = rowX + rowW - 90f;
            float btnY = rowY + (rowH - 30f) / 2f;

            float iconSize = 36f;
            float iconX = rowX + 15f;
            float iconY = rowY + (rowH - iconSize) / 2f;

            batch.draw(bombFrames[0][0], iconX, iconY, iconSize, iconSize);

            font.setColor(Color.WHITE);
            font.draw(batch, "Grenade", iconX + iconSize + 20f, rowY + (rowH / 2f) + 5f);

            String btnText = isBombEquipped ? "Unequip" : "Equip";
            float textOffset = isBombEquipped ? 12f : 20f;
            font.draw(batch, btnText, btnX + textOffset, btnY + 20f);

            itemIndex++;
        }

        batch.end();
    }

    private void drawHud(WorldSnapshot snapshot, float delta) {
        if (partnerBannerSecondsLeft > 0f) partnerBannerSecondsLeft -= delta;
        Matrix4 hudMatrix = new Matrix4().setToOrtho2D(0, 0, VIRTUAL_WIDTH, VIRTUAL_HEIGHT);
        float top = VIRTUAL_HEIGHT - 20f;

        // Damage Screen Vignette
        batch.setProjectionMatrix(hudMatrix);
        batch.begin();
        if (snapshot != null) {
            WorldSnapshot.PlayerState me = client.findLocalPlayer(snapshot);
            if (me != null && !me.downed && me.hp > 0f) {
                if (me.hp <= GameConstants.PLAYER_MAX_HP * 0.35f && damagedScreen2Texture != null) {
                    batch.draw(damagedScreen2Texture, 0, 0, VIRTUAL_WIDTH, VIRTUAL_HEIGHT);
                } else if (me.hp <= GameConstants.PLAYER_MAX_HP * 0.70f && damagedScreen1Texture != null) {
                    batch.draw(damagedScreen1Texture, 0, 0, VIRTUAL_WIDTH, VIRTUAL_HEIGHT);
                }
            }
        }
        batch.end();

        float tScale = 0.6f;
        float scaledTW = 0;
        float scaledTH = 0;
        float timerX = 20f;
        float timerY = top - 20f;
        float cx = 0, cy = 0;

        if (timerTexture != null && timerFrames != null) {
            scaledTW = timerFrameWidth * tScale;
            scaledTH = timerTexture.getHeight() * tScale;
            timerY = top - 10f - scaledTH;
            cx = timerX + scaledTW / 2f;
            cy = timerY + scaledTH / 2f;
        }

        float barY = scaledTH > 0 ? (timerY + (scaledTH / 2f) + 2f) : (top - 62f);

        WorldSnapshot.PlayerState p2State = null;
        if (isDualViewDebugMode && snapshot != null && snapshot.players != null && snapshot.players.size() > 1) {
            for (WorldSnapshot.PlayerState p : snapshot.players) {
                if (p != null && p.character == CharacterType.JANE) {
                    p2State = p;
                    break;
                }
            }
            if (p2State == null) p2State = snapshot.players.get(1);
        }

        float p2TimerX = VIRTUAL_WIDTH / 2f + 20f;
        float p2Cx = p2TimerX + scaledTW / 2f;
        float p2Cy = cy;
        float p2BarX = p2TimerX + scaledTW + (scaledTW > 0 ? 15f : 0f);
        float p2BarY = barY;

        // Render HP & Stamina Bars
        Gdx.gl.glEnable(GL20.GL_BLEND);
        shapes.setProjectionMatrix(hudMatrix);
        shapes.begin(ShapeRenderer.ShapeType.Filled);

        if (snapshot != null) {
            WorldSnapshot.PlayerState me = client.findLocalPlayer(snapshot);
            if (me != null) {
                float hpPercent = Math.max(0f, Math.min(me.hp, GameConstants.PLAYER_MAX_HP)) / GameConstants.PLAYER_MAX_HP;

                float barX = timerX + scaledTW + (scaledTW > 0 ? 15f : 0f);
                float barWidth = 150f, barHeight = 14f;

                shapes.setColor(0.2f, 0.2f, 0.2f, 0.8f);
                shapes.rect(barX, barY, barWidth, barHeight);
                shapes.setColor(0.15f, 0.8f, 0.3f, 1f);
                shapes.rect(barX, barY, barWidth * hpPercent, barHeight);
                shapes.setColor(0.1f, 0.1f, 0.1f, 1f);
                shapes.rect(barX + (barWidth / 3f), barY, 2f, barHeight);
                shapes.rect(barX + (barWidth * 2f / 3f), barY, 2f, barHeight);

                float sprintPercent = stamina / maxStamina;
                shapes.setColor(0.2f, 0.2f, 0.2f, 0.8f);
                shapes.rect(barX, barY - 18f, barWidth, barHeight);
                shapes.setColor(0.95f, 0.8f, 0.15f, 1f);
                shapes.rect(barX, barY - 18f, barWidth * sprintPercent, barHeight);
                shapes.setColor(0.1f, 0.1f, 0.1f, 1f);
                shapes.rect(barX + (barWidth / 3f), barY - 18f, 2f, barHeight);
                shapes.rect(barX + (barWidth * 2f / 3f), barY - 18f, 2f, barHeight);
            }

            if (p2State != null) {
                float p2HpPercent = Math.max(0f, Math.min(p2State.hp, GameConstants.PLAYER_MAX_HP)) / GameConstants.PLAYER_MAX_HP;
                float barWidth = 150f, barHeight = 14f;

                shapes.setColor(0.2f, 0.2f, 0.2f, 0.8f);
                shapes.rect(p2BarX, p2BarY, barWidth, barHeight);
                shapes.setColor(0.15f, 0.8f, 0.3f, 1f);
                shapes.rect(p2BarX, p2BarY, barWidth * p2HpPercent, barHeight);
                shapes.setColor(0.1f, 0.1f, 0.1f, 1f);
                shapes.rect(p2BarX + (barWidth / 3f), p2BarY, 2f, barHeight);
                shapes.rect(p2BarX + (barWidth * 2f / 3f), p2BarY, 2f, barHeight);

                float p2SprintPercent = p2Stamina / maxStamina;
                shapes.setColor(0.2f, 0.2f, 0.2f, 0.8f);
                shapes.rect(p2BarX, p2BarY - 18f, barWidth, barHeight);
                shapes.setColor(0.95f, 0.8f, 0.15f, 1f);
                shapes.rect(p2BarX, p2BarY - 18f, barWidth * p2SprintPercent, barHeight);
                shapes.setColor(0.1f, 0.1f, 0.1f, 1f);
                shapes.rect(p2BarX + (barWidth / 3f), p2BarY - 18f, 2f, barHeight);
                shapes.rect(p2BarX + (barWidth * 2f / 3f), p2BarY - 18f, 2f, barHeight);
            }
        }
        shapes.end();

        // Circular Immunity Ring
        if (timerTexture != null && timerFrames != null) {
            shapes.begin(ShapeRenderer.ShapeType.Line);
            float radius = Math.max(scaledTW, scaledTH) / 2f + 4f;
            float immRatio = 1.0f; // Infinite time
            float r = 0.2f;
            float g = 0.9f;
            float b = 0.4f;

            shapes.setColor(0.15f, 0.15f, 0.15f, 0.9f);
            for (int i = 0; i < 5; i++) {
                shapes.circle(cx, cy, radius - i, 60);
            }

            shapes.setColor(r, g, b, 1f);
            for (int i = 0; i < 5; i++) {
                shapes.arc(cx, cy, radius - i, 90f, -360f * immRatio, 60);
            }

            if (p2State != null) {
                shapes.setColor(0.15f, 0.15f, 0.15f, 0.9f);
                for (int i = 0; i < 5; i++) {
                    shapes.circle(p2Cx, p2Cy, radius - i, 60);
                }

                shapes.setColor(r, g, b, 1f);
                for (int i = 0; i < 5; i++) {
                    shapes.arc(p2Cx, p2Cy, radius - i, 90f, -360f * immRatio, 60);
                }
            }
            shapes.end();
        }
        Gdx.gl.glDisable(GL20.GL_BLEND);

        batch.begin();

        if (timerTexture != null && timerFrames != null) {
            batch.draw(timerFrames[0], timerX, timerY, scaledTW, scaledTH);
            if (p2State != null) {
                batch.draw(timerFrames[0], p2TimerX, timerY, scaledTW, scaledTH);
            }
        }

        font.setColor(Color.WHITE);
        font.draw(batch, "LEVEL " + levelNumber + "   |   " + client.getMatchMode() + "   |   " + (game.isHost() ? "HOST" : "CLIENT"), 20f, top);

        font.setColor(Color.GOLD);
        font.draw(batch, "COINS: " + coins, 20f, top - 18f);

        font.setColor(Color.CYAN);
        font.draw(batch, "TIME: INFINITE", 20f, top - 36f);

        if (p2State != null) {
            font.setColor(Color.CYAN);
            font.draw(batch, "TIME: INFINITE", p2TimerX + scaledTW + 15f, top - 36f);
        }

        // Draw Heal Ability HUD Slot
        if (healIconTexture != null) {
            float badgeX = timerX + scaledTW + 180f;
            float badgeY = timerY + scaledTH - 32f;
            float bSize = 48f; // heal_icon.png: +50% from previous 32px size

            batch.setColor(healCooldown > 0f ? new Color(0.6f, 0.6f, 0.6f, 0.7f) : Color.WHITE);
            batch.draw(healIconTexture, badgeX, (badgeY - 6f), bSize, bSize);
            batch.setColor(Color.WHITE);

            font.setColor(0.910f, 0.690f, 0.165f, 1f);
            font.draw(batch, "[H]", badgeX + 6f, badgeY + bSize + 14f);

            if (healCooldown > 0f) {
                font.setColor(Color.YELLOW);
                font.draw(batch, String.format("%.1fs", healCooldown), badgeX + bSize + 6f, badgeY + 20f);
            } else {
                font.setColor(new Color(0.2f, 1.0f, 0.4f, 1f));
                font.draw(batch, "READY", badgeX + bSize + 6f, badgeY + 20f);
            }
        }

        float objectiveX = 1005f;
        float objectiveY = isMinimapOpen ? 520f : top;

        if (levelNumber == 1 && showObjectivesOverlay) {
                font.setColor(0.910f, 0.690f, 0.165f, 1f);
                font.draw(batch, "HOSPITAL OBJECTIVES  [O: Hide]", objectiveX, objectiveY);
                objectiveY -= 20f;

                font.setColor(herbPartsCollected >= 2 ? Color.GREEN : Color.WHITE);
                font.draw(batch, (herbPartsCollected >= 2 ? "[DONE] " : "[ ] ") + "Find Herb Parts (" + herbPartsCollected + "/2)", objectiveX, objectiveY);
                objectiveY -= 18f;

                font.setColor(isJaneRevived ? Color.GREEN : Color.WHITE);
                font.draw(batch, (isJaneRevived ? "[DONE] " : "[ ] ") + "Revive Senseless Jane (+3 Coins)", objectiveX, objectiveY);
                objectiveY -= 18f;

                long livingRescued = levelVillagers.stream().filter(v -> !v.isDead && v.isRescued).count();
                font.setColor(livingRescued == 2 ? Color.GREEN : Color.WHITE);
                font.draw(batch, (livingRescued == 2 ? "[DONE] " : "[ ] ") + "Escort Ward Villagers (" + livingRescued + "/2)", objectiveX, objectiveY);
                objectiveY -= 18f;

                font.setColor(hasStairsKey ? Color.GREEN : Color.WHITE);
                font.draw(batch, (hasStairsKey ? "[DONE] " : "[ ] ") + "Retrieve Staff Room Key", objectiveX, objectiveY);
                objectiveY -= 18f;

                if (isAmbushActive) {
                    AmbushZombie boss = null;
                    long minionsAlive = 0;
                    for (AmbushZombie az : ambushZombies) {
                        if (az.dead) continue;
                        if (az.isBoss) boss = az;
                        else minionsAlive++;
                    }

                    if (boss != null) {
                        font.setColor(Color.CORAL);
                        font.draw(batch, "[!] Boss: " + (int) boss.hp + "/" + (int) boss.maxHp + " HP", objectiveX, objectiveY);
                    } else {
                        font.setColor(Color.GREEN);
                        font.draw(batch, "[DONE] Mutated Boss Slain", objectiveX, objectiveY);
                    }
                    objectiveY -= 18f;

                    if (minionsAlive > 0) {
                        font.setColor(Color.GOLD);
                        font.draw(batch, "[!] Horde: " + minionsAlive + " remaining", objectiveX, objectiveY);
                    } else {
                        font.setColor(Color.GREEN);
                        font.draw(batch, "[DONE] Ambush Horde Cleared", objectiveX, objectiveY);
                    }
                    objectiveY -= 18f;
                }

                boolean canEscape = hasStairsKey && isAmbushDefeated;
                font.setColor(canEscape ? Color.GREEN : Color.GRAY);
                font.draw(batch, (canEscape ? "[READY] " : "[LOCKED] ") + "Escape Upstairs (Floor 2)", objectiveX, objectiveY);
        } else if (levelNumber == 6) {
            font.setColor(0.910f, 0.690f, 0.165f, 1f);
            font.draw(batch, "FINAL MUTATION CONTAINMENT", objectiveX, objectiveY);
            objectiveY -= 22f;

            if (isAmbushActive) {
                AmbushZombie boss = null;
                long minionsAlive = 0;
                for (AmbushZombie az : ambushZombies) {
                    if (az.dead) continue;
                    if (az.isBoss) boss = az;
                    else minionsAlive++;
                }

                if (boss != null) {
                    font.setColor(Color.CORAL);
                    font.draw(batch, "[!] Final Mutation: " + (int) boss.hp + "/" + (int) boss.maxHp + " HP", objectiveX, objectiveY);
                } else {
                    font.setColor(Color.GREEN);
                    font.draw(batch, "[DONE] Final Mutation Destroyed", objectiveX, objectiveY);
                }
                objectiveY -= 20f;

                if (minionsAlive > 0) {
                    font.setColor(Color.FIREBRICK);
                    font.draw(batch, "[!] Swarm: " + minionsAlive + " remaining", objectiveX, objectiveY);
                } else {
                    font.setColor(Color.GREEN);
                    font.draw(batch, "[DONE] Swarm Cleared", objectiveX, objectiveY);
                }
                objectiveY -= 20f;
            }

            font.setColor(isAmbushDefeated ? Color.GREEN : Color.GRAY);
            font.draw(batch, (isAmbushDefeated ? "[VICTORY] " : "[OBJECTIVE] ") + "Eliminate Extinction Source", objectiveX, objectiveY);
        } else if (levelNumber == 3 && showObjectivesOverlay) {
            font.setColor(0.910f, 0.690f, 0.165f, 1f);
            font.draw(batch, "ROAD APPROACH (LEVEL 3)", objectiveX, objectiveY);
            objectiveY -= 22f;

            if (isAmbushActive) {
                AmbushZombie boss = null;
                long minionsAlive = 0;
                for (AmbushZombie az : ambushZombies) {
                    if (az.dead) continue;
                    if (az.isBoss) boss = az;
                    else minionsAlive++;
                }

                if (boss != null) {
                    font.setColor(Color.CORAL);
                    font.draw(batch, "[!] Sector Alpha: " + (int) boss.hp + "/" + (int) boss.maxHp + " HP", objectiveX, objectiveY);
                } else {
                    font.setColor(Color.GREEN);
                    font.draw(batch, "[DONE] Sector Alpha Neutralized", objectiveX, objectiveY);
                }
                objectiveY -= 20f;

                if (minionsAlive > 0) {
                    font.setColor(Color.FIREBRICK);
                    font.draw(batch, "[!] Hostiles: " + minionsAlive + " remaining", objectiveX, objectiveY);
                } else {
                    font.setColor(Color.GREEN);
                    font.draw(batch, "[DONE] Hostiles Cleared", objectiveX, objectiveY);
                }
                objectiveY -= 20f;
            }

            font.setColor(isAmbushDefeated ? Color.GREEN : Color.WHITE);
            font.draw(batch, (isAmbushDefeated ? "[DONE] " : "[ ] ") + "Clear all zombies", objectiveX, objectiveY);
            objectiveY -= 20f;

            font.setColor(hasAmbulanceKey ? Color.GREEN : Color.WHITE);
            font.draw(batch, (hasAmbulanceKey ? "[DONE] " : "[ ] ") + "Find the ambulance key", objectiveX, objectiveY);
            objectiveY -= 20f;

            boolean extractionReady = isAmbushDefeated && hasAmbulanceKey;
            font.setColor(extractionReady ? Color.LIME : Color.GRAY);
            font.draw(batch, (extractionReady ? "[READY] " : "[LOCKED] ") + "Return to Jane at the ambulance", objectiveX, objectiveY);
        } else if (levelNumber == 4 && showObjectivesOverlay) {
            font.setColor(0.910f, 0.690f, 0.165f, 1f);
            font.draw(batch, "SUBTERRANEAN PUZZLE (LEVEL 4)", objectiveX, objectiveY);
            objectiveY -= 22f;

            boolean puzzleSolved = level4PuzzlePartsCollected == LEVEL_4_PUZZLE_PART_COUNT;
            font.setColor(puzzleSolved ? Color.GREEN : Color.WHITE);
            font.draw(batch, (puzzleSolved ? "[DONE] " : "[ ] ") + "Collect puzzle parts ("
                    + level4PuzzlePartsCollected + "/3)", objectiveX, objectiveY);
            objectiveY -= 20f;

            font.setColor(puzzleSolved ? Color.LIME : Color.GRAY);
            font.draw(batch, (puzzleSolved ? "[READY] " : "[LOCKED] ")
                    + "Return to the endpoint", objectiveX, objectiveY);
        } else if (levelNumber == 5 && showObjectivesOverlay) {
            font.setColor(0.910f, 0.690f, 0.165f, 1f);
            font.draw(batch, "RESEARCH FACILITY (LEVEL 5)", objectiveX, objectiveY);
            objectiveY -= 22f;

            AmbushZombie redBrute = ambushZombies.stream().filter(z -> z.isBoss).findFirst().orElse(null);
            boolean bruteDefeated = redBrute == null || redBrute.dead;
            font.setColor(bruteDefeated ? Color.GREEN : Color.CORAL);
            String bruteStatus = bruteDefeated ? "[DONE] Defeat the big red zombie"
                    : "[!] Big red zombie: " + (int) redBrute.hp + "/" + (int) redBrute.maxHp + " HP";
            font.draw(batch, bruteStatus, objectiveX, objectiveY);
            objectiveY -= 20f;

            font.setColor(hasLabPasskey ? Color.GREEN : Color.WHITE);
            font.draw(batch, (hasLabPasskey ? "[DONE] " : "[ ] ") + "Acquire Lab Passkey Card", objectiveX, objectiveY);
            objectiveY -= 20f;

            font.setColor(hasLabPasskey ? Color.LIME : Color.GRAY);
            font.draw(batch, (hasLabPasskey ? "[READY] " : "[LOCKED] ") + "Return to the facility gate", objectiveX, objectiveY);
        } else if (showObjectivesOverlay && snapshot != null
                && snapshot.objectives != null && !snapshot.objectives.isEmpty()) {
            font.setColor(0.910f, 0.690f, 0.165f, 1f);
            font.draw(batch, "ROADSIDE OBJECTIVES", objectiveX, objectiveY);
            objectiveY -= 22f;
            for (WorldSnapshot.ObjectiveState objective : snapshot.objectives) {
                font.setColor(objective.complete ? Color.GREEN : Color.WHITE);
                String marker = objective.complete ? "[DONE] " : "[ ] ";
                font.draw(batch, marker + objectiveLabel(objective.objectiveId)
                                + "  " + objective.progress + "/" + objective.target,
                        objectiveX, objectiveY);
                objectiveY -= 20f;
            }
        }

        if (showCollisionOverlay && snapshot != null && tileMap != null) {
            WorldSnapshot.PlayerState here = client.findLocalPlayer(snapshot);
            if (here != null) {
                int cellX = tileMap.toCell(here.x);
                int cellYWorld = tileMap.toCell(here.y);
                int cellYFile = tileMap.getCollisionHeight() - 1 - cellYWorld;
                font.setColor(Color.YELLOW);
                font.draw(batch, String.format("F1 collision overlay   tile %.2f,%.2f   cell %d,%d   map-file row %d col %d", here.x, here.y, cellX, cellYWorld, cellYFile, cellX), 20f, top - 48f);
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

        boolean canPickUpMachete = false;
        if (snapshot != null && snapshot.items != null) {
            WorldSnapshot.PlayerState me = client.findLocalPlayer(snapshot);
            if (me != null && !me.downed && me.hp > 0) {
                for (WorldSnapshot.ItemState item : snapshot.items) {
                    float distX = me.x - item.x;
                    float distY = me.y - item.y;
                    float dist = (float) Math.sqrt(distX * distX + distY * distY);
                    if (dist <= 1.5f) {
                        canPickUpMachete = true;
                        break;
                    }
                }
            }
        }

        boolean canPickUpBomb = false;
        if (!hasBomb && mockBombX != -1f && snapshot != null) {
            WorldSnapshot.PlayerState me = client.findLocalPlayer(snapshot);
            if (me != null && !me.downed && me.hp > 0) {
                float distX = me.x - mockBombX;
                float distY = me.y - mockBombY;
                if (Math.sqrt(distX * distX + distY * distY) <= 1.5f) {
                    canPickUpBomb = true;
                }
            }
        }

        WorldSnapshot.PlayerState localPlayer = snapshot == null ? null : client.findLocalPlayer(snapshot);
        CampaignLevelPlan.Feature nearbyFeature = nearbyIncompleteFeature(localPlayer);
        boolean nearLevelExit = localPlayer != null && isNearLevelExit(localPlayer);
        boolean canAdvanceLevel = nearLevelExit && areLevelObjectivesComplete(snapshot)
                && (levelNumber != 1 || (hasStairsKey && isAmbushDefeated))
                && (levelNumber != 3 || (hasAmbulanceKey && isAmbushDefeated))
                && (levelNumber != 4 || level4PuzzlePartsCollected == LEVEL_4_PUZZLE_PART_COUNT)
                && (levelNumber != 5 || hasLabPasskey);

        boolean canPickUpKey = false;
        if (levelNumber == 1 && !hasStairsKey && localPlayer != null) {
            float distX = localPlayer.x - keyX;
            float distY = localPlayer.y - keyY;
            if (Math.hypot(distX, distY) <= 1.5f) {
                canPickUpKey = true;
            }
        }

        boolean canPickUpAmbulanceKey = levelNumber == 3 && !hasAmbulanceKey && localPlayer != null
                && Math.hypot(localPlayer.x - ambulanceKeyX, localPlayer.y - ambulanceKeyY) <= 1.5f;
        boolean canPickUpPuzzlePart = false;
        if (levelNumber == 4 && localPlayer != null) {
            for (Vector2 part : level4PuzzleParts) {
                if (Math.hypot(localPlayer.x - part.x, localPlayer.y - part.y) <= 1.5f) {
                    canPickUpPuzzlePart = true;
                    break;
                }
            }
        }

        LevelVillager nearVillager = null;
        if (localPlayer != null) {
            for (LevelVillager v : levelVillagers) {
                if (!v.isDead && Math.hypot(localPlayer.x - v.x, localPlayer.y - v.y) <= 1.8f) {
                    nearVillager = v;
                    break;
                }
            }
        }

        boolean canPickHerb1 = levelNumber == 1 && !herb1Collected && localPlayer != null && Math.hypot(localPlayer.x - HERB_1_X, localPlayer.y - HERB_1_Y) <= 1.5f;
        boolean canPickHerb2 = levelNumber == 1 && !herb2Collected && localPlayer != null && Math.hypot(localPlayer.x - HERB_2_X, localPlayer.y - HERB_2_Y) <= 1.5f;
        boolean canPickFloorMedkit = levelNumber == 1 && !hasPickedFloorMedkit && localPlayer != null && Math.hypot(localPlayer.x - MEDKIT_X, localPlayer.y - MEDKIT_Y) <= 1.5f;
        boolean canInteractJane = levelNumber == 1 && !isJaneRevived && localPlayer != null && Math.hypot(localPlayer.x - JANE_PHARMACY_X, localPlayer.y - JANE_PHARMACY_Y) <= 1.8f;

        float promptCenterX = isDualViewDebugMode ? (VIRTUAL_WIDTH * 0.25f) : (VIRTUAL_WIDTH * 0.5f);

        if (nearbyFeature != null) {
            font.setColor(Color.GOLD);
            font.draw(batch, "Press [E] — " + nearbyFeature.label(),
                    promptCenterX - 150f, VIRTUAL_HEIGHT / 2f - 50f);
        } else if (nearLevelExit && levelNumber == 1 && !hasStairsKey) {
            font.setColor(Color.CORAL);
            font.draw(batch, "[LOCKED] Staff Room Key Required! Find the key in Staff Room.",
                    promptCenterX - 190f, VIRTUAL_HEIGHT / 2f - 50f);
        } else if (nearLevelExit && levelNumber == 1 && !isAmbushDefeated) {
            long remaining = ambushZombies.stream().filter(z -> !z.dead).count();
            boolean bossAlive = ambushZombies.stream().anyMatch(z -> z.isBoss && !z.dead);
            font.setColor(Color.FIREBRICK);
            if (bossAlive) {
                font.draw(batch, "[LOCKED] Defeat Mutated Boss & Horde (" + remaining + " remaining)",
                        promptCenterX - 180f, VIRTUAL_HEIGHT / 2f - 50f);
            } else {
                font.draw(batch, "[LOCKED] Defeat Ambush Zombies (" + remaining + " remaining)",
                        promptCenterX - 160f, VIRTUAL_HEIGHT / 2f - 50f);
            }
        } else if (canPickUpAmbulanceKey) {
            font.setColor(Color.GOLD);
            font.draw(batch, "Press [E] — Pick up Ambulance Key",
                    promptCenterX - 130f, VIRTUAL_HEIGHT / 2f - 50f);
        } else if (nearLevelExit && levelNumber == 3 && !isAmbushDefeated) {
            long remaining = ambushZombies.stream().filter(z -> !z.dead).count();
            font.setColor(Color.FIREBRICK);
            font.draw(batch, "[LOCKED] Clear all zombies (" + remaining + " remaining)",
                    promptCenterX - 155f, VIRTUAL_HEIGHT / 2f - 50f);
        } else if (nearLevelExit && levelNumber == 3 && !hasAmbulanceKey) {
            font.setColor(Color.CORAL);
            font.draw(batch, "[LOCKED] Find the ambulance key somewhere on the map",
                    promptCenterX - 185f, VIRTUAL_HEIGHT / 2f - 50f);
        } else if (canPickUpPuzzlePart) {
            font.setColor(Color.GOLD);
            font.draw(batch, "Press [E] — Collect Puzzle Part ("
                            + level4PuzzlePartsCollected + "/3)",
                    promptCenterX - 150f, VIRTUAL_HEIGHT / 2f - 50f);
        } else if (nearLevelExit && levelNumber == 4
                && level4PuzzlePartsCollected < LEVEL_4_PUZZLE_PART_COUNT) {
            font.setColor(Color.CORAL);
            font.draw(batch, "[LOCKED] Find all puzzle parts ("
                            + level4PuzzlePartsCollected + "/3)",
                    promptCenterX - 150f, VIRTUAL_HEIGHT / 2f - 50f);
        } else if (nearLevelExit && levelNumber == 5 && !hasLabPasskey) {
            font.setColor(Color.CORAL);
            font.draw(batch, "[LOCKED] Defeat the big red zombie for the Lab Passkey Card",
                    promptCenterX - 205f, VIRTUAL_HEIGHT / 2f - 50f);
        } else if (canAdvanceLevel) {
            font.setColor(Color.GOLD);
            String nextName = levelNumber == 1 ? "Hospital Floor 2" : (levelNumber == 2 ? "Roadside Village" : "Level " + (levelNumber + 1));
            String proceedText = levelNumber == 3
                    ? "Press [E] — Unlock ambulance and proceed to LEVEL 4"
                    : (levelNumber == 4
                    ? "Press [E] — Activate solved puzzle and proceed to LEVEL 5"
                    : (levelNumber == 5
                    ? "Press [E] — Use Lab Passkey and proceed to LEVEL 6"
                    : "Press [E] to proceed to LEVEL " + (levelNumber + 1) + " (" + nextName + ")"));
            font.draw(batch, proceedText,
                    promptCenterX - 160f, VIRTUAL_HEIGHT / 2f - 50f);
        } else if (nearLevelExit) {
            font.setColor(Color.LIGHT_GRAY);
            font.draw(batch, "Complete the remaining objectives to unlock this exit",
                    promptCenterX - 160f, VIRTUAL_HEIGHT / 2f - 50f);
        } else if (canInteractJane) {
            font.setColor(Color.PINK);
            if (hasReviveKit) {
                font.draw(batch, "Press [E] — Revive Senseless Jane with Mixed Herb Kit (+3 Coins)", promptCenterX - 220f, VIRTUAL_HEIGHT / 2f - 50f);
            } else {
                font.draw(batch, "[E] Jane is unconscious! Find 2 Broken Herb Parts to craft Revive Kit", promptCenterX - 220f, VIRTUAL_HEIGHT / 2f - 50f);
            }
        } else if (canPickHerb1 || canPickHerb2) {
            font.setColor(Color.YELLOW);
            font.draw(batch, "Press [E] — Pick Up Herb Part (" + (herbPartsCollected + 1) + "/2)", promptCenterX - 140f, VIRTUAL_HEIGHT / 2f - 50f);
        } else if (canPickFloorMedkit) {
            font.setColor(Color.GREEN);
            font.draw(batch, "Press [E] — Pick Up Field Medkit", promptCenterX - 110f, VIRTUAL_HEIGHT / 2f - 50f);
        } else if (nearVillager != null && !nearVillager.isRescued) {
            font.setColor(Color.CYAN);
            font.draw(batch, "Press [E] — Talk & Escort " + nearVillager.name, promptCenterX - 130f, VIRTUAL_HEIGHT / 2f - 50f);
        } else if (nearVillager != null) {
            String actionWord = nearVillager.isStaying ? "FOLLOW squad" : "HOLD position";
            if (nearVillager.hp < nearVillager.maxHp) {
                boolean isJaneChar = (localPlayer != null && localPlayer.character == CharacterType.JANE);
                font.setColor(Color.LIME);
                font.draw(batch, "Press [H] — Heal " + nearVillager.name + " (" + (isJaneChar ? "+50" : "+35") + " HP)", promptCenterX - 140f, VIRTUAL_HEIGHT / 2f - 40f);
                font.setColor(Color.CYAN);
                font.draw(batch, "Press [X] — Order to " + actionWord, promptCenterX - 100f, VIRTUAL_HEIGHT / 2f - 62f);
            } else {
                font.setColor(Color.CYAN);
                font.draw(batch, "Press [X] — Order " + nearVillager.name + " to " + actionWord, promptCenterX - 150f, VIRTUAL_HEIGHT / 2f - 50f);
            }
        } else if (canPickUpKey) {
            font.setColor(Color.GOLD);
            font.draw(batch, "Press [E] to Pick Up Staff Room Key", promptCenterX - 115f, VIRTUAL_HEIGHT / 2f - 50f);
        } else if (canPickUpMachete && canPickUpBomb) {
            font.setColor(Color.YELLOW);
            font.draw(batch, "Press [E] to Pick Up Items", promptCenterX - 80f, VIRTUAL_HEIGHT / 2f - 50f);
        } else if (canPickUpMachete) {
            font.setColor(Color.YELLOW);
            font.draw(batch, "Press [E] to Pick Up Machete", promptCenterX - 85f, VIRTUAL_HEIGHT / 2f - 50f);
        } else if (canPickUpBomb) {
            font.setColor(Color.YELLOW);
            font.draw(batch, "Press [E] to Pick Up Grenade", promptCenterX - 85f, VIRTUAL_HEIGHT / 2f - 50f);
        }

        // Jane (P2) Prompts in Dual-View Mode
        if (isDualViewDebugMode && snapshot != null && snapshot.players != null && snapshot.players.size() > 1) {
            WorldSnapshot.PlayerState p2 = snapshot.players.get(1);
            if (p2 != null && !(levelNumber == 1 && !isJaneRevived)) {
                float promptCenterXP2 = VIRTUAL_WIDTH * 0.75f;
                LevelVillager nearVillagerP2 = null;
                for (LevelVillager v : levelVillagers) {
                    if (!v.isDead && Math.hypot(p2.x - v.x, p2.y - v.y) <= 1.8f) {
                        nearVillagerP2 = v;
                        break;
                    }
                }
                if (nearVillagerP2 != null && !nearVillagerP2.isRescued) {
                    font.setColor(Color.CYAN);
                    font.draw(batch, "Press [NUM 3] or [E] — Talk & Escort " + nearVillagerP2.name, promptCenterXP2 - 150f, VIRTUAL_HEIGHT / 2f - 50f);
                } else if (nearVillagerP2 != null) {
                    String actionWord = nearVillagerP2.isStaying ? "FOLLOW squad" : "HOLD position";
                    if (nearVillagerP2.hp < nearVillagerP2.maxHp) {
                        font.setColor(Color.LIME);
                        font.draw(batch, "Press [NUM 9] or [H] — Heal " + nearVillagerP2.name + " (+50 HP)", promptCenterXP2 - 140f, VIRTUAL_HEIGHT / 2f - 40f);
                        font.setColor(Color.CYAN);
                        font.draw(batch, "Press [NUM 7] or [X] — Order to " + actionWord, promptCenterXP2 - 120f, VIRTUAL_HEIGHT / 2f - 62f);
                    } else {
                        font.setColor(Color.CYAN);
                        font.draw(batch, "Press [NUM 7] or [X] — Order " + nearVillagerP2.name + " to " + actionWord, promptCenterXP2 - 150f, VIRTUAL_HEIGHT / 2f - 50f);
                    }
                }
            }
        }

        font.setColor(Color.GRAY);
        font.draw(batch, "[M] MAP", 20f, 30f);

        // Interactive On-Screen Collision Blocks Button
        float collBtnW = 210f;
        float collBtnH = 26f;
        float collBtnX = (VIRTUAL_WIDTH - collBtnW) / 2f;
        float collBtnY = VIRTUAL_HEIGHT - 32f;

        float mouseX = Gdx.input.getX() * (VIRTUAL_WIDTH / (float) Gdx.graphics.getWidth());
        float mouseY = (Gdx.graphics.getHeight() - Gdx.input.getY()) * (VIRTUAL_HEIGHT / (float) Gdx.graphics.getHeight());
        boolean isHovered = mouseX >= collBtnX && mouseX <= collBtnX + collBtnW && mouseY >= collBtnY && mouseY <= collBtnY + collBtnH;

        if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT) && isHovered) {
            showCollisionOverlay = !showCollisionOverlay;
            showBanner("Collision Overlay: " + (showCollisionOverlay ? "ON" : "OFF") + " [C / F1]");
        }

        batch.end();

        Gdx.gl.glEnable(GL20.GL_BLEND);
        shapes.setProjectionMatrix(hudMatrix);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        if (showCollisionOverlay) {
            shapes.setColor(isHovered ? new Color(0.2f, 0.7f, 0.3f, 0.95f) : new Color(0.12f, 0.45f, 0.18f, 0.85f));
        } else {
            shapes.setColor(isHovered ? new Color(0.35f, 0.35f, 0.4f, 0.9f) : new Color(0.15f, 0.15f, 0.2f, 0.8f));
        }
        shapes.rect(collBtnX, collBtnY, collBtnW, collBtnH);
        shapes.end();

        shapes.begin(ShapeRenderer.ShapeType.Line);
        shapes.setColor(showCollisionOverlay ? Color.LIME : (isHovered ? Color.GOLD : Color.GRAY));
        shapes.rect(collBtnX, collBtnY, collBtnW, collBtnH);
        shapes.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);

        batch.begin();
        font.setColor(showCollisionOverlay ? Color.WHITE : Color.LIGHT_GRAY);
        String collText = (showCollisionOverlay ? "[✓] COLLISION: ON" : "[ ] COLLISION: OFF") + " [Click/C]";
        font.draw(batch, collText, collBtnX + 12f, collBtnY + 18f);
        batch.end();

        drawMinimap(snapshot, hudMatrix, delta);
        if (showAxisDebug) {
            drawAxisDebugHud(snapshot, hudMatrix);
        }
    }

    private void drawMinimap(WorldSnapshot snapshot, Matrix4 hudMatrix, float delta) {
        if (!isMinimapOpen || tileMap == null) return;
        minimapStateTime += delta;

        float radarRadius = 66f;
        float radarCx = VIRTUAL_WIDTH - radarRadius - 20f;
        float radarCy = VIRTUAL_HEIGHT - radarRadius - 20f;
        float mmW = radarRadius * 2f;
        float mmH = radarRadius * 2f;
        float mmX = radarCx - radarRadius;
        float mmY = radarCy - radarRadius;

        float mapTilesW = tileMap.getWidth();
        float mapTilesH = tileMap.getHeight();
        if (mapTilesW <= 0 || mapTilesH <= 0) return;

        // 1. Circular radar background
        Gdx.gl.glEnable(GL20.GL_BLEND);
        shapes.setProjectionMatrix(hudMatrix);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0.025f, 0.05f, 0.08f, 0.94f);
        shapes.circle(radarCx, radarCy, radarRadius + 5f, 96);
        shapes.end();

        // Stencil the tactical map and every marker into a true circle.
        Gdx.gl.glClearStencil(0);
        Gdx.gl.glClear(GL20.GL_STENCIL_BUFFER_BIT);
        Gdx.gl.glEnable(GL20.GL_STENCIL_TEST);
        Gdx.gl.glStencilMask(0xFF);
        Gdx.gl.glStencilFunc(GL20.GL_ALWAYS, 1, 0xFF);
        Gdx.gl.glStencilOp(GL20.GL_REPLACE, GL20.GL_REPLACE, GL20.GL_REPLACE);
        Gdx.gl.glColorMask(false, false, false, false);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.circle(radarCx, radarCy, radarRadius, 96);
        shapes.end();
        Gdx.gl.glColorMask(true, true, true, true);
        Gdx.gl.glStencilMask(0x00);
        Gdx.gl.glStencilFunc(GL20.GL_EQUAL, 1, 0xFF);
        Gdx.gl.glStencilOp(GL20.GL_KEEP, GL20.GL_KEEP, GL20.GL_KEEP);

        // 2. Render tactical map view
        batch.setProjectionMatrix(hudMatrix);
        batch.begin();
        if (minimapRegion != null) {
            batch.setColor(0.70f, 0.82f, 0.95f, 0.85f); // tactical radar tint
            batch.draw(minimapRegion, mmX, mmY, mmW, mmH);
            batch.setColor(Color.WHITE);
        } else if (mapTexture != null) {
            batch.setColor(0.70f, 0.82f, 0.95f, 0.85f);
            batch.draw(mapTexture, mmX, mmY, mmW, mmH);
            batch.setColor(Color.WHITE);
        }

        batch.end();

        // 3. Radar grid
        shapes.setProjectionMatrix(hudMatrix);
        shapes.begin(ShapeRenderer.ShapeType.Line);
        shapes.setColor(0.20f, 0.50f, 0.80f, 0.25f);
        shapes.circle(radarCx, radarCy, radarRadius * 0.66f, 72);
        shapes.circle(radarCx, radarCy, radarRadius * 0.33f, 64);
        shapes.line(mmX, radarCy, mmX + mmW, radarCy);
        shapes.line(radarCx, mmY, radarCx, mmY + mmH);
        shapes.end();

        // 4. Entity blips
        shapes.begin(ShapeRenderer.ShapeType.Filled);

        // A. Exit Beacon (Stairs / Tunnel)
        LevelExit.forLevel(levelNumber).ifPresent(exit -> {
            float ex = mmX + (exit.tileX() / mapTilesW) * mmW;
            float ey = mmY + (exit.tileY() / mapTilesH) * mmH;
            boolean unlocked = switch (levelNumber) {
                case 1 -> hasStairsKey && isAmbushDefeated;
                case 3 -> hasAmbulanceKey && isAmbushDefeated;
                case 4 -> level4PuzzlePartsCollected == LEVEL_4_PUZZLE_PART_COUNT;
                case 5 -> hasLabPasskey;
                default -> true;
            };
            shapes.setColor(unlocked ? Color.LIME : Color.ORANGE);
            float pulse = 3.5f + (float) Math.sin(minimapStateTime * 5.0f) * 1.2f;
            shapes.circle(ex, ey, pulse, 10);
        });

        if (levelNumber == 1) {
            // Senseless Jane in Pharmacy (Pink Dot)
            if (!isJaneRevived) {
                float jx = mmX + (JANE_PHARMACY_X / mapTilesW) * mmW;
                float jy = mmY + (JANE_PHARMACY_Y / mapTilesH) * mmH;
                shapes.setColor(Color.PINK);
                float pulse = 3.6f + (float) Math.sin(minimapStateTime * 5.0f) * 1.2f;
                shapes.circle(jx, jy, pulse, 10);
            } else {
                float jx = mmX + (janeX / mapTilesW) * mmW;
                float jy = mmY + (janeY / mapTilesH) * mmH;
                shapes.setColor(Color.CYAN);
                shapes.circle(jx, jy, 3.2f, 8);
            }

            // Herb Parts (Yellow Dots)
            if (!herb1Collected) {
                float hx = mmX + (HERB_1_X / mapTilesW) * mmW;
                float hy = mmY + (HERB_1_Y / mapTilesH) * mmH;
                shapes.setColor(Color.YELLOW);
                shapes.circle(hx, hy, 3.0f, 8);
            }
            if (!herb2Collected) {
                float hx = mmX + (HERB_2_X / mapTilesW) * mmW;
                float hy = mmY + (HERB_2_Y / mapTilesH) * mmH;
                shapes.setColor(Color.YELLOW);
                shapes.circle(hx, hy, 3.0f, 8);
            }

            // Villagers (Blue Dots)
            for (LevelVillager v : levelVillagers) {
                if (v.isDead) continue;
                float vx = mmX + (v.x / mapTilesW) * mmW;
                float vy = mmY + (v.y / mapTilesH) * mmH;
                shapes.setColor(Color.ROYAL);
                shapes.circle(vx, vy, 3.2f, 8);
            }


            // Staff Room Key Blip (Cyan/Gold Dot at 24.5, 26.5)
            if (!hasStairsKey) {
                float kx = mmX + (keyX / mapTilesW) * mmW;
                float ky = mmY + (keyY / mapTilesH) * mmH;
                shapes.setColor(Color.CYAN);
                float pulse = 3.4f + (float) Math.sin(minimapStateTime * 6.0f) * 1.0f;
                shapes.circle(kx, ky, pulse, 8);
            }
        }

        // C. Middle Zombie
        if (!isZombieDead) {
            float zx = mmX + (middleZombieX / mapTilesW) * mmW;
            float zy = mmY + (middleZombieY / mapTilesH) * mmH;
            shapes.setColor(Color.RED);
            shapes.circle(zx, zy, 2.6f, 8);
        }

        // D. Ambush Zombies & Mutated Boss
        if (isAmbushActive) {
            for (AmbushZombie az : ambushZombies) {
                if (az.dead) continue;
                float ax = mmX + (az.x / mapTilesW) * mmW;
                float ay = mmY + (az.y / mapTilesH) * mmH;
                if (az.isBoss) {
                    shapes.setColor(Color.RED);
                    float pulse = 4.8f + (float) Math.sin(minimapStateTime * 6.0f) * 1.4f;
                    shapes.circle(ax, ay, pulse, 10);
                } else {
                    shapes.setColor(Color.GOLD);
                    shapes.circle(ax, ay, 2.8f, 8);
                }
            }
        }

        // E. Campaign Features (Level 2)
        if (levelNumber == 2 && levelFeatures != null) {
            for (CampaignLevelPlan.Feature feat : levelFeatures) {
                if (completedFeatureIds.contains(feat.actionId())) continue;
                float fx = mmX + (feat.tileX() / mapTilesW) * mmW;
                float fy = mmY + (feat.tileY() / mapTilesH) * mmH;
                shapes.setColor(Color.YELLOW);
                shapes.circle(fx, fy, 2.8f, 8);
            }
        }

        // F. Players
        if (snapshot != null && snapshot.players != null) {
            WorldSnapshot.PlayerState me = client != null ? client.findLocalPlayer(snapshot) : null;
            for (WorldSnapshot.PlayerState player : snapshot.players) {
                if (player.downed || player.hp <= 0f) continue;
                if (levelNumber == 1 && !isJaneRevived && player.character == CharacterType.JANE) continue;
                float px = mmX + (player.x / mapTilesW) * mmW;
                float py = mmY + (player.y / mapTilesH) * mmH;
                boolean isLocal = me != null && player.playerId == me.playerId;

                shapes.setColor(isLocal ? Color.LIME : Color.CYAN);
                shapes.circle(px, py, 3.5f, 10);

                shapes.setColor(Color.WHITE);
                shapes.circle(px, py, 1.4f, 6);
            }
        }

        shapes.end();

        Gdx.gl.glStencilMask(0xFF);
        Gdx.gl.glDisable(GL20.GL_STENCIL_TEST);

        // 5. Fixed frame, sweep marker, and compact M-key label.
        shapes.begin(ShapeRenderer.ShapeType.Line);
        shapes.setColor(0.12f, 0.72f, 0.92f, 0.95f);
        shapes.circle(radarCx, radarCy, radarRadius + 1f, 96);
        shapes.setColor(0.91f, 0.69f, 0.16f, 0.78f);
        shapes.circle(radarCx, radarCy, radarRadius + 5f, 96);
        float sweepAngle = minimapStateTime * 0.65f;
        shapes.setColor(0.20f, 0.90f, 0.82f, 0.55f);
        shapes.line(radarCx, radarCy,
                radarCx + (float) Math.cos(sweepAngle) * (radarRadius - 5f),
                radarCy + (float) Math.sin(sweepAngle) * (radarRadius - 5f));
        shapes.end();

        batch.setProjectionMatrix(hudMatrix);
        batch.begin();
        font.setColor(0.910f, 0.690f, 0.165f, 1f);
        String header = levelNumber == 1 ? "WARD" : (levelNumber == 2 ? "FLOOR 2" : "TACTICAL MAP");
        font.draw(batch, header, radarCx - radarRadius, mmY - 7f, mmW, Align.center, false);
        font.setColor(Color.LIGHT_GRAY);
        font.draw(batch, "[M]", radarCx - 11f, mmY - 25f);
        batch.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    private void checkZombiePatrolObjective() {
        if (levelNumber == 2 && isZombieDead && !zombieObjectiveSent) {
            levelFeatures.stream()
                    .filter(feature -> feature.type() == CampaignLevelPlan.FeatureType.ZOMBIE_ENCOUNTER)
                    .findFirst()
                    .ifPresent(this::completeFeature);
            zombieObjectiveSent = true;
        }
    }

    private void drawAxisDebugHud(WorldSnapshot snapshot, Matrix4 hudMatrix) {
        if (!showAxisDebug) return;

        WorldSnapshot.PlayerState elric = null;
        WorldSnapshot.PlayerState jane = null;

        if (snapshot != null && snapshot.players != null) {
            for (WorldSnapshot.PlayerState p : snapshot.players) {
                if (p.character == CharacterType.JANE) {
                    jane = p;
                } else {
                    if (elric == null) elric = p;
                }
            }
            if (elric == null && !snapshot.players.isEmpty()) {
                elric = snapshot.players.get(0);
            }
            if (jane == null && snapshot.players.size() > 1) {
                jane = snapshot.players.get(1);
            }
        }

        WorldSnapshot.PlayerState localPlayer = snapshot == null ? null : client.findLocalPlayer(snapshot);
        float elricX = elric != null ? elric.x : (localPlayer != null ? localPlayer.x : -1f);
        float elricY = elric != null ? elric.y : (localPlayer != null ? localPlayer.y : -1f);
        float elricHp = elric != null ? elric.hp : (localPlayer != null ? localPlayer.hp : 0f);

        float janePosX = -1f;
        float janePosY = -1f;
        float janeHpVal = jane != null ? jane.hp : janeHp;
        if (jane != null) {
            janePosX = jane.x;
            janePosY = jane.y;
        } else if (levelNumber == 1 && !isJaneRevived) {
            janePosX = JANE_PHARMACY_X;
            janePosY = JANE_PHARMACY_Y;
        } else if (janeX != -1f && janeY != -1f) {
            janePosX = janeX;
            janePosY = janeY;
        }

        float boxW = 500f;
        float boxH = 92f;
        float boxX = (VIRTUAL_WIDTH - boxW) / 2f;
        float boxY = VIRTUAL_HEIGHT - boxH - 52f;

        Gdx.gl.glEnable(GL20.GL_BLEND);
        shapes.setProjectionMatrix(hudMatrix);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0.05f, 0.08f, 0.12f, 0.88f);
        shapes.rect(boxX, boxY, boxW, boxH);
        shapes.end();

        shapes.begin(ShapeRenderer.ShapeType.Line);
        shapes.setColor(0.2f, 0.8f, 1.0f, 0.8f);
        shapes.rect(boxX, boxY, boxW, boxH);
        shapes.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);

        batch.setProjectionMatrix(hudMatrix);
        batch.begin();
        font.setColor(Color.YELLOW);
        font.draw(batch, "[J] COORDINATES OVERLAY", boxX + 16f, boxY + boxH - 12f);

        if (elricX != -1f) {
            font.setColor(Color.CYAN);
            font.draw(batch, String.format("ELRIC:  X = %6.2f, Y = %6.2f   Tile: (%2d, %2d)   HP: %.0f",
                    elricX, elricY, (int) elricX, (int) elricY, elricHp), boxX + 16f, boxY + boxH - 32f);
        } else {
            font.setColor(Color.GRAY);
            font.draw(batch, "ELRIC:  Not spawned", boxX + 16f, boxY + boxH - 32f);
        }

        if (janePosX != -1f) {
            font.setColor(new Color(1f, 0.4f, 0.9f, 1f));
            font.draw(batch, String.format("JANE:   X = %6.2f, Y = %6.2f   Tile: (%2d, %2d)   HP: %.0f",
                    janePosX, janePosY, (int) janePosX, (int) janePosY, janeHpVal), boxX + 16f, boxY + boxH - 52f);
        } else {
            font.setColor(Color.GRAY);
            font.draw(batch, "JANE:   Not spawned", boxX + 16f, boxY + boxH - 52f);
        }

        if (elricX != -1f && janePosX != -1f) {
            float dx = janePosX - elricX;
            float dy = janePosY - elricY;
            float dist = (float) Math.hypot(dx, dy);
            font.setColor(Color.WHITE);
            font.draw(batch, String.format("DELTA:  dX = %+.2f, dY = %+.2f   Distance: %.2f tiles", dx, dy, dist),
                    boxX + 16f, boxY + boxH - 72f);
        }
        batch.end();
    }

    private static String objectiveLabel(String objectiveId) {
        if (objectiveId == null) return "Unknown objective";
        return switch (objectiveId) {
            case "l2_infected" -> "Clear zombie patrol";
            case "l2_rescue" -> "Rescue villagers";
            case "l2_puzzle" -> "Restore relay puzzle";
            default -> objectiveId;
        };
    }

    private boolean isNearLevelExit(WorldSnapshot.PlayerState player) {
        return player != null
                && LevelExit.forLevel(levelNumber)
                .map(exit -> exit.contains(player.x, player.y))
                .orElse(false);
    }

    private boolean areLevelObjectivesComplete(WorldSnapshot snapshot) {
        if (snapshot == null || snapshot.objectives == null) {
            return false;
        }
        for (WorldSnapshot.ObjectiveState objective : snapshot.objectives) {
            if (!objective.complete) {
                return false;
            }
        }
        return true;
    }

    private String currentObjectiveSummary(WorldSnapshot snapshot) {
        if (levelNumber == 1) {
            if (herbPartsCollected < 2) {
                return "Find Herb Parts (" + herbPartsCollected + "/2)";
            }
            if (!isJaneRevived) {
                return "Use the mixed herb kit to revive Jane in the Pharmacy";
            }
            long rescued = levelVillagers.stream().filter(v -> !v.isDead && v.isRescued).count();
            if (rescued < 2) {
                return "Escort and protect the ward survivors (" + rescued + "/2)";
            }
            if (!hasStairsKey) {
                return "Retrieve the Staff Room Key";
            }
            if (!isAmbushDefeated) {
                return "Neutralize the corridor swarm and mutated boss";
            }
            return "Escape upstairs to Hospital Floor 2";
        }

        if (levelNumber == 3) {
            if (!isAmbushDefeated) {
                long remaining = ambushZombies.stream().filter(z -> !z.dead).count();
                return "Clear all roadside hostiles (" + remaining + " remaining)";
            }
            if (!hasAmbulanceKey) {
                return "Find the ambulance key";
            }
            return "Return to Jane at the ambulance";
        }

        if (levelNumber == 4) {
            if (level4PuzzlePartsCollected < LEVEL_4_PUZZLE_PART_COUNT) {
                return "Collect restoration puzzle parts (" + level4PuzzlePartsCollected + "/3)";
            }
            return "Return to the subterranean endpoint";
        }

        if (levelNumber == 5) {
            if (!hasLabPasskey) {
                AmbushZombie redBrute = ambushZombies.stream().filter(z -> z.isBoss).findFirst().orElse(null);
                if (redBrute != null && !redBrute.dead) {
                    return "Defeat the big red zombie (" + (int) redBrute.hp + "/" + (int) redBrute.maxHp + " HP)";
                }
                return "Collect the Lab Passkey Card";
            }
            return "Return to the research facility gate";
        }

        if (snapshot != null && snapshot.objectives != null) {
            for (WorldSnapshot.ObjectiveState objective : snapshot.objectives) {
                if (!objective.complete) {
                    return objectiveLabel(objective.objectiveId) + " ("
                            + objective.progress + "/" + objective.target + ")";
                }
            }
            if (!snapshot.objectives.isEmpty()) {
                return "All objectives complete - proceed to the level exit";
            }
        }

        return "Complete the current mission objective";
    }

    private void drawPauseOverlay(WorldSnapshot snapshot) {
        Matrix4 hudMatrix = new Matrix4().setToOrtho2D(0, 0, VIRTUAL_WIDTH, VIRTUAL_HEIGHT);
        Gdx.gl.glEnable(GL20.GL_BLEND);

        shapes.setProjectionMatrix(hudMatrix);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0.015f, 0.025f, 0.045f, 0.78f);
        shapes.rect(0, 0, VIRTUAL_WIDTH, VIRTUAL_HEIGHT);

        float panelW = 680f, panelH = 480f;
        float panelX = (VIRTUAL_WIDTH - panelW) / 2f, panelY = (VIRTUAL_HEIGHT - panelH) / 2f;

        shapes.setColor(0.025f, 0.045f, 0.075f, 0.98f);
        shapes.rect(panelX, panelY, panelW, panelH);
        shapes.setColor(0.91f, 0.69f, 0.16f, 1f);
        shapes.rect(panelX, panelY + panelH - 4f, panelW, 4f);
        shapes.end();

        shapes.begin(ShapeRenderer.ShapeType.Line);
        shapes.setColor(0.20f, 0.55f, 0.78f, 0.95f);
        shapes.rect(panelX, panelY, panelW, panelH);
        shapes.end();

        Vector2 mouseCoords = screenToHudCoordinates(Gdx.input.getX(), Gdx.input.getY());
        boolean mouseJustPressed = Gdx.input.isButtonJustPressed(Input.Buttons.LEFT);

        if (pauseObjectivesOpen) {
            drawPauseObjectivesContent(snapshot, hudMatrix, panelX, panelY, panelW, panelH,
                    mouseCoords, mouseJustPressed);
            return;
        }
        if (pauseControlsOpen) {
            drawPauseControlsContent(hudMatrix, panelX, panelY, panelW, panelH,
                    mouseCoords, mouseJustPressed);
            return;
        }

        float btnW = 560f, btnH = 48f, btnX = (VIRTUAL_WIDTH - btnW) / 2f;

        float btn1Y = panelY + 205f;
        boolean btn1Hovered = mouseCoords.x >= btnX && mouseCoords.x <= btnX + btnW && mouseCoords.y >= btn1Y && mouseCoords.y <= btn1Y + btnH;
        if (btn1Hovered && mouseJustPressed) {
            paused = false;
            pauseObjectivesOpen = false;
            pauseControlsOpen = false;
            client.sendEvent(GameConstants.EVENT_RESUME, "");
        }

        float btnObjectivesY = panelY + 145f;
        boolean btnObjectivesHovered = mouseCoords.x >= btnX && mouseCoords.x <= btnX + btnW
                && mouseCoords.y >= btnObjectivesY && mouseCoords.y <= btnObjectivesY + btnH;
        if ((btnObjectivesHovered && mouseJustPressed) || Gdx.input.isKeyJustPressed(Input.Keys.O)) {
            pauseObjectivesOpen = true;
            Gdx.gl.glDisable(GL20.GL_BLEND);
            return;
        }

        float btnControlsY = panelY + 85f;
        boolean btnControlsHovered = mouseCoords.x >= btnX && mouseCoords.x <= btnX + btnW
                && mouseCoords.y >= btnControlsY && mouseCoords.y <= btnControlsY + btnH;
        if ((btnControlsHovered && mouseJustPressed) || Gdx.input.isKeyJustPressed(Input.Keys.K)) {
            pauseControlsOpen = true;
            Gdx.gl.glDisable(GL20.GL_BLEND);
            return;
        }

        float btn2Y = panelY + 25f;
        boolean btn2Hovered = mouseCoords.x >= btnX && mouseCoords.x <= btnX + btnW && mouseCoords.y >= btn2Y && mouseCoords.y <= btn2Y + btnH;
        if (!returningToLauncher
                && ((btn2Hovered && mouseJustPressed)
                || Gdx.input.isKeyJustPressed(Input.Keys.Q))) {
            returningToLauncher = true;
            if (bridge.hasLauncher()) {
                bridge.requestReturnToLauncher(() -> Gdx.app.postRunnable(Gdx.app::exit));
            } else {
                game.setScreen(new MainMenuScreen(game, client, bridge));
            }
            return;
        }

        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(btn1Hovered ? new Color(0.20f, 0.34f, 0.52f, 1f) : new Color(0.075f, 0.12f, 0.19f, 1f));
        shapes.rect(btnX, btn1Y, btnW, btnH);

        shapes.setColor(btnObjectivesHovered ? new Color(0.18f, 0.36f, 0.31f, 1f) : new Color(0.065f, 0.15f, 0.13f, 1f));
        shapes.rect(btnX, btnObjectivesY, btnW, btnH);

        shapes.setColor(btnControlsHovered ? new Color(0.30f, 0.25f, 0.10f, 1f) : new Color(0.14f, 0.12f, 0.055f, 1f));
        shapes.rect(btnX, btnControlsY, btnW, btnH);

        shapes.setColor(btn2Hovered ? new Color(0.46f, 0.12f, 0.14f, 1f) : new Color(0.24f, 0.065f, 0.075f, 1f));
        shapes.rect(btnX, btn2Y, btnW, btnH);

        shapes.setColor(0.91f, 0.69f, 0.16f, 1f);
        if (btn1Hovered) shapes.rect(btnX, btn1Y, 5f, btnH);
        if (btnObjectivesHovered) shapes.rect(btnX, btnObjectivesY, 5f, btnH);
        if (btnControlsHovered) shapes.rect(btnX, btnControlsY, 5f, btnH);
        if (btn2Hovered) shapes.rect(btnX, btn2Y, 5f, btnH);
        shapes.end();

        shapes.begin(ShapeRenderer.ShapeType.Line);
        shapes.setColor(btn1Hovered ? new Color(0.91f, 0.69f, 0.16f, 1f) : new Color(0.4f, 0.5f, 0.65f, 1f));
        shapes.rect(btnX, btn1Y, btnW, btnH);

        shapes.setColor(btnObjectivesHovered ? new Color(0.20f, 0.83f, 0.60f, 1f) : new Color(0.3f, 0.6f, 0.45f, 1f));
        shapes.rect(btnX, btnObjectivesY, btnW, btnH);

        shapes.setColor(btnControlsHovered ? new Color(0.91f, 0.69f, 0.16f, 1f) : new Color(0.52f, 0.43f, 0.18f, 1f));
        shapes.rect(btnX, btnControlsY, btnW, btnH);

        shapes.setColor(btn2Hovered ? new Color(1f, 0.4f, 0.4f, 1f) : new Color(0.65f, 0.25f, 0.25f, 1f));
        shapes.rect(btnX, btn2Y, btnW, btnH);
        shapes.end();

        Gdx.gl.glDisable(GL20.GL_BLEND);

        batch.setProjectionMatrix(hudMatrix);
        batch.begin();
        pauseFont.setColor(new Color(0.91f, 0.69f, 0.16f, 1f));
        pauseFont.draw(batch, returningToLauncher ? "RETURNING TO MAIN MENU..." : "GAME PAUSED",
                panelX + 40f, panelY + panelH - 30f, panelW - 80f, Align.center, false);

        pauseFont.setColor(new Color(0.22f, 0.78f, 1f, 1f));
        pauseFont.draw(batch, "CURRENT OBJECTIVE", panelX + 40f, panelY + panelH - 72f,
                panelW - 80f, Align.center, false);

        pauseFont.setColor(Color.WHITE);
        pauseFont.draw(batch, currentObjectiveSummary(snapshot), panelX + 40f, panelY + panelH - 106f,
                panelW - 80f, Align.center, true);

        pauseFont.setColor(new Color(0.68f, 0.72f, 0.78f, 1f));
        pauseFont.draw(batch, "Select an option or use its displayed shortcut",
                panelX + 40f, panelY + panelH - 158f, panelW - 80f, Align.center, false);

        pauseFont.setColor(btn1Hovered ? Color.WHITE : Color.LIGHT_GRAY);
        pauseFont.draw(batch, "RESUME GAME   [ESC]", btnX, btn1Y + 32f, btnW, Align.center, false);

        pauseFont.setColor(btnObjectivesHovered ? Color.WHITE : new Color(0.82f, 0.94f, 0.87f, 1f));
        pauseFont.draw(batch, "OBJECTIVES   [O]", btnX, btnObjectivesY + 32f, btnW, Align.center, false);

        pauseFont.setColor(btnControlsHovered ? Color.WHITE : new Color(0.95f, 0.84f, 0.52f, 1f));
        pauseFont.draw(batch, "CONTROLS   [K]", btnX, btnControlsY + 32f, btnW, Align.center, false);

        pauseFont.setColor(btn2Hovered ? Color.WHITE : new Color(0.96f, 0.58f, 0.60f, 1f));
        pauseFont.draw(batch, "EXIT TO MAIN MENU   [Q]", btnX, btn2Y + 32f, btnW, Align.center, false);
        batch.end();
    }

    private Vector2 screenToHudCoordinates(float screenX, float screenY) {
        float viewportWidth = Math.max(1f, viewport.getScreenWidth());
        float viewportHeight = Math.max(1f, viewport.getScreenHeight());
        float hudX = (screenX - viewport.getScreenX()) * VIRTUAL_WIDTH / viewportWidth;
        float screenYFromBottom = Gdx.graphics.getHeight() - screenY;
        float hudY = (screenYFromBottom - viewport.getScreenY()) * VIRTUAL_HEIGHT / viewportHeight;
        return new Vector2(hudX, hudY);
    }

    private void drawPauseObjectivesContent(WorldSnapshot snapshot, Matrix4 hudMatrix,
                                            float panelX, float panelY, float panelW, float panelH,
                                            Vector2 mouseCoords, boolean mouseJustPressed) {
        float backW = 560f;
        float backH = 48f;
        float backX = (VIRTUAL_WIDTH - backW) / 2f;
        float backY = panelY + 24f;
        boolean backHovered = mouseCoords.x >= backX && mouseCoords.x <= backX + backW
                && mouseCoords.y >= backY && mouseCoords.y <= backY + backH;

        if ((backHovered && mouseJustPressed) || Gdx.input.isKeyJustPressed(Input.Keys.O)) {
            pauseObjectivesOpen = false;
            Gdx.gl.glDisable(GL20.GL_BLEND);
            return;
        }

        shapes.setProjectionMatrix(hudMatrix);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(backHovered ? new Color(0.24f, 0.34f, 0.50f, 1f) : new Color(0.10f, 0.15f, 0.24f, 1f));
        shapes.rect(backX, backY, backW, backH);
        if (backHovered) {
            shapes.setColor(new Color(0.96f, 0.67f, 0.10f, 1f));
            shapes.rect(backX, backY, 5f, backH);
        }
        shapes.end();

        shapes.begin(ShapeRenderer.ShapeType.Line);
        shapes.setColor(backHovered ? new Color(0.91f, 0.69f, 0.16f, 1f) : new Color(0.4f, 0.5f, 0.65f, 1f));
        shapes.rect(backX, backY, backW, backH);
        shapes.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);

        List<String> objectives = pauseObjectiveLines(snapshot);
        batch.setProjectionMatrix(hudMatrix);
        batch.begin();
        pauseFont.setColor(new Color(0.96f, 0.67f, 0.10f, 1f));
        pauseFont.draw(batch, missionObjectivesTitle(), panelX + 40f, panelY + panelH - 28f,
                panelW - 80f, Align.center, false);

        float lineY = panelY + panelH - 78f;
        for (String objective : objectives) {
            pauseFont.setColor(pauseObjectiveColor(objective));
            pauseFont.draw(batch, objective, panelX + 58f, lineY, panelW - 116f, Align.left, true);
            lineY -= 36f;
        }

        pauseFont.setColor(backHovered ? Color.WHITE : Color.LIGHT_GRAY);
        pauseFont.draw(batch, "BACK TO PAUSE MENU   [ESC]", backX, backY + 32f, backW, Align.center, false);
        batch.end();
    }

    private void drawPauseControlsContent(Matrix4 hudMatrix,
                                          float panelX, float panelY, float panelW, float panelH,
                                          Vector2 mouseCoords, boolean mouseJustPressed) {
        float backW = 560f;
        float backH = 48f;
        float backX = (VIRTUAL_WIDTH - backW) / 2f;
        float backY = panelY + 24f;
        boolean backHovered = mouseCoords.x >= backX && mouseCoords.x <= backX + backW
                && mouseCoords.y >= backY && mouseCoords.y <= backY + backH;

        if ((backHovered && mouseJustPressed) || Gdx.input.isKeyJustPressed(Input.Keys.K)) {
            pauseControlsOpen = false;
            Gdx.gl.glDisable(GL20.GL_BLEND);
            return;
        }

        String[][] leftControls = {
                {"WASD", "MOVE"},
                {"SHIFT", "SPRINT / EVADE"},
                {"SPACE / CLICK", "MELEE ATTACK"},
                {"E", "INTERACT / LOOT"},
                {"H", "HEAL"},
                {"X", "SURVIVOR STAY / FOLLOW"},
                {"M", "MINIMAP"},
                {"O", "OBJECTIVES"}
        };
        String[][] rightControls = {
                {"I", "INVENTORY"},
                {"ESC", "PAUSE / BACK"},
                {"CTRL + S", "SAVE SLOTS"},
                {"F11", "FULLSCREEN"},
                {"F12", "SCREENSHOT"},
                {"J", "COORDINATES"},
                {"C / F1", "COLLISION OVERLAY"},
                {"F3", "DEBUG SPLIT-SCREEN"}
        };

        float leftX = panelX + 34f;
        float rightX = panelX + panelW / 2f + 6f;
        float columnW = panelW / 2f - 40f;
        float rowTop = panelY + panelH - 94f;
        float rowH = 34f;

        shapes.setProjectionMatrix(hudMatrix);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        for (int i = 0; i < leftControls.length; i++) {
            float rowY = rowTop - i * rowH - 24f;
            shapes.setColor(i % 2 == 0
                    ? new Color(0.08f, 0.14f, 0.23f, 0.92f)
                    : new Color(0.11f, 0.18f, 0.28f, 0.92f));
            shapes.rect(leftX, rowY, columnW, 28f);
            shapes.rect(rightX, rowY, columnW, 28f);
        }
        shapes.setColor(backHovered ? new Color(0.24f, 0.34f, 0.50f, 1f) : new Color(0.10f, 0.15f, 0.24f, 1f));
        shapes.rect(backX, backY, backW, backH);
        if (backHovered) {
            shapes.setColor(new Color(0.96f, 0.67f, 0.10f, 1f));
            shapes.rect(backX, backY, 5f, backH);
        }
        shapes.end();

        shapes.begin(ShapeRenderer.ShapeType.Line);
        shapes.setColor(new Color(0.18f, 0.55f, 0.76f, 0.85f));
        for (int i = 0; i < leftControls.length; i++) {
            float rowY = rowTop - i * rowH - 24f;
            shapes.rect(leftX, rowY, columnW, 28f);
            shapes.rect(rightX, rowY, columnW, 28f);
        }
        shapes.setColor(backHovered ? new Color(0.96f, 0.67f, 0.10f, 1f) : new Color(0.40f, 0.55f, 0.72f, 1f));
        shapes.rect(backX, backY, backW, backH);
        shapes.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);

        batch.setProjectionMatrix(hudMatrix);
        batch.begin();
        pauseFont.setColor(new Color(0.96f, 0.67f, 0.10f, 1f));
        pauseFont.draw(batch, "CONTROL REFERENCE", panelX + 40f, panelY + panelH - 28f,
                panelW - 80f, Align.center, false);
        font.setColor(new Color(0.62f, 0.72f, 0.82f, 1f));
        font.draw(batch, "READ ONLY  -  KEY REBINDING COMING LATER", panelX + 40f,
                panelY + panelH - 55f, panelW - 80f, Align.center, false);

        for (int i = 0; i < leftControls.length; i++) {
            float textY = rowTop - i * rowH - 4f;
            drawPauseControlRow(leftControls[i], leftX, textY, columnW);
            drawPauseControlRow(rightControls[i], rightX, textY, columnW);
        }

        pauseFont.setColor(backHovered ? Color.WHITE : Color.LIGHT_GRAY);
        pauseFont.draw(batch, "BACK TO PAUSE MENU   [ESC]", backX, backY + 32f, backW, Align.center, false);
        batch.end();
    }

    private void drawPauseControlRow(String[] control, float x, float y, float width) {
        font.setColor(new Color(0.98f, 0.72f, 0.20f, 1f));
        font.draw(batch, control[0], x + 10f, y, 92f, Align.left, false);
        font.setColor(new Color(0.90f, 0.94f, 0.98f, 1f));
        font.draw(batch, control[1], x + 102f, y, width - 112f, Align.left, false);
    }

    private String missionObjectivesTitle() {
        return switch (levelNumber) {
            case 1 -> "HOSPITAL OBJECTIVES";
            case 3 -> "ROAD APPROACH OBJECTIVES";
            case 4 -> "SUBTERRANEAN OBJECTIVES";
            case 5 -> "RESEARCH FACILITY OBJECTIVES";
            default -> "MISSION OBJECTIVES";
        };
    }

    private List<String> pauseObjectiveLines(WorldSnapshot snapshot) {
        List<String> lines = new ArrayList<>();

        if (levelNumber == 1) {
            long rescued = levelVillagers.stream().filter(v -> !v.isDead && v.isRescued).count();
            lines.add((herbPartsCollected >= 2 ? "[DONE] " : "[ ] ")
                    + "Find Herb Parts (" + herbPartsCollected + "/2)");
            lines.add((isJaneRevived ? "[DONE] " : "[ ] ") + "Revive Senseless Jane");
            lines.add((rescued == 2 ? "[DONE] " : "[ ] ")
                    + "Escort Ward Villagers (" + rescued + "/2)");
            lines.add((hasStairsKey ? "[DONE] " : "[ ] ") + "Retrieve Staff Room Key");
            lines.add((isAmbushDefeated ? "[DONE] " : "[ ] ")
                    + "Neutralize the mutated boss and corridor swarm");
            boolean canEscape = hasStairsKey && isAmbushDefeated;
            lines.add((canEscape ? "[READY] " : "[LOCKED] ") + "Escape Upstairs (Floor 2)");
            return lines;
        }

        if (levelNumber == 3) {
            lines.add((isAmbushDefeated ? "[DONE] " : "[ ] ") + "Clear all roadside hostiles");
            lines.add((hasAmbulanceKey ? "[DONE] " : "[ ] ") + "Find the ambulance key");
            boolean extractionReady = isAmbushDefeated && hasAmbulanceKey;
            lines.add((extractionReady ? "[READY] " : "[LOCKED] ") + "Return to Jane at the ambulance");
            return lines;
        }

        if (levelNumber == 4) {
            boolean puzzleSolved = level4PuzzlePartsCollected == LEVEL_4_PUZZLE_PART_COUNT;
            lines.add((puzzleSolved ? "[DONE] " : "[ ] ") + "Collect puzzle parts ("
                    + level4PuzzlePartsCollected + "/" + LEVEL_4_PUZZLE_PART_COUNT + ")");
            lines.add((puzzleSolved ? "[READY] " : "[LOCKED] ") + "Return to the endpoint");
            return lines;
        }

        if (levelNumber == 5) {
            AmbushZombie redBrute = ambushZombies.stream().filter(z -> z.isBoss).findFirst().orElse(null);
            boolean bruteDefeated = redBrute == null || redBrute.dead;
            lines.add((bruteDefeated ? "[DONE] " : "[ ] ") + "Defeat the big red zombie");
            lines.add((hasLabPasskey ? "[DONE] " : "[ ] ") + "Acquire Lab Passkey Card");
            lines.add((hasLabPasskey ? "[READY] " : "[LOCKED] ") + "Return to the facility gate");
            return lines;
        }

        if (snapshot != null && snapshot.objectives != null) {
            for (WorldSnapshot.ObjectiveState objective : snapshot.objectives) {
                lines.add((objective.complete ? "[DONE] " : "[ ] ")
                        + objectiveLabel(objective.objectiveId) + " ("
                        + objective.progress + "/" + objective.target + ")");
            }
        }
        if (lines.isEmpty()) lines.add("[ ] Complete the current mission objective");
        return lines;
    }

    private Color pauseObjectiveColor(String objective) {
        if (objective.startsWith("[DONE]") || objective.startsWith("[READY]")) return Color.GREEN;
        if (objective.startsWith("[LOCKED]")) return Color.GRAY;
        if (objective.startsWith("[!]")) return Color.CORAL;
        return Color.WHITE;
    }

    private InputCommand readLocalInput(WorldSnapshot.PlayerState me, float delta) {
        InputCommand input = new InputCommand();
        float proposedMoveX = (Gdx.input.isKeyPressed(Input.Keys.D) ? 1 : 0) - (Gdx.input.isKeyPressed(Input.Keys.A) ? 1 : 0);
        float proposedMoveY = (Gdx.input.isKeyPressed(Input.Keys.W) ? 1 : 0) - (Gdx.input.isKeyPressed(Input.Keys.S) ? 1 : 0);

        if (me != null) {
            PlayerAnimState anim = animStates.get(me.playerId);
            if (anim != null && anim.isAttacking) {
                proposedMoveX = 0f;
                proposedMoveY = 0f;
            } else {
                float playerSpeed = 4.0f * delta;
                float playerRadius = 0.25f;
                boolean isCurrentlyStuck = !isWalkable(me.x, me.y, playerRadius);
                if (!isCurrentlyStuck) {
                    if (!isWalkable(me.x + (proposedMoveX * playerSpeed), me.y, playerRadius)) proposedMoveX = 0;
                    if (!isWalkable(me.x, me.y + (proposedMoveY * playerSpeed), playerRadius)) proposedMoveY = 0;
                }
            }
        }

        input.moveX = proposedMoveX;
        input.moveY = proposedMoveY;

        boolean leftClickAttack = Gdx.input.isButtonJustPressed(Input.Buttons.LEFT) && !isInventoryOpen;
        boolean numpadAttack = !isDualViewDebugMode && (Gdx.input.isKeyJustPressed(Input.Keys.NUM_0) || Gdx.input.isKeyJustPressed(Input.Keys.NUMPAD_0) || Gdx.input.isKeyJustPressed(Input.Keys.NUM_1) || Gdx.input.isKeyJustPressed(Input.Keys.NUMPAD_1));
        input.attackPressed = Gdx.input.isKeyJustPressed(Input.Keys.SPACE) || leftClickAttack || numpadAttack;

        input.interactHeld = Gdx.input.isKeyPressed(Input.Keys.E);
        input.interactPressed = Gdx.input.isKeyJustPressed(Input.Keys.E);

        boolean movingCommand = proposedMoveX != 0f || proposedMoveY != 0f;
        input.abilityPressed = Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT) && stamina > 0f && movingCommand;

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

    @Override
    public void pause() { }

    @Override
    public void resume() { }

    @Override
    public void hide() {
        stopLevelMusic();
        client.setOnEvent(null);
        client.setOnLevelTransition(null);
    }

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

        float cardW = 340f, cardH = 145f, gapX = 20f, gapY = 16f;
        float startX = (VIRTUAL_WIDTH - (3 * cardW + 2 * gapX)) / 2f, startY = VIRTUAL_HEIGHT - 120f;
        Vector3 m = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
        viewport.unproject(m);
        int clickedSlot = -1;
        boolean justPressed = Gdx.input.isButtonJustPressed(Input.Buttons.LEFT);

        for (int i = 0; i < GameConstants.SAVE_SLOT_COUNT; i++) {
            float x = startX + (i % 3) * (cardW + gapX), y = startY - (i / 3 + 1) * cardH - (i / 3) * gapY;
            boolean hovered = m.x >= x && m.x <= x + cardW && m.y >= y && m.y <= y + cardH;
            if (hovered && justPressed) clickedSlot = i + 1;
            SaveSlotDto slot = (overlaySlots != null && i < overlaySlots.size()) ? overlaySlots.get(i) : SaveSlotDto.empty(i + 1);

            shapes.setColor(hovered ? new Color(0.18f, 0.24f, 0.35f, 1f) : (slot != null && slot.occupied() ? new Color(0.1f, 0.14f, 0.22f, 1f) : new Color(0.07f, 0.1f, 0.15f, 1f)));
            shapes.rect(x, y, cardW, cardH);
            shapes.end();

            shapes.begin(ShapeRenderer.ShapeType.Line);
            shapes.setColor(hovered ? new Color(0.91f, 0.69f, 0.16f, 1f) : (slot != null && slot.occupied() ? new Color(0.3f, 0.45f, 0.65f, 0.8f) : new Color(0.2f, 0.25f, 0.35f, 0.5f)));
            shapes.rect(x, y, cardW, cardH);
            shapes.end();
            shapes.begin(ShapeRenderer.ShapeType.Filled);
        }

        float btnW = 200f, btnH = 40f, btnX = (VIRTUAL_WIDTH - btnW) / 2f, btnY = 30f;
        boolean btnHovered = m.x >= btnX && m.x <= btnX + btnW && m.y >= btnY && m.y <= btnY + btnH;
        if (btnHovered && justPressed) isSaveOverlayOpen = false;

        shapes.setColor(btnHovered ? new Color(0.25f, 0.32f, 0.45f, 1f) : new Color(0.12f, 0.16f, 0.25f, 1f));
        shapes.rect(btnX, btnY, btnW, btnH);
        shapes.end();

        shapes.begin(ShapeRenderer.ShapeType.Line);
        shapes.setColor(btnHovered ? new Color(0.91f, 0.69f, 0.16f, 1f) : new Color(0.4f, 0.45f, 0.55f, 1f));
        shapes.rect(btnX, btnY, btnW, btnH);
        shapes.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);

        if (clickedSlot == -1) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_1) || Gdx.input.isKeyJustPressed(Input.Keys.NUMPAD_1)) clickedSlot = 1;
            else if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_2) || Gdx.input.isKeyJustPressed(Input.Keys.NUMPAD_2)) clickedSlot = 2;
            else if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_3) || Gdx.input.isKeyJustPressed(Input.Keys.NUMPAD_3)) clickedSlot = 3;
            else if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_4) || Gdx.input.isKeyJustPressed(Input.Keys.NUMPAD_4)) clickedSlot = 4;
            else if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_5) || Gdx.input.isKeyJustPressed(Input.Keys.NUMPAD_5)) clickedSlot = 5;
            else if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_6) || Gdx.input.isKeyJustPressed(Input.Keys.NUMPAD_6)) clickedSlot = 6;
            else if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_7) || Gdx.input.isKeyJustPressed(Input.Keys.NUMPAD_7)) clickedSlot = 7;
            else if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_8) || Gdx.input.isKeyJustPressed(Input.Keys.NUMPAD_8)) clickedSlot = 8;
            else if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_9) || Gdx.input.isKeyJustPressed(Input.Keys.NUMPAD_9)) clickedSlot = 9;
        }

        if (clickedSlot != -1) {
            if (game.isHost() && game.getServer() != null) {
                SaveSlotDto slotDto = game.getServer().captureSave(clickedSlot);
                if (slotDto != null) {
                    bridge.requestSave(clickedSlot, slotDto);
                    overlaySlots = bridge.getSaveSlots();
                    showBanner("Game Saved to Slot " + clickedSlot + "!");
                    isSaveOverlayOpen = false;
                }
            } else showBanner("Only Host can save");
        }

        batch.setProjectionMatrix(hudMatrix);
        batch.begin();

        font.setColor(new Color(0.91f, 0.69f, 0.16f, 1f));
        font.draw(batch, "SAVE GAME", startX, VIRTUAL_HEIGHT - 35f);

        font.setColor(Color.LIGHT_GRAY);
        font.draw(batch, "Select a slot to save progress. Click or press keys [1-9]. (Ctrl+S / ESC to close)", startX, VIRTUAL_HEIGHT - 65f);

        for (int i = 0; i < GameConstants.SAVE_SLOT_COUNT; i++) {
            float x = startX + (i % 3) * (cardW + gapX), y = startY - (i / 3 + 1) * cardH - (i / 3) * gapY;
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

    private void takeInGameScreenshot() {
        try {
            int w = Gdx.graphics.getBackBufferWidth();
            int h = Gdx.graphics.getBackBufferHeight();
            Pixmap pixmap = ScreenUtils.getFrameBufferPixmap(0, 0, w, h);
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            FileHandle dir = Gdx.files.local("screenshots");
            if (!dir.exists()) {
                dir.mkdirs();
            }
            FileHandle file = dir.child("screenshot_" + timestamp + ".png");
            PixmapIO.writePNG(file, pixmap);
            pixmap.dispose();
            showBanner("Screenshot saved: " + file.name());
        } catch (Throwable t) {
            showBanner("Screenshot failed: " + t.getMessage());
        }
    }

    public void setDualViewDebugMode(boolean enabled) {
        this.isDualViewDebugMode = enabled;
    }

    public void setShowCollisionOverlay(boolean show) {
        this.showCollisionOverlay = show;
    }

    @Override
    public void dispose() {
        stopLevelMusic();
        if (batch != null) batch.dispose();
        if (shapes != null) shapes.dispose();
        if (font != null) font.dispose();
        if (pauseFont != null) pauseFont.dispose();
        if (mapTexture != null) mapTexture.dispose();
        if (markerTexture != null) markerTexture.dispose();
        if (playerTexture != null) playerTexture.dispose();
        if (idleTexture != null) idleTexture.dispose();
        if (villagerV1WalkTexture != null) villagerV1WalkTexture.dispose();
        if (villagerV1IdleTexture != null) villagerV1IdleTexture.dispose();
        if (villagerV2WalkTexture != null) villagerV2WalkTexture.dispose();
        if (villagerV2IdleTexture != null) villagerV2IdleTexture.dispose();
        if (janeKnockedTexture != null) janeKnockedTexture.dispose();
        if (femalePlayerTexture != null) femalePlayerTexture.dispose();
        if (p2Texture != null) p2Texture.dispose();
        if (p2IdleTexture != null) p2IdleTexture.dispose();
        if (p2SprintTexture != null) p2SprintTexture.dispose();
        if (p2MeleeTexture != null) p2MeleeTexture.dispose();
        if (p2MeleeHitTexture != null) p2MeleeHitTexture.dispose();
        if (p2IdleMeleeTexture != null) p2IdleMeleeTexture.dispose();
        if (p2BombTexture != null) p2BombTexture.dispose();
        if (p2BombIdleTexture != null) p2BombIdleTexture.dispose();
        if (playerSprintTexture != null && playerSprintTexture != playerTexture) playerSprintTexture.dispose();
        if (zombieTexture != null) zombieTexture.dispose();
        if (zombieIdleTexture != null && zombieIdleTexture != zombieTexture) zombieIdleTexture.dispose();
        if (zombieBiteTexture != null && zombieBiteTexture != zombieTexture) zombieBiteTexture.dispose();
        if (bloodTexture != null) bloodTexture.dispose();
        if (inventoryTexture != null) inventoryTexture.dispose();
        if (bombTexture != null) bombTexture.dispose();
        if (bombEffectTexture != null) bombEffectTexture.dispose();
        if (damagedScreen1Texture != null) damagedScreen1Texture.dispose();
        if (damagedScreen2Texture != null) damagedScreen2Texture.dispose();
        if (timerTexture != null) timerTexture.dispose();
        if (playerBombTexture != null && playerBombTexture != playerTexture) playerBombTexture.dispose();
        if (playerBombIdleTexture != null && playerBombIdleTexture != idleTexture) playerBombIdleTexture.dispose();
        if (playerBombThrowTexture != null && playerBombThrowTexture != playerTexture) playerBombThrowTexture.dispose();
        if (meleeTexture != null) meleeTexture.dispose();
        if (playerMeleeTexture != null && playerMeleeTexture != playerTexture) playerMeleeTexture.dispose();
        if (idleMeleeTexture != null && idleMeleeTexture != idleTexture) idleMeleeTexture.dispose();
        if (meleeInventoryTexture != null) meleeInventoryTexture.dispose();
        if (meleeHitTexture != null) meleeHitTexture.dispose();
        if (keyTexture != null) keyTexture.dispose();
        if (healIconTexture != null) healIconTexture.dispose();
        if (healEffectTexture != null) healEffectTexture.dispose();
        if (herbTexture != null) herbTexture.dispose();
        if (medicTexture != null) medicTexture.dispose();
        if (macheteSound != null) macheteSound.dispose();
        if (swingSound != null) swingSound.dispose();
        if (bombSound != null) bombSound.dispose();
    }
}
