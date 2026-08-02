package com.infectedhour.core.net;

import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Server;
import com.infectedhour.core.entities.Collidable;
import com.infectedhour.core.entities.ContaminationZone;
import com.infectedhour.core.entities.Enemy;
import com.infectedhour.core.entities.Player;
import com.infectedhour.core.level.TileMap;
import com.infectedhour.core.systems.AISystem;
import com.infectedhour.core.systems.CollisionSystem;
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

public class GameServer {

    private static final Logger LOG = Logger.getLogger(GameServer.class.getName());

    private static final int WRITE_BUFFER_BYTES = 65536;
    private static final int OBJECT_BUFFER_BYTES = 16384;

    private final Server server;
    private final Map<Integer, ConnectedPlayer> playersByConnectionId = new ConcurrentHashMap<>();
    private final Object rosterLock = new Object();

    /** Open field used until a level pushes its real grid in via {@link #loadTileMap}. */
    private static final TileMap DEFAULT_TILE_MAP = TileMap.allWalkable(60, 40);

    // One CollisionSystem shared by movement and AI, so players and enemies can
    // never drift apart on what counts as a wall (TRD §4).
    private final CollisionSystem collisionSystem = new CollisionSystem(DEFAULT_TILE_MAP);
    private final MovementSystem movementSystem = new MovementSystem(collisionSystem);
    private final CombatSystem combatSystem = new CombatSystem();
    private final ContaminationSystem contaminationSystem = new ContaminationSystem();
    private final ObjectiveSystem objectiveSystem = new ObjectiveSystem();
    private final AISystem aiSystem = new AISystem(collisionSystem);

    private final List<Enemy> enemies = new ArrayList<>();
    private final List<ContaminationZone> zones = new ArrayList<>();
    private TileMap tileMap = DEFAULT_TILE_MAP;
    /** Reusable scratch list for the per-tick separation pass — avoids allocating 60x/second. */
    private final List<Collidable> collidableScratch = new ArrayList<>();

    private final Map<String, Set<Integer>> sentCloudTiles = new LinkedHashMap<>();

    private String hostDisplayName = "Host";
    private String backendUrl = "http://localhost:" + GameConstants.DEFAULT_BACKEND_PORT;

    private long serverTick = 0;
    private float snapshotAccumulator = 0f;
    private volatile boolean running = false;

    private float reconnectWaitRemainingSeconds = 0f;
    private volatile boolean paused = false;

    private volatile SessionListener sessionListener = SessionListener.NO_OP;

    private static final float SIM_STEP = 1f / GameConstants.SIMULATION_TICK_HZ;
    private static final float SNAPSHOT_INTERVAL = 1f / GameConstants.SNAPSHOT_BROADCAST_HZ;

    public GameServer() {
        this.server = new Server(WRITE_BUFFER_BYTES, OBJECT_BUFFER_BYTES);
    }

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

    private void onJoinRequest(Connection connection, JoinRequest joinRequest) {
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
                return;
            }
            if (playersByConnectionId.size() >= GameConstants.MAX_PLAYERS) {
                LOG.warning(() -> "Rejecting " + joinRequest.displayName + ": lobby full");
                connection.sendTCP(new JoinReject(GameConstants.REJECT_LOBBY_FULL));
                return;
            }

            CharacterType character = playersByConnectionId.isEmpty() ? CharacterType.ELRIC : CharacterType.JANE;
            String playerId = joinRequest.playerId != null && !joinRequest.playerId.isBlank()
                    ? joinRequest.playerId
                    : character.name().toLowerCase() + "-" + connection.getID();

            joined = new ConnectedPlayer(connection.getID(), playerId,
                    orDefault(joinRequest.displayName, character.name()), character);
            playersByConnectionId.put(connection.getID(), joined);
            mode = currentMatchMode();

