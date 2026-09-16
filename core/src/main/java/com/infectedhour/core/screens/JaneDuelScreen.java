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
import com.badlogic.gdx.math.Matrix4;
import com.infectedhour.core.InfectedHourGame;
import com.infectedhour.core.bridge.GameBridge;
import com.infectedhour.core.net.GameClient;
import com.infectedhour.shared.constants.GameConstants;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Story Climax Duel: Elric vs. Undercover Agent Jane.
 * Triggered when Elric chooses to deliver the Antidote to Oscorp rather than save Elena.
 *
 * Mechanics from design doc:
 * - Jane deals 60 DMG per hit.
 * - Elric deals 50 DMG per hit.
 * - Parry mechanism: Press [F] or [Q] right before Jane strikes to parry and stun her for 1.3s.
 * - Dodge mechanism: Press [SHIFT] or [X] during windup to roll with invulnerability frames.
 */
public class JaneDuelScreen implements Screen {

    private static final float WIDTH = 1280f;
    private static final float HEIGHT = 720f;

    private static final float ELRIC_MAX_HP = 200f;
    private static final float JANE_MAX_HP = 300f;

    private static final float ELRIC_DAMAGE = 50f;
    private static final float JANE_DAMAGE = 60f;

    private final InfectedHourGame game;
    private final GameClient client;
    private final GameBridge bridge;

    private SpriteBatch batch;
    private ShapeRenderer shapes;
    private BitmapFont font;
    private BitmapFont titleFont;
    private Matrix4 projection;

    private Texture backgroundTexture;
    private Texture femaleTexture;
    private Texture elricTexture;
    private Texture pixelTexture;

    // Player (Elric) state
    private float elricHp = ELRIC_MAX_HP;
    private float elricX = 350f;
    private float elricY = 320f;
    private float elricAttackCooldown = 0f;
    private float elricParryTimer = 0f;      // Active parry window (0.38s)
    private float elricParryCooldown = 0f;
    private float elricDodgeTimer = 0f;      // Invulnerability roll (0.4s)
    private float elricDodgeCooldown = 0f;
    private float elricHurtFlash = 0f;

    // Boss (Jane) state
    private enum JaneState { APPROACH, WINDUP, STRIKE, RECOVER, STUNNED }
    private JaneState janeState = JaneState.APPROACH;
    private float janeHp = JANE_MAX_HP;
    private float janeX = 900f;
    private float janeY = 320f;
    private float janeStateTimer = 0f;
    private float janeHurtFlash = 0f;

    // Combat float text
    private static class FloatingText {
        String text;
        float x, y;
        float elapsed;
        Color color;
        FloatingText(String text, float x, float y, Color color) {
            this.text = text;
            this.x = x;
            this.y = y;
            this.color = color;
            this.elapsed = 0f;
        }
    }
    private final List<FloatingText> floatingTexts = new ArrayList<>();

    private boolean isDuelOver = false;
    private boolean elricWon = false;
    private float postDuelTimer = 0f;

    public JaneDuelScreen(InfectedHourGame game, GameClient client, GameBridge bridge) {
        this.game = game;
        this.client = client;
        this.bridge = bridge;
    }

    @Override
    public void show() {
        batch = new SpriteBatch();
        shapes = new ShapeRenderer();
        font = new BitmapFont();
        font.getData().setScale(1.2f);
        titleFont = new BitmapFont();
        titleFont.getData().setScale(2.2f);
        projection = new Matrix4().setToOrtho2D(0f, 0f, WIDTH, HEIGHT);

        Pixmap p = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        p.setColor(Color.WHITE);
        p.fill();
        pixelTexture = new Texture(p);
        p.dispose();

        if (Gdx.files.internal("story_intro.png").exists()) {
            try {
                backgroundTexture = new Texture(Gdx.files.internal("story_intro.png"));
                backgroundTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            } catch (Exception ignored) { }
        }
        if (Gdx.files.internal("player 2/player_melee.png").exists()) {
            try {
                femaleTexture = new Texture(Gdx.files.internal("player 2/player_melee.png"));
            } catch (Exception ignored) { }
        } else if (Gdx.files.internal("player 2/player.png").exists()) {
            try {
                femaleTexture = new Texture(Gdx.files.internal("player 2/player.png"));
            } catch (Exception ignored) { }
        } else if (Gdx.files.internal("female_player.png").exists()) {
            try {
                femaleTexture = new Texture(Gdx.files.internal("female_player.png"));
            } catch (Exception ignored) { }
        }
        if (Gdx.files.internal("player_melee.png").exists()) {
            try {
                elricTexture = new Texture(Gdx.files.internal("player_melee.png"));
            } catch (Exception ignored) { }
        }
    }

