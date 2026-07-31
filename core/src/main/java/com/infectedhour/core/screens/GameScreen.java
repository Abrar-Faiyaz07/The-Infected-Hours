package com.infectedhour.core.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.infectedhour.core.InfectedHourGame;
import com.infectedhour.core.bridge.GameBridge;
import com.infectedhour.core.net.GameClient;
import com.infectedhour.shared.constants.GameConstants;
import com.infectedhour.shared.network.CharacterType;
import com.infectedhour.shared.network.InputCommand;
import com.infectedhour.shared.network.WorldSnapshot;

/**
 * Core gameplay loop screen (App Flow §2): explore -> objectives -> manage
 * contamination -> clear/fail.
 *
 * <p>This screen is the clearest statement of the networking model. It does
 * exactly three things per frame, in this order:
 * <ol>
 *   <li><b>Step the simulation</b> — host only, via the fixed-timestep
 *       accumulator. A joining machine skips this entirely; it owns no world.</li>
 *   <li><b>Send intent</b> — the local keyboard becomes an
 *       {@link InputCommand}, rate-limited to 30 Hz. It is never applied
 *       locally; the host decides what it means.</li>
 *   <li><b>Draw the interpolated snapshot</b> — including this machine's own
 *       player. There is no local-vs-remote rendering split, which is what
 *       "no client-side prediction" actually buys: one code path, and the two
 *       laptops cannot disagree about what the world looks like.</li>
 * </ol>
 *
 * <p>Placeholder art: entities are drawn as coloured quads. Swapping in the
 * texture atlas changes only {@link #drawWorld}, nothing about the networking.
 */
public class GameScreen implements Screen {

    /** World units are tiles; this many screen pixels per tile. */
    private static final float PIXELS_PER_TILE = 48f;

    private final InfectedHourGame game;
    private final GameClient client;
    private final GameBridge bridge;
    private final int levelNumber;

    private SpriteBatch batch;
    private ShapeRenderer shapes;
    private BitmapFont font;

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

        // ================ TEAMMATE TASK: LEVEL SETUP ================
        // TODO(screens): load the level and create the world:
        //  1. var def = new LevelLoader().loadDefinition(levelNumber);
        //     levelLoader.loadMap(def);   // implement core/level first!
        //  2. Register objectives on the HOST's ObjectiveSystem:
        //     game.getServer().getObjectiveSystem().register(...)
        //     — clients receive them in WorldSnapshot.objectives, they never
        //     register their own.
        //  3. OrthogonalTiledMapRenderer for the map + the texture atlas.
        // ============================================================

        client.setOnEvent(event -> {
            if (event.type == null) {
                return;
            }
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

        // 1. Authoritative step (host only). Runs even while this machine shows
        //    its pause overlay — only the HOST truly pauses the sim, and it does
        //    that inside GameServer, not here.
        game.stepSimulation(delta);

        Gdx.gl.glClearColor(0.055f, 0.078f, 0.125f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // 2. Intent out.
        if (!paused) {
            client.sendInputIfDue(readLocalInput(), delta);
        }

        // 3. World in. Null until the first snapshot lands — always guard it.
        WorldSnapshot snapshot = client.getInterpolatedSnapshot(System.currentTimeMillis());
        if (snapshot != null) {
            drawWorld(snapshot);
        }
        drawHud(snapshot, delta);

        if (paused) {
            drawPauseOverlay();
        }
    }

    /**
     * Camera-less flat draw of the interpolated snapshot.
     *
     * <p>TEAMMATE TASK (screens): replace the quads with sprites and add a
     * camera that follows {@code client.findLocalPlayer(snapshot)}. Read
     * positions from the snapshot only — never from a local entity — or the
     * two laptops will drift apart.
     */
    private void drawWorld(WorldSnapshot snapshot) {
        shapes.begin(ShapeRenderer.ShapeType.Filled);

        if (snapshot.enemies != null) {
            shapes.setColor(0.482f, 0.310f, 0.651f, 1f); // toxic-purple #7B4FA6
            for (WorldSnapshot.EnemyState enemy : snapshot.enemies) {
                shapes.rect(enemy.x * PIXELS_PER_TILE, enemy.y * PIXELS_PER_TILE,
                        PIXELS_PER_TILE * 0.8f, PIXELS_PER_TILE * 0.8f);
            }
        }

        if (snapshot.players != null) {
            for (WorldSnapshot.PlayerState player : snapshot.players) {
                if (player.downed) {
                    shapes.setColor(0.4f, 0.4f, 0.4f, 1f);
                } else if (player.character == CharacterType.ELRIC) {
                    shapes.setColor(0.298f, 0.686f, 0.427f, 1f); // safe-green
                } else {
                    shapes.setColor(0.910f, 0.690f, 0.165f, 1f); // accent-gold
                }
                shapes.rect(player.x * PIXELS_PER_TILE, player.y * PIXELS_PER_TILE,
                        PIXELS_PER_TILE, PIXELS_PER_TILE);
            }
        }
        shapes.end();
    }

    private void drawHud(WorldSnapshot snapshot, float delta) {
        float top = Gdx.graphics.getHeight() - 20f;

        if (partnerBannerSecondsLeft > 0f) {
            partnerBannerSecondsLeft -= delta;
        }

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
            font.setColor(0.910f, 0.353f, 0.310f, 1f); // accent-red
            font.draw(batch, partnerBanner, 20f, top - 80f);
        }

        font.setColor(Color.GRAY);
        font.draw(batch, "WASD move   E interact   SPACE attack   SHIFT ability   ESC pause", 20f, 30f);
        batch.end();

        // ============== TEAMMATE TASK: REAL HUD ==============
        // TODO(ui): replace this debug text with the Scene2D widgets from
        // UI/UX doc §3 — health bar, contamination bar, global meter,
        // 4-slot hotbar, minimap. Feed them from the same snapshot fields
        // used above; do not add a second source of truth.
        // =====================================================
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

        // ============== TEAMMATE TASK: PAUSE PANEL ==============
        // TODO(screens): UI/UX doc §2 screen 12 — Resume, Settings (volume),
        // Abandon Match (host) / Leave (client). The PAUSE/RESUME event is
        // already sent above so the partner sees "Host paused".
        // ========================================================
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
    }
}
