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
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.infectedhour.core.InfectedHourGame;
import com.infectedhour.core.bridge.GameBridge;
import com.infectedhour.core.net.GameClient;
import com.infectedhour.shared.network.CharacterType;
import java.util.ArrayList;
import java.util.List;

public class BossScreen implements Screen {

    private static final float WIDTH = 1280f;
    private static final float HEIGHT = 720f;

    // Scaling Adjustments
    private static final float BOSS_SCALE = 0.50f;
    private static final float JANE_SCALE = 0.5f;
    private static final float ELRIC_SCALE = 0.5f;
    private static final float SCIENTIST_SCALE = 0.45f;
    private static final float AURA_SCALE = 0.25f;
    private static final float SHIELD_SCALE = 0.35f;
    private static final float VOID_SCALE = 0.2f;

    private final InfectedHourGame game;
    private final GameClient client;
    private final GameBridge bridge;
    private final CharacterType playerCharacter;

    private SpriteBatch batch;
    private ShapeRenderer shapes;
    private BitmapFont font;
    private Matrix4 projection;

    private Texture mapTexture;

    // Scientist State & Textures (4x4 Spritesheet)
    private Texture scientistTexture;
    private TextureRegion[][] scientistFrames;
    private int scientistWidth, scientistHeight;
    private float scientistX = 400f, scientistY = 300f, scientistHp = 500f;
    private boolean isScientistAlive = true;
    private float scientistRoamTimer = 0f;
    private float scientistTargetX = 400f, scientistTargetY = 300f;
    private float scientistHitCooldown = 0f;
    private float scientistAnimTime = 0f;
    private int scientistFacing = 0; // 0=Down, 1=Left, 2=Right, 3=Up

    // Boss Spawn / Load Sequence States
    private float bossSpawnSequenceTimer = 0f;
    private enum BossSpawnState { NONE, FLICKER_FIRST_FRAME, FULL_LOAD_ANIMATION, ACTIVE }
    private BossSpawnState bossSpawnState = BossSpawnState.NONE;
    private float bossSpawnX = WIDTH / 2f;
    private float bossSpawnY = HEIGHT * 0.575f;

    // Phase 2 Intro Sequence States
    private float phaseTwoIntroTimer = 0f;
    private enum PhaseTwoIntroState { NONE, FLICKER_FIRST_FRAME, FULL_LOAD_ANIMATION, ACTIVE }
    private PhaseTwoIntroState phaseTwoIntroState = PhaseTwoIntroState.NONE;

    // Boss Loading State
    private Texture bossLoadTexture;
    private TextureRegion[][] bossLoadFrames;
    private int bossLoadWidth, bossLoadHeight;
    private boolean isBossLoaded = false;
    private float bossLoadTimer = 0f;

    // Boss Textures, State & Phase 2 Mechanics
    private Texture bossTexture;
    private TextureRegion[][] bossFrames;
    private int bossFrameWidth, bossFrameHeight;
    private float bossX = WIDTH / 2f, bossY = HEIGHT * 0.575f, bossSpeed = 85f, bossPulseTimer = 0f;
    private int bossFacing = 0;
    private float bossAnimTime = 0f;
    private float bossHp = 1000f;
    private float bossHitCooldown = 0f;
    private enum BossSpecialState { NORMAL, INTRO, AURA_RETURN }
    private BossSpecialState bossSpecialState = BossSpecialState.NORMAL;
    private float bossInvulnerableTimer = 0f;
    private boolean phaseTwoTriggeredOnce = false;

    // Boss Aura Texture (Single Frame)
    private Texture bossAuraTexture;

    // Void Projectile/Explosion (1x4 sheet)
    private Texture voidTexture;
    private TextureRegion[][] voidFrames;
    private int voidWidth, voidHeight;
    private float voidSpawnTimer = 0f;

    private static class ActiveVoid {
        float x, y, timeElapsed = 0f;
    }
    private final List<ActiveVoid> activeVoids = new ArrayList<>();

    // Elric Textures & Sprint/Hit
    private Texture elricWalkTexture, elricIdleTexture, elricMeleeTexture, elricIdleMeleeTexture, elricSprintTexture;
    private TextureRegion[][] elricWalkFrames, elricIdleFrames, elricMeleeFrames, elricIdleMeleeFrames, elricSprintFrames;
    private Texture elricBombTexture, elricBombIdleTexture, elricBombThrowTexture;
    private TextureRegion[][] elricBombFrames, elricBombIdleFrames, elricBombThrowFrames;
    private Texture elricMeleeHitTexture;
    private TextureRegion[][] elricMeleeHitFrames;
    private int elricMeleeHitFrameWidth, elricMeleeHitFrameHeight;
    private boolean elricUsesTwoFrameMelee = false;

    // Jane Textures & Sprint/Hit
    private Texture janeWalkTexture, janeIdleTexture, janeMeleeTexture, janeIdleMeleeTexture, janeSprintTexture;
    private TextureRegion[][] janeWalkFrames, janeIdleFrames, janeMeleeFrames, janeIdleMeleeFrames, janeSprintFrames;
    private Texture janeBombTexture, janeBombIdleTexture, janeBombThrowTexture;
    private TextureRegion[][] janeBombFrames, janeBombIdleFrames, janeBombThrowFrames;
    private Texture janeMeleeHitTexture;
    private TextureRegion[][] janeMeleeHitFrames;
    private int janeMeleeHitFrameWidth, janeMeleeHitFrameHeight;
    private boolean janeUsesTwoFrameMelee = false;

    // Inventory & Projectile Textures
    private Texture inventoryTexture, meleeInventoryTexture, bombTexture, bombEffectTexture;
    private TextureRegion[][] bombFrames, bombEffectFrames;
    private boolean isInventoryOpen = false;
    private boolean hasMachete = true, isMacheteEquipped = false;
    private boolean hasBomb = true, isBombEquipped = false;
    private float bombCooldown = 0f;

    private static class ActiveBomb { float startX, startY, targetX, targetY, timeElapsed = 0f, totalDuration = 0.6f; }
    private static class ActiveExplosion { float x, y, timeElapsed = 0f, totalDuration = 0.4f; }

    private final List<ActiveBomb> activeBombs = new ArrayList<>();
    private final List<ActiveExplosion> activeExplosions = new ArrayList<>();

    // Player State
    private float playerX = 640f, playerY = 140f;
    private int playerFacing = 3;
    private boolean playerMoving, playerAttacking, playerSprinting, attackTriggeredThisFrame = false;
    private float playerAnimTime, playerAttackTime, playerStamina = 100f;
    private boolean paused;

    public BossScreen(InfectedHourGame game, GameClient client, GameBridge bridge, CharacterType playerCharacter) {
        this.game = game; this.client = client; this.bridge = bridge;
        this.playerCharacter = playerCharacter != null ? playerCharacter : CharacterType.ELRIC;
    }

    public BossScreen(InfectedHourGame game, GameClient client, GameBridge bridge) {
        this(game, client, bridge, CharacterType.ELRIC);
    }