    @Override
    public void render(float delta) {
        game.stepSimulation(delta);

        update(delta);

        Gdx.gl.glClearColor(0.04f, 0.05f, 0.07f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // 1. Background
        batch.setProjectionMatrix(projection);
        batch.begin();
        if (backgroundTexture != null) {
            batch.setColor(0.35f, 0.25f, 0.25f, 1f);
            batch.draw(backgroundTexture, 0f, 0f, WIDTH, HEIGHT);
            batch.setColor(Color.WHITE);
        }
        // Dark arena shade
        batch.setColor(0.02f, 0.03f, 0.05f, 0.70f);
        batch.draw(pixelTexture, 0f, 0f, WIDTH, HEIGHT);
        batch.setColor(Color.WHITE);
        batch.end();

        // 2. Arena Floor & Visual Ring
        shapes.setProjectionMatrix(projection);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0.08f, 0.10f, 0.14f, 0.85f);
        shapes.rect(150f, 140f, 980f, 400f);
        shapes.setColor(0.85f, 0.2f, 0.25f, 0.35f);
        shapes.rect(150f, 138f, 980f, 4f);
        shapes.rect(150f, 538f, 980f, 4f);
        shapes.end();

        // 3. Characters & Combat Effects
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        // Elric shadow & indicator
        shapes.setColor(0.1f, 0.1f, 0.15f, 0.5f);
        shapes.ellipse(elricX - 25f, elricY - 12f, 50f, 18f);

        // Jane shadow
        shapes.setColor(0.1f, 0.1f, 0.15f, 0.5f);
        shapes.ellipse(janeX - 25f, janeY - 12f, 50f, 18f);

        // Parry aura around Elric
        if (elricParryTimer > 0f) {
            shapes.setColor(0.2f, 0.8f, 1.0f, 0.6f);
            shapes.circle(elricX, elricY + 30f, 48f);
        }
        // Dodge trail around Elric
        if (elricDodgeTimer > 0f) {
            shapes.setColor(0.4f, 1.0f, 0.4f, 0.4f);
            shapes.circle(elricX, elricY + 30f, 40f);
        }
        // Jane Windup telegraph
        if (janeState == JaneState.WINDUP) {
            float blink = (float) Math.sin(janeStateTimer * 20.0f) * 0.5f + 0.5f;
            shapes.setColor(1.0f, 0.15f, 0.15f, 0.4f + blink * 0.4f);
            shapes.circle(janeX, janeY + 30f, 55f);
        }
        shapes.end();

        // 4. Sprites
        batch.begin();
        // Draw Elric
        if (elricHurtFlash > 0f) batch.setColor(1f, 0.3f, 0.3f, 1f);
        else batch.setColor(Color.WHITE);
        if (elricTexture != null) {
            batch.draw(elricTexture, elricX - 32f, elricY, 64f, 72f, 0, 0, 32, 32, false, false);
        } else {
            // Fallback placeholder rectangle
            batch.draw(pixelTexture, elricX - 20f, elricY, 40f, 65f);
        }

        // Draw Jane
        if (janeHurtFlash > 0f) batch.setColor(1f, 0.3f, 0.3f, 1f);
        else if (janeState == JaneState.STUNNED) batch.setColor(0.6f, 0.8f, 1f, 0.8f);
        else batch.setColor(Color.WHITE);
        if (femaleTexture != null) {
            int fw = femaleTexture.getWidth() >= 64 ? femaleTexture.getWidth() / 8 : 32;
            int fh = femaleTexture.getHeight() >= 64 ? femaleTexture.getHeight() / 4 : 32;
            int srcY = femaleTexture.getHeight() >= 64 ? fh : 0;
            batch.draw(femaleTexture, janeX - 28f, janeY, 56f, 68f, 0, srcY, fw, fh, false, false);
        } else {
            batch.draw(pixelTexture, janeX - 18f, janeY, 36f, 65f);
        }
        batch.setColor(Color.WHITE);

        // Floating texts
        for (FloatingText ft : floatingTexts) {
            font.setColor(ft.color.r, ft.color.g, ft.color.b, Math.max(0f, 1f - (ft.elapsed / 1.1f)));
            font.draw(batch, ft.text, ft.x, ft.y + ft.elapsed * 35f);
        }
        font.setColor(Color.WHITE);

        // 5. HUD - Health Bars & Instructions
        drawHUD();

        batch.end();
    }

