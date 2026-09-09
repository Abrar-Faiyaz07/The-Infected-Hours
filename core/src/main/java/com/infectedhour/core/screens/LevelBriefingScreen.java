package com.infectedhour.core.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.infectedhour.core.InfectedHourGame;
import com.infectedhour.core.bridge.GameBridge;
import com.infectedhour.core.level.LevelDefinition;
import com.infectedhour.core.level.LevelLoader;
import com.infectedhour.core.net.GameClient;
import com.infectedhour.shared.constants.GameConstants;
import com.infectedhour.shared.network.MatchMode;

/**
 * "Mission list with icons, controls reminder, Both players ready sync gate"
 * (UI/UX doc §2, screen 10).
 *
 * <p>This is where the reliable (TCP) half of the protocol earns its keep: the
 * ready gate must not be able to lose a message, or one laptop sits waiting
 * forever while the other has already started. READY travels as an
 * {@link com.infectedhour.shared.network.EventMessage} over TCP, which the
 * host relays to the partner.
 */
public class LevelBriefingScreen implements Screen {

    private final InfectedHourGame game;
    private final GameClient client;
    private final GameBridge bridge;
    private final int levelNumber;
    private final LevelDefinition definition;

    private SpriteBatch batch;
    private BitmapFont font;
    private BitmapFont titleFont;

    private volatile boolean localReady = false;
    /** Set only by a real READY event — never inferred from the match mode. */
    private volatile boolean partnerReadyReceived = false;
    private float elapsed = 0f;

    public LevelBriefingScreen(InfectedHourGame game, GameClient client, GameBridge bridge, int levelNumber) {
        this.game = game;
        this.client = client;
        this.bridge = bridge;
        this.levelNumber = levelNumber;
        this.definition = new LevelLoader().loadDefinition(levelNumber);
    }

    @Override
    public void show() {
        batch = new SpriteBatch();
        font = new BitmapFont();
        titleFont = new BitmapFont();
        titleFont.getData().setScale(2f);

        // The partner's READY arrives on KryoNet's thread; flipping a volatile
        // boolean is all that happens there — the render thread reads it next frame.
        client.setOnEvent(event -> {
            if (GameConstants.EVENT_READY.equals(event.type)) {
                partnerReadyReceived = true;
            }
        });
    }

    @Override
    public void render(float delta) {
        elapsed += delta;

        // The host owns the clock even on a briefing screen — keeping the sim
        // stepping here means snapshots keep flowing and the joining laptop can
        // already see it is connected.
        game.stepSimulation(delta);

        // Recomputed every frame, never latched: the host joins its own loopback
        // before the partner exists, so it starts in SOLO and flips to COOP when
        // the partner arrives. Latching "solo means partner is ready" would let
        // the host start alone and strand the partner on this screen.
        boolean partnerGateOpen = partnerReadyReceived || client.getMatchMode() == MatchMode.SOLO;

        if (!localReady && (Gdx.input.isKeyJustPressed(Input.Keys.E)
                || Gdx.input.isKeyJustPressed(Input.Keys.ENTER)
                || Gdx.input.isKeyJustPressed(Input.Keys.SPACE))) {
            localReady = true;
            client.sendEvent(GameConstants.EVENT_READY, String.valueOf(levelNumber));
        }

        Gdx.gl.glClearColor(0.055f, 0.078f, 0.125f, 1f); // bg-night #0E1420
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        float width = Gdx.graphics.getWidth();
        float top = Gdx.graphics.getHeight() - 60f;

        batch.begin();
        titleFont.setColor(0.910f, 0.690f, 0.165f, 1f); // accent-gold #E8B02A
        titleFont.draw(batch, "LEVEL " + levelNumber + " — " + definition.name().toUpperCase(), 60f, top);

        font.setColor(Color.WHITE);
        font.draw(batch, connectionLine(), 60f, top - 60f);
        font.draw(batch, "Controls:  WASD move  |  E interact  |  SPACE attack  |  SHIFT ability  |  ESC pause",
                60f, top - 90f);

        font.setColor(0.910f, 0.690f, 0.165f, 1f);
        font.draw(batch, "MISSION", 60f, top - 130f);
        font.setColor(Color.WHITE);
        String[] mission = missionLines();
        for (int i = 0; i < mission.length; i++) {
            font.draw(batch, "•  " + mission[i], 60f, top - 158f - i * 24f);
        }

        font.setColor(localReady ? Color.LIME : Color.WHITE);
        font.draw(batch, localReady ? "You: READY" : "Press E / ENTER when you are ready", 60f, top - 260f);

        font.setColor(partnerGateOpen ? Color.LIME : Color.LIGHT_GRAY);
        font.draw(batch, partnerLine(), 60f, top - 290f);

        font.setColor(Color.GRAY);
        font.draw(batch, netDebugLine(), 60f, 40f);
        batch.end();

        if (localReady && partnerGateOpen) {
            advance();
        }
    }

    private String connectionLine() {
        if (!client.isJoinAccepted()) {
            return "Connecting to host…";
        }
        return "Connected as " + client.getLocalCharacter()
                + "  |  host: " + client.getHostDisplayName()
                + "  |  mode: " + client.getMatchMode();
    }

    private String partnerLine() {
        if (client.getMatchMode() == MatchMode.SOLO) {
            return "Solo session — no partner to wait for.";
        }
        if (partnerReadyReceived) {
            return "Partner: READY";
        }
        // UX rule 1: never block silently on a network wait.
        int dots = ((int) (elapsed * 2f)) % 4;
        return "Waiting for partner" + ".".repeat(dots);
    }

    private String netDebugLine() {
        return "tcp " + GameConstants.KRYONET_TCP_PORT
                + "  udp " + GameConstants.KRYONET_UDP_PORT
                + "  |  " + (client.isConnected() ? "link up" : "link down")
                + (game.isHost() ? "  |  hosting" : "");
    }

    private String[] missionLines() {
        return switch (levelNumber) {
            case 1 -> new String[]{
                    "Search Ashgrove Hospital and reach the upstairs evacuation sign.",
                    "Survive the infected patients blocking the route."
            };
            case 2 -> new String[]{
                    "Clear the zombie patrol outside the hospital.",
                    "Rescue two villagers stranded beside the road.",
                    "Solve both power-relay puzzles and enter the service tunnel."
            };
            case 3 -> new String[]{
                    "Descend into the hidden laboratory.",
                    "Break the Virus Heart's shield, attack its core, and finish the outbreak."
            };
            default -> new String[]{"Survive and complete the operation."};
        };
    }

    private void advance() {
        if (levelNumber == 1) {
            game.setScreen(new StoryPanelScreen(game, client, bridge, StoryPanelScreen.Sequence.INTRO, levelNumber));
        } else if (levelNumber == GameConstants.BOSS_LEVEL_NUMBER) {
            game.setScreen(new BossScreen(game, client, bridge));
        } else {
            game.setScreen(new GameScreen(game, client, bridge, levelNumber));
        }
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
        if (font != null) font.dispose();
        if (titleFont != null) titleFont.dispose();
    }
}