    private Texture loadTextureSafely(String internalPath) {
        if (Gdx.files.internal(internalPath).exists()) {
            try { return new Texture(Gdx.files.internal(internalPath)); }
            catch (Exception e) { Gdx.app.log("BossScreen", "Failed loading " + internalPath); }
        }
        return null;
    }

    private Texture createColorTexture(int width, int height, Color color) {
        Pixmap pixmap = new Pixmap(width, height, Pixmap.Format.RGBA8888);
        pixmap.setColor(color); pixmap.fill();
        Texture tex = new Texture(pixmap); pixmap.dispose(); return tex;
    }

    @Override
    public void show() {
        batch = new SpriteBatch(); shapes = new ShapeRenderer(); font = new BitmapFont();
        projection = new Matrix4().setToOrtho2D(0f, 0f, WIDTH, HEIGHT);

        String[] mapCandidates = {"map_final.png", "map_final.jpg", "map3.png"};
        for (String candidate : mapCandidates) {
            mapTexture = loadTextureSafely(candidate);
            if (mapTexture != null) {
                mapTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
                break;
            }
        }

        // Load Scientist Texture (4x4 Spritesheet)
        scientistTexture = loadTextureSafely("scientist.png");
        if (scientistTexture != null) {
            scientistTexture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
            scientistWidth = scientistTexture.getWidth() / 3;
            scientistHeight = scientistTexture.getHeight() / 4;
            scientistFrames = TextureRegion.split(scientistTexture, scientistWidth, scientistHeight);
        }

        // Load Boss Aura Texture (Single Frame)
        bossAuraTexture = loadTextureSafely("boss_aura.png");
        if (bossAuraTexture != null) {
            bossAuraTexture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        }

        // Load Void Texture (1x4 Spritesheet)
        voidTexture = loadTextureSafely("void.png");
        if (voidTexture != null) {
            voidTexture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
            voidWidth = voidTexture.getWidth() / 4;
            voidHeight = voidTexture.getHeight() / 1;
            voidFrames = TextureRegion.split(voidTexture, voidWidth, voidHeight);
        }

        // Load Boss Load Sprite Sheet (1x2)
        bossLoadTexture = loadTextureSafely("boss_load.png");
        if (bossLoadTexture != null) {
            bossLoadTexture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
            bossLoadWidth = bossLoadTexture.getWidth() / 2;
            bossLoadHeight = bossLoadTexture.getHeight() / 1;
            bossLoadFrames = TextureRegion.split(bossLoadTexture, bossLoadWidth, bossLoadHeight);
        }

        bossTexture = loadTextureSafely("boss.png");
        if (bossTexture == null) bossTexture = loadTextureSafely("boss.jpg");
        if (bossTexture != null) {
            bossTexture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
            bossFrameWidth = bossTexture.getWidth() / 8; bossFrameHeight = bossTexture.getHeight() / 4;
            bossFrames = TextureRegion.split(bossTexture, bossFrameWidth, bossFrameHeight);
        }

        // Load Elric Textures & Sprint/Hit
        elricWalkTexture = loadTextureSafely("player.png");
        if (elricWalkTexture != null) elricWalkFrames = TextureRegion.split(elricWalkTexture, elricWalkTexture.getWidth() / 8, elricWalkTexture.getHeight() / 4);

        elricIdleTexture = loadTextureSafely("player_idle.png");
        if (elricIdleTexture != null) elricIdleFrames = TextureRegion.split(elricIdleTexture, elricIdleTexture.getWidth() / 8, elricIdleTexture.getHeight() / 4);

        elricSprintTexture = loadTextureSafely("sprint.png");
        if (elricSprintTexture != null) elricSprintFrames = TextureRegion.split(elricSprintTexture, elricSprintTexture.getWidth() / 8, elricSprintTexture.getHeight() / 4);

        elricMeleeTexture = loadTextureSafely("player_melee.png");
        if (elricMeleeTexture != null) elricMeleeFrames = TextureRegion.split(elricMeleeTexture, elricMeleeTexture.getWidth() / 8, elricMeleeTexture.getHeight() / 4);

        elricIdleMeleeTexture = loadTextureSafely("player_idle_melee.png");
        if (elricIdleMeleeTexture != null) elricIdleMeleeFrames = TextureRegion.split(elricIdleMeleeTexture, elricIdleMeleeTexture.getWidth() / 8, elricIdleMeleeTexture.getHeight() / 4);

        elricMeleeHitTexture = loadTextureSafely("melee_hit.png");
        if (elricMeleeHitTexture != null) {
            elricUsesTwoFrameMelee = true;
            elricMeleeHitTexture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
            int emCols = elricMeleeHitTexture.getHeight() > elricMeleeHitTexture.getWidth() ? 2 : 4;
            int emRows = elricMeleeHitTexture.getHeight() > elricMeleeHitTexture.getWidth() ? 4 : 2;
            elricMeleeHitFrameWidth = elricMeleeHitTexture.getWidth() / emCols;
            elricMeleeHitFrameHeight = elricMeleeHitTexture.getHeight() / emRows;
            elricMeleeHitFrames = TextureRegion.split(elricMeleeHitTexture, elricMeleeHitFrameWidth, elricMeleeHitFrameHeight);
        }

        elricBombTexture = loadTextureSafely("player_bomb.png");
        if (elricBombTexture != null) elricBombFrames = TextureRegion.split(elricBombTexture, elricBombTexture.getWidth() / 8, elricBombTexture.getHeight() / 4);

        elricBombIdleTexture = loadTextureSafely("player_bomb_idle.png");
        if (elricBombIdleTexture != null) elricBombIdleFrames = TextureRegion.split(elricBombIdleTexture, elricBombIdleTexture.getWidth() / 8, elricBombIdleTexture.getHeight() / 4);

        elricBombThrowTexture = loadTextureSafely("bomb_throw.png");
        if (elricBombThrowTexture != null) elricBombThrowFrames = TextureRegion.split(elricBombThrowTexture, elricBombThrowTexture.getWidth() / 2, elricBombThrowTexture.getHeight() / 4);

        // Load Jane Textures & Sprint/Hit
        janeWalkTexture = loadTextureSafely("player 2/player.png");
        if (janeWalkTexture == null) janeWalkTexture = loadTextureSafely("player 2/map_player.png");
        if (janeWalkTexture != null) janeWalkFrames = TextureRegion.split(janeWalkTexture, janeWalkTexture.getWidth() / 8, janeWalkTexture.getHeight() / 4);

        janeIdleTexture = loadTextureSafely("player 2/player_idle_final.png");
        if (janeIdleTexture == null) janeIdleTexture = loadTextureSafely("player 2/player_idle.png");
        if (janeIdleTexture != null) janeIdleFrames = TextureRegion.split(janeIdleTexture, janeIdleTexture.getWidth() / 8, janeIdleTexture.getHeight() / 4);

        janeSprintTexture = loadTextureSafely("player 2/sprint.png");
        if (janeSprintTexture != null) janeSprintFrames = TextureRegion.split(janeSprintTexture, janeSprintTexture.getWidth() / 8, janeSprintTexture.getHeight() / 4);

        janeMeleeTexture = loadTextureSafely("player 2/player_melee.png");
        if (janeMeleeTexture != null) janeMeleeFrames = TextureRegion.split(janeMeleeTexture, janeMeleeTexture.getWidth() / 8, janeMeleeTexture.getHeight() / 4);

        janeIdleMeleeTexture = loadTextureSafely("player 2/player_idle_melee.png");
        if (janeIdleMeleeTexture != null) janeIdleMeleeFrames = TextureRegion.split(janeIdleMeleeTexture, janeIdleMeleeTexture.getWidth() / 8, janeIdleMeleeTexture.getHeight() / 4);

        janeMeleeHitTexture = loadTextureSafely("player 2/melee_hit.png");
        if (janeMeleeHitTexture != null) {
            janeUsesTwoFrameMelee = true;
            janeMeleeHitTexture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
            int jmCols = janeMeleeHitTexture.getHeight() > janeMeleeHitTexture.getWidth() ? 2 : 4;
            int jmRows = janeMeleeHitTexture.getHeight() > janeMeleeHitTexture.getWidth() ? 4 : 2;
            janeMeleeHitFrameWidth = janeMeleeHitTexture.getWidth() / jmCols;
            janeMeleeHitFrameHeight = janeMeleeHitTexture.getHeight() / jmRows;
            janeMeleeHitFrames = TextureRegion.split(janeMeleeHitTexture, janeMeleeHitFrameWidth, janeMeleeHitFrameHeight);
        } else {
            janeMeleeHitTexture = loadTextureSafely("player 2/player_melee.png");
            if (janeMeleeHitTexture != null) {
                janeUsesTwoFrameMelee = false;
                janeMeleeHitTexture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
                janeMeleeHitFrameWidth = janeMeleeHitTexture.getWidth() / 8;
                janeMeleeHitFrameHeight = janeMeleeHitTexture.getHeight() / 4;
                janeMeleeHitFrames = TextureRegion.split(janeMeleeHitTexture, janeMeleeHitFrameWidth, janeMeleeHitFrameHeight);
            }
        }

        janeBombTexture = loadTextureSafely("player 2/player_bomb.png");
        if (janeBombTexture != null) janeBombFrames = TextureRegion.split(janeBombTexture, janeBombTexture.getWidth() / 8, janeBombTexture.getHeight() / 4);

        janeBombIdleTexture = loadTextureSafely("player 2/player_bomb_idle.png");
        if (janeBombIdleTexture != null) janeBombIdleFrames = TextureRegion.split(janeBombIdleTexture, janeBombIdleTexture.getWidth() / 8, janeBombIdleTexture.getHeight() / 4);

        janeBombThrowTexture = loadTextureSafely("player 2/bomb_throw.png");
        if (janeBombThrowTexture != null) janeBombThrowFrames = TextureRegion.split(janeBombThrowTexture, janeBombThrowTexture.getWidth() / 2, janeBombThrowTexture.getHeight() / 4);

        // Load Inventory & Effects
        inventoryTexture = loadTextureSafely("inventory.png");
        if (inventoryTexture == null) inventoryTexture = createColorTexture(400, 300, new Color(0.1f, 0.12f, 0.15f, 0.9f));

        meleeInventoryTexture = loadTextureSafely("melee_inventory.png");
        if (meleeInventoryTexture == null) meleeInventoryTexture = createColorTexture(36, 36, new Color(0.7f, 0.75f, 0.85f, 1f));

        bombTexture = loadTextureSafely("bomb.png");
        if (bombTexture == null) bombTexture = createColorTexture(16 * 8, 16, new Color(0.25f, 0.25f, 0.3f, 1f));
        bombFrames = TextureRegion.split(bombTexture, bombTexture.getWidth() / 8, bombTexture.getHeight());

        bombEffectTexture = loadTextureSafely("bomb_effect.png");
        if (bombEffectTexture == null) bombEffectTexture = createColorTexture(32 * 8, 32, new Color(1f, 0.6f, 0.1f, 0.85f));
        bombEffectFrames = TextureRegion.split(bombEffectTexture, bombEffectTexture.getWidth() / 8, bombEffectTexture.getHeight());
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.02f, 0.025f, 0.035f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        attackTriggeredThisFrame = false;
        handleInput(delta);

        if (!paused && !isInventoryOpen) {
            updatePlayer(delta);
            if (isScientistAlive) {
                updateScientist(delta);
            } else if (bossSpawnState != BossSpawnState.ACTIVE) {
                updateBossSpawnSequence(delta);
            } else if (phaseTwoIntroState != PhaseTwoIntroState.ACTIVE && phaseTwoIntroState != PhaseTwoIntroState.NONE) {
                updatePhaseTwoIntro(delta);
            } else {
                updateBoss(delta);
            }
        }

        // Draw Map
        batch.setProjectionMatrix(projection);
        batch.begin();
        if (mapTexture != null) {
            batch.setColor(0.50f, 0.52f, 0.56f, 1f);
            batch.draw(mapTexture, 0f, 0f, WIDTH, HEIGHT);
            batch.setColor(Color.WHITE);
        }
        batch.end();
        if (mapTexture == null) drawProceduralArena();

        // Draw Scientist or Boss Spawn Sequence or Phase 2 Intro or Boss
        if (isScientistAlive) {
            drawScientist(delta);
        } else if (bossSpawnState != BossSpawnState.ACTIVE) {
            drawBossSpawnSequence();
        } else if (phaseTwoIntroState != PhaseTwoIntroState.ACTIVE && phaseTwoIntroState != PhaseTwoIntroState.NONE) {
            drawPhaseTwoIntro();
        } else {
            drawBoss(delta);
        }

        // Draw Player, Voids/Explosions, and Projectiles
        batch.begin();
        drawVoids(delta);
        drawProjectiles(delta);
        drawPlayer();
        batch.end();

        // Draw HUD & UI overlays
        drawInterface();
        if (isInventoryOpen) drawInventoryOverlay();
        if (paused) drawPauseOverlay();
    }