    private void update(float delta) {
        if (isDuelOver) {
            postDuelTimer += delta;
            if (elricWon) {
                if (Gdx.input.isKeyJustPressed(Input.Keys.E) || Gdx.input.isKeyJustPressed(Input.Keys.SPACE) || postDuelTimer >= 4f) {
                    bridge.notifyMatchEnded(new GameBridge.MatchOutcome("VICTORY", GameConstants.BOSS_LEVEL_NUMBER));
                    if (bridge.hasLauncher()) {
                        bridge.requestReturnToLauncher(() -> Gdx.app.postRunnable(Gdx.app::exit));
                    } else {
                        game.setScreen(new MainMenuScreen(game, client, bridge));
                    }
                }
            } else {
                if (Gdx.input.isKeyJustPressed(Input.Keys.R)) {
                    // Retry duel
                    elricHp = ELRIC_MAX_HP;
                    janeHp = JANE_MAX_HP;
                    elricX = 350f;
                    elricY = 320f;
                    janeX = 900f;
                    janeY = 320f;
                    janeState = JaneState.APPROACH;
                    janeStateTimer = 0f;
                    isDuelOver = false;
                    floatingTexts.clear();
                }
            }
            return;
        }

        // Timers
        if (elricAttackCooldown > 0f) elricAttackCooldown -= delta;
        if (elricParryTimer > 0f) elricParryTimer -= delta;
        if (elricParryCooldown > 0f) elricParryCooldown -= delta;
        if (elricDodgeTimer > 0f) elricDodgeTimer -= delta;
        if (elricDodgeCooldown > 0f) elricDodgeCooldown -= delta;
        if (elricHurtFlash > 0f) elricHurtFlash -= delta;
        if (janeHurtFlash > 0f) janeHurtFlash -= delta;

        // Player Controls
        float moveSpeed = (elricDodgeTimer > 0f) ? 380f : 210f;
        if (Gdx.input.isKeyPressed(Input.Keys.A) || Gdx.input.isKeyPressed(Input.Keys.LEFT)) {
            elricX = Math.max(180f, elricX - moveSpeed * delta);
        }
        if (Gdx.input.isKeyPressed(Input.Keys.D) || Gdx.input.isKeyPressed(Input.Keys.RIGHT)) {
            elricX = Math.min(1080f, elricX + moveSpeed * delta);
        }
        if (Gdx.input.isKeyPressed(Input.Keys.W) || Gdx.input.isKeyPressed(Input.Keys.UP)) {
            elricY = Math.min(480f, elricY + moveSpeed * delta);
        }
        if (Gdx.input.isKeyPressed(Input.Keys.S) || Gdx.input.isKeyPressed(Input.Keys.DOWN)) {
            elricY = Math.max(160f, elricY - moveSpeed * delta);
        }

        // Dodge roll [SHIFT] or [X]
        if ((Gdx.input.isKeyJustPressed(Input.Keys.SHIFT_LEFT) || Gdx.input.isKeyJustPressed(Input.Keys.X))
                && elricDodgeCooldown <= 0f && elricParryTimer <= 0f) {
            elricDodgeTimer = 0.40f;
            elricDodgeCooldown = 0.90f;
            floatingTexts.add(new FloatingText("DODGE ROLL!", elricX - 25f, elricY + 50f, Color.LIME));
        }

        // Parry [F] or [Q]
        if ((Gdx.input.isKeyJustPressed(Input.Keys.F) || Gdx.input.isKeyJustPressed(Input.Keys.Q))
                && elricParryCooldown <= 0f && elricDodgeTimer <= 0f) {
            elricParryTimer = 0.38f;
            elricParryCooldown = 0.95f;
            floatingTexts.add(new FloatingText("PARRY STANCE", elricX - 25f, elricY + 50f, Color.CYAN));
        }

        // Attack [SPACE] or [E] (50 DMG)
        if ((Gdx.input.isKeyJustPressed(Input.Keys.SPACE) || Gdx.input.isKeyJustPressed(Input.Keys.E))
                && elricAttackCooldown <= 0f) {
            elricAttackCooldown = 0.50f;
            float distToJane = (float) Math.hypot(elricX - janeX, elricY - janeY);
            if (distToJane <= 110f) {
                float dmg = ELRIC_DAMAGE;
                if (janeState == JaneState.STUNNED) dmg += 25f; // bonus damage on stunned boss
                janeHp -= dmg;
                janeHurtFlash = 0.25f;
                floatingTexts.add(new FloatingText("-" + (int) dmg + " HP", janeX - 20f, janeY + 60f, Color.YELLOW));
                if (janeHp <= 0f) {
                    janeHp = 0f;
                    isDuelOver = true;
                    elricWon = true;
                    postDuelTimer = 0f;
                    floatingTexts.add(new FloatingText("AGENT JANE SUBDUED!", janeX - 60f, janeY + 80f, Color.GOLD));
                }
            }
        }

        // Jane AI
        updateJaneAI(delta);

        // Update floating texts
        Iterator<FloatingText> it = floatingTexts.iterator();
        while (it.hasNext()) {
            FloatingText ft = it.next();
            ft.elapsed += delta;
            if (ft.elapsed >= 1.1f) it.remove();
        }
    }

