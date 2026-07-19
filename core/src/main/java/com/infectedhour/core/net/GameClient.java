package com.infectedhour.core.net;

import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Listener;
import com.infectedhour.core.bridge.GameBridge;
import com.infectedhour.shared.constants.GameConstants;
import com.infectedhour.shared.network.*;

import java.io.IOException;

/**
 * Runs on both laptops: the host runs one internally (connecting to its own
 * localhost server) as well as the client. Sends InputCommand at
 * CLIENT_INPUT_SEND_HZ (30) and renders via snapshot interpolation — NO
 * client-side prediction in this build (TRD §5).
 */
public class GameClient {

    private final Client client = new Client();
    private final GameBridge bridge;
    private final SnapshotInterpolator interpolator = new SnapshotInterpolator(GameConstants.INTERPOLATION_BUFFER_MS);

    private float inputSendAccumulator = 0f;
    private static final float INPUT_SEND_INTERVAL = 1f / GameConstants.CLIENT_INPUT_SEND_HZ;

    public GameClient(GameBridge bridge) {
        this.bridge = bridge;
    }

    public void connect(String hostAddress) {
        NetworkRegistration.register(client);

        client.addListener(new Listener() {
            @Override
            public void received(com.esotericsoftware.kryonet.Connection connection, Object object) {
                if (object instanceof WorldSnapshot snapshot) {
                    interpolator.pushSnapshot(snapshot);
                } else if (object instanceof LevelTransition transition) {
                    onLevelTransition(transition);
                } else if (object instanceof MatchResultMessage result) {
                    bridge.notifyMatchEnded(new GameBridge.MatchOutcome(result.result, result.finalLevelReached));
                } else if (object instanceof JoinReject reject) {
                    // ============ TEAMMATE TASK: JOIN REJECTED ============
                    // TODO(net): show reject.reason to the player, never crash.
                    //  - "LOBBY_FULL"       -> toast "Lobby is full"
                    //  - "VERSION_MISMATCH" -> "Update required on this machine"
                    //  Then disconnect() and return to the JavaFX lobby via the
                    //  bridge (add a callback like bridge.notifyJoinFailed(reason)).
                    // ======================================================
                }
            }

            @Override
            public void disconnected(com.esotericsoftware.kryonet.Connection connection) {
                // ============ TEAMMATE TASK: HOST DROPPED ============
                // TODO(net): App Flow par.3 — host drop handling.
                //  1. Show a "Connection to host lost" overlay for ~3s.
                //  2. Dispose the libGDX window (Gdx.app.exit()) — the
                //     GameBridge.onGameWindowClosed callback re-shows the
                //     JavaFX lobby automatically.
                //  3. Pass an error flag through the bridge so the lobby
                //     shows the red "Lost" connection chip (UI/UX doc par.7).
                // =====================================================
            }
        });

        try {
            client.start();
            client.connect(5000, hostAddress, GameConstants.KRYONET_TCP_PORT, GameConstants.KRYONET_UDP_PORT);
        } catch (IOException e) {
            throw new RuntimeException("Failed to connect GameClient to " + hostAddress, e);
        }
    }

    private void onLevelTransition(LevelTransition transition) {
        // ============ TEAMMATE TASK: LEVEL TRANSITION ============
        // TODO(net): react to the host's transition message.
        //  - Must run on the render thread: Gdx.app.postRunnable(() -> ...)
        //  - game.setScreen(new LevelBriefingScreen(game, this, bridge,
        //        transition.nextLevelNumber));
        //    (LevelBriefingScreen already routes level 3 to BossScreen.)
        //  - You need a Game reference here — pass it into GameClient's
        //    constructor or add a setter.
        // =========================================================
    }

    /** Call every render frame; internally rate-limits actual sends to CLIENT_INPUT_SEND_HZ. */
    public void sendInputIfDue(InputCommand input, float delta) {
        inputSendAccumulator += delta;
        if (inputSendAccumulator >= INPUT_SEND_INTERVAL) {
            inputSendAccumulator -= INPUT_SEND_INTERVAL;
            client.sendUDP(input);
        }
    }

    /** Renderer calls this each frame to get the interpolated world state to draw. */
    public WorldSnapshot getInterpolatedSnapshot(long renderTimeMs) {
        return interpolator.getInterpolated(renderTimeMs);
    }

    public void disconnect() {
        client.stop();
        client.close();
    }
}
