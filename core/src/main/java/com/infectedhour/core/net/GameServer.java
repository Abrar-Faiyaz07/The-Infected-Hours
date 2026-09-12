package com.infectedhour.core.net;

import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Server;
import com.infectedhour.core.entities.ContaminationZone;
import com.infectedhour.core.entities.Enemy;
import com.infectedhour.core.entities.Player;
import com.infectedhour.core.level.CampaignLevelPlan;
import com.infectedhour.core.level.LevelDefinition;
import com.infectedhour.core.level.LevelLoader;
import com.infectedhour.core.level.TileMap;
import com.infectedhour.core.level.LevelExit;
import com.infectedhour.core.systems.AISystem;
import com.infectedhour.core.systems.CheckpointSystem;
import com.infectedhour.core.systems.CollisionSystem;
import com.infectedhour.core.systems.CombatSystem;
import com.infectedhour.core.systems.ContaminationSystem;
import com.infectedhour.core.systems.MovementSystem;
import com.infectedhour.core.systems.ObjectiveSystem;
import com.infectedhour.shared.constants.GameConstants;
import com.infectedhour.shared.dto.SaveSlotDto;
import com.infectedhour.shared.level.Checkpoint;
import com.infectedhour.shared.level.CheckpointRegistry;
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
import java.util.Optional;
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

    /**
     * One collision system shared by movement and AI, so players and enemies
     * obey exactly one set of rules. It starts with no grid — {@link #loadTileMap}
     * supplies the level's once GameScreen has parsed it.
     */
    private final CollisionSystem collisionSystem = new CollisionSystem();
    private final MovementSystem movementSystem = new MovementSystem(collisionSystem);
    private final CombatSystem combatSystem = new CombatSystem();
    private final ContaminationSystem contaminationSystem = new ContaminationSystem();
    private final ObjectiveSystem objectiveSystem = new ObjectiveSystem();
    private final AISystem aiSystem = new AISystem(collisionSystem);
    private final CheckpointSystem checkpointSystem = new CheckpointSystem();

    private final List<Enemy> enemies = new ArrayList<>();
    private final List<ContaminationZone> zones = new ArrayList<>();
    private final List<WorldSnapshot.ItemState> items = new ArrayList<>();
    private int mapWidthInTiles = 64;
    private volatile int currentLevelNumber = 1;
    private volatile LevelExit activeLevelExit = LevelExit.forLevel(1).orElse(null);
    private volatile boolean levelTransitionBroadcast;
    private final Set<String> completedObjectiveActions = new HashSet<>();

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
        volatile String equippedWeapon = "NONE";

        ConnectedPlayer(int connectionId, String playerId, String displayName, CharacterType character) {
            this.connectionId = connectionId;
            this.playerId = playerId;
            this.displayName = displayName;
            this.entity = new Player(playerId, character);
        }
    }

    private volatile CharacterType hostCharacter = CharacterType.ELRIC;

    public void setHostCharacter(CharacterType hostCharacter) {
        this.hostCharacter = hostCharacter != null ? hostCharacter : CharacterType.ELRIC;
    }

    public CharacterType getHostCharacter() {
        return hostCharacter;
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

        // Spawn test machete to the right of host
        WorldSnapshot.ItemState testMachete = new WorldSnapshot.ItemState();
        testMachete.id = "machete_1";
        testMachete.type = "MELEE";
        testMachete.x = spawnX(hostCharacter) + 1.5f;
        testMachete.y = spawnY(hostCharacter);
        addItem(testMachete);
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

            CharacterType character = playersByConnectionId.isEmpty()
                    ? hostCharacter
                    : (hostCharacter == CharacterType.ELRIC ? CharacterType.JANE : CharacterType.ELRIC);
            String playerId = joinRequest.playerId != null && !joinRequest.playerId.isBlank()
                    ? joinRequest.playerId
                    : character.name().toLowerCase() + "-" + connection.getID();

            String displayName = orDefault(joinRequest.displayName, character.name());
            if (displayName.equalsIgnoreCase(hostDisplayName)) {
                displayName = displayName + " (P2)";
            }

            joined = new ConnectedPlayer(connection.getID(), playerId,
                    displayName, character);
            playersByConnectionId.put(connection.getID(), joined);
            mode = currentMatchMode();

            sentCloudTiles.clear();
        }

        joined.entity.setPosition(spawnX(joined.entity.getCharacter()), spawnY(joined.entity.getCharacter()));

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

        // Handle healing ability
        if ("PLAYER_HEAL".equals(event.type)) {
            if (sender.entity.getHp() > 0f && !sender.entity.isDowned()) {
                float healAmount = 35f;
                try {
                    healAmount = Float.parseFloat(event.payload);
                } catch (NumberFormatException ignored) {}
                sender.entity.heal(healAmount);
                LOG.info(() -> sender.displayName + " healed (" + sender.entity.getHp() + " HP)");
            }
            return;
        }

        // Level exits use TCP because a one-frame E press must never disappear
        // as a dropped/rate-limited UDP input packet. Position and objective
        // completion are still validated by the authoritative host.
        if (GameConstants.EVENT_LEVEL_EXIT_REQUEST.equals(event.type)) {
            tryUseLevelExit(sender);
            return;
        }

        if (GameConstants.EVENT_OBJECTIVE_PROGRESS.equals(event.type)) {
            tryAdvanceLevelObjective(sender, event.payload);
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
        }        for (ConnectedPlayer connected : playersByConnectionId.values()) {
            InputCommand input = connected.latestInput;
            if (input != null && !connected.entity.isDowned() && connected.entity.getHp() > 0f) {
                movementSystem.apply(connected.entity, input, delta);

                if (input.interactPressed) {
                    java.util.Iterator<WorldSnapshot.ItemState> iterator = items.iterator();
                    while (iterator.hasNext()) {
                        WorldSnapshot.ItemState item = iterator.next();

                        float distX = connected.entity.getX() - item.x;
                        float distY = connected.entity.getY() - item.y;
                        float distance = (float) Math.sqrt(distX * distX + distY * distY);

                        if (distance <= 1.0f) {
                            iterator.remove();
                            connected.equippedWeapon = item.type;
                            LOG.info(() -> connected.displayName + " picked up: " + item.type);
                            break;
                        }
                    }
                    input.interactPressed = false;
                }
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

        // Only the host tracks checkpoints, for the same reason it owns everything
        // else: two machines deciding independently when a checkpoint was reached
        // would disagree, and the save would depend on who pressed the button.
        checkpointSystem.updateAndDetectNew(players).ifPresent(reached -> {
            LOG.info(() -> "Checkpoint reached: " + reached.qualifiedName());
            broadcastEvent(GameConstants.EVENT_CHECKPOINT_REACHED, reached.id());
        });

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

        snapshot.items = new ArrayList<>(this.items);

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
            state.equippedWeapon = connected.equippedWeapon;
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
            WorldSnapshot.CloudFrontierDelta deltaMsg = new WorldSnapshot.CloudFrontierDelta();
            deltaMsg.cloudId = zone.getCloudId();
            deltaMsg.addedTileIndices = added.stream().mapToInt(Integer::intValue).toArray();
            snapshot.cloudDeltas.add(deltaMsg);
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

    public void addItem(WorldSnapshot.ItemState item) {
        items.add(item);
    }

    public void addContaminationZone(ContaminationZone zone) {
        zones.add(zone);
    }

    public void setMapWidthInTiles(int mapWidthInTiles) {
        this.mapWidthInTiles = Math.max(1, mapWidthInTiles);
    }

    /** Configures exits and objectives from the campaign's single source of truth. */
    public void configureLevel(int levelNumber) {
        configureLevel(new LevelLoader().loadDefinition(levelNumber));
    }

    public synchronized void configureLevel(LevelDefinition definition) {
        boolean enteringNewLevel = this.currentLevelNumber != definition.levelNumber();
        this.currentLevelNumber = definition.levelNumber();
        this.activeLevelExit = LevelExit.forLevel(definition.levelNumber()).orElse(null);
        this.levelTransitionBroadcast = false;
        this.completedObjectiveActions.clear();

        objectiveSystem.clear();
        for (LevelDefinition.ObjectiveSpec spec : definition.objectives()) {
            objectiveSystem.register(spec.id(), ObjectiveSystem.ObjectiveType.valueOf(spec.type()), spec.target());
        }
        if (enteringNewLevel) {
            Checkpoint start = CheckpointRegistry.firstOf(definition.levelNumber());
            int playerIndex = 0;
            for (ConnectedPlayer connected : playersByConnectionId.values()) {
                connected.entity.setPosition(start.spawnTileX() + playerIndex * 0.5f, start.spawnTileY());
                playerIndex++;
            }
        }
        LOG.info(() -> "Configured " + definition.name() + " with "
                + definition.objectives().size() + " objectives");
    }

    /**
     * Accepts one interaction only once and only while the sender is standing
     * at the matching landmark.  This keeps co-op clients from double-counting
     * the same survivor or relay.
     */
    private synchronized void tryAdvanceLevelObjective(ConnectedPlayer sender, String actionId) {
        CampaignLevelPlan.Feature feature = CampaignLevelPlan.findFeature(currentLevelNumber, actionId).orElse(null);
        if (sender == null || feature == null || completedObjectiveActions.contains(actionId)) {
            return;
        }
        if (!feature.contains(sender.entity.getX(), sender.entity.getY())) {
            return;
        }
        if (!objectiveSystem.getObjectives().containsKey(feature.objectiveId())) {
            return;
        }

        completedObjectiveActions.add(actionId);
        objectiveSystem.incrementProgress(feature.objectiveId());
        broadcastEvent(GameConstants.EVENT_OBJECTIVE_PROGRESS, actionId);
        if (objectiveSystem.isObjectiveComplete(feature.objectiveId())) {
            broadcastEvent(GameConstants.EVENT_OBJECTIVE_COMPLETE, feature.objectiveId());
        }
    }

    private synchronized void tryUseLevelExit(ConnectedPlayer sender) {
        if (sender == null || activeLevelExit == null || levelTransitionBroadcast) {
            return;
        }
        if (!activeLevelExit.contains(sender.entity.getX(), sender.entity.getY())) {
            return;
        }
        if (!objectiveSystem.areAllObjectivesComplete()) {
            return;
        }

        int nextLevelNumber = currentLevelNumber + 1;
        if (nextLevelNumber > GameConstants.LEVEL_COUNT) {
            return;
        }

        levelTransitionBroadcast = true;
        LOG.info(() -> sender.displayName + " activated the level exit: "
                + currentLevelNumber + " -> " + nextLevelNumber);
        broadcastLevelTransition(nextLevelNumber);
    }

    /**
     * Hands the level's collision grid to the simulation. Called by
     * {@code GameScreen} once {@code LevelLoader} has parsed the {@code .map}
     * resource — until it lands, movement is unblocked.
     */
    public void loadTileMap(TileMap tileMap) {
        collisionSystem.setTileMap(tileMap);
        if (tileMap != null) {
            this.mapWidthInTiles = tileMap.getWidth();
            LOG.info(() -> "Collision grid active: " + tileMap.getWidth() + "x" + tileMap.getHeight());
        }
    }

    public CollisionSystem getCollisionSystem() {
        return collisionSystem;
    }

    // ------------------------------------------------------------------
    // Save / load
    // ------------------------------------------------------------------

    /**
     * Freeze the current run into a save slot.
     *
     * <p>Only the host can do this, and that is the point: it owns the only
     * authoritative copy of the world, so a save taken here is guaranteed
     * consistent. Letting a client build its own save would capture an
     * interpolated view that is ~100 ms stale and missing anything outside its
     * snapshot.
     *
     * @param slotNumber 1..9
     * @return the slot, ready to PUT to the backend, or null if the run has not
     *         reached a checkpoint yet
     */
    public SaveSlotDto captureSave(int slotNumber) {
        Checkpoint checkpoint = checkpointSystem.getLastReachedCheckpoint();
        if (checkpoint == null) {
            LOG.warning("Refusing to save: the run has not reached a checkpoint yet");
            return null;
        }

        // Save the host's own player — in co-op each machine saves its own run.
        Player player = playersByConnectionId.values().stream()
                .map(connected -> connected.entity)
                .findFirst()
                .orElse(null);

        float hp = player == null ? 100f : player.getHp();
        float personalContamination = player == null ? 0f : player.getPersonalContaminationPct();
        String character = player == null ? null : player.getCharacter().name();

        return new SaveSlotDto(
                slotNumber,
                true,
                checkpoint.levelNumber(),
                levelNameFor(checkpoint.levelNumber()),
                checkpoint.id(),
                checkpoint.name(),
                checkpointSystem.reachedCount(),
                elapsedPlaytimeSeconds(),
                character,
                hp,
                personalContamination,
                contaminationSystem.getGlobalContaminationPct(),
                serialiseInventory(player),
                serialiseObjectives(),
                java.time.Instant.now().toString());
    }

    /**
     * Rebuild the run from a save slot. Applied before the first tick, so the
     * first snapshot clients receive already reflects the restored state and
     * nobody ever renders the pre-load world.
     */
    public void restoreFrom(SaveSlotDto slot) {
        if (slot == null || !slot.occupied()) {
            return;
        }
        Checkpoint checkpoint = CheckpointRegistry.byId(slot.checkpointId()).orElse(null);
        if (checkpoint == null) {
            // An unknown id means the save predates a checkpoint rename. Falling
            // back to the level's start is far better than refusing to load.
            LOG.warning(() -> "Save references unknown checkpoint '" + slot.checkpointId()
                    + "'; starting the level from its first checkpoint instead");
            checkpoint = CheckpointRegistry.firstOf(Math.max(1, slot.levelNumber()));
        }
        checkpointSystem.restoreTo(checkpoint.id());
        currentLevelNumber = checkpoint.levelNumber();
        contaminationSystem.setGlobalContaminationPct(slot.globalContaminationPct());

        for (ConnectedPlayer connected : playersByConnectionId.values()) {
            connected.entity.setPosition(checkpoint.spawnTileX(), checkpoint.spawnTileY());
            connected.entity.restoreVitals(slot.playerHp(), slot.personalContaminationPct());
        }
        LOG.info(() -> "Restored save at " + slot.checkpointId());
    }

    public CheckpointSystem getCheckpointSystem() {
        return checkpointSystem;
    }

    /** Whether any connected player is standing close enough to save right now. */
    public Optional<Checkpoint> checkpointInRange() {
        for (ConnectedPlayer connected : playersByConnectionId.values()) {
            Optional<Checkpoint> found = checkpointSystem.checkpointInRange(connected.entity);
            if (found.isPresent()) {
                return found;
            }
        }
        return Optional.empty();
    }

    private long elapsedPlaytimeSeconds() {
        return (long) (serverTick / (float) GameConstants.SIMULATION_TICK_HZ);
    }

    private static String levelNameFor(int levelNumber) {
        return switch (levelNumber) {
            case 1 -> "Ashgrove Hospital";
            case 2 -> "Roadside Village";
            case 3 -> "Hidden Laboratory";
            default -> "Level " + levelNumber;
        };
    }

    /**
     * TEAMMATE TASK (inventory): serialise the player's real carried items once
     * {@code Player.inventory} holds a proper Item type. The save column and the
     * whole round trip are already in place — only this method needs changing.
     */
    private static String serialiseInventory(Player player) {
        return "[]";
    }

    /** Objective progress as a flat {@code {"id": progress}} map. */
    private String serialiseObjectives() {
        StringBuilder json = new StringBuilder("{");
        boolean first = true;
        for (ObjectiveSystem.ObjectiveState state : objectiveSystem.getObjectives().values()) {
            if (!first) {
                json.append(',');
            }
            json.append('"').append(state.id).append("\":").append(state.progress);
            first = false;
        }
        return json.append('}').toString();
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

    /**
     * Spawn points, validated against {@code maps/level1.map} by
     * {@code CheckpointPlacementTest}.
     *
     * <p>These were originally (4,4) and (6,4). Tile (4,4) is <b>inside a wall</b>
     * in the real hospital layout, so once collision was switched on the player
     * spawned embedded in geometry and {@code moveWithCollision} correctly
     * refused every step — the character simply would not move.
     *
     * <p>Coordinates are tile centres (x.5): an integer coordinate sits on the
     * boundary between two tiles, so a 0.25-radius collider straddles both and
     * can clip a wall that touches only one of them.
     */
    private static float spawnX(CharacterType character) {
        return character == CharacterType.ELRIC ? 9.5f : 10.5f;
    }

    private static float spawnY(CharacterType character) {
        return 8.5f;
    }

    private static String orDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