    private void updateJaneAI(float delta) {
        float dist = (float) Math.hypot(elricX - janeX, elricY - janeY);

        switch (janeState) {
            case APPROACH -> {
                float speed = 195f;
                float angle = (float) Math.atan2(elricY - janeY, elricX - janeX);
                janeX += (float) Math.cos(angle) * speed * delta;
                janeY += (float) Math.sin(angle) * speed * delta;

                if (dist <= 100f) {
                    janeState = JaneState.WINDUP;
                    janeStateTimer = 0.55f; // Telegraph window before strike
                    floatingTexts.add(new FloatingText("!", janeX, janeY + 65f, Color.RED));
                }
            }
            case WINDUP -> {
                janeStateTimer -= delta;
                if (janeStateTimer <= 0f) {
                    // Strike!
                    executeJaneStrike();
                    janeState = JaneState.RECOVER;
                    janeStateTimer = 0.70f;
                }
            }
            case STRIKE -> { }
            case RECOVER -> {
                // Step slightly back after strike
                janeStateTimer -= delta;
                float angle = (float) Math.atan2(janeY - elricY, janeX - elricX);
                janeX = Math.min(1080f, Math.max(180f, janeX + (float) Math.cos(angle) * 75f * delta));
                janeY = Math.min(480f, Math.max(160f, janeY + (float) Math.sin(angle) * 75f * delta));

                if (janeStateTimer <= 0f) {
                    janeState = JaneState.APPROACH;
                }
            }
            case STUNNED -> {
                janeStateTimer -= delta;
                if (janeStateTimer <= 0f) {
                    janeState = JaneState.APPROACH;
                }
            }
        }
    }

    private void executeJaneStrike() {
        float dist = (float) Math.hypot(elricX - janeX, elricY - janeY);
        if (dist > 130f) {
            floatingTexts.add(new FloatingText("MISSED!", elricX, elricY + 50f, Color.WHITE));
            return;
        }

        // Check if player parried
        if (elricParryTimer > 0f) {
            // SUCCESSFUL PARRY!
            janeState = JaneState.STUNNED;
            janeStateTimer = 1.3f;
            floatingTexts.add(new FloatingText("PARRIED! JANE STUNNED!", elricX - 40f, elricY + 70f, Color.CYAN));
            return;
        }

        // Check if player dodged
        if (elricDodgeTimer > 0f) {
            // SUCCESSFUL DODGE!
            floatingTexts.add(new FloatingText("DODGED!", elricX, elricY + 60f, Color.LIME));
            return;
        }

        // Hit player for 60 DMG
        elricHp -= JANE_DAMAGE;
        elricHurtFlash = 0.35f;
        floatingTexts.add(new FloatingText("-" + (int) JANE_DAMAGE + " HP", elricX, elricY + 50f, Color.RED));

        if (elricHp <= 0f) {
            elricHp = 0f;
            isDuelOver = true;
            elricWon = false;
            postDuelTimer = 0f;
            floatingTexts.add(new FloatingText("ELRIC WAS DEFEATED", elricX - 50f, elricY + 70f, Color.FIREBRICK));
        }
    }

