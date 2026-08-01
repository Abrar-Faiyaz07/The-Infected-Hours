package com.infectedhour.core.net;

import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Server;
import com.infectedhour.core.entities.ContaminationZone;
import com.infectedhour.core.entities.Enemy;
import com.infectedhour.core.entities.Player;
import com.infectedhour.core.systems.AISystem;
import com.infectedhour.core.systems.CombatSystem;
import com.infectedhour.core.systems.ContaminationSystem;
import com.infectedhour.core.systems.MovementSystem;
import com.infectedhour.core.systems.ObjectiveSystem;
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * The authoritative half of the session (TRD §5). Runs on the host laptop
 * (Laptop A / Elric) and owns the only copy of the world that matters:
 *
 * <ul>
 *   <li>simulates at {@code SIMULATION_TICK_HZ} (60)</li>
 *   <li>broadcasts {@link WorldSnapshot} over UDP at {@code SNAPSHOT_BROADCAST_HZ} (20)</li>
 *   <li>receives {@link InputCommand} over UDP at {@code CLIENT_INPUT_SEND_HZ} (30)</li>
 *   <li>carries joins, events and results over TCP, where ordering and delivery matter</li>
 * </ul>
 *
 * <h2>Both players go through the same door</h2>
 * The host runs a {@link GameClient} against its own loopback, so the host's
 * own player is created by exactly the same {@link JoinRequest} handshake as
 * the remote player. There is no "local player" special case anywhere in this
 * class — the first connection to hand in a valid JoinRequest becomes ELRIC,
 * the second becomes JANE.
 *
 * <h2>Threading</h2>
 * KryoNet delivers callbacks on its own update thread; the simulation runs on
 * the libGDX render thread via {@link #fixedTimestepUpdate(float)}. The two
 * meet at exactly three places, and each is handled explicitly:
 * <ol>
 *   <li>joins/disconnects mutate the roster — guarded by {@link #rosterLock},
 *       so "is there room?" and "take the slot" cannot interleave;</li>
 *   <li>the roster map is a {@link ConcurrentHashMap}, so the sim thread can
 *       iterate it while a join is in flight;</li>
 *   <li>each connection's latest input is a single volatile reference — a
 *       torn read is impossible and a dropped input is simply last-writer-wins,
 *       which is the correct behaviour for a 30Hz input stream anyway.</li>
 * </ol>
 * World state (enemies, clouds, objectives) is touched only by the sim thread.
 */
public class GameServer {

    private static final Logger LOG = Logger.getLogger(GameServer.class.getName());

    /**
     * KryoNet's no-arg {@code Server()} allocates a 2048-byte object buffer.
     * A snapshot carrying two players, a dozen enemies and a cloud delta blows
     * straight past that and dies with "Buffer overflow", so the buffers are
     * sized against the TRD §8 budget (snapshot ≤ 8 KB) with headroom.
     */
    private static final int WRITE_BUFFER_BYTES = 65536;
    private static final int OBJECT_BUFFER_BYTES = 16384;

    private final Server server;
    private final Map<Integer, ConnectedPlayer> playersByConnectionId = new ConcurrentHashMap<>();
    private final Object rosterLock = new Object();

    private final MovementSystem movementSystem = new MovementSystem();
    private final CombatSystem combatSystem = new CombatSystem();
    private final ContaminationSystem contaminationSystem = new ContaminationSystem();
    private final ObjectiveSystem objectiveSystem = new ObjectiveSystem();
    private final AISystem aiSystem = new AISystem();

    // --- world state: sim thread only ---
    private final List<Enemy> enemies = new ArrayList<>();
    private final List<ContaminationZone> zones = new ArrayList<>();
    private int mapWidthInTiles = 64;

    /** Cloud tiles already sent, per cloud, so snapshots can carry deltas instead of whole tile sets. */
    private final Map<String, Set<Integer>> sentCloudTiles = new LinkedHashMap<>();

    private String hostDisplayName = "Host";
    private String backendUrl = "http://localhost:" + GameConstants.DEFAULT_BACKEND_PORT;

    private long serverTick = 0;
    private float snapshotAccumulator = 0f;
    private volatile boolean running = false;

    /** Set when a player drops mid-match; the sim is frozen until it expires or they return. */
    private float reconnectWaitRemainingSeconds = 0f;
    private volatile boolean paused = false;

    private volatile SessionListener sessionListener = SessionListener.NO_OP;

    private static final float SIM_STEP = 1f / GameConstants.SIMULATION_TICK_HZ;
    private static final float SNAPSHOT_INTERVAL = 1f / GameConstants.SNAPSHOT_BROADCAST_HZ;

    public GameServer() {
        this.server = new Server(WRITE_BUFFER_BYTES, OBJECT_BUFFER_BYTES);
    }

    /** Lets the Host Lobby react to roster changes. Called from KryoNet's thread — marshal before touching UI. */
    public interface SessionListener {
        SessionListener NO_OP = new SessionListener() {
        };

        default void onPlayerJoined(String playerId, String displayName, CharacterType character, MatchMode mode) {
        }

        default void onPlayerLeft(String playerId, String displayName) {
        }

        default void onConvertedToSolo() {
        }
    }

    /** One connected participant: their identity, their entity, and their most recent input. */
    private static final class ConnectedPlayer {
        final int connectionId;
        final String playerId;
        final String displayName;
        final Player entity;
        volatile InputCommand latestInput = new InputCommand();

        ConnectedPlayer(int connectionId, String playerId, String displayName, CharacterType character) {
            this.connectionId = connectionId;
            this.playerId = playerId;
            this.displayName = displayName;
            this.entity = new Player(playerId, character);
        }
    }

    // ------------------------------------------------------------------
    // Lifecycle
    // ------------------------------------------------------------------

    /**
     * Binds TCP 54555 + UDP 54777 and starts accepting joins.
     *
     * @param hostDisplayName name shown to the joining player
     * @param backendUrl      absolute URL of the Spring Boot instance; handed to the
     *                        client in {@link JoinAccept} so BOTH laptops write saves
     *                        to the same backend (TRD §6)
     * @throws IOException if the ports are taken — the caller shows this in the
     *                     lobby rather than crashing (TRD §9)
     */
    public void start(String hostDisplayName, String backendUrl) throws IOException {
        if (running) {
            return;
        }
        if (hostDisplayName != null && !hostDisplayName.isBlank()) {
            this.hostDisplayName = hostDisplayName;
        }
        if (backendUrl != null && !backendUrl.isBlank()) {
            this.backendUrl = backendUrl;
        }

        NetworkRegistration.register(server);
        server.addListener(new Listener() {
            @Override
            public void received(Connection connection, Object object) {
                if (object instanceof InputCommand input) {
                    onInput(connection, input);
                } else if (object instanceof JoinRequest joinRequest) {
                    onJoinRequest(connection, joinRequest);
                } else if (object instanceof EventMessage event) {
                    onClientEvent(connection, event);
                }
            }

            @Override
            public void disconnected(Connection connection) {
                onDisconnected(connection);
            }
        });

        server.bind(GameConstants.KRYONET_TCP_PORT, GameConstants.KRYONET_UDP_PORT);
        server.start();
        running = true;
        LOG.info(() -> "GameServer listening on TCP " + GameConstants.KRYONET_TCP_PORT
                + " / UDP " + GameConstants.KRYONET_UDP_PORT + " as \"" + this.hostDisplayName + "\"");
    }

    /** Convenience overload for the dev shortcut ({@code ./gradlew lwjgl3:run}). */
    public void start() throws IOException {
        start(hostDisplayName, backendUrl);
    }

    public void stop() {
        running = false;
        server.stop();
        server.close();
        LOG.info("GameServer stopped");
    }

    public boolean isRunning() {
        return running;
    }

    public void setSessionListener(SessionListener listener) {
        this.sessionListener = listener != null ? listener : SessionListener.NO_OP;
    }

    // ------------------------------------------------------------------
    // Handshake
    // ------------------------------------------------------------------

    private void onJoinRequest(Connection connection, JoinRequest joinRequest) {
        // Version first: a client built against a different message layout would
        // otherwise fail later with an unreadable Kryo error.
        if (joinRequest.protocolVersion != GameConstants.PROTOCOL_VERSION) {
            LOG.warning(() -> "Rejecting " + joinRequest.displayName + ": protocol "
                    + joinRequest.protocolVersion + " != " + GameConstants.PROTOCOL_VERSION);
            connection.sendTCP(new JoinReject(GameConstants.REJECT_VERSION_MISMATCH));
            return;
        }

        ConnectedPlayer joined;
        MatchMode mode;
        synchronized (rosterLock) {
            if (playersByConnectionId.containsKey(connection.getID())) {
                return; // duplicate JoinRequest — ignore, already seated
            }
            if (playersByConnectionId.size() >= GameConstants.MAX_PLAYERS) {
                LOG.warning(() -> "Rejecting " + joinRequest.displayName + ": lobby full");
                connection.sendTCP(new JoinReject(GameConstants.REJECT_LOBBY_FULL));
                return;
            }

            // First seat is the host's own loopback client (Elric), second is the partner (Jane).
            CharacterType character = playersByConnectionId.isEmpty() ? CharacterType.ELRIC : CharacterType.JANE;
            String playerId = joinRequest.playerId != null && !joinRequest.playerId.isBlank()
                    ? joinRequest.playerId
                    : character.name().toLowerCase() + "-" + connection.getID();

            joined = new ConnectedPlayer(connection.getID(), playerId,
                    orDefault(joinRequest.displayName, character.name()), character);
            playersByConnectionId.put(connection.getID(), joined);
            mode = currentMatchMode();

            // A newcomer has none of the cloud history, so force the next snapshot
            // to resend every tile instead of only the newest frontier.
            sentCloudTiles.clear();
        }

        joined.entity.setPosition(spawnX(joined.entity.getCharacter()), spawnY(joined.entity.getCharacter()));

        connection.sendTCP(new JoinAccept(backendUrl, hostDisplayName,
                joined.entity.getCharacter(), joined.playerId, mode));

        LOG.info(() -> "Accepted " + joined.displayName + " as " + joined.entity.getCharacter()
                + " (" + playersByConnectionId.size() + "/" + GameConstants.MAX_PLAYERS + ")");

        // Someone came back inside the 60s window: unfreeze instead of going solo.
        if (paused && reconnectWaitRemainingSeconds > 0f) {
            paused = false;
            reconnectWaitRemainingSeconds = 0f;
            broadcastEvent(GameConstants.EVENT_PARTNER_RECONNECTED, joined.displayName);
        } else if (playersByConnectionId.size() > 1) {
            broadcastEvent(GameConstants.EVENT_PARTNER_JOINED, joined.displayName);
        }

        sessionListener.onPlayerJoined(joined.playerId, joined.displayName, joined.entity.getCharacter(), mode);
    }

    private void onInput(Connection connection, InputCommand input) {
        ConnectedPlayer player = playersByConnectionId.get(connection.getID());
        if (player != null) {
            player.latestInput = input; // last-writer-wins is correct for a 30Hz stream
        }
    }

    private void onClientEvent(Connection connection, EventMessage event) {
        ConnectedPlayer sender = playersByConnectionId.get(connection.getID());
        if (sender == null || event.type == null) {
            return;
        }
        // Reliable client->host events are relayed to everyone else so both
        // screens agree (READY gate, pause overlay, objective toasts).
        for (Connection other : server.getConnections()) {
            if (other.getID() != connection.getID()) {
                other.sendTCP(event);
            }
        }
    }

    private void onDisconnected(Connection connection) {
        ConnectedPlayer gone;
        synchronized (rosterLock) {
            gone = playersByConnectionId.remove(connection.getID());
        }
        if (gone == null) {
            return;
        }
        LOG.info(() -> gone.displayName + " disconnected (" + playersByConnectionId.size()
                + "/" + GameConstants.MAX_PLAYERS + " remaining)");

        sessionListener.onPlayerLeft(gone.playerId, gone.displayName);

        // Nobody left means the host itself is shutting down — nothing to wait for.
        if (playersByConnectionId.isEmpty()) {
            paused = false;
            reconnectWaitRemainingSeconds = 0f;
            return;
        }

        // App Flow §3: freeze, show "waiting for player", count down 60s, then go solo.
        paused = true;
        reconnectWaitRemainingSeconds = GameConstants.RECONNECT_WAIT_SECONDS;
        broadcastEvent(GameConstants.EVENT_PARTNER_DISCONNECTED, gone.displayName);
    }

    // ------------------------------------------------------------------
    // Simulation
    // ------------------------------------------------------------------

    /**
     * One authoritative step. Drive it from the render loop's accumulator so
     * there is exactly one clock and one owner — never from its own thread:
     *
     * <pre>
     *   accumulator += delta;
     *   while (accumulator &gt;= GameServer.SIM_STEP_SECONDS) {
     *       server.fixedTimestepUpdate(GameServer.SIM_STEP_SECONDS);
     *       accumulator -= GameServer.SIM_STEP_SECONDS;
     *   }
     * </pre>
     */
    public void fixedTimestepUpdate(float delta) {
        serverTick++;

        if (paused) {
            // Frozen for a reconnect: keep broadcasting so the surviving client
            // still renders (and still sees its "waiting for player" overlay),
            // but advance no game state.
            reconnectWaitRemainingSeconds -= delta;
            if (reconnectWaitRemainingSeconds <= 0f) {
                paused = false;
                reconnectWaitRemainingSeconds = 0f;
                broadcastEvent(GameConstants.EVENT_CONVERTED_TO_SOLO, "");
                sessionListener.onConvertedToSolo();
                LOG.info("Reconnect window expired — converting to SOLO");
            }
            broadcastSnapshotIfDue(delta);
            return;
        }

        List<Player> players = livePlayerEntities();

        for (ConnectedPlayer connected : playersByConnectionId.values()) {
            InputCommand input = connected.latestInput;
            if (input != null && !connected.entity.isDowned()) {
                movementSystem.apply(connected.entity, input, delta);
            }
            connected.entity.update(delta);
        }

        contaminationSystem.tickGlobal(delta);

        for (Enemy enemy : enemies) {
            aiSystem.update(enemy, players, delta);
        }
        enemies.removeIf(Enemy::isDead);

        for (ContaminationZone zone : zones) {
            if (zone.tickAndCheckShouldExpand(delta)) {
                zone.addTiles(contaminationSystem.computeFrontierExpansion(zone, mapWidthInTiles));
            }
            for (Player player : players) {
                if (zone.contains(tileIndexOf(player))) {
                    contaminationSystem.tickPlayerInZone(player, delta);
                }
            }
        }

        broadcastSnapshotIfDue(delta);
    }

    /** Seconds per authoritative step — use this for the accumulator in the render loop. */
    public static final float SIM_STEP_SECONDS = SIM_STEP;

    private void broadcastSnapshotIfDue(float delta) {
        snapshotAccumulator += delta;
        if (snapshotAccumulator >= SNAPSHOT_INTERVAL) {
            snapshotAccumulator -= SNAPSHOT_INTERVAL;
            broadcastSnapshot();
        }
    }

    // ------------------------------------------------------------------
    // Snapshot
    // ------------------------------------------------------------------

    /**
     * Builds and broadcasts the world state over UDP.
     *
     * <p>Every list here is an {@link ArrayList} on purpose: Kryo serializes the
     * concrete runtime class, and only {@code ArrayList} is registered in
     * {@code NetworkMessages}. {@code List.of(...)} would throw
     * "Class is not registered: ImmutableCollections$ListN".
     */
    private void broadcastSnapshot() {
        WorldSnapshot snapshot = new WorldSnapshot();
        snapshot.serverTick = serverTick;
        snapshot.globalContaminationPct = contaminationSystem.getGlobalContaminationPct();

        snapshot.players = new ArrayList<>();
        for (ConnectedPlayer connected : playersByConnectionId.values()) {
            Player entity = connected.entity;
            WorldSnapshot.PlayerState state = new WorldSnapshot.PlayerState();
            state.playerId = connected.playerId;
            state.character = entity.getCharacter();
            state.x = entity.getX();
            state.y = entity.getY();
            state.hp = entity.getHp();
            state.personalContaminationPct = entity.getPersonalContaminationPct();
            state.downed = entity.isDowned();
            state.reviveSecondsRemaining = entity.getReviveSecondsRemaining();
            snapshot.players.add(state);
        }

        snapshot.enemies = new ArrayList<>();
        for (Enemy enemy : enemies) {
            WorldSnapshot.EnemyState state = new WorldSnapshot.EnemyState();
            state.enemyId = enemy.getEnemyId();
            state.type = enemy.getType();
            state.x = enemy.getX();
            state.y = enemy.getY();
            state.hp = enemy.getHp();
            snapshot.enemies.add(state);
        }

        snapshot.cloudDeltas = new ArrayList<>();
        for (ContaminationZone zone : zones) {
            Set<Integer> alreadySent = sentCloudTiles.computeIfAbsent(zone.getCloudId(), id -> new HashSet<>());
            Set<Integer> current = zone.getOccupiedTileIndices();
            if (alreadySent.size() == current.size() && alreadySent.containsAll(current)) {
                continue; // nothing new this tick — the whole point of the delta
            }
            List<Integer> added = new ArrayList<>();
            for (Integer tile : current) {
                if (!alreadySent.contains(tile)) {
                    added.add(tile);
                }
            }
            if (added.isEmpty()) {
                continue;
            }
            WorldSnapshot.CloudFrontierDelta delta = new WorldSnapshot.CloudFrontierDelta();
            delta.cloudId = zone.getCloudId();
            delta.addedTileIndices = new int[added.size()];
            for (int i = 0; i < added.size(); i++) {
                delta.addedTileIndices[i] = added.get(i);
            }
            snapshot.cloudDeltas.add(delta);
            alreadySent.addAll(added);
        }

        snapshot.objectives = new ArrayList<>();
        for (ObjectiveSystem.ObjectiveState source : objectiveSystem.getObjectives().values()) {
            WorldSnapshot.ObjectiveState state = new WorldSnapshot.ObjectiveState();
            state.objectiveId = source.id;
            state.type = source.type.name();
            state.progress = source.progress;
            state.target = source.target;
            state.complete = source.isComplete();
            snapshot.objectives.add(state);
        }

        server.sendToAllUDP(snapshot);
    }

    // ------------------------------------------------------------------
    // Reliable broadcasts (TCP)
    // ------------------------------------------------------------------

    public void broadcastEvent(String type, String payload) {
        server.sendToAllTCP(new EventMessage(type, payload));
    }

    public void broadcastLevelTransition(int nextLevelNumber) {
        server.sendToAllTCP(new LevelTransition(nextLevelNumber,
                nextLevelNumber == GameConstants.BOSS_LEVEL_NUMBER));
    }

    public void broadcastMatchResult(String result, int finalLevelReached) {
        server.sendToAllTCP(new MatchResultMessage(result, finalLevelReached));
    }

    // ------------------------------------------------------------------
    // World setup + queries
    // ------------------------------------------------------------------

    public void addEnemy(Enemy enemy) {
        enemies.add(enemy);
    }

    public void addContaminationZone(ContaminationZone zone) {
        zones.add(zone);
    }

    public void setMapWidthInTiles(int mapWidthInTiles) {
        this.mapWidthInTiles = Math.max(1, mapWidthInTiles);
    }

    public ContaminationSystem getContaminationSystem() {
        return contaminationSystem;
    }

    public ObjectiveSystem getObjectiveSystem() {
        return objectiveSystem;
    }

    public CombatSystem getCombatSystem() {
        return combatSystem;
    }

    public int getConnectedPlayerCount() {
        return playersByConnectionId.size();
    }

    public MatchMode currentMatchMode() {
        return playersByConnectionId.size() >= GameConstants.MAX_PLAYERS ? MatchMode.COOP : MatchMode.SOLO;
    }

    public boolean isWaitingForReconnect() {
        return paused;
    }

    public int getReconnectSecondsRemaining() {
        return (int) Math.ceil(Math.max(0f, reconnectWaitRemainingSeconds));
    }

    public long getServerTick() {
        return serverTick;
    }

    private List<Player> livePlayerEntities() {
        Collection<ConnectedPlayer> connected = playersByConnectionId.values();
        List<Player> entities = new ArrayList<>(connected.size());
        for (ConnectedPlayer player : connected) {
            entities.add(player.entity);
        }
        return entities;
    }

    private int tileIndexOf(Player player) {
        int tileX = (int) Math.floor(player.getX());
        int tileY = (int) Math.floor(player.getY());
        return tileY * mapWidthInTiles + tileX;
    }

    private static float spawnX(CharacterType character) {
        return character == CharacterType.ELRIC ? 4f : 6f;
    }

    private static float spawnY(CharacterType character) {
        return 4f;
    }

    private static String orDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
