package com.infectedhour.core.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
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
import com.infectedhour.shared.constants.GameConstants;
import com.infectedhour.shared.dto.SaveSlotDto;
import com.infectedhour.shared.network.CharacterType;
import com.infectedhour.shared.network.InputCommand;
import com.infectedhour.shared.network.WorldSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

    // HUD Damage Screen Tracking
    private Texture damagedScreen1Texture;
    private Texture damagedScreen2Texture;

    // Immunity Timer Tracking
    private Texture timerTexture;
    private TextureRegion[] timerFrames;
    private int timerFrameWidth;
    private float maxImmunityTime = 120f;
    private float currentImmunityTime = 120f;

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

    private float middleZombieHp = 300f;
    private boolean isZombieDead = false;

    private final Map<String, PlayerAnimState> animStates = new HashMap<>();
    private final ZombieAnimState zombieAnim = new ZombieAnimState();

    private float stamina = 100f;
    private final float maxStamina = 100f;

    private float biteCooldown = 0.8f;
    private boolean isBeingBitten = false;

    // Level 1 Key & Ambush Mechanics
    private boolean hasStairsKey = false;
    private boolean isAmbushActive = false;
    private boolean isAmbushDefeated = false;
    private Texture keyTexture;
    private float keyX = 38.0f;
    private float keyY = 27.0f;

    private static class AmbushZombie {
        float x, y;
        float hp = 100f;
        float maxHp = 100f;
        boolean isBoss = false;
        boolean dead = false;
        float stateTime = 0f;
        boolean biting = false;
        float biteCooldown = 0.8f;

        AmbushZombie(float x, float y, boolean isBoss) {
            this.x = x;
            this.y = y;
            this.isBoss = isBoss;
            this.maxHp = isBoss ? 450f : 100f;
            this.hp = this.maxHp;
        }
    }
    private final List<AmbushZombie> ambushZombies = new ArrayList<>();

    // Minimap Radar
    private boolean isMinimapOpen = true;
    private float minimapStateTime = 0f;
    private TextureRegion minimapRegion;

    // Save Overlay & State
    private boolean isSaveOverlayOpen = false;
    private List<SaveSlotDto> overlaySlots = null;

    private boolean paused = false;
    private boolean returningToLauncher = false;
    private volatile boolean levelTransitionInProgress = false;
    private boolean isDualViewDebugMode = false;
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
    }

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
        levelFeatures = CampaignLevelPlan.featuresFor(levelNumber);

        if (levelNumber == 2) {
            levelFeatures.stream()
                    .filter(feature -> feature.type() == CampaignLevelPlan.FeatureType.ZOMBIE_ENCOUNTER)
                    .findFirst()
                    .ifPresent(feature -> {
                        middleZombieX = feature.tileX();
                        middleZombieY = feature.tileY();
                    });
        }

        this.collisionSystem = new CollisionSystem(tileMap);

        if (game.isHost()) {
            game.getServer().loadTileMap(tileMap);
            game.getServer().setMapWidthInTiles(levelLoader.getMapWidthInTiles());
            game.getServer().configureLevel(def);
        }

        if (levelNumber == 1) {
            mapTexture = new Texture(Gdx.files.internal("map.png"));
            mapTexture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        } else if (levelNumber == 2) {
            Texture customMap2 = loadTextureSafely("map2.png");
            if (customMap2 != null) {
                mapTexture = customMap2;
                mapTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            } else {
                mapTexture = createRoadsideVillageTexture();
            }
        } else {
            Texture customMap3 = loadTextureSafely("map3.png");
            if (customMap3 != null) {
                mapTexture = customMap3;
                mapTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            } else {
                Texture fallback = loadTextureSafely("map2.png");
                mapTexture = fallback != null ? fallback : createRoadsideVillageTexture();
            }
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
                default -> { }
            }
        });

        client.setOnLevelTransition(transition -> {
            if (transition.nextLevelNumber != levelNumber + 1 || levelTransitionInProgress) {
                return;
            }
            levelTransitionInProgress = true;
            Gdx.app.postRunnable(() -> {
                StoryPanelScreen.Sequence storySequence = levelNumber == 1
                        ? StoryPanelScreen.Sequence.AFTER_LEVEL_1
                        : StoryPanelScreen.Sequence.AFTER_LEVEL_2;
                game.setScreen(new StoryPanelScreen(game, client, bridge, storySequence, levelNumber));
            });
        });
    }

    private boolean isWalkable(float x, float y, float radius) {
        if (collisionSystem == null) return true;
        return !collisionSystem.overlapsBlockedTile(x, y, radius);
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

        if (bombCooldown > 0f) {
            bombCooldown -= delta;
        }

        if (!paused && currentImmunityTime > 0f) {
            currentImmunityTime = Math.max(0f, currentImmunityTime - delta);
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.H)) {
            currentImmunityTime = Math.min(maxImmunityTime, currentImmunityTime + 30f);
            showBanner("Immunity +30s");
        }

        if (snapshot != null) {
            me = client.findLocalPlayer(snapshot);
            if (me != null && (me.downed || me.hp <= 0f || currentImmunityTime <= 0f)) {
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
                    bridge.requestReturnToLauncher(() -> Gdx.app.postRunnable(Gdx.app::exit));
                }
                return;
            }

            if (me != null && "MELEE".equals(me.equippedWeapon) && !hasMachete) {
                hasMachete = true;
                if (!isBombEquipped) isMacheteEquipped = true;
            }
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            if (isSaveOverlayOpen) {
                isSaveOverlayOpen = false;
            } else if (isInventoryOpen) {
                isInventoryOpen = false;
            } else {
                paused = !paused;
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

        if (!paused && !isInventoryOpen && me != null) {
            handleLevelFeatureInteraction(me);
        }

        if (!paused && !isInventoryOpen && !levelTransitionInProgress
                && me != null && isNearLevelExit(me)
                && Gdx.input.isKeyJustPressed(Input.Keys.E)) {
            if (levelNumber == 1 && !hasStairsKey) {
                showBanner("The stairs are locked! Find the hospital key first!");
            } else if (levelNumber == 1 && !isAmbushDefeated) {
                long remaining = ambushZombies.stream().filter(z -> !z.dead).count();
                boolean bossAlive = ambushZombies.stream().anyMatch(z -> z.isBoss && !z.dead);
                if (bossAlive) {
                    showBanner("Defeat the Mutated Boss & horde (" + remaining + " remaining) first!");
                } else {
                    showBanner("Defeat the remaining " + remaining + " ambush zombies first!");
                }
            } else if (areLevelObjectivesComplete(snapshot)) {
                String nextName = levelNumber == 1 ? "Hospital Ground Floor" : (levelNumber == 2 ? "Roadside Village" : "Level " + (levelNumber + 1));
                showBanner("Proceeding to Level " + (levelNumber + 1) + " (" + nextName + ")…");
                client.sendEvent(GameConstants.EVENT_LEVEL_EXIT_REQUEST, String.valueOf(levelNumber));
            } else {
                showBanner("Complete remaining objectives before proceeding!");
            }
        }

        if (levelNumber == 1 && !hasStairsKey && me != null) {
            float distX = me.x - keyX;
            float distY = me.y - keyY;
            if (Math.sqrt(distX * distX + distY * distY) <= 1.5f) {
                if (Gdx.input.isKeyJustPressed(Input.Keys.E)) {
                    hasStairsKey = true;
                    isAmbushActive = true;
                    ambushZombies.clear();
                    // Boss zombie guarding the door corridor:
                    ambushZombies.add(new AmbushZombie(41.0f, 29.5f, true));
                    // 3 regular zombies swarming:
                    ambushZombies.add(new AmbushZombie(36.0f, 26.5f, false));
                    ambushZombies.add(new AmbushZombie(40.5f, 25.5f, false));
                    ambushZombies.add(new AmbushZombie(37.5f, 29.0f, false));
                    showBanner("DOOR BREACHED! MUTATED BOSS & ZOMBIE HORDE EMERGE!");
                }
            }
        }

        if (!hasBomb && mockBombX != -1f && me != null) {
            float distX = me.x - mockBombX;
            float distY = me.y - mockBombY;
            if (Math.sqrt(distX * distX + distY * distY) <= 1.5f) {
                if (Gdx.input.isKeyJustPressed(Input.Keys.E)) {
                    hasBomb = true;
                    showBanner("Picked up Grenade!");
                }
            }
        }

        if (!paused && !isInventoryOpen && me != null) {
            boolean attackCmd = Gdx.input.isKeyJustPressed(Input.Keys.SPACE) || Gdx.input.isButtonJustPressed(Input.Buttons.LEFT);
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

        if (levelNumber == 2 && isZombieDead && !zombieObjectiveSent) {
            levelFeatures.stream()
                    .filter(feature -> feature.type() == CampaignLevelPlan.FeatureType.ZOMBIE_ENCOUNTER)
                    .findFirst()
                    .ifPresent(this::completeFeature);
            zombieObjectiveSent = true;
        }

        game.stepSimulation(delta);

        Gdx.gl.glClearColor(0.055f, 0.078f, 0.125f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        if (!paused) client.sendInputIfDue(readLocalInput(me, delta), delta);

        boolean movementKeysPressed = Gdx.input.isKeyPressed(Input.Keys.W) || Gdx.input.isKeyPressed(Input.Keys.A) ||
                Gdx.input.isKeyPressed(Input.Keys.S) || Gdx.input.isKeyPressed(Input.Keys.D);
        boolean wantsToSprint = Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT) && movementKeysPressed;

        if (wantsToSprint && stamina > 0f) {
            stamina = Math.max(0f, stamina - (45f * delta));
        } else if (!wantsToSprint && stamina < maxStamina) {
            stamina = Math.min(maxStamina, stamina + (30f * delta));
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.F3)) {
            isDualViewDebugMode = !isDualViewDebugMode;
            showBanner(isDualViewDebugMode ? "Dual View Debug Mode: ON (Split Screen)" : "Dual View Debug Mode: OFF");
        }

        if (snapshot != null) {
            TextureRegion zombieFrame = updateMiddleZombie(me, delta);

            if (isDualViewDebugMode && snapshot.players != null && !snapshot.players.isEmpty()) {
                WorldSnapshot.PlayerState p1 = snapshot.players.get(0);
                WorldSnapshot.PlayerState p2 = snapshot.players.size() > 1 ? snapshot.players.get(1) : p1;

                int screenW = Gdx.graphics.getWidth();
                int screenH = Gdx.graphics.getHeight();
                int halfW = screenW / 2;

                Gdx.gl.glViewport(0, 0, halfW, screenH);
                camera.position.set(Math.round(p1.x * PIXELS_PER_TILE), Math.round(p1.y * PIXELS_PER_TILE), 0);
                camera.update();

                batch.setProjectionMatrix(camera.combined);
                batch.begin();
                drawLevelMap();
                drawWorld(snapshot, delta, zombieFrame, me);
                batch.end();

                Gdx.gl.glViewport(halfW, 0, halfW, screenH);
                camera.position.set(Math.round(p2.x * PIXELS_PER_TILE), Math.round(p2.y * PIXELS_PER_TILE), 0);
                camera.update();

                batch.setProjectionMatrix(camera.combined);
                batch.begin();
                drawLevelMap();
                drawWorld(snapshot, delta, zombieFrame, me);
                batch.end();

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
                font.draw(batch, "P1: " + (p1.character != null ? p1.character.name() : "ELRIC"), 20f, VIRTUAL_HEIGHT - 40f);
                font.draw(batch, "P2: " + (p2.character != null ? p2.character.name() : "JANE"), VIRTUAL_WIDTH / 2f + 20f, VIRTUAL_HEIGHT - 40f);
                batch.end();
            } else {
                if (me != null) {
                    camera.position.set(Math.round(me.x * PIXELS_PER_TILE), Math.round(me.y * PIXELS_PER_TILE), 0);
                    camera.update();
                }

                batch.setProjectionMatrix(camera.combined);
                batch.begin();
                drawLevelMap();
                drawWorld(snapshot, delta, zombieFrame, me);
                batch.end();
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
                        shapes.rect(px, py, barW * (player.hp / 100f), barH);
                    }
                }
            }

            if (!isZombieDead && me != null) {
                float barW = 24f;
                float barH = 3f;
                float zx = Math.round((middleZombieX * PIXELS_PER_TILE) - (barW / 2f) + 8f);
                float zy = Math.round((middleZombieY * PIXELS_PER_TILE) + (zombieFrameHeight / 2f) + 22f);

                shapes.setColor(Color.BLACK);
                shapes.rect(zx - 1f, zy - 1f, barW + 2f, barH + 2f);

                shapes.setColor(Color.YELLOW);
                shapes.rect(zx, zy, barW * (middleZombieHp / 300f), barH);
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

            if (showCollisionOverlay) drawCollisionOverlay();
        }

        drawHud(snapshot, delta);
        if (paused) drawPauseOverlay();
    }

    private TextureRegion updateMiddleZombie(WorldSnapshot.PlayerState me, float delta) {
        if (isZombieDead || me == null) {
            return null;
        }

        float aggroRadiusTiles = 5.0f;
        float loseRadiusTiles = 8.0f;
        float zombieSpeed = 2.0f;

        float midDistX = me.x - middleZombieX;
        float midDistY = me.y - middleZombieY;
        float midDistance = (float) Math.sqrt(midDistX * midDistX + midDistY * midDistY);

        boolean hasLOS = hasLineOfSight(middleZombieX, middleZombieY, me.x, me.y);

        if (midDistance <= 0.8f && !me.downed && me.hp > 0f && hasLOS) {
            isBeingBitten = true;
            biteCooldown -= delta;
            if (biteCooldown <= 0f) {
                client.sendEvent("ZOMBIE_BITE_DAMAGE", "33.4");
                biteCooldown = 0.8f;
            }
        } else {
            isBeingBitten = false;
            biteCooldown = 0.8f;
        }

        if (!middleZombieChasing && midDistance <= aggroRadiusTiles && hasLOS) {
            middleZombieChasing = true;
        } else if (middleZombieChasing && (midDistance >= loseRadiusTiles || !hasLOS)) {
            middleZombieChasing = false;
        }

        boolean zombieMoving = middleZombieChasing && midDistance > 0.5f;

        if (isBeingBitten) {
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
            float moveX = (midDistX / midDistance) * zombieSpeed * delta;
            float moveY = (midDistY / midDistance) * zombieSpeed * delta;

            float zombieRadius = 0.25f;
            float nextX = middleZombieX + moveX;
            float nextY = middleZombieY + moveY;

            if (isWalkable(nextX, middleZombieY, zombieRadius)) middleZombieX = nextX;
            if (isWalkable(middleZombieX, nextY, zombieRadius)) middleZombieY = nextY;

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

    private void checkAmbushCompletion() {
        if (!isAmbushActive || isAmbushDefeated) return;
        long remaining = ambushZombies.stream().filter(z -> !z.dead).count();
        if (remaining == 0) {
            isAmbushDefeated = true;
            showBanner("MUTATED BOSS & HORDE DEFEATED! The stairs to Ground Floor are unlocked!");
        }
    }

    private TextureRegion getAmbushZombieFrame(AmbushZombie az, WorldSnapshot.PlayerState me, float delta) {
        if (az.dead || me == null) return null;

        az.stateTime += delta;
        float distXP = me.x - az.x;
        float distYP = me.y - az.y;
        float distP = (float) Math.sqrt(distXP * distXP + distYP * distYP);

        boolean hasLOS = hasLineOfSight(az.x, az.y, me.x, me.y);

        float biteDist = az.isBoss ? 1.0f : 0.8f;
        if (distP <= biteDist && !me.downed && me.hp > 0f && hasLOS) {
            az.biting = true;
            az.biteCooldown -= delta;
            if (az.biteCooldown <= 0f) {
                float biteDmg = az.isBoss ? 35.0f : 20.0f;
                client.sendEvent("ZOMBIE_BITE_DAMAGE", String.valueOf(biteDmg));
                az.biteCooldown = az.isBoss ? 1.0f : 0.8f;
            }
        } else {
            az.biting = false;
            az.biteCooldown = az.isBoss ? 1.0f : 0.8f;
        }

        int dirRow;
        if (Math.abs(distXP) > Math.abs(distYP)) dirRow = distXP > 0 ? 2 : 1;
        else dirRow = distYP > 0 ? 3 : 0;

        if (az.biting && zombieBiteFrames != null) {
            float biteProgress = (az.isBoss ? 1.0f : 0.8f) - az.biteCooldown;
            if (biteProgress < 0f) biteProgress = 0f;
            int biteCol = (int) (biteProgress / 0.4f);
            if (biteCol >= 2) biteCol = 1;
            int safeRow = dirRow % zombieBiteFrames.length;
            int safeCol = biteCol % zombieBiteFrames[0].length;
            return zombieBiteFrames[safeRow][safeCol];
        }

        if (distP > 0.5f && distP <= 14.0f) {
            float speed = az.isBoss ? 1.4f : 1.7f;
            float moveX = (distXP / distP) * speed * delta;
            float moveY = (distYP / distP) * speed * delta;
            float nextX = az.x + moveX;
            float nextY = az.y + moveY;
            float radius = az.isBoss ? 0.35f : 0.25f;
            if (isWalkable(nextX, az.y, radius)) az.x = nextX;
            if (isWalkable(az.x, nextY, radius)) az.y = nextY;

            if (zombieFrames != null) {
                int walkCol = ((int) (az.stateTime / 0.15f)) % 8;
                int safeRow = dirRow % zombieFrames.length;
                int safeCol = walkCol % zombieFrames[0].length;
                return zombieFrames[safeRow][safeCol];
            }
        } else if (zombieIdleFrames != null) {
            int idleCol = ((int) (az.stateTime / 0.5f)) % 2;
            int safeRow = dirRow % zombieIdleFrames.length;
            int safeCol = idleCol % zombieIdleFrames[0].length;
            return zombieIdleFrames[safeRow][safeCol];
        }

        return zombieFrames != null ? zombieFrames[0][0] : null;
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
                                bloodPools.add(new Vector2(az.x, az.y));
                            }
                        }
                    }
                    checkAmbushCompletion();
                }

                if (me != null && !me.downed && me.hp > 0f) {
                    float distP = (float) Math.sqrt(Math.pow(me.x - exp.x, 2) + Math.pow(me.y - exp.y, 2));
                    float dmgP = 0f;
                    if (distP <= 0.5f) dmgP = 300f;
                    else if (distP <= 1.0f) dmgP = 200f;
                    else if (distP <= 1.5f) dmgP = 100f;

                    if (dmgP > 0f) {
                        client.sendEvent("ZOMBIE_BITE_DAMAGE", String.valueOf(dmgP));
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
            float currentZDrawWidth = isBeingBitten ? zombieBiteFrameWidth : zombieFrameWidth;
            float currentZDrawHeight = isBeingBitten ? zombieBiteFrameHeight : zombieFrameHeight;
            float midDrawX = Math.round((middleZombieX * PIXELS_PER_TILE) - (currentZDrawWidth / 2f));
            float midDrawY = Math.round((middleZombieY * PIXELS_PER_TILE) - SPRITE_FEET_INSET_PX);
            batch.draw(zombieFrame, midDrawX, midDrawY, currentZDrawWidth, currentZDrawHeight);
        }

        // 5b. Ambush Zombies & Boss
        if (isAmbushActive) {
            for (AmbushZombie az : ambushZombies) {
                if (az.dead) continue;
                TextureRegion azFrame = getAmbushZombieFrame(az, me, delta);
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

        // 6. Players
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

                boolean isLocalPlayer = me != null && player.playerId.equals(me.playerId);
                boolean showMacheteSprite = isLocalPlayer ? isMacheteEquipped : "MELEE".equals(player.equippedWeapon);
                boolean showBombSprite = isLocalPlayer && isBombEquipped;

                boolean localSprint = isLocalPlayer && Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT) && stamina > 0f && moving;
                boolean remoteSprint = !isLocalPlayer && (Math.abs(dx) > 0.08f || Math.abs(dy) > 0.08f);
                boolean isSprintingAnim = localSprint || remoteSprint;

                TextureRegion[][] activeRunFrames = playerFrames;
                TextureRegion[][] activeIdleFrames = idleFrames;

                int animFrames = 4;
                int idleAnimFrames = 2;
                int activeOffset = characterOffset;

                if (showMacheteSprite) {
                    activeIdleFrames = idleMeleeFrames;
                } else if (showBombSprite) {
                    activeIdleFrames = playerBombIdleFrames;
                    idleAnimFrames = (playerBombIdleFrames[0].length >= 8) ? 8 : 2;
                    activeOffset = 0;
                }

                if (isSprintingAnim) {
                    activeRunFrames = playerSprintFrames;
                    animFrames = (playerSprintFrames[0].length >= 8) ? 8 : 4;
                    activeOffset = 0;
                } else if (showMacheteSprite) {
                    activeRunFrames = playerMeleeFrames;
                } else if (showBombSprite) {
                    activeRunFrames = playerBombFrames;
                    animFrames = (playerBombFrames[0].length >= 8) ? 8 : 4;
                    activeOffset = 0;
                }

                anim.currentColumn = anim.currentColumn % animFrames;

                if (moving) {
                    if (Math.abs(dx) > 0.005f) anim.currentRow = dx > 0 ? 2 : 1;
                    else anim.currentRow = dy > 0 ? 3 : 0;

                    anim.stateTime += delta;
                    float runSpeed = isSprintingAnim ? 0.07f : 0.15f;
                    if (anim.stateTime > runSpeed) {
                        anim.currentColumn = (anim.currentColumn + 1) % animFrames;
                        anim.stateTime = 0f;
                    }

                    int safeRow = anim.currentRow % activeRunFrames.length;
                    int safeCol = (anim.currentColumn + activeOffset) % activeRunFrames[0].length;
                    currentFrame = activeRunFrames[safeRow][safeCol];
                } else {
                    anim.stateTime += delta;
                    float idleSpeed = showBombSprite ? 0.15f : 0.5f;
                    if (anim.stateTime > idleSpeed) {
                        anim.currentColumn = (anim.currentColumn + 1) % idleAnimFrames;
                        anim.stateTime = 0f;
                    }

                    int safeRow = anim.currentRow % activeIdleFrames.length;
                    int safeCol = (anim.currentColumn + activeOffset) % activeIdleFrames[0].length;
                    currentFrame = activeIdleFrames[safeRow][safeCol];
                }

                float currentDrawWidth = currentFrame.getRegionWidth();
                float currentDrawHeight = currentFrame.getRegionHeight();

                // ── UPDATED: Bomb sprites scaled down to 0.9x ──
                if (showBombSprite && !isSprintingAnim) {
                    currentDrawWidth *= 0.9f;
                    currentDrawHeight *= 0.9f;
                }

                if (anim.isAttacking && showMacheteSprite) {
                    anim.attackTime += delta;
                    float attackSpeed = 0.25f;
                    int attackFrame = (int) (anim.attackTime / attackSpeed);

                    if (attackFrame >= 1 && !anim.damageApplied && isLocalPlayer) {
                        anim.damageApplied = true;
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
                                    middleZombieHp -= 100f;
                                    if (middleZombieHp <= 0f) {
                                        isZombieDead = true;
                                        isBeingBitten = false;
                                        bloodPools.add(new Vector2(middleZombieX, middleZombieY));
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
                                    float bound = az.isBoss ? 1.5f : 1.5f;
                                    if (anim.currentRow == 3 && azDistY > 0 && Math.abs(azDistX) <= bound) azHit = true;
                                    else if (anim.currentRow == 0 && azDistY < 0 && Math.abs(azDistX) <= bound) azHit = true;
                                    else if (anim.currentRow == 2 && azDistX > 0 && Math.abs(azDistY) <= bound) azHit = true;
                                    else if (anim.currentRow == 1 && azDistX < 0 && Math.abs(azDistY) <= bound) azHit = true;

                                    if (azHit) {
                                        az.hp -= 100f;
                                        if (az.hp <= 0f) {
                                            az.dead = true;
                                            bloodPools.add(new Vector2(az.x, az.y));
                                        }
                                    }
                                }
                            }
                            checkAmbushCompletion();
                        }
                    }

                    if (attackFrame >= 2) {
                        anim.isAttacking = false;
                    } else {
                        int safeRow = anim.currentRow % meleeHitFrames.length;
                        int safeCol = attackFrame % meleeHitFrames[0].length;

                        currentFrame = meleeHitFrames[safeRow][safeCol];
                        currentDrawWidth = meleeHitFrameWidth * 0.8f;
                        currentDrawHeight = meleeHitFrameHeight * 0.8f;
                    }
                } else if (anim.isAttacking && showBombSprite) {
                    anim.attackTime += delta;
                    float attackSpeed = 0.125f;
                    int attackFrame = (int) (anim.attackTime / attackSpeed);

                    if (attackFrame >= 2) {
                        anim.isAttacking = false;
                    } else {
                        int safeRow = anim.currentRow % playerBombThrowFrames.length;
                        int safeCol = attackFrame % playerBombThrowFrames[0].length;

                        currentFrame = playerBombThrowFrames[safeRow][safeCol];

                        currentDrawWidth = currentFrame.getRegionWidth() * 0.6f;
                        currentDrawHeight = currentFrame.getRegionHeight() * 0.6f;
                    }
                }

                anim.lastX = player.x;
                anim.lastY = player.y;

                float drawX = Math.round((player.x * PIXELS_PER_TILE) - (currentDrawWidth / 2f));
                float drawY = Math.round((player.y * PIXELS_PER_TILE) - SPRITE_FEET_INSET_PX);
                batch.draw(currentFrame, drawX, drawY, currentDrawWidth, currentDrawHeight);
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
                if (me.hp <= 35f && damagedScreen2Texture != null) {
                    batch.draw(damagedScreen2Texture, 0, 0, VIRTUAL_WIDTH, VIRTUAL_HEIGHT);
                } else if (me.hp <= 70f && damagedScreen1Texture != null) {
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

        // Render HP & Stamina Bars
        Gdx.gl.glEnable(GL20.GL_BLEND);
        shapes.setProjectionMatrix(hudMatrix);
        shapes.begin(ShapeRenderer.ShapeType.Filled);

        if (snapshot != null) {
            WorldSnapshot.PlayerState me = client.findLocalPlayer(snapshot);
            if (me != null) {
                float hpPercent = Math.max(0f, Math.min(me.hp, 100f)) / 100f;

                float barX = timerX + scaledTW + (scaledTW > 0 ? 15f : 0f);
                float barY = timerY + scaledTH - 15f;
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
        }
        shapes.end();

        // Circular Immunity Ring
        if (timerTexture != null && timerFrames != null) {
            shapes.begin(ShapeRenderer.ShapeType.Line);
            float radius = Math.max(scaledTW, scaledTH) / 2f + 4f;
            float immRatio = currentImmunityTime / maxImmunityTime;
            float r = 0.7f + (1f - 0.7f) * immRatio;
            float g = 0.5f + (0.84f - 0.5f) * immRatio;
            float b = 0.2f + (0f - 0.2f) * immRatio;

            shapes.setColor(0.15f, 0.15f, 0.15f, 0.9f);
            for (int i = 0; i < 5; i++) {
                shapes.circle(cx, cy, radius - i, 60);
            }

            shapes.setColor(r, g, b, 1f);
            for (int i = 0; i < 5; i++) {
                shapes.arc(cx, cy, radius - i, 90f, -360f * immRatio, 60);
            }
            shapes.end();
        }
        Gdx.gl.glDisable(GL20.GL_BLEND);

        batch.begin();

        if (timerTexture != null && timerFrames != null) {
            int tFrame = 7 - (int) ((currentImmunityTime / maxImmunityTime) * 7f);
            tFrame = Math.max(0, Math.min(7, tFrame));
            batch.draw(timerFrames[tFrame], timerX, timerY, scaledTW, scaledTH);
        }

        font.setColor(Color.WHITE);
        font.draw(batch, "LEVEL " + levelNumber + "   |   " + client.getMatchMode() + "   |   " + (game.isHost() ? "HOST" : "CLIENT"), 20f, top);

        float objectiveX = 1010f;
        float objectiveY = isMinimapOpen ? 556f : top;

        if (levelNumber == 1) {
            font.setColor(0.910f, 0.690f, 0.165f, 1f);
            font.draw(batch, "HOSPITAL LEVEL 1", objectiveX, objectiveY);
            objectiveY -= 22f;

            font.setColor(hasStairsKey ? Color.GREEN : Color.WHITE);
            font.draw(batch, (hasStairsKey ? "[DONE] " : "[ ] ") + "Find Stairs Key", objectiveX, objectiveY);
            objectiveY -= 20f;

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
                    font.draw(batch, "[DONE] Mutated Boss Defeated", objectiveX, objectiveY);
                }
                objectiveY -= 20f;

                if (minionsAlive > 0) {
                    font.setColor(Color.FIREBRICK);
                    font.draw(batch, "[!] Horde: " + minionsAlive + " left", objectiveX, objectiveY);
                } else {
                    font.setColor(Color.GREEN);
                    font.draw(batch, "[DONE] Horde Eliminated", objectiveX, objectiveY);
                }
                objectiveY -= 20f;
            }

            boolean canEscape = hasStairsKey && isAmbushDefeated;
            font.setColor(canEscape ? Color.GREEN : Color.GRAY);
            font.draw(batch, (canEscape ? "[READY] " : "[LOCKED] ") + "Escape to Ground Floor", objectiveX, objectiveY);
        } else if (snapshot != null && snapshot.objectives != null && !snapshot.objectives.isEmpty()) {
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
                && (levelNumber != 1 || (hasStairsKey && isAmbushDefeated));

        boolean canPickUpKey = false;
        if (levelNumber == 1 && !hasStairsKey && localPlayer != null) {
            float distX = localPlayer.x - keyX;
            float distY = localPlayer.y - keyY;
            if (Math.sqrt(distX * distX + distY * distY) <= 1.5f) {
                canPickUpKey = true;
            }
        }

        if (nearbyFeature != null) {
            font.setColor(Color.GOLD);
            font.draw(batch, "Press [E] — " + nearbyFeature.label(),
                    VIRTUAL_WIDTH / 2f - 150f, VIRTUAL_HEIGHT / 2f - 50f);
        } else if (nearLevelExit && levelNumber == 1 && !hasStairsKey) {
            font.setColor(Color.CORAL);
            font.draw(batch, "[LOCKED] Stairs Key Required! Explore the hospital ward.",
                    VIRTUAL_WIDTH / 2f - 180f, VIRTUAL_HEIGHT / 2f - 50f);
        } else if (nearLevelExit && levelNumber == 1 && !isAmbushDefeated) {
            long remaining = ambushZombies.stream().filter(z -> !z.dead).count();
            boolean bossAlive = ambushZombies.stream().anyMatch(z -> z.isBoss && !z.dead);
            font.setColor(Color.FIREBRICK);
            if (bossAlive) {
                font.draw(batch, "[LOCKED] Defeat Mutated Boss & Horde (" + remaining + " remaining)",
                        VIRTUAL_WIDTH / 2f - 180f, VIRTUAL_HEIGHT / 2f - 50f);
            } else {
                font.draw(batch, "[LOCKED] Defeat Ambush Zombies (" + remaining + " remaining)",
                        VIRTUAL_WIDTH / 2f - 160f, VIRTUAL_HEIGHT / 2f - 50f);
            }
        } else if (canAdvanceLevel) {
            font.setColor(Color.GOLD);
            String nextName = levelNumber == 1 ? "Hospital Ground Floor" : (levelNumber == 2 ? "Roadside Village" : "Level " + (levelNumber + 1));
            font.draw(batch, "Press [E] to proceed to LEVEL " + (levelNumber + 1) + " (" + nextName + ")",
                    VIRTUAL_WIDTH / 2f - 160f, VIRTUAL_HEIGHT / 2f - 50f);
        } else if (nearLevelExit) {
            font.setColor(Color.LIGHT_GRAY);
            font.draw(batch, "Complete the remaining objectives to unlock this exit",
                    VIRTUAL_WIDTH / 2f - 160f, VIRTUAL_HEIGHT / 2f - 50f);
        } else if (canPickUpKey) {
            font.setColor(Color.GOLD);
            font.draw(batch, "Press [E] to Pick Up Stairs Key", VIRTUAL_WIDTH / 2f - 95f, VIRTUAL_HEIGHT / 2f - 50f);
        } else if (canPickUpMachete && canPickUpBomb) {
            font.setColor(Color.YELLOW);
            font.draw(batch, "Press [E] to Pick Up Items", VIRTUAL_WIDTH / 2f - 80f, VIRTUAL_HEIGHT / 2f - 50f);
        } else if (canPickUpMachete) {
            font.setColor(Color.YELLOW);
            font.draw(batch, "Press [E] to Pick Up Machete", VIRTUAL_WIDTH / 2f - 85f, VIRTUAL_HEIGHT / 2f - 50f);
        } else if (canPickUpBomb) {
            font.setColor(Color.YELLOW);
            font.draw(batch, "Press [E] to Pick Up Grenade", VIRTUAL_WIDTH / 2f - 85f, VIRTUAL_HEIGHT / 2f - 50f);
        }

        font.setColor(Color.GRAY);
        font.draw(batch, "WASD move   E interact   SPACE attack / LMB   SHIFT sprint   M map   ESC pause   I inventory   F3 debug   H immunity", 20f, 30f);
        batch.end();

        drawMinimap(snapshot, hudMatrix, delta);
    }

    private void drawMinimap(WorldSnapshot snapshot, Matrix4 hudMatrix, float delta) {
        if (!isMinimapOpen || tileMap == null) return;
        minimapStateTime += delta;

        float mmW = 150f;
        float mmH = 110f;
        float mmX = VIRTUAL_WIDTH - mmW - 20f;
        float mmY = VIRTUAL_HEIGHT - mmH - 36f;

        float mapTilesW = tileMap.getWidth();
        float mapTilesH = tileMap.getHeight();
        if (mapTilesW <= 0 || mapTilesH <= 0) return;

        // 1. Semi-transparent background & Header Bar
        Gdx.gl.glEnable(GL20.GL_BLEND);
        shapes.setProjectionMatrix(hudMatrix);
        shapes.begin(ShapeRenderer.ShapeType.Filled);

        // Header tab
        shapes.setColor(0.08f, 0.12f, 0.18f, 0.92f);
        shapes.rect(mmX, mmY + mmH, mmW, 18f);

        // Main map frame background
        shapes.setColor(0.05f, 0.08f, 0.12f, 0.88f);
        shapes.rect(mmX, mmY, mmW, mmH);
        shapes.end();

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

        // Header text
        font.setColor(0.910f, 0.690f, 0.165f, 1f);
        String header = levelNumber == 1 ? "MAP: WARD" : (levelNumber == 2 ? "MAP: GROUND" : "MAP: VILLAGE");
        font.draw(batch, header, mmX + 6f, mmY + mmH + 14f);

        font.setColor(Color.LIGHT_GRAY);
        font.draw(batch, "[M]", mmX + mmW - 22f, mmY + mmH + 14f);
        batch.end();

        // 3. Grid lines & border
        shapes.setProjectionMatrix(hudMatrix);
        shapes.begin(ShapeRenderer.ShapeType.Line);

        // Header border
        shapes.setColor(0.20f, 0.50f, 0.80f, 0.90f);
        shapes.rect(mmX, mmY + mmH, mmW, 18f);

        // Outer border
        shapes.setColor(0.20f, 0.50f, 0.80f, 0.90f);
        shapes.rect(mmX, mmY, mmW, mmH);

        // Subtle crosshairs
        shapes.setColor(0.20f, 0.50f, 0.80f, 0.25f);
        shapes.line(mmX, mmY + mmH / 2f, mmX + mmW, mmY + mmH / 2f);
        shapes.line(mmX + mmW / 2f, mmY, mmX + mmW / 2f, mmY + mmH);
        shapes.end();

        // 4. Entity blips
        shapes.begin(ShapeRenderer.ShapeType.Filled);

        // A. Exit Beacon (Stairs / Tunnel)
        LevelExit.forLevel(levelNumber).ifPresent(exit -> {
            float ex = mmX + (exit.tileX() / mapTilesW) * mmW;
            float ey = mmY + (exit.tileY() / mapTilesH) * mmH;
            boolean unlocked = levelNumber != 1 || (hasStairsKey && isAmbushDefeated);
            shapes.setColor(unlocked ? Color.LIME : Color.ORANGE);
            float pulse = 3.5f + (float) Math.sin(minimapStateTime * 5.0f) * 1.2f;
            shapes.circle(ex, ey, pulse, 10);
        });

        // B. Key Blip (Level 1 only, when not yet collected)
        if (levelNumber == 1 && !hasStairsKey) {
            float kx = mmX + (keyX / mapTilesW) * mmW;
            float ky = mmY + (keyY / mapTilesH) * mmH;
            shapes.setColor(Color.GOLD);
            float pulse = 3.2f + (float) Math.sin(minimapStateTime * 6.0f) * 1.0f;
            shapes.circle(kx, ky, pulse, 8);
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
                    shapes.setColor(Color.MAGENTA);
                    float pulse = 4.5f + (float) Math.sin(minimapStateTime * 5.0f) * 1.2f;
                    shapes.circle(ax, ay, pulse, 10);
                } else {
                    shapes.setColor(Color.RED);
                    shapes.circle(ax, ay, 2.6f, 8);
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
        Gdx.gl.glDisable(GL20.GL_BLEND);
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

    private void drawPauseOverlay() {
        Matrix4 hudMatrix = new Matrix4().setToOrtho2D(0, 0, VIRTUAL_WIDTH, VIRTUAL_HEIGHT);
        Gdx.gl.glEnable(GL20.GL_BLEND);

        shapes.setProjectionMatrix(hudMatrix);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0.04f, 0.06f, 0.1f, 0.85f);
        shapes.rect(0, 0, VIRTUAL_WIDTH, VIRTUAL_HEIGHT);

        float panelW = 420f, panelH = 240f;
        float panelX = (VIRTUAL_WIDTH - panelW) / 2f, panelY = (VIRTUAL_HEIGHT - panelH) / 2f;

        shapes.setColor(0.08f, 0.12f, 0.2f, 0.95f);
        shapes.rect(panelX, panelY, panelW, panelH);
        shapes.end();

        shapes.begin(ShapeRenderer.ShapeType.Line);
        shapes.setColor(0.91f, 0.69f, 0.16f, 1f);
        shapes.rect(panelX, panelY, panelW, panelH);
        shapes.end();

        Vector3 mouseCoords = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
        viewport.unproject(mouseCoords);
        boolean mouseJustPressed = Gdx.input.isButtonJustPressed(Input.Buttons.LEFT);

        float btnW = 320f, btnH = 45f, btnX = (VIRTUAL_WIDTH - btnW) / 2f, btn1Y = panelY + 120f;
        boolean btn1Hovered = mouseCoords.x >= btnX && mouseCoords.x <= btnX + btnW && mouseCoords.y >= btn1Y && mouseCoords.y <= btn1Y + btnH;
        if (btn1Hovered && mouseJustPressed) {
            paused = false;
            client.sendEvent(GameConstants.EVENT_RESUME, "");
        }

        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(btn1Hovered ? new Color(0.25f, 0.35f, 0.5f, 1f) : new Color(0.15f, 0.2f, 0.3f, 1f));
        shapes.rect(btnX, btn1Y, btnW, btnH);
        shapes.end();

        shapes.begin(ShapeRenderer.ShapeType.Line);
        shapes.setColor(btn1Hovered ? new Color(0.91f, 0.69f, 0.16f, 1f) : new Color(0.4f, 0.5f, 0.65f, 1f));
        shapes.rect(btnX, btn1Y, btnW, btnH);
        shapes.end();

        float btn2Y = panelY + 50f;
        boolean btn2Hovered = mouseCoords.x >= btnX && mouseCoords.x <= btnX + btnW && mouseCoords.y >= btn2Y && mouseCoords.y <= btn2Y + btnH;
        if (!returningToLauncher
                && ((btn2Hovered && mouseJustPressed)
                || Gdx.input.isKeyJustPressed(Input.Keys.Q)
                || Gdx.input.isKeyJustPressed(Input.Keys.M))) {
            returningToLauncher = true;
            bridge.requestReturnToLauncher(() -> Gdx.app.postRunnable(Gdx.app::exit));
            return;
        }

        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(btn2Hovered ? new Color(0.5f, 0.18f, 0.18f, 1f) : new Color(0.28f, 0.12f, 0.12f, 1f));
        shapes.rect(btnX, btn2Y, btnW, btnH);
        shapes.end();

        shapes.begin(ShapeRenderer.ShapeType.Line);
        shapes.setColor(btn2Hovered ? new Color(1f, 0.4f, 0.4f, 1f) : new Color(0.65f, 0.25f, 0.25f, 1f));
        shapes.rect(btnX, btn2Y, btnW, btnH);
        shapes.end();

        Gdx.gl.glDisable(GL20.GL_BLEND);

        batch.setProjectionMatrix(hudMatrix);
        batch.begin();
        font.setColor(new Color(0.91f, 0.69f, 0.16f, 1f));
        font.draw(batch, returningToLauncher ? "RETURNING TO MAIN MENU…" : "GAME PAUSED",
                returningToLauncher ? VIRTUAL_WIDTH / 2f - 112f : VIRTUAL_WIDTH / 2f - 60f,
                panelY + panelH - 25f);

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
        input.attackPressed = Gdx.input.isKeyJustPressed(Input.Keys.SPACE) || leftClickAttack;

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

    @Override
    public void dispose() {
        if (batch != null) batch.dispose();
        if (shapes != null) shapes.dispose();
        if (font != null) font.dispose();
        if (mapTexture != null) mapTexture.dispose();
        if (markerTexture != null) markerTexture.dispose();
        if (playerTexture != null) playerTexture.dispose();
        if (idleTexture != null) idleTexture.dispose();
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
    }
}