package com.infectedhour.core.net;

import com.infectedhour.core.bridge.GameBridge;
import com.infectedhour.shared.constants.GameConstants;
import com.infectedhour.shared.network.CharacterType;
import com.infectedhour.shared.network.InputCommand;
import com.infectedhour.shared.network.JoinAccept;
import com.infectedhour.shared.network.MatchMode;
import com.infectedhour.shared.network.WorldSnapshot;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The "loopback host+client run in one JVM" smoke check TRD §10 asks for on
 * every day networking is touched. It exercises the real KryoNet stack over
 * real sockets — no mocks — because the failures this is guarding against
 * (unregistered Kryo classes, buffer overflow, a handshake that never
 * completes) only ever appear on the wire.
 *
 * <p><b>Requires the game ports to be free.</b> Stop any running copy of the
 * game before running the suite, or {@code bind()} will fail with
 * "Address already in use".
 */
class LoopbackSessionTest {

    private static final long AWAIT_SECONDS = 10;

    private GameServer server;
    private final List<GameClient> clients = new ArrayList<>();

    @AfterEach
    void tearDown() {
        clients.forEach(GameClient::disconnect);
        clients.clear();
        if (server != null) {
            server.stop();
            server = null;
        }
    }

    @Test
    @DisplayName("host seats the first joiner as Elric and the second as Jane")
    void twoPlayersJoinAndGetDistinctCharacters() throws Exception {
        startServer();

        JoinAccept first = joinAndAwaitAccept("p1", "Elric Player");
        assertEquals(CharacterType.ELRIC, first.assignedCharacter,
                "the host's own loopback client takes the first seat");
        assertEquals("p1", first.assignedPlayerId);
        assertEquals("http://10.0.0.7:8080", first.backendUrl,
                "both laptops must be pointed at the same backend (TRD §6)");

        JoinAccept second = joinAndAwaitAccept("p2", "Jane Player");
        assertEquals(CharacterType.JANE, second.assignedCharacter);
        assertNotEquals(first.assignedPlayerId, second.assignedPlayerId);

        assertEquals(2, server.getConnectedPlayerCount());
        assertEquals(MatchMode.COOP, server.currentMatchMode());
    }

    @Test
    @DisplayName("a third connection is rejected instead of silently ignored")
    void thirdPlayerIsRejectedAsLobbyFull() throws Exception {
        startServer();
        joinAndAwaitAccept("p1", "One");
        joinAndAwaitAccept("p2", "Two");

        AtomicReference<String> reason = new AtomicReference<>();
        CountDownLatch rejected = new CountDownLatch(1);
        GameBridge bridge = new GameBridge();
        bridge.setOnJoinFailed(r -> {
            reason.set(r);
            rejected.countDown();
        });

        GameClient third = new GameClient(bridge);
        clients.add(third);
        third.connect("localhost", "p3", "Three");

        assertTrue(rejected.await(AWAIT_SECONDS, TimeUnit.SECONDS), "no rejection arrived");
        assertEquals(GameConstants.REJECT_LOBBY_FULL, reason.get());
        assertEquals(2, server.getConnectedPlayerCount());
    }

    @Test
    @DisplayName("the host learns it is in co-op once the partner joins")
    void hostMatchModeFlipsToCoopWhenPartnerArrives() throws Exception {
        startServer();

        // The host joins its own loopback first, so its JoinAccept says SOLO —
        // there genuinely is nobody else yet.
        CountDownLatch hostAccepted = new CountDownLatch(1);
        GameClient host = new GameClient(new GameBridge());
        clients.add(host);
        host.setOnJoinAccepted(accept -> hostAccepted.countDown());
        host.connect("localhost", "host-1", "Host");
        assertTrue(hostAccepted.await(AWAIT_SECONDS, TimeUnit.SECONDS));
        assertEquals(MatchMode.SOLO, host.getMatchMode());

        // Regression: JoinAccept.matchMode is a one-time snapshot. Without the
        // PARTNER_JOINED update the host stays SOLO forever, skips the ready
        // gate, and starts the level alone while the partner waits.
        joinAndAwaitAccept("client-1", "Partner");

        long deadline = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(AWAIT_SECONDS);
        while (host.getMatchMode() != MatchMode.COOP && System.currentTimeMillis() < deadline) {
            Thread.sleep(20);
        }
        assertEquals(MatchMode.COOP, host.getMatchMode(),
                "host should have been told a partner joined");
    }