    private void handleInput(float delta) {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            paused = !paused;
            if (paused) Gdx.input.setCursorCatched(false);
            return;
        }

        if (paused) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.Q)) returnToLauncher();
            return;
        }

        // Inventory Toggle
        if (Gdx.input.isKeyJustPressed(Input.Keys.I)) {
            isInventoryOpen = !isInventoryOpen;
            Gdx.input.setCursorCatched(!isInventoryOpen);
        }

        // Quick Equip / Toggle Toggles (Press 1 or 2 to equip, press again to unselect)
        if (!isInventoryOpen) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_1)) {
                isMacheteEquipped = !isMacheteEquipped;
                if (isMacheteEquipped) isBombEquipped = false;
            }
            if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_2)) {
                isBombEquipped = !isBombEquipped;
                if (isBombEquipped) isMacheteEquipped = false;
            }
        }
    }

    private void updateScientist(float delta) {
        if (scientistHitCooldown > 0f) scientistHitCooldown -= delta;
        scientistAnimTime += delta;

        // Check if player hits the scientist
        if (attackTriggeredThisFrame && scientistHitCooldown <= 0f) {
            float hitDist = 90f;
            float dx = playerX - scientistX;
            float dy = playerY - scientistY;
            float dist = (float) Math.sqrt(dx * dx + dy * dy);
            if (dist <= hitDist) {
                scientistHp -= 100f; // Damage scientist
                scientistHitCooldown = 0.4f;

                if (scientistHp <= 0f) {
                    isScientistAlive = false;
                    bossSpawnX = WIDTH / 2f;
                    bossSpawnY = HEIGHT * 0.575f;
                    bossX = WIDTH / 2f;
                    bossY = HEIGHT * 0.575f;
                    bossSpawnState = BossSpawnState.FLICKER_FIRST_FRAME;
                    bossSpawnSequenceTimer = 0f;
                    return;
                }
            }
        }

        float moveX = 0f;
        float moveY = 0f;

        // Random Roam & Flee Logic
        float distToPlayer = (float) Math.hypot(playerX - scientistX, playerY - scientistY);
        if (distToPlayer < 180f) {
            float fleeAngle = MathUtils.atan2(scientistY - playerY, scientistX - playerX);
            moveX = MathUtils.cos(fleeAngle) * 110f;
            moveY = MathUtils.sin(fleeAngle) * 110f;

            scientistX = MathUtils.clamp(scientistX + moveX * delta, 90f, WIDTH - 90f);
            scientistY = MathUtils.clamp(scientistY + moveY * delta, 90f, HEIGHT - 120f);
        } else {
            scientistRoamTimer -= delta;
            if (scientistRoamTimer <= 0f) {
                scientistRoamTimer = MathUtils.random(2f, 4f);
                scientistTargetX = MathUtils.random(150f, WIDTH - 150f);
                scientistTargetY = MathUtils.random(150f, HEIGHT - 150f);
            }
            float dx = scientistTargetX - scientistX;
            float dy = scientistTargetY - scientistY;
            float dist = (float) Math.sqrt(dx * dx + dy * dy);
            if (dist > 5f) {
                moveX = (dx / dist) * 60f;
                moveY = (dy / dist) * 60f;
                scientistX += moveX * delta;
                scientistY += moveY * delta;
            }
        }

        if (Math.abs(moveX) > Math.abs(moveY)) {
            scientistFacing = (moveX > 0) ? 2 : 1;
        } else if (moveY != 0) {
            scientistFacing = (moveY > 0) ? 3 : 0;
        }
    }

    private void updateBossSpawnSequence(float delta) {
        bossSpawnSequenceTimer += delta;
        if (bossSpawnState == BossSpawnState.FLICKER_FIRST_FRAME) {
            if (bossSpawnSequenceTimer >= 1.0f) {
                bossSpawnState = BossSpawnState.FULL_LOAD_ANIMATION;
                bossSpawnSequenceTimer = 0f;
            }
        } else if (bossSpawnState == BossSpawnState.FULL_LOAD_ANIMATION) {
            if (bossSpawnSequenceTimer >= 1.0f) {
                bossSpawnState = BossSpawnState.ACTIVE;
                isBossLoaded = true;
                bossX = bossSpawnX;
                bossY = bossSpawnY;
            }
        }
    }

    private void updatePhaseTwoIntro(float delta) {
        phaseTwoIntroTimer += delta;
        if (phaseTwoIntroState == PhaseTwoIntroState.FLICKER_FIRST_FRAME) {
            if (phaseTwoIntroTimer >= 0.5f) {
                phaseTwoIntroState = PhaseTwoIntroState.FULL_LOAD_ANIMATION;
                phaseTwoIntroTimer = 0f;
            }
        } else if (phaseTwoIntroState == PhaseTwoIntroState.FULL_LOAD_ANIMATION) {
            if (phaseTwoIntroTimer >= 0.5f) {
                phaseTwoIntroState = PhaseTwoIntroState.ACTIVE;
                bossSpecialState = BossSpecialState.AURA_RETURN;
                bossInvulnerableTimer = 15f;
                voidSpawnTimer = 0f;
            }
        }
    }

    private void updatePlayer(float delta) {
        if (bombCooldown > 0f) bombCooldown -= delta;

        float moveX = 0f, moveY = 0f;
        if (Gdx.input.isKeyPressed(Input.Keys.W) || Gdx.input.isKeyPressed(Input.Keys.UP)) moveY += 1f;
        if (Gdx.input.isKeyPressed(Input.Keys.S) || Gdx.input.isKeyPressed(Input.Keys.DOWN)) moveY -= 1f;
        if (Gdx.input.isKeyPressed(Input.Keys.A) || Gdx.input.isKeyPressed(Input.Keys.LEFT)) moveX -= 1f;
        if (Gdx.input.isKeyPressed(Input.Keys.D) || Gdx.input.isKeyPressed(Input.Keys.RIGHT)) moveX += 1f;

        playerSprinting = (Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT) || Gdx.input.isKeyPressed(Input.Keys.SHIFT_RIGHT))
                && playerStamina > 10f && (moveX != 0 || moveY != 0);

        float speed = playerSprinting ? 320f : 210f;
        playerStamina = playerSprinting ? Math.max(0f, playerStamina - delta * 25f) : Math.min(100f, playerStamina + delta * 18f);

        playerMoving = (moveX != 0 || moveY != 0);
        if (playerMoving) {
            float len = (float) Math.sqrt(moveX * moveX + moveY * moveY);
            playerX = MathUtils.clamp(playerX + (moveX / len) * speed * delta, 90f, WIDTH - 90f);
            playerY = MathUtils.clamp(playerY + (moveY / len) * speed * delta, 90f, HEIGHT - 120f);
            playerAnimTime += delta;
            playerFacing = Math.abs(moveX) > Math.abs(moveY) ? (moveX > 0 ? 2 : 1) : (moveY > 0 ? 3 : 0);
        } else {
            playerAnimTime += delta * 0.5f;
        }

        boolean attackTrigger = Gdx.input.isKeyJustPressed(Input.Keys.SPACE) || Gdx.input.isButtonJustPressed(Input.Buttons.LEFT);
        if (attackTrigger && !playerAttacking) {
            attackTriggeredThisFrame = true;
            if (isBombEquipped) {
                if (bombCooldown <= 0f) {
                    playerAttacking = true;
                    playerAttackTime = 0f;

                    ActiveBomb b = new ActiveBomb();
                    b.startX = playerX;
                    b.startY = playerY;

                    float throwDist = 70f;
                    if (playerFacing == 3) { b.targetX = playerX; b.targetY = playerY + throwDist; }
                    else if (playerFacing == 0) { b.targetX = playerX; b.targetY = playerY - throwDist; }
                    else if (playerFacing == 1) { b.targetX = playerX - throwDist; b.targetY = playerY; }
                    else if (playerFacing == 2) { b.targetX = playerX + throwDist; b.targetY = playerY; }

                    activeBombs.add(b);
                    bombCooldown = 0.8f;
                }
            } else if (isMacheteEquipped) {
                playerAttacking = true;
                playerAttackTime = 0f;
            }
        }

        if (playerAttacking) {
            playerAttackTime += delta;
            float frameDur = 0.25f;
            int maxFrames = 2;
            if ((int) (playerAttackTime / frameDur) >= maxFrames) playerAttacking = false;
        }
    }

    private void updateBoss(float delta) {
        if (!isBossLoaded || bossSpawnState != BossSpawnState.ACTIVE) return;
        bossPulseTimer += delta;
        if (bossHitCooldown > 0f) bossHitCooldown -= delta;

        // Check for Boss HP Phase 2 Trigger & Damage
        if (bossSpecialState == BossSpecialState.NORMAL) {
            if (attackTriggeredThisFrame && bossHitCooldown <= 0f) {
                float hitDist = 110f;
                float dx = playerX - bossX;
                float dy = playerY - bossY;
                float dist = (float) Math.sqrt(dx * dx + dy * dy);
                if (dist <= hitDist) {
                    bossHp -= 50f;
                    bossHitCooldown = 0.4f;

                    // Trigger Phase 2 intro sequence only once when HP drops to 500 or below
                    if (bossHp <= 500f && !phaseTwoTriggeredOnce) {
                        phaseTwoTriggeredOnce = true;
                        // Instantly teleport boss to spawn point for intro sequence
                        bossX = bossSpawnX;
                        bossY = bossSpawnY;
                        phaseTwoIntroState = PhaseTwoIntroState.FLICKER_FIRST_FRAME;
                        phaseTwoIntroTimer = 0f;
                    }
                }
            }

            // Normal tracking behavior
            float dx = playerX - bossX, dy = playerY - bossY, dist = (float) Math.sqrt(dx * dx + dy * dy);
            if (dist > 2f) {
                bossX += (dx / dist) * bossSpeed * delta;
                bossY += (dy / dist) * bossSpeed * delta;
                bossFacing = Math.abs(dx) > Math.abs(dy) ? (dx > 0 ? 2 : 1) : (dy > 0 ? 3 : 0);
                bossAnimTime += delta;
            } else {
                bossAnimTime += delta * 0.5f;
            }
        } else if (bossSpecialState == BossSpecialState.AURA_RETURN) {
            // Ensure boss stays locked at spawn point during Phase 2 with a tiny floating up-and-down motion
            bossX = bossSpawnX;
            bossY = bossSpawnY + MathUtils.sin(bossPulseTimer * 4f) * 6f;

            // Manage Invulnerability and Void spawns over 15s (1 every second)
            bossInvulnerableTimer -= delta;
            voidSpawnTimer += delta;

            if (voidSpawnTimer >= 1.0f && bossInvulnerableTimer > 0f) {
                voidSpawnTimer = 0f;
                ActiveVoid v = new ActiveVoid();
                v.x = playerX;
                v.y = playerY;
                activeVoids.add(v);
            }

            if (bossInvulnerableTimer <= 0f) {
                bossSpecialState = BossSpecialState.NORMAL;
                phaseTwoIntroState = PhaseTwoIntroState.NONE;
            }
        }
    }

    private void drawProceduralArena() {
        shapes.setProjectionMatrix(projection); shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0.045f, 0.06f, 0.08f, 1f); shapes.rect(70f, 70f, WIDTH - 140f, HEIGHT - 140f);
        shapes.setColor(0.09f, 0.14f, 0.17f, 1f);
        for (int x = 120; x < 1180; x += 80) shapes.rect(x, 90f, 2f, HEIGHT - 180f);
        for (int y = 100; y < 620; y += 70) shapes.rect(90f, y, WIDTH - 180f, 2f);
        shapes.end();
    }

    private void drawScientist(float delta) {
        batch.setProjectionMatrix(projection);
        batch.begin();
        if (scientistFrames != null) {
            int col = ((int) (scientistAnimTime / 0.15f)) % 3;
            int row = scientistFacing % 4;
            TextureRegion frame = scientistFrames[row][col];
            float drawW = scientistWidth * SCIENTIST_SCALE;
            float drawH = scientistHeight * SCIENTIST_SCALE;
            batch.draw(frame, scientistX - drawW / 2f, scientistY - drawH / 2f, drawW, drawH);
        } else if (scientistTexture != null) {
            float sW = scientistTexture.getWidth() * SCIENTIST_SCALE;
            float sH = scientistTexture.getHeight() * SCIENTIST_SCALE;
            batch.draw(scientistTexture, scientistX - sW / 2f, scientistY - sH / 2f, sW, sH);
        }
        batch.end();
    }

    private void drawBossSpawnSequence() {
        batch.setProjectionMatrix(projection);
        batch.begin();
        if (bossSpawnState == BossSpawnState.FLICKER_FIRST_FRAME && bossLoadFrames != null) {
            if (((int)(bossSpawnSequenceTimer * 10)) % 2 == 0) {
                TextureRegion frame = bossLoadFrames[0][0];
                float drawW = bossLoadWidth * 0.5f * BOSS_SCALE;
                float drawH = bossLoadHeight * 0.5f * BOSS_SCALE;
                batch.draw(frame, bossSpawnX - drawW / 2f, bossSpawnY - drawH / 2f, drawW, drawH);
            }
        } else if (bossSpawnState == BossSpawnState.FULL_LOAD_ANIMATION && bossLoadFrames != null) {
            int frameIdx = Math.min(1, (int) (bossSpawnSequenceTimer / 0.5f));
            TextureRegion frame = bossLoadFrames[0][frameIdx];
            float drawW = bossLoadWidth * 0.5f * BOSS_SCALE;
            float drawH = bossLoadHeight * 0.5f * BOSS_SCALE;
            batch.draw(frame, bossSpawnX - drawW / 2f, bossSpawnY - drawH / 2f, drawW, drawH);
        }
        batch.end();
    }

    private void drawPhaseTwoIntro() {
        batch.setProjectionMatrix(projection);
        batch.begin();
        if (phaseTwoIntroState == PhaseTwoIntroState.FLICKER_FIRST_FRAME && bossLoadFrames != null) {
            if (((int)(phaseTwoIntroTimer * 10)) % 2 == 0) {
                TextureRegion frame = bossLoadFrames[0][0];
                float drawW = bossLoadWidth * 0.5f * BOSS_SCALE;
                float drawH = bossLoadHeight * 0.5f * BOSS_SCALE;
                batch.draw(frame, bossSpawnX - drawW / 2f, bossSpawnY - drawH / 2f, drawW, drawH);
            }
        } else if (phaseTwoIntroState == PhaseTwoIntroState.FULL_LOAD_ANIMATION && bossLoadFrames != null) {
            int frameIdx = Math.min(1, (int) (phaseTwoIntroTimer / 0.5f));
            TextureRegion frame = bossLoadFrames[0][frameIdx];
            float drawW = bossLoadWidth * 0.5f * BOSS_SCALE;
            float drawH = bossLoadHeight * 0.5f * BOSS_SCALE;
            batch.draw(frame, bossSpawnX - drawW / 2f, bossSpawnY - drawH / 2f, drawW, drawH);
        }
        batch.end();
    }

    private void drawBoss(float delta) {
        batch.setProjectionMatrix(projection);
        batch.begin();

        // If in Phase 2 AURA_RETURN state, render black flame shielding effect + single-frame boss_aura
        if (bossSpecialState == BossSpecialState.AURA_RETURN && bossAuraTexture != null) {
            // Draw an organic pulsing black flame halo ring beneath the aura
            float flameScaleAnim = SHIELD_SCALE + (float)Math.sin(bossPulseTimer * 8f) * 0.04f;
            float flameW = bossAuraTexture.getWidth() * flameScaleAnim;
            float flameH = bossAuraTexture.getHeight() * flameScaleAnim;
            batch.setColor(0.05f, 0.05f, 0.05f, 0.85f); // Deep dark black flame tint
            batch.draw(bossAuraTexture, bossX - flameW / 2f, bossY - flameH / 2f, flameW, flameH);
            batch.setColor(Color.WHITE);

            // Draw primary single-frame boss aura texture
            float auraW = bossAuraTexture.getWidth() * AURA_SCALE;
            float auraH = bossAuraTexture.getHeight() * AURA_SCALE;
            batch.draw(bossAuraTexture, bossX - auraW / 2f, bossY - auraH / 2f, auraW, auraH);
        } else {
            if (bossFrames != null) {
                TextureRegion currentFrame = bossFrames[bossFacing % 4][((int) (bossAnimTime / 0.15f)) % 8];
                float drawWidth = bossFrameWidth * BOSS_SCALE, drawHeight = bossFrameHeight * BOSS_SCALE;
                batch.draw(currentFrame, bossX - drawWidth / 2f, bossY - drawHeight / 2f, drawWidth, drawHeight);
            } else if (bossTexture != null) {
                float bW = bossTexture.getWidth() * BOSS_SCALE, bH = bossTexture.getHeight() * BOSS_SCALE;
                batch.draw(bossTexture, bossX - bW / 2f, bossY - bH / 2f, bW, bH);
            }
        }

        batch.end();
    }

    private void drawVoids(float delta) {
        for (int i = activeVoids.size() - 1; i >= 0; i--) {
            ActiveVoid v = activeVoids.get(i);
            if (!paused && !isInventoryOpen) v.timeElapsed += delta;

            if (v.timeElapsed >= 1.5f) {
                activeVoids.remove(i);
                continue;
            }

            if (voidFrames != null) {
                int frameIdx = Math.min(3, (int) ((v.timeElapsed / 1.5f) * 4));
                TextureRegion frame = voidFrames[0][frameIdx];
                float vW = voidWidth * VOID_SCALE;
                float vH = voidHeight * VOID_SCALE;
                batch.draw(frame, v.x - vW / 2f, v.y - vH / 2f, vW, vH);
            }
        }
    }

    private void drawProjectiles(float delta) {
        for (int i = activeBombs.size() - 1; i >= 0; i--) {
            ActiveBomb b = activeBombs.get(i);
            if (!paused && !isInventoryOpen) b.timeElapsed += delta;

            float t = b.timeElapsed / b.totalDuration;
            float currX = b.startX + (b.targetX - b.startX) * t;
            float currY = b.startY + (b.targetY - b.startY) * t;

            if (t >= 1.0f) {
                ActiveExplosion exp = new ActiveExplosion();
                exp.x = b.targetX; exp.y = b.targetY;
                activeExplosions.add(exp);
                activeBombs.remove(i);
                continue;
            }

            float arcHeightPx = 4.0f * 37.5f * t * (1.0f - t);
            int frameIdx = (int) ((b.timeElapsed / 0.05f) % 8);

            if (bombFrames != null) {
                float scaledW = bombFrames[0][frameIdx].getRegionWidth() * 0.1f;
                float scaledH = bombFrames[0][frameIdx].getRegionHeight() * 0.1f;
                batch.draw(bombFrames[0][frameIdx], currX - scaledW / 2f, currY - scaledH / 2f + arcHeightPx, scaledW, scaledH);
            }
        }

        for (int i = activeExplosions.size() - 1; i >= 0; i--) {
            ActiveExplosion exp = activeExplosions.get(i);
            if (!paused && !isInventoryOpen) exp.timeElapsed += delta;

            if (exp.timeElapsed >= exp.totalDuration) {
                activeExplosions.remove(i);
                continue;
            }

            int frameIdx = Math.min(7, (int) ((exp.timeElapsed / exp.totalDuration) * 8));
            if (bombEffectFrames != null) {
                float expW = bombEffectFrames[0][frameIdx].getRegionWidth() * 0.5f;
                float expH = bombEffectFrames[0][frameIdx].getRegionHeight() * 0.5f;
                batch.draw(bombEffectFrames[0][frameIdx], exp.x - expW / 2f, exp.y - expH / 2f, expW, expH);
            }
        }
    }

    private void drawPlayer() {
        TextureRegion frame = null;
        float baseW = 48f, baseH = 54f;
        float drawW = 48f, drawH = 54f;

        if (playerCharacter == CharacterType.ELRIC) {
            float specificScale = 1.1f;
            if (playerAttacking) {
                if (isBombEquipped && elricBombThrowFrames != null) {
                    int col = Math.min((int) (playerAttackTime / 0.25f), 1);
                    frame = elricBombThrowFrames[playerFacing % elricBombThrowFrames.length][col];
                    if (frame != null) {
                        baseW = frame.getRegionWidth() * (0.6f * 0.75f * 0.81f);
                        baseH = frame.getRegionHeight() * (0.6f * 0.75f * 0.81f);
                    }
                } else if (isMacheteEquipped && elricMeleeHitFrames != null) {
                    int col = Math.min((int) (playerAttackTime / 0.25f), 1);
                    frame = elricMeleeHitFrames[playerFacing % elricMeleeHitFrames.length][col];
                    if (frame != null) {
                        baseW = elricMeleeHitFrameWidth * 0.9f;
                        baseH = elricMeleeHitFrameHeight * 0.9f;
                    }
                    specificScale = 1.1f;
                }
            }

            if (frame == null) {
                if (playerSprinting && elricSprintFrames != null) {
                    frame = elricSprintFrames[playerFacing % elricSprintFrames.length][((int) (playerAnimTime / 0.08f)) % 8];
                    specificScale = 1.1f;
                } else if (playerMoving) {
                    if (isBombEquipped && elricBombFrames != null) {
                        frame = elricBombFrames[playerFacing % elricBombFrames.length][((int) (playerAnimTime / 0.10f)) % 8];
                    } else if (isMacheteEquipped && elricMeleeFrames != null) {
                        frame = elricMeleeFrames[playerFacing % elricMeleeFrames.length][((int) (playerAnimTime / 0.10f)) % 8];
                    } else {
                        frame = elricWalkFrames[playerFacing % elricWalkFrames.length][((int) (playerAnimTime / 0.10f)) % 8];
                    }
                    specificScale = 1.1f;
                } else {
                    if (isBombEquipped && elricBombIdleFrames != null) {
                        frame = elricBombIdleFrames[playerFacing % elricBombIdleFrames.length][((int) (playerAnimTime / 0.14f)) % 8];
                        specificScale = 1.1f * 0.9f;
                    } else if (isMacheteEquipped && elricIdleMeleeFrames != null) {
                        frame = elricIdleMeleeFrames[playerFacing % elricIdleMeleeFrames.length][((int) (playerAnimTime / 0.14f)) % 8];
                        specificScale = 1.0f;
                    } else {
                        frame = elricIdleFrames[playerFacing % elricIdleFrames.length][((int) (playerAnimTime / 0.14f)) % 8];
                        specificScale = 1.1f;
                    }
                }
                if (frame != null) {
                    baseW = frame.getRegionWidth();
                    baseH = frame.getRegionHeight();
                }
            }

            if (isBombEquipped && playerAttacking) {
                drawW = baseW;
                drawH = baseH;
            } else {
                drawW = baseW * (ELRIC_SCALE * specificScale);
                drawH = baseH * (ELRIC_SCALE * specificScale);
            }
        } else {
            // JANE - strictly using original untouched logic and scaling
            float specificScale = 1.0f;

            if (playerAttacking) {
                if (isBombEquipped && janeBombThrowFrames != null) {
                    int col = Math.min((int) (playerAttackTime / 0.25f), 1);
                    frame = janeBombThrowFrames[playerFacing % janeBombThrowFrames.length][col];
                    if (frame != null) {
                        baseW = frame.getRegionWidth();
                        baseH = frame.getRegionHeight();
                    }
                    specificScale = 0.9f;
                } else if (isMacheteEquipped && janeMeleeHitFrames != null) {
                    if (janeUsesTwoFrameMelee) {
                        int col = Math.min((int) (playerAttackTime / 0.25f), 1);
                        frame = janeMeleeHitFrames[playerFacing % janeMeleeHitFrames.length][col];
                    } else {
                        int col = (int) (playerAttackTime / 0.25f) % janeMeleeHitFrames[0].length;
                        frame = janeMeleeHitFrames[playerFacing % janeMeleeHitFrames.length][col];
                    }
                    if (frame != null) {
                        baseW = janeMeleeHitFrameWidth;
                        baseH = janeMeleeHitFrameHeight;
                    }
                    specificScale = 0.7f;
                }
            }

            if (frame == null) {
                if (playerSprinting && janeSprintFrames != null) {
                    frame = janeSprintFrames[playerFacing % janeSprintFrames.length][((int) (playerAnimTime / 0.08f)) % 8];
                    specificScale = 1.0f;
                } else if (playerMoving) {
                    if (isBombEquipped && janeBombFrames != null) {
                        frame = janeBombFrames[playerFacing % janeBombFrames.length][((int) (playerAnimTime / 0.10f)) % 8];
                    } else if (isMacheteEquipped && janeMeleeFrames != null) {
                        frame = janeMeleeFrames[playerFacing % janeMeleeFrames.length][((int) (playerAnimTime / 0.10f)) % 8];
                    } else {
                        frame = janeWalkFrames[playerFacing % janeWalkFrames.length][((int) (playerAnimTime / 0.10f)) % 8];
                    }
                    specificScale = 1.0f;
                } else {
                    if (isBombEquipped && janeBombIdleFrames != null) {
                        frame = janeBombIdleFrames[playerFacing % janeBombIdleFrames.length][((int) (playerAnimTime / 0.14f)) % 8];
                        specificScale = 1.1f;
                    } else if (isMacheteEquipped && janeIdleMeleeFrames != null) {
                        frame = janeIdleMeleeFrames[playerFacing % janeIdleMeleeFrames.length][((int) (playerAnimTime / 0.14f)) % 8];
                        specificScale = 1.0f;
                    } else {
                        frame = janeIdleFrames[playerFacing % 4][((int) (playerAnimTime / 0.14f)) % 8];
                        specificScale = 1.0f;
                    }
                }
                if (frame != null) {
                    baseW = frame.getRegionWidth();
                    baseH = frame.getRegionHeight();
                }
            }

            drawW = baseW * (JANE_SCALE * specificScale);
            drawH = baseH * (JANE_SCALE * specificScale);
        }

        if (frame != null) batch.draw(frame, playerX - drawW / 2f, playerY - drawH / 2f, drawW, drawH);
    }

    private void drawInventoryOverlay() {
        batch.setProjectionMatrix(projection);
        batch.begin();

        float invW = inventoryTexture.getWidth(), invH = inventoryTexture.getHeight();
        float invX = Math.round((WIDTH - invW) / 2f), invY = Math.round((HEIGHT - invH) / 2f);
        batch.draw(inventoryTexture, invX, invY);
        batch.end();

        Gdx.gl.glEnable(GL20.GL_BLEND);
        shapes.setProjectionMatrix(projection);

        float rowW = invW - 40f, rowH = 50f, rowSpacing = 5f, rowX = invX + 20f, startY = invY + invH - 115f;
        float mouseX = Gdx.input.getX() * (WIDTH / (float)Gdx.graphics.getWidth());
        float mouseY = (Gdx.graphics.getHeight() - Gdx.input.getY()) * (HEIGHT / (float)Gdx.graphics.getHeight());
        boolean justPressed = Gdx.input.isButtonJustPressed(Input.Buttons.LEFT), isPressed = Gdx.input.isButtonPressed(Input.Buttons.LEFT);

        int itemIndex = 0;
        shapes.begin(ShapeRenderer.ShapeType.Filled);

        if (hasMachete) {
            float rowY = startY - (itemIndex * (rowH + rowSpacing));
            float btnW = 80f, btnH = 30f, btnX = rowX + rowW - btnW - 10f, btnY = rowY + (rowH - btnH) / 2f;
            boolean rowHovered = mouseX >= rowX && mouseX <= rowX + rowW && mouseY >= rowY && mouseY <= rowY + rowH;
            boolean btnHovered = mouseX >= btnX && mouseX <= btnX + btnW && mouseY >= btnY && mouseY <= btnY + btnH;

            if (btnHovered && justPressed) {
                isMacheteEquipped = !isMacheteEquipped;
                if (isMacheteEquipped) isBombEquipped = false;
            }

            shapes.setColor(rowHovered ? new Color(1f, 1f, 1f, 0.08f) : new Color(1f, 1f, 1f, 0.02f));
            shapes.rect(rowX, rowY, rowW, rowH);

            shapes.setColor(isMacheteEquipped
                    ? ((btnHovered && isPressed) ? new Color(0.5f, 0.2f, 0.2f, 1f) : (btnHovered ? new Color(0.7f, 0.3f, 0.3f, 1f) : new Color(0.6f, 0.25f, 0.25f, 1f)))
                    : ((btnHovered && isPressed) ? new Color(0.15f, 0.45f, 0.15f, 1f) : (btnHovered ? new Color(0.25f, 0.65f, 0.25f, 1f) : new Color(0.2f, 0.55f, 0.2f, 1f))));
            shapes.rect(btnX, btnY, btnW, btnH);
            itemIndex++;
        }

        if (hasBomb) {
            float rowY = startY - (itemIndex * (rowH + rowSpacing));
            float btnW = 80f, btnH = 30f, btnX = rowX + rowW - btnW - 10f, btnY = rowY + (rowH - btnH) / 2f;
            boolean rowHovered = mouseX >= rowX && mouseX <= rowX + rowW && mouseY >= rowY && mouseY <= rowY + rowH;
            boolean btnHovered = mouseX >= btnX && mouseX <= btnX + btnW && mouseY >= btnY && mouseY <= btnY + btnH;

            if (btnHovered && justPressed) {
                isBombEquipped = !isBombEquipped;
                if (isBombEquipped) isMacheteEquipped = false;
            }

            shapes.setColor(rowHovered ? new Color(1f, 1f, 1f, 0.08f) : new Color(1f, 1f, 1f, 0.02f));
            shapes.rect(rowX, rowY, rowW, rowH);

            shapes.setColor(isBombEquipped
                    ? ((btnHovered && isPressed) ? new Color(0.5f, 0.2f, 0.2f, 1f) : (btnHovered ? new Color(0.7f, 0.3f, 0.3f, 1f) : new Color(0.6f, 0.25f, 0.25f, 1f)))
                    : ((btnHovered && isPressed) ? new Color(0.15f, 0.45f, 0.15f, 1f) : (btnHovered ? new Color(0.25f, 0.65f, 0.25f, 1f) : new Color(0.2f, 0.55f, 0.2f, 1f))));
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
            float btnX = rowX + rowW - 90f, btnY = rowY + (rowH - 30f) / 2f;
            float iconSize = 36f, iconX = rowX + 15f, iconY = rowY + (rowH - iconSize) / 2f;

            batch.draw(meleeInventoryTexture, iconX, iconY, iconSize, iconSize);
            font.setColor(Color.WHITE); font.draw(batch, "Machete", iconX + iconSize + 20f, rowY + (rowH / 2f) + 5f);
            font.draw(batch, isMacheteEquipped ? "Unequip" : "Equip", btnX + (isMacheteEquipped ? 12f : 20f), btnY + 20f);
            itemIndex++;
        }

        if (hasBomb) {
            float rowY = startY - (itemIndex * (rowH + rowSpacing));
            float btnX = rowX + rowW - 90f, btnY = rowY + (rowH - 30f) / 2f;
            float iconSize = 36f, iconX = rowX + 15f, iconY = rowY + (rowH - iconSize) / 2f;

            if (bombFrames != null) batch.draw(bombFrames[0][0], iconX, iconY, iconSize, iconSize);
            font.setColor(Color.WHITE); font.draw(batch, "Grenade", iconX + iconSize + 20f, rowY + (rowH / 2f) + 5f);
            font.draw(batch, isBombEquipped ? "Unequip" : "Equip", btnX + (isBombEquipped ? 12f : 20f), btnY + 20f);
            itemIndex++;
        }
        batch.end();
    }

    private void drawInterface() {
        shapes.setProjectionMatrix(projection);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0.12f, 0.15f, 0.2f, 0.8f);
        shapes.rect(30f, 30f, 180f, 12f);
        shapes.setColor(0.2f, 0.75f, 0.95f, 1f);
        shapes.rect(30f, 30f, 180f * (playerStamina / 100f), 12f);
        shapes.end();

        batch.setProjectionMatrix(projection);
        batch.begin();
        font.setColor(0.95f, 0.85f, 0.35f, 1f);
        font.draw(batch, "OPERATIVE: " + playerCharacter.name() + "   (STAMINA)", 30f, 58f);
        font.setColor(Color.LIGHT_GRAY);
        font.draw(batch, "Press [I] to open Inventory | [1] Machete | [2] Grenade", 230f, 40f);
        batch.end();
    }

    private void drawPauseOverlay() {
        Gdx.gl.glEnable(GL20.GL_BLEND);
        shapes.setProjectionMatrix(projection);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0.03f, 0.04f, 0.06f, 0.75f);
        shapes.rect(0f, 0f, WIDTH, HEIGHT);
        shapes.setColor(0.12f, 0.16f, 0.22f, 0.9f);
        shapes.rect(WIDTH / 2f - 200f, HEIGHT / 2f - 100f, 400f, 200f);
        shapes.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);

        batch.setProjectionMatrix(projection);
        batch.begin();
        font.setColor(0.95f, 0.85f, 0.3f, 1f);
        font.draw(batch, "PAUSED", WIDTH / 2f - 30f, HEIGHT / 2f + 50f);
        font.setColor(Color.WHITE);
        font.draw(batch, "[ESC]  RESUME", WIDTH / 2f - 50f, HEIGHT / 2f + 10f);
        font.setColor(0.95f, 0.35f, 0.35f, 1f);
        font.draw(batch, "[Q]    QUIT TO LAUNCHER", WIDTH / 2f - 85f, HEIGHT / 2f - 30f);
        batch.end();
    }

    private void returnToLauncher() {
        if (bridge != null) bridge.requestReturnToLauncher(() -> Gdx.app.postRunnable(Gdx.app::exit));
        else Gdx.app.exit();
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
        if (mapTexture != null) mapTexture.dispose();
        if (bossTexture != null) bossTexture.dispose();
        if (bossLoadTexture != null) bossLoadTexture.dispose();
        if (elricWalkTexture != null) elricWalkTexture.dispose();
        if (elricIdleTexture != null) elricIdleTexture.dispose();
        if (elricMeleeTexture != null) elricMeleeTexture.dispose();
        if (elricIdleMeleeTexture != null) elricIdleMeleeTexture.dispose();
        if (elricSprintTexture != null) elricSprintTexture.dispose();
        if (elricBombTexture != null) elricBombTexture.dispose();
        if (elricBombIdleTexture != null) elricBombIdleTexture.dispose();
        if (elricBombThrowTexture != null) elricBombThrowTexture.dispose();
        if (janeWalkTexture != null) janeWalkTexture.dispose();
        if (janeIdleTexture != null) janeIdleTexture.dispose();
        if (janeMeleeTexture != null) janeMeleeTexture.dispose();
        if (janeIdleMeleeTexture != null) janeIdleMeleeTexture.dispose();
        if (janeSprintTexture != null) janeSprintTexture.dispose();
        if (janeBombTexture != null) janeBombTexture.dispose();
        if (janeBombIdleTexture != null) janeBombIdleTexture.dispose();
        if (janeBombThrowTexture != null) janeBombThrowTexture.dispose();
        if (inventoryTexture != null) inventoryTexture.dispose();
        if (meleeInventoryTexture != null) meleeInventoryTexture.dispose();
        if (bombTexture != null) bombTexture.dispose();
        if (bombEffectTexture != null) bombEffectTexture.dispose();
        if (elricMeleeHitTexture != null) elricMeleeHitTexture.dispose();
        if (janeMeleeHitTexture != null) janeMeleeHitTexture.dispose();
        if (bossAuraTexture != null) bossAuraTexture.dispose();
        if (voidTexture != null) voidTexture.dispose();
    }
}