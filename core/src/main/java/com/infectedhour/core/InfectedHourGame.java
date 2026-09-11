package com.infectedhour.core;

import com.badlogic.gdx.Game;
import com.infectedhour.core.bridge.GameBridge;
import com.infectedhour.core.net.GameClient;
import com.infectedhour.core.net.GameServer;
import com.infectedhour.core.net.SessionConfig;
import com.infectedhour.core.screens.LevelBriefingScreen;
import com.infectedhour.shared.constants.GameConstants;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * libGDX application entry point. Booted by fx-launcher on a dedicated
 * thread (TRD §2) — NEVER constructed on the JavaFX Application Thread.
 *
 * <p>Holds the session's networking role. The host runs a {@link GameServer}
 * <em>and</em> a {@link GameClient} pointed at its own loopback; a joining
 * machine runs only the client. That symmetry means every screen below this
 * class reads the world through exactly one API — the client's interpolated
 * snapshot — whether or not this machine happens to be simulating it.
 */
public class InfectedHourGame extends Game {

    private static final Logger LOG = Logger.getLogger(InfectedHourGame.class.getName());

    private final SessionConfig session;
    private final GameBridge bridge;

    private GameServer server; // non-null only on the host
    private GameClient client;
    private boolean firstFrameReported;

    public InfectedHourGame(SessionConfig session, GameBridge bridge) {
        this.session = session;
        this.bridge = bridge;
    }

    @Override
    public void create() {
        if (session.host()) {
            server = new GameServer();
            server.setHostCharacter(session.preferredCharacter());
            try {
                server.start(session.displayName(), session.advertisedBackendUrl());
            } catch (IOException e) {
                // Ports busy (a stale game still running, or another host on this
                // machine). Tell the launcher instead of dying with a stack trace.
                LOG.log(Level.SEVERE, "Could not bind the game ports", e);
                server = null;
                bridge.notifyJoinFailed("PORTS_BUSY");
                com.badlogic.gdx.Gdx.app.exit();
                return;
            }
        }

        client = new GameClient(bridge);
        boolean connected = client.connect(session.effectiveHostAddress(),
                session.playerId(), session.displayName());
        if (!connected) {
            // connect() already reported the reason through the bridge.
            com.badlogic.gdx.Gdx.app.exit();
            return;
        }

        // Applied before the first tick, so the very first snapshot clients
        // receive already reflects the restored world — nobody ever sees the
        // pre-load state flash on screen.
        if (server != null && session.isLoadingSave()) {
            server.restoreFrom(session.loadedSlot());
        }

        bridge.setOnSaveConfirmed(slotNumber -> {
            com.badlogic.gdx.Gdx.app.postRunnable(() -> {
                if (server != null) {
                    server.broadcastEvent(GameConstants.EVENT_GAME_SAVED, String.valueOf(slotNumber));
                }
            });
        });

        if (session.host() && !session.isLoadingSave()) {
            setScreen(new com.infectedhour.core.screens.MainMenuScreen(this, client, bridge));
        } else {
            setScreen(new LevelBriefingScreen(this, client, bridge, session.startingLevel()));
        }
    }

    @Override
    public void render() {
        super.render();
        if (!firstFrameReported) {
            firstFrameReported = true;
            bridge.notifyGameReady();
        }
    }

    public boolean isHost() {
        return session.host();
    }

    public SessionConfig getSession() {
        return session;
    }

    public GameClient getClient() {
        return client;
    }

    public GameServer getServer() {
        return server;
    }

    public GameBridge getBridge() {
        return bridge;
    }

    /**
     * Drives the authoritative simulation from the render loop's accumulator
     * (TRD §4). One clock, one owner — the sim never gets its own thread.
     * No-op on a joining machine, which owns no world state.
     */
    public void stepSimulation(float delta) {
        if (server == null) {
            return;
        }
        simulationAccumulator += Math.min(delta, MAX_FRAME_SECONDS);
        while (simulationAccumulator >= GameServer.SIM_STEP_SECONDS) {
            server.fixedTimestepUpdate(GameServer.SIM_STEP_SECONDS);
            simulationAccumulator -= GameServer.SIM_STEP_SECONDS;
        }
    }

    private float simulationAccumulator = 0f;
    /** Clamp so one long frame (window drag, GC pause) cannot trigger a catch-up spiral. */
    private static final float MAX_FRAME_SECONDS = 0.25f;

    @Override
    public void dispose() {
        if (getScreen() != null) getScreen().dispose();
        if (client != null) client.disconnect();
        if (server != null) server.stop();
        bridge.onGameWindowClosed();
    }

    /** Exposed for the HUD's net-debug line. */
    public int getMaxPlayers() {
        return GameConstants.MAX_PLAYERS;
    }
}
