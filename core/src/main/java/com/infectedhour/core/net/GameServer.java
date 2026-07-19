package com.infectedhour.core.net;

import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Server;
import com.infectedhour.core.entities.Player;
import com.infectedhour.core.systems.*;
import com.infectedhour.shared.constants.GameConstants;
import com.infectedhour.shared.network.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Runs on the host laptop (Laptop A / Elric, TRD §5). Owns the authoritative
 * simulation:
 *   - simulates at SIMULATION_TICK_HZ (60)
 *   - broadcasts WorldSnapshot at SNAPSHOT_BROADCAST_HZ (20)
 *   - receives InputCommand from the connected client at CLIENT_INPUT_SEND_HZ (30)
 *
 * UDP discovery (port 54777) lets fx-launcher's Host Lobby be found by
 * Join Lobby before the KryoNet session even starts — that part is handled
 * by KryoNet's built-in Server#start / discovery handler.
 */
public class GameServer {

    private final Server server = new Server();
    private final Map<Integer, InputCommand> latestInputByConnectionId = new ConcurrentHashMap<>();

    private final MovementSystem movementSystem = new MovementSystem();
    private final CombatSystem combatSystem = new CombatSystem();
    private final ContaminationSystem contaminationSystem = new ContaminationSystem();
    private final ObjectiveSystem objectiveSystem = new ObjectiveSystem();
    private final AISystem aiSystem = new AISystem();

    private long serverTick = 0;
    private float snapshotAccumulator = 0f;
    private static final float SIM_STEP = 1f / GameConstants.SIMULATION_TICK_HZ;
    private static final float SNAPSHOT_INTERVAL = 1f / GameConstants.SNAPSHOT_BROADCAST_HZ;

    public void start() {
        NetworkRegistration.register(server);

        server.addListener(new Listener() {
            @Override
            public void received(Connection connection, Object object) {
                if (object instanceof InputCommand input) {
                    latestInputByConnectionId.put(connection.getID(), input);
                } else if (object instanceof JoinRequest joinRequest) {
                    onJoinRequest(connection, joinRequest);
                }
            }

            @Override
            public void disconnected(Connection connection) {
                latestInputByConnectionId.remove(connection.getID());
                // ============ TEAMMATE TASK: CLIENT DISCONNECT ============
                // TODO(net): implement WAITING_RECONNECT (App Flow par.3).
                //  1. Broadcast EventMessage("PARTNER_DISCONNECTED", "") so the
                //     host screen shows "Waiting for player... 60s".
                //  2. Pause the simulation (skip fixedTimestepUpdate ticks).
                //  3. Countdown GameConstants.RECONNECT_WAIT_SECONDS (60s).
                //  4. Reconnect -> resume; expiry -> convert to SOLO and play on.
                // ==========================================================
            }
        });

        try {
            server.bind(GameConstants.KRYONET_TCP_PORT, GameConstants.KRYONET_UDP_PORT);
            server.start();
        } catch (IOException e) {
            throw new RuntimeException("Failed to start GameServer", e);
        }

        // ============== TEAMMATE TASK: WIRE THE SIM LOOP ==============
        // TODO(net): GameScreen must call fixedTimestepUpdate(players, SIM_STEP)
        // from its render() using the accumulator pattern (TRD par.4):
        //     accumulator += delta;
        //     while (accumulator >= SIM_STEP) {
        //         server.fixedTimestepUpdate(players, SIM_STEP);
        //         accumulator -= SIM_STEP;
        //     }
        // Do NOT run the simulation on its own thread — one clock, one owner.
        // ==============================================================
    }

    private void onJoinRequest(Connection connection, JoinRequest joinRequest) {
        if (latestInputByConnectionId.size() >= 1) {
            connection.sendTCP(new JoinReject("LOBBY_FULL"));
            return;
        }
        // ================ TEAMMATE TASK: ACCEPT THE JOIN ================
        // TODO(net): finish the join handshake.
        //  1. Version check first: if joinRequest.protocolVersion !=
        //     GameConstants.PROTOCOL_VERSION ->
        //     connection.sendTCP(new JoinReject("VERSION_MISMATCH")); return;
        //  2. Otherwise accept:
        //     connection.sendTCP(new JoinAccept(backendUrl, hostDisplayName,
        //         CharacterType.JANE));
        //     (backendUrl = "http://" + hostLanIp + ":8080" — pass both values
        //      into this class from fx-launcher when the lobby starts hosting)
        //  3. Mark "player 2 connected" so the Host Lobby's Start button can
        //     switch from Solo to Co-op (notify the FX side via a callback).
        // ================================================================
    }

    /** Call once per fixed simulation step (60 Hz) from the render loop's accumulator. */
    public void fixedTimestepUpdate(List<Player> players, float delta) {
        serverTick++;

        for (Player player : players) {
            InputCommand input = latestInputByConnectionId.values().stream().findFirst().orElse(null);
            if (input != null) {
                movementSystem.apply(player, input, delta);
            }
        }

        contaminationSystem.tickGlobal(delta);
        // ================= TEAMMATE TASK: SIM TICK BODY =================
        // TODO(net): complete the per-tick simulation:
        //  1. Enemies:  for each enemy -> aiSystem.update(enemy, players, delta)
        //     then remove dead enemies from the list.
        //  2. Clouds:   for each ContaminationZone:
        //       if (zone.tickAndCheckShouldExpand(delta)) {
        //           var newTiles = contaminationSystem
        //               .computeFrontierExpansion(zone, mapWidthInTiles);
        //           zone.addTiles(newTiles); // remember them for the delta
        //       }
        //  3. Players in clouds: if a player's current tile is inside any
        //     zone -> contaminationSystem.tickPlayerInZone(player, delta)
        //  4. Villagers: update(delta); expired villager -> fail check (L2).
        //  5. Win/lose: all objectives done -> broadcast LevelTransition;
        //     global contamination lethal OR both players downed -> fail event.
        // ================================================================

        snapshotAccumulator += delta;
        if (snapshotAccumulator >= SNAPSHOT_INTERVAL) {
            snapshotAccumulator -= SNAPSHOT_INTERVAL;
            broadcastSnapshot(players);
        }
    }

    private void broadcastSnapshot(List<Player> players) {
        WorldSnapshot snapshot = new WorldSnapshot();
        snapshot.serverTick = serverTick;
        snapshot.globalContaminationPct = contaminationSystem.getGlobalContaminationPct();
        // ============== TEAMMATE TASK: FILL THE SNAPSHOT ==============
        // TODO(net): populate every field before sending:
        //  - snapshot.players: one PlayerState per player (id, character, x, y,
        //    hp, personalContaminationPct, downed, reviveSecondsRemaining)
        //  - snapshot.enemies: one EnemyState per LIVING enemy
        //  - snapshot.cloudDeltas: ONLY tiles added since the last snapshot
        //    (keep a "lastSentTiles" set per zone and diff — sending full
        //     tile sets will blow the 8KB budget, TRD par.8)
        //  - snapshot.objectives: mirror ObjectiveSystem's progress/target
        // ==============================================================
        server.sendToAllUDP(snapshot);
    }

    public ContaminationSystem getContaminationSystem() {
        return contaminationSystem;
    }

    public ObjectiveSystem getObjectiveSystem() {
        return objectiveSystem;
    }

    public void stop() {
        server.stop();
        server.close();
    }
}
