package com.infectedhour.core;

import com.badlogic.gdx.Game;
import com.infectedhour.core.bridge.GameBridge;
import com.infectedhour.core.net.GameClient;
import com.infectedhour.core.net.GameServer;
import com.infectedhour.core.screens.LevelBriefingScreen;

/**
 * libGDX application entry point. Booted by fx-launcher on a dedicated
 * thread (TRD §2) — NEVER constructed on the JavaFX Application Thread.
 *
 * Holds the session's networking role (host runs {@link GameServer} + a
 * local client; a pure client runs only {@link GameClient}) and the
 * {@link GameBridge} used to talk back to the JavaFX window on match end.
 */
public class InfectedHourGame extends Game {

    private final boolean isHost;
    private final String hostAddressIfClient; // null when isHost
    private final GameBridge bridge;

    private GameServer server; // non-null only when isHost
    private GameClient client; // always non-null once connected

    public InfectedHourGame(boolean isHost, String hostAddressIfClient, GameBridge bridge) {
        this.isHost = isHost;
        this.hostAddressIfClient = hostAddressIfClient;
        this.bridge = bridge;
    }

    @Override
    public void create() {
        if (isHost) {
            server = new GameServer();
            server.start();
        }
        client = new GameClient(bridge);
        client.connect(isHost ? "localhost" : hostAddressIfClient);

        setScreen(new LevelBriefingScreen(this, client, bridge, 1));
    }

    public boolean isHost() {
        return isHost;
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

    @Override
    public void dispose() {
        if (getScreen() != null) getScreen().dispose();
        if (client != null) client.disconnect();
        if (server != null) server.stop();
        bridge.onGameWindowClosed();
    }
}