    private void drawHUD() {
        // Top Banner
        titleFont.setColor(0.910f, 0.690f, 0.165f, 1f);
        titleFont.draw(batch, "CLIMAX DUEL: RETRIEVE THE ANTIDOTE", 340f, 690f);

        // Elric HP Bar
        font.setColor(0.3f, 0.9f, 1.0f, 1f);
        font.draw(batch, "ELRIC (OSCORP OPERATIVE) — [50 DMG]", 80f, 640f);
        batch.setColor(0.2f, 0.2f, 0.2f, 0.8f);
        batch.draw(pixelTexture, 80f, 610f, 320f, 16f);
        batch.setColor(0.2f, 0.8f, 0.4f, 1.0f);
        batch.draw(pixelTexture, 80f, 610f, 320f * (elricHp / ELRIC_MAX_HP), 16f);
        font.setColor(Color.WHITE);
        font.draw(batch, (int) elricHp + " / " + (int) ELRIC_MAX_HP + " HP", 410f, 624f);

        // Jane HP Bar
        font.setColor(1.0f, 0.3f, 0.4f, 1f);
        font.draw(batch, "AGENT JANE (UNDERCOVER GOV) — [60 DMG]", 780f, 640f);
        batch.setColor(0.2f, 0.2f, 0.2f, 0.8f);
        batch.draw(pixelTexture, 780f, 610f, 360f, 16f);
        batch.setColor(0.9f, 0.2f, 0.25f, 1.0f);
        batch.draw(pixelTexture, 780f, 610f, 360f * (janeHp / JANE_MAX_HP), 16f);
        font.setColor(Color.WHITE);
        font.draw(batch, (int) janeHp + " / " + (int) JANE_MAX_HP + " HP", 1150f, 624f);

        // Bottom Controls Guide
        batch.setColor(0.01f, 0.02f, 0.03f, 0.85f);
        batch.draw(pixelTexture, 0f, 0f, WIDTH, 90f);
        batch.setColor(Color.WHITE);

        font.setColor(0.910f, 0.690f, 0.165f, 1f);
        font.draw(batch, "CONTROLS:", 60f, 65f);
        font.setColor(Color.WHITE);
        font.draw(batch, "[SPACE] Strike (50 DMG)    |    [F / Q] Parry (Timed Stun)    |    [SHIFT / X] Dodge Roll    |    [WASD] Move", 180f, 65f);

        font.setColor(0.7f, 0.75f, 0.8f, 1f);
        font.draw(batch, "TIP: Jane slashes for 60 DMG. Watch for the red flash (!) and PARRY to stun her for counter-attacks!", 180f, 35f);

        // Victory / Defeat Overlays
        if (isDuelOver) {
            batch.setColor(0f, 0f, 0f, 0.75f);
            batch.draw(pixelTexture, 0f, 0f, WIDTH, HEIGHT);
            batch.setColor(Color.WHITE);

            if (elricWon) {
                titleFont.setColor(Color.GOLD);
                titleFont.draw(batch, "AGENT JANE SUBDUED", 420f, 430f);
                font.setColor(Color.WHITE);
                font.draw(batch, "Elric secures the Antidote for Oscorp transport.", 430f, 370f);
                font.draw(batch, "The viral cure will be taken to headquarters. Press [E] to conclude.", 380f, 330f);
            } else {
                titleFont.setColor(Color.FIREBRICK);
                titleFont.draw(batch, "OPERATION COMPROMISED", 390f, 430f);
                font.setColor(Color.WHITE);
                font.draw(batch, "Jane's tactical blade overwhelmed Elric's defenses.", 420f, 370f);
                font.setColor(Color.YELLOW);
                font.draw(batch, "Press [R] to Retry the Duel", 500f, 320f);
            }
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
        if (backgroundTexture != null) backgroundTexture.dispose();
        if (femaleTexture != null) femaleTexture.dispose();
        if (elricTexture != null) elricTexture.dispose();
        if (pixelTexture != null) pixelTexture.dispose();
    }
}
