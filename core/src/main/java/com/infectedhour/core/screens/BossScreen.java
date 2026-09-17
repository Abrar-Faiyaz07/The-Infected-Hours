package com.infectedhour.core.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector2;
import com.infectedhour.core.InfectedHourGame;
import com.infectedhour.core.bridge.GameBridge;
import com.infectedhour.core.level.LevelDefinition;
import com.infectedhour.core.net.GameClient;
import com.infectedhour.core.systems.BossPhaseSystem;
import com.infectedhour.shared.network.CharacterType;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Encounter screen for Level 3's final confrontation against the Virus Heart.
 * Supports choosing between Jane and Elric, with the companion operative fighting
 * alongside as an ally. Loads map_final.png and boss.png with graceful fallbacks.
 */
public class BossScreen implements Screen {

    private static final float WIDTH = 1280f;
    private static final float HEIGHT = 720f;
    private static final float FEMALE_MELEE_SCALE = 1.1f;

    private final InfectedHourGame game;
    private final GameClient client;
    private final GameBridge bridge;
    private final CharacterType playerCharacter;
    private final CharacterType companionCharacter;
    private final BossPhaseSystem boss = new BossPhaseSystem();

    private SpriteBatch batch;
    private ShapeRenderer shapes;
    private BitmapFont font;
    private BitmapFont titleFont;
    private Matrix4 projection;

    // Textures & Visuals
    private Texture mapTexture;
    private Texture bossTexture;

    // Elric Textures & Animations
    private Texture elricWalkTexture;
    private TextureRegion[][] elricWalkFrames;
    private Texture elricIdleTexture;
    private TextureRegion[][] elricIdleFrames;
    private Texture elricMeleeTexture;
    private TextureRegion[][] elricMeleeFrames;

    // Jane Textures & Animations
    private Texture janeWalkTexture;
    private TextureRegion[][] janeWalkFrames;
    private Texture janeIdleTexture;
    private TextureRegion[][] janeIdleFrames;
    private Texture janeMeleeTexture;
    private TextureRegion[][] janeMeleeFrames;
    private boolean janeUsesTwoFrameMelee;

    // Player State
    private float playerX = 640f;
    private float playerY = 140f;
    private int playerFacing = 3; // 0=Down, 1=Left, 2=Right, 3=Up
    private boolean playerMoving;
    private float playerAnimTime;
    private boolean playerAttacking;
    private float playerAttackTime;
    private boolean playerDamageApplied;
    private float playerStamina = 100f;

    // Companion State (Jane if player is Elric; Elric if player is Jane)
    private float compX = 580f;
    private float compY = 140f;
    private int compFacing = 3;
    private boolean compMoving;
    private float compAnimTime;
    private boolean compAttacking;
    private float compAttackTime;
    private float compAttackCooldown = 0f;
    private boolean compReadyForDestruction;

    // Boss State (Centered on the dais of the futuristic lab map)
    private float bossX = 640f;
    private float bossY = 360f;
    private float bossPulseTimer;
    private float bossHitFlashTimer;

    // 4 Pillar Specimen Tubes (Matching the 4 pillar tubes in map_final.png)
    private final float[][] nodePositions = new float[][]{
            {310f, 520f}, // Top-Left Tube
            {310f, 180f}, // Bottom-Left Tube
            {970f, 520f}, // Top-Right Tube
            {970f, 180f}  // Bottom-Right Tube
    };
    private final boolean[] nodeInjected = new boolean[4];
    private int injectedSamples;

    // Phase 3 & Flow
    private float coreHoldSeconds;
    private float victorySeconds;
    private boolean endingStarted;
    private boolean titleCardDismissed;
    private boolean paused;

    // Floating Sparks / Damage Effects
    private static class HitEffect {
        float x, y;
        float life;
        float maxLife;
        Color color;
        String text;
    }
    private final List<HitEffect> effects = new ArrayList<>();

    public BossScreen(InfectedHourGame game, GameClient client, GameBridge bridge, CharacterType playerCharacter) {
        this.game = game;
        this.client = client;
        this.bridge = bridge;
        this.playerCharacter = playerCharacter != null ? playerCharacter : CharacterType.ELRIC;
        this.companionCharacter = (this.playerCharacter == CharacterType.JANE)
                ? CharacterType.ELRIC : CharacterType.JANE;
    }

    public BossScreen(InfectedHourGame game, GameClient client, GameBridge bridge) {
        this(game, client, bridge, CharacterType.ELRIC);
    }