    @Test
    @DisplayName("snapshots reach the client and carry every joined player")
    void snapshotsAreBroadcastAndDeserialize() throws Exception {
        startServer();

        CountDownLatch accepted = new CountDownLatch(1);
        GameClient client = new GameClient(new GameBridge());
        clients.add(client);
        client.setOnJoinAccepted(accept -> accepted.countDown());
        client.connect("localhost", "p1", "Solo");
        assertTrue(accepted.await(AWAIT_SECONDS, TimeUnit.SECONDS), "join was never accepted");

        // Drive a second of simulated time; at 20Hz that is ~20 broadcasts, so
        // a single dropped UDP datagram cannot fail the test.
        for (int tick = 0; tick < GameConstants.SIMULATION_TICK_HZ; tick++) {
            server.fixedTimestepUpdate(GameServer.SIM_STEP_SECONDS);
        }

        WorldSnapshot snapshot = awaitSnapshot(client);
        assertNotNull(snapshot, "no snapshot arrived within the timeout");
        assertNotNull(snapshot.players);
        assertEquals(1, snapshot.players.size());
        assertEquals("p1", snapshot.players.get(0).playerId);
        assertEquals(CharacterType.ELRIC, snapshot.players.get(0).character);
        assertNotNull(snapshot.objectives, "objectives list must be present, even when empty");
        assertTrue(snapshot.globalContaminationPct > 0f,
                "the global doom clock should have advanced over a simulated second");
    }

    @Test
    @DisplayName("input sent by the client moves that client's player on the host")
    void clientInputDrivesTheAuthoritativeSimulation() throws Exception {
        startServer();

        CountDownLatch accepted = new CountDownLatch(1);
        GameClient client = new GameClient(new GameBridge());
        clients.add(client);
        client.setOnJoinAccepted(accept -> accepted.countDown());
        client.connect("localhost", "p1", "Mover");
        assertTrue(accepted.await(AWAIT_SECONDS, TimeUnit.SECONDS));

        WorldSnapshot before = null;
        for (int tick = 0; tick < GameConstants.SIMULATION_TICK_HZ && before == null; tick++) {
            server.fixedTimestepUpdate(GameServer.SIM_STEP_SECONDS);
            before = client.getInterpolatedSnapshot(System.currentTimeMillis());
        }
        assertNotNull(before);
        float startX = before.players.get(0).x;

        InputCommand right = new InputCommand();
        right.moveX = 1f;
        // sendInputIfDue rate-limits to 30Hz, so hand it a full interval of time.
        client.sendInputIfDue(right, 1f);

        float movedX = startX;
        for (int tick = 0; tick < GameConstants.SIMULATION_TICK_HZ * 2; tick++) {
            server.fixedTimestepUpdate(GameServer.SIM_STEP_SECONDS);
            WorldSnapshot now = client.getInterpolatedSnapshot(System.currentTimeMillis());
            if (now != null && !now.players.isEmpty()) {
                movedX = now.players.get(0).x;
                if (movedX > startX + 0.5f) {
                    break;
                }
            }
            Thread.sleep(1);
        }
        assertTrue(movedX > startX,
                "host should have applied the client's input: " + startX + " -> " + movedX);
    }

    // ------------------------------------------------------------------

    private void startServer() throws Exception {
        server = new GameServer();
        server.start("Test Host", "http://10.0.0.7:8080");
    }

    private JoinAccept joinAndAwaitAccept(String playerId, String displayName) throws Exception {
        AtomicReference<JoinAccept> received = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        GameClient client = new GameClient(new GameBridge());
        clients.add(client);
        client.setOnJoinAccepted(accept -> {
            received.set(accept);
            latch.countDown();
        });
        client.connect("localhost", playerId, displayName);

        assertTrue(latch.await(AWAIT_SECONDS, TimeUnit.SECONDS),
                "join was never accepted for " + displayName);
        return received.get();
    }

    private WorldSnapshot awaitSnapshot(GameClient client) throws InterruptedException {
        long deadline = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(AWAIT_SECONDS);
        while (System.currentTimeMillis() < deadline) {
            WorldSnapshot snapshot = client.getInterpolatedSnapshot(System.currentTimeMillis());
            if (snapshot != null) {
                return snapshot;
            }
            Thread.sleep(20);
        }
        return null;
    }
}
