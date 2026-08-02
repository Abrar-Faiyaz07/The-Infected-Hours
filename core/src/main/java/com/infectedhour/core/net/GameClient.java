package com.infectedhour.core.net;

import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.infectedhour.core.bridge.GameBridge;
import com.infectedhour.shared.constants.GameConstants;
import com.infectedhour.shared.network.CharacterType;
import com.infectedhour.shared.network.EventMessage;
import com.infectedhour.shared.network.InputCommand;
import com.infectedhour.shared.network.JoinAccept;
import com.infectedhour.shared.network.JoinReject;
import com.infectedhour.shared.network.JoinRequest;
import com.infectedhour.shared.network.LevelTransition;
import com.infectedhour.shared.network.MatchMode;
import com.infectedhour.shared.network.MatchResultMessage;
import com.infectedhour.shared.network.WorldSnapshot;

import java.io.IOException;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The viewing half of the session (TRD §5). Runs on BOTH laptops — the host
 * runs one against its own loopback so there is a single rendering path for
 * everyone, and the join laptop runs one against the host's LAN address.
 *
 * <p>Sends {@link InputCommand} at {@code CLIENT_INPUT_SEND_HZ} (30) over UDP
 * and renders every entity — including its own player — from interpolated
 * snapshots. There is deliberately <b>no client-side prediction</b> in this
 * build: on a LAN the 50–100 ms of input delay is acceptable, and prediction +
 * reconciliation is the single hardest piece of networking code to get right
 * (TRD §5 marks it [STRETCH]).
 *
 * <h2>Failure is a UI state, not an exception</h2>
 * Nothing here throws on a network problem. A refused connection, a full
 * lobby, a version mismatch, or a vanished host all end up on
 * {@link GameBridge#notifyJoinFailed(String)}, which the launcher turns into a
 * dialog — TRD §9: "network failures surface as toasts, never crashes".
 */
public class GameClient {

    private static final Logger LOG = Logger.getLogger(GameClient.class.getName());

    /** Mirrors GameServer's buffers — both ends must be able to hold a whole snapshot. */
    private static final int WRITE_BUFFER_BYTES = 65536;
    private static final int OBJECT_BUFFER_BYTES = 16384;

    private final Client client;
    private final GameBridge bridge;
    private final SnapshotInterpolator interpolator =
            new SnapshotInterpolator(GameConstants.INTERPOLATION_BUFFER_MS);

    private volatile String localPlayerId;
    private volatile CharacterType localCharacter;
    private volatile String backendUrl;
    private volatile String hostDisplayName;
    private volatile MatchMode matchMode = MatchMode.SOLO;
    private volatile boolean joinAccepted = false;
    /** True once we asked to leave, so the disconnect callback stays quiet. */
    private volatile boolean shuttingDown = false;

    private volatile Consumer<JoinAccept> onJoinAccepted = accept -> {
    };
    private volatile Consumer<EventMessage> onEvent = event -> {
    };
    private volatile Consumer<LevelTransition> onLevelTransition = transition -> {
    };

    private float inputSendAccumulator = 0f;
    private long clientTick = 0;
    private static final float INPUT_SEND_INTERVAL = 1f / GameConstants.CLIENT_INPUT_SEND_HZ;

    public GameClient(GameBridge bridge) {
        this.bridge = bridge != null ? bridge : new GameBridge();
        this.client = new Client(WRITE_BUFFER_BYTES, OBJECT_BUFFER_BYTES);
    }

    // ------------------------------------------------------------------
    // Connect / handshake
    // ------------------------------------------------------------------

    /**
     * Opens the KryoNet session and performs the join handshake.
     *
     * @param hostAddress "localhost" on the host laptop, the host's LAN IPv4 on the other
     * @param playerId    stable identity (backend player id, or a generated one offline)
     * @param displayName shown in the partner's lobby
     * @return true if the transport connected. The <b>seat</b> is only confirmed once
     *         {@link JoinAccept} arrives — see {@link #isJoinAccepted()} — because the
     *         host can still reject a connected client (full lobby, wrong version).
     */
    public boolean connect(String hostAddress, String playerId, String displayName) {
        NetworkRegistration.register(client);

        client.addListener(new Listener() {
            @Override
            public void received(Connection connection, Object object) {
                GameClient.this.received(object);
            }

            @Override
            public void disconnected(Connection connection) {
                GameClient.this.onDisconnected();
            }
        });

        client.start();
        try {
            client.connect(GameConstants.CONNECT_TIMEOUT_MS, hostAddress,
                    GameConstants.KRYONET_TCP_PORT, GameConstants.KRYONET_UDP_PORT);
        } catch (IOException e) {
            LOG.log(Level.WARNING, e, () -> "Could not reach a host at " + hostAddress);
            client.stop();
            bridge.notifyJoinFailed(GameConstants.REJECT_TIMEOUT);
            return false;
        }

        // TCP so the handshake cannot be silently dropped the way a UDP packet can.
        client.sendTCP(new JoinRequest(playerId, displayName, GameConstants.PROTOCOL_VERSION));
        LOG.info(() -> "Connected to " + hostAddress + "; join request sent as \"" + displayName + "\"");
        return true;
    }

    private void received(Object object) {
        if (object instanceof WorldSnapshot snapshot) {
            interpolator.pushSnapshot(snapshot);

        } else if (object instanceof JoinAccept accept) {
            joinAccepted = true;
            localPlayerId = accept.assignedPlayerId;
            localCharacter = accept.assignedCharacter;
            backendUrl = accept.backendUrl;
            hostDisplayName = accept.hostDisplayName;
            matchMode = accept.matchMode != null ? accept.matchMode : MatchMode.SOLO;
            LOG.info(() -> "Join accepted as " + localCharacter + " (" + localPlayerId + ")");
            onJoinAccepted.accept(accept);

        } else if (object instanceof JoinReject reject) {
            LOG.warning(() -> "Join rejected: " + reject.reason);
            shuttingDown = true;
            bridge.notifyJoinFailed(reject.reason != null ? reject.reason : "UNKNOWN");
            disconnect();

        } else if (object instanceof LevelTransition transition) {
            onLevelTransition.accept(transition);

        } else if (object instanceof MatchResultMessage result) {
            bridge.notifyMatchEnded(new GameBridge.MatchOutcome(result.result, result.finalLevelReached));

        } else if (object instanceof EventMessage event) {
            handleEvent(event);
        }
    }

    private void handleEvent(EventMessage event) {
        if (event.type != null) {
            switch (event.type) {
                // JoinAccept.matchMode is only a snapshot of the roster at the
                // instant THIS machine joined. The host joins its own loopback
                // first, so without these updates the host would believe it is
                // SOLO for the whole session and skip every partner gate.
                case GameConstants.EVENT_PARTNER_JOINED,
                     GameConstants.EVENT_PARTNER_RECONNECTED -> {
                    matchMode = MatchMode.COOP;
                    bridge.notifyPartnerEvent(new GameBridge.PartnerEvent(event.type, event.payload));
                }
                case GameConstants.EVENT_CONVERTED_TO_SOLO -> {
                    matchMode = MatchMode.SOLO;
                    bridge.notifyPartnerEvent(new GameBridge.PartnerEvent(event.type, event.payload));
                }
                case GameConstants.EVENT_PARTNER_DISCONNECTED ->
                        bridge.notifyPartnerEvent(new GameBridge.PartnerEvent(event.type, event.payload));
                default -> {
                    // gameplay events (READY, OBJECTIVE_COMPLETE, PAUSE/RESUME) are
                    // screen concerns — they go to whoever subscribed
                }
            }
        }
        onEvent.accept(event);
    }

    private void onDisconnected() {
        if (shuttingDown) {
            return; // we asked for this
        }
        LOG.warning("Lost the connection to the host");
        bridge.notifyJoinFailed(GameConstants.REJECT_HOST_LOST);
    }

    // ------------------------------------------------------------------
    // Sending
    // ------------------------------------------------------------------

    /** Call every render frame; internally rate-limits actual sends to CLIENT_INPUT_SEND_HZ. */
    public void sendInputIfDue(InputCommand input, float delta) {
        inputSendAccumulator += delta;
        if (inputSendAccumulator < INPUT_SEND_INTERVAL) {
            return;
        }
        inputSendAccumulator -= INPUT_SEND_INTERVAL;
        if (!client.isConnected()) {
            return;
        }
        input.clientTick = ++clientTick;
        // UDP: a lost input frame is replaced 33ms later, so retransmitting it
        // would only add latency.
        client.sendUDP(input);
    }

    /** Reliable, ordered event to the host (READY, PAUSE, objective interactions). */
    public void sendEvent(String type, String payload) {
        if (client.isConnected()) {
            client.sendTCP(new EventMessage(type, payload));
        }
    }

    // ------------------------------------------------------------------
    // Rendering + state
    // ------------------------------------------------------------------

    /** Renderer calls this each frame to get the interpolated world state to draw. May be null before the first packet. */
    public WorldSnapshot getInterpolatedSnapshot(long renderTimeMs) {
        return interpolator.getInterpolated(renderTimeMs);
    }

    /** This machine's own entry in the snapshot — camera target and HUD source. */
    public WorldSnapshot.PlayerState findLocalPlayer(WorldSnapshot snapshot) {
        if (snapshot == null || snapshot.players == null || localPlayerId == null) {
            return null;
        }
        for (WorldSnapshot.PlayerState state : snapshot.players) {
            if (localPlayerId.equals(state.playerId)) {
                return state;
            }
        }
        return null;
    }

    public void setOnJoinAccepted(Consumer<JoinAccept> callback) {
        this.onJoinAccepted = callback != null ? callback : accept -> { };
    }

    public void setOnEvent(Consumer<EventMessage> callback) {
        this.onEvent = callback != null ? callback : event -> { };
    }

    public void setOnLevelTransition(Consumer<LevelTransition> callback) {
        this.onLevelTransition = callback != null ? callback : transition -> { };
    }

    public boolean isConnected() {
        return client.isConnected();
    }

    public boolean isJoinAccepted() {
        return joinAccepted;
    }

    public String getLocalPlayerId() {
        return localPlayerId;
    }

    public CharacterType getLocalCharacter() {
        return localCharacter;
    }

    public String getBackendUrl() {
        return backendUrl;
    }

    public String getHostDisplayName() {
        return hostDisplayName;
    }

    public MatchMode getMatchMode() {
        return matchMode;
    }

    public void disconnect() {
        shuttingDown = true;
        client.stop();
        client.close();
    }
}