            sentCloudTiles.clear();
        }

        seatAtSpawn(joined.entity);

        connection.sendTCP(new JoinAccept(backendUrl, hostDisplayName,
                joined.entity.getCharacter(), joined.playerId, mode));

        LOG.info(() -> "Accepted " + joined.displayName + " as " + joined.entity.getCharacter()
                + " (" + playersByConnectionId.size() + "/" + GameConstants.MAX_PLAYERS + ")");

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
            player.latestInput = input;
        }
    }

    private void onClientEvent(Connection connection, EventMessage event) {
        ConnectedPlayer sender = playersByConnectionId.get(connection.getID());
        if (sender == null || event.type == null) {
            return;
        }

        // Handle zombie bite damage only if the player is still alive & not downed
        if ("ZOMBIE_BITE_DAMAGE".equals(event.type)) {
            if (sender.entity.getHp() > 0f && !sender.entity.isDowned()) {
                try {
                    float damage = Float.parseFloat(event.payload);
                    sender.entity.applyDamage(damage);
                } catch (NumberFormatException e) {
                    LOG.warning("Invalid zombie bite damage payload: " + event.payload);
                }
            }
            return;
        }

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

        if (playersByConnectionId.isEmpty()) {
            paused = false;
            reconnectWaitRemainingSeconds = 0f;
            return;
        }

        paused = true;
        reconnectWaitRemainingSeconds = GameConstants.RECONNECT_WAIT_SECONDS;
        broadcastEvent(GameConstants.EVENT_PARTNER_DISCONNECTED, gone.displayName);
    }

    public void fixedTimestepUpdate(float delta) {
        serverTick++;

        if (paused) {
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

        // Halt simulation ticks if player is dead/downed to prevent loop and terminal spam
        boolean allPlayersDead = !players.isEmpty();
        for (Player p : players) {
            if (p.getHp() > 0f && !p.isDowned()) {
                allPlayersDead = false;
                break;
            }
        }

        if (allPlayersDead && !players.isEmpty()) {
            broadcastSnapshotIfDue(delta);
            return;
        }

        for (ConnectedPlayer connected : playersByConnectionId.values()) {
            InputCommand input = connected.latestInput;
            if (input != null && !connected.entity.isDowned() && connected.entity.getHp() > 0f) {
                movementSystem.apply(connected.entity, input, delta);
            }
            connected.entity.update(delta);
        }

        contaminationSystem.tickGlobal(delta);

        for (Enemy enemy : enemies) {
            aiSystem.update(enemy, players, delta);
        }
        enemies.removeIf(Enemy::isDead);

        // Push apart anything left overlapping. Runs after BOTH movement and AI
        // so it resolves against final positions for this tick — separating
        // earlier would just let the AI step back into a player.
        resolveEntityOverlaps(players);

        for (ContaminationZone zone : zones) {
            if (zone.tickAndCheckShouldExpand(delta)) {
                zone.addTiles(contaminationSystem.computeFrontierExpansion(zone, tileMap.getWidth()));
            }
            for (Player player : players) {
                if (zone.contains(tileIndexOf(player))) {
                    contaminationSystem.tickPlayerInZone(player, delta);
                }
            }
        }

        broadcastSnapshotIfDue(delta);
    }

    public static final float SIM_STEP_SECONDS = SIM_STEP;

    private void broadcastSnapshotIfDue(float delta) {
        snapshotAccumulator += delta;
        if (snapshotAccumulator >= SNAPSHOT_INTERVAL) {
            snapshotAccumulator -= SNAPSHOT_INTERVAL;
            broadcastSnapshot();
        }
    }

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
                continue;
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

    public void addEnemy(Enemy enemy) {
        enemies.add(enemy);
    }

    public void addContaminationZone(ContaminationZone zone) {
        zones.add(zone);
    }

    /**
     * Installs the collision grid for the level being played. Called by the host
     * as it enters a level; until then the sim runs on an open field.
     */
    public void loadTileMap(TileMap tileMap) {
        this.tileMap = tileMap != null ? tileMap : DEFAULT_TILE_MAP;
        this.collisionSystem.setTileMap(this.tileMap);
    }

    public TileMap getTileMap() {
        return tileMap;
    }

    public CollisionSystem getCollisionSystem() {
        return collisionSystem;
    }

    /**
     * Downed players are skipped: a body on the floor should not shove a
     * teammate away from the revive they are trying to perform.
     */
    private void resolveEntityOverlaps(List<Player> players) {
        collidableScratch.clear();
        for (Player player : players) {
            if (!player.isDowned() && player.getHp() > 0f) {
                collidableScratch.add(player);
            }
        }
        collidableScratch.addAll(enemies);
        collisionSystem.separateAll(collidableScratch);
        collidableScratch.clear();
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
        return tileMap.tileIndex(TileMap.toTile(player.getX()), TileMap.toTile(player.getY()));
    }

    /**
     * Places a player at their spawn point, or at the nearest free tile if that
     * point is inside geometry. Without this, editing a map so a wall covers a
     * spawn would wedge a player permanently — they would be stuck inside a
     * blocked tile with every direction refusing to move them out.
     */
    private void seatAtSpawn(Player player) {
        float x = spawnX(player.getCharacter());
        float y = spawnY(player.getCharacter());

        if (collisionSystem.overlapsBlockedTile(x, y, player.getCollisionRadius())) {
            LOG.warning(() -> "Spawn (" + spawnX(player.getCharacter()) + ", " + spawnY(player.getCharacter())
                    + ") for " + player.getCharacter() + " is blocked — relocating to the nearest free tile");
            float[] free = findNearestFreeTileCentre(x, y, player.getCollisionRadius());
            if (free != null) {
                x = free[0];
                y = free[1];
            }
        }
        player.setPosition(x, y);
    }

    /** Outward ring search for a tile centre the collider fits in. Returns null if the map is fully blocked. */
    private float[] findNearestFreeTileCentre(float originX, float originY, float radius) {
        int originTileX = TileMap.toTile(originX);
        int originTileY = TileMap.toTile(originY);
        int maxRadius = Math.max(tileMap.getWidth(), tileMap.getHeight());

        for (int ring = 1; ring <= maxRadius; ring++) {
            for (int offsetY = -ring; offsetY <= ring; offsetY++) {
                for (int offsetX = -ring; offsetX <= ring; offsetX++) {
                    // Only the perimeter of this ring; the interior was covered already.
                    if (Math.abs(offsetX) != ring && Math.abs(offsetY) != ring) {
                        continue;
                    }
                    float centreX = originTileX + offsetX + 0.5f;
                    float centreY = originTileY + offsetY + 0.5f;
                    if (!collisionSystem.overlapsBlockedTile(centreX, centreY, radius)) {
                        return new float[] { centreX, centreY };
                    }
                }
            }
        }
        return null;
    }

    // Tile CENTRES, not corners. A position on a tile corner puts the collider
    // across four tiles at once, so a spawn point only reads as clear if all
    // four happen to be walkable. Centres keep the body inside one tile.
    private static float spawnX(CharacterType character) {
        return character == CharacterType.ELRIC ? 5.5f : 6.5f;
    }

    private static float spawnY(CharacterType character) {
        return 4.5f;
    }

    private static String orDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}