    @Override
    public void show() {
        batch = new SpriteBatch();
        shapes = new ShapeRenderer();
        font = new BitmapFont();
        titleFont = new BitmapFont();
        titleFont.getData().setScale(2.4f);
        projection = new Matrix4().setToOrtho2D(0f, 0f, WIDTH, HEIGHT);

        // 1. Try loading map_final.png, then fallback candidates
        String[] mapCandidates = {"map_final.png", "map_final.jpg", "map2_part2.png", "map3.png", "map1_floor2.png"};
        for (String candidate : mapCandidates) {
            if (Gdx.files.internal(candidate).exists()) {
                try {
                    mapTexture = new Texture(Gdx.files.internal(candidate));
                    mapTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
                    break;
                } catch (Exception ignored) { }
            }
        }

        // 2. Try loading boss.png
        if (Gdx.files.internal("boss.png").exists()) {
            try {
                bossTexture = new Texture(Gdx.files.internal("boss.png"));
                bossTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            } catch (Exception ignored) { }
        } else if (Gdx.files.internal("boss.jpg").exists()) {
            try {
                bossTexture = new Texture(Gdx.files.internal("boss.jpg"));
                bossTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            } catch (Exception ignored) { }
        }

        // 3. Load Elric assets
        elricWalkTexture = loadTextureSafely("player.png");
        if (elricWalkTexture != null) {
            elricWalkTexture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
            elricWalkFrames = TextureRegion.split(elricWalkTexture, elricWalkTexture.getWidth() / 8, elricWalkTexture.getHeight() / 4);
        }

        elricIdleTexture = loadTextureSafely("player_idle.png");
        if (elricIdleTexture != null) {
            elricIdleTexture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
            elricIdleFrames = TextureRegion.split(elricIdleTexture, elricIdleTexture.getWidth() / 8, elricIdleTexture.getHeight() / 4);
        }

        elricMeleeTexture = loadTextureSafely("melee_hit.png");
        if (elricMeleeTexture != null) {
            elricMeleeTexture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
            int mCols = 4;
            int mRows = 2;
            if (elricMeleeTexture.getHeight() > elricMeleeTexture.getWidth()) {
                mCols = 2;
                mRows = 4;
            }
            elricMeleeFrames = TextureRegion.split(elricMeleeTexture, elricMeleeTexture.getWidth() / mCols, elricMeleeTexture.getHeight() / mRows);
        }

        // 4. Load Jane assets
        janeWalkTexture = loadTextureSafely("player 2/player.png");
        if (janeWalkTexture == null) janeWalkTexture = loadTextureSafely("player 2/map_player.png");
        if (janeWalkTexture != null) {
            janeWalkTexture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
            janeWalkFrames = TextureRegion.split(janeWalkTexture, janeWalkTexture.getWidth() / 8, janeWalkTexture.getHeight() / 4);
        }

        janeIdleTexture = loadTextureSafely("player 2/player_idle_final.png");
        if (janeIdleTexture == null) janeIdleTexture = loadTextureSafely("player 2/player_idle.png");
        if (janeIdleTexture != null) {
            janeIdleTexture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
            janeIdleFrames = TextureRegion.split(janeIdleTexture, janeIdleTexture.getWidth() / 8, janeIdleTexture.getHeight() / 4);
        }

        janeMeleeTexture = loadTextureSafely("player 2/melee_hit.png");
        if (janeMeleeTexture != null) {
            janeUsesTwoFrameMelee = true;
            janeMeleeTexture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
            janeMeleeFrames = TextureRegion.split(janeMeleeTexture, janeMeleeTexture.getWidth() / 2, janeMeleeTexture.getHeight() / 4);
        } else {
            janeMeleeTexture = loadTextureSafely("player 2/player_melee.png");
            if (janeMeleeTexture != null) {
                janeUsesTwoFrameMelee = false;
                janeMeleeTexture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
                janeMeleeFrames = TextureRegion.split(janeMeleeTexture, janeMeleeTexture.getWidth() / 8, janeMeleeTexture.getHeight() / 4);
            }
        }

        if (game.isHost() && game.getServer() != null) {
            game.getServer().configureLevel(LevelDefinition.level3Boss());
        }
    }

    private Texture loadTextureSafely(String internalPath) {
        if (Gdx.files.internal(internalPath).exists()) {
            try {
                return new Texture(Gdx.files.internal(internalPath));
            } catch (Exception e) {
                Gdx.app.log("BossScreen", "Failed loading " + internalPath + ": " + e.getMessage());
            }
        }
        return null;
    }

    @Override
    public void render(float delta) {
        game.stepSimulation(delta);
        Gdx.gl.glClearColor(0.02f, 0.025f, 0.035f, 1f);
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

        handleInput(delta);
        updateEncounter(delta);
        updateCompanion(delta);
        updateEffects(delta);

        // 1. Draw Arena Map Background
        if (mapTexture != null) {
            batch.setProjectionMatrix(projection);
            batch.begin();
            batch.setColor(0.50f, 0.52f, 0.56f, 1f);
            batch.draw(mapTexture, 0f, 0f, WIDTH, HEIGHT);
            batch.setColor(Color.WHITE);
            batch.end();
        } else {
            drawProceduralArena();
        }

        // 2. Draw Injection Stations & Power Beams
        drawInjectionStations();

        // 3. Draw Boss
        drawBoss(delta);

        // 4. Draw Operatives (Player and Companion)
        drawOperatives(delta);

        // 5. Draw Hit Effects and Sparks
        drawEffects();

        // 6. Draw HUD Interface
        drawInterface();

        // 7. Pause Overlay
        if (paused) {
            drawPauseOverlay();
        }
    }

    private void handleInput(float delta) {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            paused = !paused;
            return;
        }

        if (paused) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.Q)) {
                returnToLauncher();
            }
            return;
        }

        // ── Movement ──
        float moveX = 0f;
        float moveY = 0f;
        if (Gdx.input.isKeyPressed(Input.Keys.W) || Gdx.input.isKeyPressed(Input.Keys.UP)) moveY += 1f;
        if (Gdx.input.isKeyPressed(Input.Keys.S) || Gdx.input.isKeyPressed(Input.Keys.DOWN)) moveY -= 1f;
        if (Gdx.input.isKeyPressed(Input.Keys.A) || Gdx.input.isKeyPressed(Input.Keys.LEFT)) moveX -= 1f;
        if (Gdx.input.isKeyPressed(Input.Keys.D) || Gdx.input.isKeyPressed(Input.Keys.RIGHT)) moveX += 1f;

        boolean isSprinting = (Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT) || Gdx.input.isKeyPressed(Input.Keys.SHIFT_RIGHT))
                && playerStamina > 10f && (moveX != 0 || moveY != 0);

        float speed = isSprinting ? 320f : 210f;
        if (isSprinting) {
            playerStamina = Math.max(0f, playerStamina - delta * 25f);
        } else {
            playerStamina = Math.min(100f, playerStamina + delta * 18f);
        }

        playerMoving = (moveX != 0 || moveY != 0);
        if (playerMoving) {
            float len = (float) Math.sqrt(moveX * moveX + moveY * moveY);
            moveX /= len;
            moveY /= len;

            playerX = MathUtils.clamp(playerX + moveX * speed * delta, 90f, WIDTH - 90f);
            playerY = MathUtils.clamp(playerY + moveY * speed * delta, 90f, HEIGHT - 120f);
            playerAnimTime += delta;

            if (Math.abs(moveX) > Math.abs(moveY)) {
                playerFacing = moveX > 0 ? 2 : 1;
            } else {
                playerFacing = moveY > 0 ? 3 : 0;
            }
        } else {
            playerAnimTime += delta * 0.5f;
        }

        // ── Attack (SPACE, 0, NUMPAD_0, Left Click) ──
        boolean attackTrigger = Gdx.input.isKeyJustPressed(Input.Keys.SPACE)
                || Gdx.input.isKeyJustPressed(Input.Keys.NUMPAD_0)
                || Gdx.input.isKeyJustPressed(Input.Keys.NUM_0)
                || Gdx.input.isButtonJustPressed(Input.Buttons.LEFT);

        if (attackTrigger && !playerAttacking) {
            playerAttacking = true;
            playerAttackTime = 0f;
            playerDamageApplied = false;
        }

        // ── Interact / Inject (E key) ──
        if (Gdx.input.isKeyJustPressed(Input.Keys.E)) {
            interactAtStations();
        }
    }

    private void interactAtStations() {
        if (boss.getCurrentPhase() == BossPhaseSystem.Phase.SHIELD) {
            // Find closest station
            int closest = -1;
            float closestDist = Float.MAX_VALUE;
            for (int i = 0; i < nodePositions.length; i++) {
                if (!nodeInjected[i]) {
                    float d = Vector2.dst(playerX, playerY, nodePositions[i][0], nodePositions[i][1]);
                    if (d < closestDist) {
                        closestDist = d;
                        closest = i;
                    }
                }
            }

            // Allow inject if close (< 140f) or anywhere as debug shortcut
            if (closest != -1 && (closestDist < 140f || true)) {
                nodeInjected[closest] = true;
                injectedSamples++;
                addEffect(nodePositions[closest][0], nodePositions[closest][1] + 30f,
                        new Color(0.2f, 0.9f, 0.5f, 1f), "SAMPLE INJECTED!");
                if (injectedSamples >= nodePositions.length) {
                    boss.onShieldWeakened();
                    addEffect(bossX, bossY + 40f, new Color(1f, 0.8f, 0.2f, 1f), "SHIELD BREACHED!");
                }
            }
        }
    }

    private void updateEncounter(float delta) {
        bossPulseTimer += delta;
        if (bossHitFlashTimer > 0f) bossHitFlashTimer -= delta;

        // Player attack animation and damage tick
        if (playerAttacking) {
            playerAttackTime += delta;
            float frameDur = (playerCharacter == CharacterType.JANE && !janeUsesTwoFrameMelee) ? 0.05f : 0.22f;
            int currentFrame = (int) (playerAttackTime / frameDur);

            if (currentFrame >= 1 && !playerDamageApplied) {
                playerDamageApplied = true;
                applyPlayerAttack();
            }

            int maxFrames = (playerCharacter == CharacterType.JANE && !janeUsesTwoFrameMelee) ? 8 : 2;
            if (currentFrame >= maxFrames) {
                playerAttacking = false;
            }
        }

        switch (boss.getCurrentPhase()) {
            case SHIELD -> {
                // Wait for all 3 nodes
            }
            case EXPOSURE -> {
                // Exposure timer ticks, boss exposed
                boss.tickExposure(delta, 0f);
                if (boss.getCurrentPhase() == BossPhaseSystem.Phase.SHIELD) {
                    injectedSamples = 0;
                    for (int i = 0; i < nodeInjected.length; i++) nodeInjected[i] = false;
                    addEffect(bossX, bossY, new Color(0.9f, 0.2f, 0.2f, 1f), "SHIELD RECHARGED!");
                }
            }
            case CORE_DESTRUCTION -> {
                boolean playerHolding = Gdx.input.isKeyPressed(Input.Keys.E)
                        || Gdx.input.isKeyPressed(Input.Keys.SPACE)
                        || Gdx.input.isKeyPressed(Input.Keys.NUMPAD_0);
                if (playerHolding) {
                    coreHoldSeconds = Math.min(1.25f, coreHoldSeconds + delta);
                } else {
                    coreHoldSeconds = Math.max(0f, coreHoldSeconds - delta * 1.5f);
                }
                if (coreHoldSeconds >= 1.2f) {
                    boss.resolveCoreDestructionAttempt(true, compReadyForDestruction);
                    addEffect(bossX, bossY, new Color(1f, 0.95f, 0.4f, 1f), "CORE DETONATED!");
                }
            }
            case DEFEATED -> {
                victorySeconds += delta;
                if (victorySeconds >= 4.0f && !endingStarted) {
                    endingStarted = true;
                    game.setScreen(new StoryPanelScreen(game, client, bridge, StoryPanelScreen.Sequence.ENDING, 3));
                }
                if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER) || Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
                    returnToLauncher();
                }
            }
        }
    }

    private void applyPlayerAttack() {
        float dToBoss = Vector2.dst(playerX, playerY, bossX, bossY);
        if (boss.getCurrentPhase() == BossPhaseSystem.Phase.EXPOSURE) {
            if (dToBoss < 210f) {
                float dmg = (playerCharacter == CharacterType.ELRIC) ? 0.09f : 0.07f;
                boss.tickExposure(0.01f, dmg);
                bossHitFlashTimer = 0.16f;
                addEffect(bossX + MathUtils.random(-30f, 30f), bossY + MathUtils.random(-30f, 30f),
                        new Color(1f, 0.3f, 0.3f, 1f), "-" + Math.round(dmg * 100f) + " CORE");
            }
        } else if (boss.getCurrentPhase() == BossPhaseSystem.Phase.SHIELD) {
            if (dToBoss < 210f) {
                addEffect(playerX, playerY + 30f, new Color(0.4f, 0.8f, 1f, 1f), "DEFLECTED BY SHIELD!");
            }
        } else if (boss.getCurrentPhase() == BossPhaseSystem.Phase.CORE_DESTRUCTION) {
            if (dToBoss < 240f) {
                coreHoldSeconds += 0.25f;
            }
        }
    }

    private void updateCompanion(float delta) {
        compAnimTime += delta;
        compAttackCooldown -= delta;

        // Companion attack animation
        if (compAttacking) {
            compAttackTime += delta;
            float frameDur = (companionCharacter == CharacterType.JANE && !janeUsesTwoFrameMelee) ? 0.05f : 0.22f;
            int maxFrames = (companionCharacter == CharacterType.JANE && !janeUsesTwoFrameMelee) ? 8 : 2;
            if (compAttackTime >= frameDur * maxFrames) {
                compAttacking = false;
            }
        }

        float targetX = playerX - 55f;
        float targetY = playerY;

        switch (boss.getCurrentPhase()) {
            case SHIELD -> {
                // Companion assists with an un-injected node if player is handling another
                int targetNode = -1;
                for (int i = 0; i < nodeInjected.length; i++) {
                    if (!nodeInjected[i]) {
                        targetNode = i;
                        break;
                    }
                }
                if (targetNode != -1) {
                    targetX = nodePositions[targetNode][0];
                    targetY = nodePositions[targetNode][1] - 30f;
                    float d = Vector2.dst(compX, compY, targetX, targetY);
                    if (d < 35f && !nodeInjected[targetNode]) {
                        // Companion injects!
                        nodeInjected[targetNode] = true;
                        injectedSamples++;
                        addEffect(targetX, targetY + 35f, new Color(0.2f, 0.9f, 0.5f, 1f),
                                companionCharacter.name() + " INJECTED SAMPLE!");
                        if (injectedSamples >= nodePositions.length) {
                            boss.onShieldWeakened();
                            addEffect(bossX, bossY + 40f, new Color(1f, 0.8f, 0.2f, 1f), "SHIELD BREACHED!");
                        }
                    }
                }
            }
            case EXPOSURE -> {
                // Companion charges to the boss and attacks
                targetX = bossX + 60f;
                targetY = bossY - 70f;
                float d = Vector2.dst(compX, compY, targetX, targetY);
                if (d < 50f && compAttackCooldown <= 0f) {
                    compAttacking = true;
                    compAttackTime = 0f;
                    compAttackCooldown = 0.65f;
                    float compDmg = 0.035f;
                    boss.tickExposure(0.01f, compDmg);
                    bossHitFlashTimer = 0.12f;
                    addEffect(bossX + 40f, bossY - 20f, new Color(0.3f, 0.9f, 0.6f, 1f),
                            "-" + Math.round(compDmg * 100f));
                }
            }
            case CORE_DESTRUCTION -> {
                targetX = bossX - 70f;
                targetY = bossY - 60f;
                compReadyForDestruction = true;
            }
            case DEFEATED -> {
                targetX = playerX + 60f;
                targetY = playerY;
            }
        }

        // Companion movement towards target
        float dx = targetX - compX;
        float dy = targetY - compY;
        float dist = (float) Math.sqrt(dx * dx + dy * dy);

        if (dist > 18f) {
            compMoving = true;
            dx /= dist;
            dy /= dist;
            float cSpeed = 195f;
            compX += dx * cSpeed * delta;
            compY += dy * cSpeed * delta;

            if (Math.abs(dx) > Math.abs(dy)) {
                compFacing = dx > 0 ? 2 : 1;
            } else {
                compFacing = dy > 0 ? 3 : 0;
            }
        } else {
            compMoving = false;
        }
    }

    private void addEffect(float x, float y, Color color, String text) {
        HitEffect eff = new HitEffect();
        eff.x = x;
        eff.y = y;
        eff.life = 0f;
        eff.maxLife = 0.9f;
        eff.color = color;
        eff.text = text;
        effects.add(eff);
    }

    private void updateEffects(float delta) {
        Iterator<HitEffect> it = effects.iterator();
        while (it.hasNext()) {
            HitEffect e = it.next();
            e.life += delta;
            e.y += delta * 35f;
            if (e.life >= e.maxLife) it.remove();
        }
    }

    private void drawProceduralArena() {
        shapes.setProjectionMatrix(projection);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0.045f, 0.06f, 0.08f, 1f);
        shapes.rect(70f, 70f, WIDTH - 140f, HEIGHT - 140f);

        // Floor grid
        shapes.setColor(0.09f, 0.14f, 0.17f, 1f);
        for (int x = 120; x < 1180; x += 80) shapes.rect(x, 90f, 2f, HEIGHT - 180f);
        for (int y = 100; y < 620; y += 70) shapes.rect(90f, y, WIDTH - 180f, 2f);

        // Containment circle boundary
        shapes.setColor(0.14f, 0.26f, 0.28f, 0.8f);
        shapes.circle(bossX, bossY, 220f, 64);
        shapes.setColor(0.045f, 0.06f, 0.08f, 1f);
        shapes.circle(bossX, bossY, 216f, 64);
        shapes.end();
    }

    private void drawInjectionStations() {
        shapes.setProjectionMatrix(projection);
        shapes.begin(ShapeRenderer.ShapeType.Filled);

        for (int i = 0; i < nodePositions.length; i++) {
            float nx = nodePositions[i][0];
            float ny = nodePositions[i][1];
            boolean done = nodeInjected[i];

            // Pedestal base
            shapes.setColor(0.12f, 0.16f, 0.20f, 1f);
            shapes.rect(nx - 24f, ny - 16f, 48f, 32f);

            // Glowing terminal core
            if (done) {
                shapes.setColor(0.2f, 0.95f, 0.45f, 1f);
            } else {
                float pulse = 0.5f + 0.5f * (float) Math.sin(bossPulseTimer * 3.5f + i);
                shapes.setColor(0.1f, 0.6f + pulse * 0.35f, 0.9f, 1f);
            }
            shapes.circle(nx, ny + 4f, 12f, 24);

            // Energy cable / beam to boss
            if (boss.getCurrentPhase() == BossPhaseSystem.Phase.SHIELD) {
                shapes.setColor(done ? new Color(0.2f, 0.9f, 0.4f, 0.4f) : new Color(0.2f, 0.6f, 0.9f, 0.6f));
                shapes.rectLine(nx, ny + 4f, bossX, bossY, done ? 1.5f : 3f);
            }
        }
        shapes.end();

        // Node labels
        batch.setProjectionMatrix(projection);
        batch.begin();
        for (int i = 0; i < nodePositions.length; i++) {
            float nx = nodePositions[i][0];
            float ny = nodePositions[i][1];
            boolean done = nodeInjected[i];
            font.setColor(done ? new Color(0.4f, 0.95f, 0.5f, 1f) : new Color(0.4f, 0.8f, 1f, 1f));
            font.draw(batch, done ? "[INJECTED]" : "[E] INJECT", nx - 34f, ny - 22f);
        }
        batch.end();
    }

    private void drawBoss(float delta) {
        float hp = boss.getCoreHpPct();
        float pulse = 0.95f + 0.05f * (float) Math.sin(bossPulseTimer * 4f);

        // If Phase 1 (Shield), draw energy shield around boss
        if (boss.getCurrentPhase() == BossPhaseSystem.Phase.SHIELD) {
            Gdx.gl.glEnable(GL20.GL_BLEND);
            shapes.setProjectionMatrix(projection);
            shapes.begin(ShapeRenderer.ShapeType.Filled);
            shapes.setColor(0.15f, 0.45f, 0.85f, 0.35f + 0.1f * (float) Math.sin(bossPulseTimer * 5f));
            shapes.circle(bossX, bossY, 140f * pulse, 48);
            shapes.setColor(0.4f, 0.75f, 1.0f, 0.7f);
            shapes.circle(bossX, bossY, 142f * pulse, 3);
            shapes.end();
        }

        if (bossTexture != null) {
            // Draw boss.png
            batch.setProjectionMatrix(projection);
            batch.begin();
            if (bossHitFlashTimer > 0f) {
                batch.setColor(1f, 0.35f, 0.35f, 1f);
            } else {
                batch.setColor(1f, 1f, 1f, 1f);
            }
            float bW = 200f * pulse;
            float bH = 200f * pulse;
            batch.draw(bossTexture, bossX - bW / 2f, bossY - bH / 2f, bW, bH);
            batch.setColor(Color.WHITE);
            batch.end();
        } else {
            // Procedural Virus Heart Core
            shapes.setProjectionMatrix(projection);
            shapes.begin(ShapeRenderer.ShapeType.Filled);
            shapes.setColor(0.16f, 0.02f, 0.055f, 1f);
            shapes.circle(bossX, bossY, 126f * pulse, 64);
            shapes.setColor(0.42f + (1f - hp) * 0.25f, 0.04f, 0.10f, 1f);
            shapes.circle(bossX, bossY, 82f * pulse, 64);
            shapes.setColor(bossHitFlashTimer > 0 ? Color.WHITE : new Color(0.92f, 0.16f, 0.24f, 0.85f));
            shapes.circle(bossX, bossY, (38f + hp * 18f) * pulse, 48);
            shapes.end();
        }
    }

    private void drawOperatives(float delta) {
        batch.setProjectionMatrix(projection);
        batch.begin();

        // 1. Draw Companion
        drawCharacter(companionCharacter, compX, compY, compFacing, compMoving,
                compAttacking, compAttackTime, compAnimTime,
                companionCharacter.name() + " (ALLY)", false);

        // 2. Draw Player
        drawCharacter(playerCharacter, playerX, playerY, playerFacing, playerMoving,
                playerAttacking, playerAttackTime, playerAnimTime,
                "[YOU] " + playerCharacter.name(), true);

        batch.end();
    }

    private void drawCharacter(CharacterType type, float x, float y, int facing,
                              boolean moving, boolean attacking, float attackTime,
                              float animTime, String label, boolean isLocal) {
        TextureRegion frame = null;
        float drawW = 48f;
        float drawH = 54f;

        if (type == CharacterType.ELRIC) {
            if (attacking && elricMeleeFrames != null) {
                int col = Math.min((int) (attackTime / 0.22f), elricMeleeFrames[0].length - 1);
                int row = facing % elricMeleeFrames.length;
                frame = elricMeleeFrames[row][col];
                drawW = frame.getRegionWidth() * 0.85f;
                drawH = frame.getRegionHeight() * 0.85f;
            } else if (moving && elricWalkFrames != null) {
                int col = ((int) (animTime / 0.10f)) % 8;
                int row = facing % elricWalkFrames.length;
                frame = elricWalkFrames[row][col];
                drawW = frame.getRegionWidth();
                drawH = frame.getRegionHeight();
            } else if (elricIdleFrames != null) {
                int col = ((int) (animTime / 0.14f)) % 8;
                int row = facing % elricIdleFrames.length;
                frame = elricIdleFrames[row][col];
                drawW = frame.getRegionWidth();
                drawH = frame.getRegionHeight();
            }
        } else {
            // JANE
            if (attacking && janeMeleeFrames != null) {
                if (janeUsesTwoFrameMelee) {
                    int col = Math.min((int) (attackTime / 0.22f), janeMeleeFrames[0].length - 1);
                    int row = facing % janeMeleeFrames.length;
                    frame = janeMeleeFrames[row][col];
                    drawW = frame.getRegionWidth() * 0.8f * FEMALE_MELEE_SCALE;
                    drawH = frame.getRegionHeight() * 0.8f * FEMALE_MELEE_SCALE;
                } else {
                    int col = ((int) (attackTime / 0.05f)) % 8;
                    int row = facing % janeMeleeFrames.length;
                    frame = janeMeleeFrames[row][col];
                    drawW = frame.getRegionWidth() * FEMALE_MELEE_SCALE;
                    drawH = frame.getRegionHeight() * FEMALE_MELEE_SCALE;
                }
            } else if (moving && janeWalkFrames != null) {
                int col = ((int) (animTime / 0.10f)) % 8;
                int row = facing % janeWalkFrames.length;
                frame = janeWalkFrames[row][col];
                drawW = frame.getRegionWidth();
                drawH = frame.getRegionHeight();
            } else if (janeIdleFrames != null) {
                int col = ((int) (animTime / 0.14f)) % 8;
                int row = facing % janeIdleFrames.length;
                frame = janeIdleFrames[row][col];
                drawW = frame.getRegionWidth();
                drawH = frame.getRegionHeight();
            }
        }

        if (frame != null) {
            batch.draw(frame, x - drawW / 2f, y - drawH / 2f, drawW, drawH);
        }

        // Label above operative
        font.setColor(isLocal ? new Color(1f, 0.85f, 0.3f, 1f) : new Color(0.4f, 0.85f, 1f, 1f));
        font.draw(batch, label, x - 32f, y + drawH / 2f + 14f);
    }

    private void drawEffects() {
        batch.setProjectionMatrix(projection);
        batch.begin();
        for (HitEffect eff : effects) {
            font.setColor(eff.color);
            font.draw(batch, eff.text, eff.x - 25f, eff.y);
        }
        batch.end();
    }

    private void drawInterface() {
        // ── Boss Health Bar ──
        shapes.setProjectionMatrix(projection);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0.08f, 0.09f, 0.11f, 1f);
        shapes.rect(340f, 650f, 600f, 22f);
        shapes.setColor(0.88f, 0.12f, 0.18f, 1f);
        shapes.rect(340f, 650f, 600f * boss.getCoreHpPct(), 22f);

        // Core Overload Progress (Phase 3)
        if (boss.getCurrentPhase() == BossPhaseSystem.Phase.CORE_DESTRUCTION) {
            shapes.setColor(0.08f, 0.09f, 0.11f, 1f);
            shapes.rect(440f, 140f, 400f, 18f);
            shapes.setColor(0.91f, 0.69f, 0.16f, 1f);
            shapes.rect(440f, 140f, 400f * Math.min(1f, coreHoldSeconds / 1.2f), 18f);
        }

        // ── Player Stamina Bar (Bottom-Left) ──
        shapes.setColor(0.12f, 0.15f, 0.2f, 0.8f);
        shapes.rect(30f, 30f, 180f, 12f);
        shapes.setColor(0.2f, 0.75f, 0.95f, 1f);
        shapes.rect(30f, 30f, 180f * (playerStamina / 100f), 12f);
        shapes.end();

        batch.setProjectionMatrix(projection);
        batch.begin();
        font.setColor(Color.WHITE);
        font.draw(batch, "VIRUS HEART  " + Math.round(boss.getCoreHpPct() * 100f) + "%", 570f, 690f);

        // Player Tag
        font.setColor(0.95f, 0.85f, 0.35f, 1f);
        font.draw(batch, "OPERATIVE: " + playerCharacter.name() + "   (STAMINA)", 30f, 58f);

        font.setColor(0.91f, 0.69f, 0.16f, 1f);
        switch (boss.getCurrentPhase()) {
            case SHIELD -> font.draw(batch,
                    "PHASE 1 - Press [E] at the 4 specimen tubes to weaken shield (" + injectedSamples + "/" + nodePositions.length + ")", 340f, 90f);
            case EXPOSURE -> font.draw(batch,
                    "PHASE 2 - CORE EXPOSED! Attack the Virus Heart with [SPACE / 0 / CLICK]!", 350f, 90f);
            case CORE_DESTRUCTION -> font.draw(batch,
                    "FINAL PHASE - Hold [E / SPACE] to overload core together!", 410f, 90f);
            case DEFEATED -> {
                titleFont.setColor(0.25f, 0.95f, 0.52f, 1f);
                titleFont.draw(batch, "CONTAINMENT RESTORED", 390f, 390f);
                font.setColor(Color.WHITE);
                font.draw(batch, "Press [ENTER] or [SPACE] to return to Launcher", 470f, 330f);
            }
        }
        batch.end();
    }

    private void drawTitleCard() {
        shapes.setProjectionMatrix(projection);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0.02f, 0.025f, 0.035f, 0.95f);
        shapes.rect(0f, 0f, WIDTH, HEIGHT);
        shapes.setColor(0.35f, 0.035f, 0.055f, 0.85f);
        shapes.rect(0f, 280f, WIDTH, 150f);
        shapes.end();

        batch.setProjectionMatrix(projection);
        batch.begin();
        font.setColor(0.62f, 0.66f, 0.70f, 1f);
        font.draw(batch, "LEVEL 3  /  HIDDEN LABORATORY", 520f, 470f);
        titleFont.setColor(0.91f, 0.20f, 0.24f, 1f);
        titleFont.draw(batch, "THE VIRUS HEART", 440f, 380f);
        font.setColor(Color.WHITE);
        font.draw(batch, "Lead Operative: " + playerCharacter.name() + "   |   Ally: " + companionCharacter.name(), 480f, 320f);
        font.setColor(0.91f, 0.69f, 0.16f, 1f);
        font.draw(batch, "[E] or [SPACE] ENTER THE CONTAINMENT CHAMBER", 440f, 230f);
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

        batch.setProjectionMatrix(projection);
        batch.begin();
        titleFont.setColor(0.95f, 0.85f, 0.3f, 1f);
        titleFont.draw(batch, "PAUSED", WIDTH / 2f - 85f, HEIGHT / 2f + 65f);
        font.setColor(Color.WHITE);
        font.draw(batch, "[ESC]  RESUME ENCOUNTER", WIDTH / 2f - 95f, HEIGHT / 2f + 10f);
        font.setColor(0.95f, 0.35f, 0.35f, 1f);
        font.draw(batch, "[Q]    RETURN TO LAUNCHER", WIDTH / 2f - 95f, HEIGHT / 2f - 30f);
        batch.end();
    }

    private void returnToLauncher() {
        if (bridge != null) {
            bridge.requestReturnToLauncher(() -> Gdx.app.postRunnable(Gdx.app::exit));
        } else {
            Gdx.app.exit();
        }
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
        if (bossTexture != null) bossTexture.dispose();
        if (elricWalkTexture != null) elricWalkTexture.dispose();
        if (elricIdleTexture != null) elricIdleTexture.dispose();
        if (elricMeleeTexture != null) elricMeleeTexture.dispose();
        if (janeWalkTexture != null) janeWalkTexture.dispose();
        if (janeIdleTexture != null) janeIdleTexture.dispose();
        if (janeMeleeTexture != null) janeMeleeTexture.dispose();
    }
}

