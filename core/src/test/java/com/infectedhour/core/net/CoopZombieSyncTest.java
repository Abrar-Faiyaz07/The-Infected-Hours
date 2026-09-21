package com.infectedhour.core.net;

import com.infectedhour.core.bridge.GameBridge;
import com.infectedhour.shared.network.EventMessage;
import com.infectedhour.shared.network.JoinAccept;
import com.infectedhour.shared.network.WorldSnapshot;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Co-op zombie sync over the real KryoNet stack: the host's zombie state and
 * the shared stairs key reach only the partner, kills reach both players, and
 * a bite the host's zombie AI reports lands on exactly the bitten player.
 *
 * <p><b>Requires the game ports to be free</b> (same as {@link LoopbackSessionTest}).
 */
class CoopZombieSyncTest {

    private static final long AWAIT_SECONDS = 10;

    private GameServer server;
    private final List<GameClient> clients = new ArrayList<>();

    /** A connected client plus every event it has received. */
    private record Peer(GameClient client, Queue<EventMessage> events) {
        boolean received(String type, String payload) {
            return events.stream().anyMatch(e -> type.equals(e.type) && payload.equals(e.payload));
        }

        long count(String type) {
            return events.stream().filter(e -> type.equals(e.type)).count();
        }
    }

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
    @DisplayName("host's zombie positions reach the partner only, not back to the host")
    void zombieStateGoesOnlyToThePartner() throws Exception {
        startServer();
        Peer host = join("host-1", "Host");
        Peer partner = join("partner-1", "Partner");

        String state = "L1|m,22.5,16.25,2,1|0,42.0,28.0,1,2";
        host.client().sendEvent("ZOMBIE_STATE", state);

        awaitTrue(() -> partner.received("ZOMBIE_STATE", state), "partner never got the zombie state");
        Thread.sleep(300); // give a wrongly echoed copy time to arrive
        assertEquals(0, host.count("ZOMBIE_STATE"), "host must not receive its own zombie state");
    }

    @Test
    @DisplayName("a kill on either screen is relayed to both players")
    void zombieKillReachesBothPlayers() throws Exception {
        startServer();
        Peer host = join("host-1", "Host");
        Peer partner = join("partner-1", "Partner");

        partner.client().sendEvent("ZOMBIE_KILLED", "ambush:2");
        awaitTrue(() -> host.received("ZOMBIE_KILLED", "ambush:2"), "host never learned of the partner's kill");
        awaitTrue(() -> partner.received("ZOMBIE_KILLED", "ambush:2"), "partner's own kill was not echoed");

        host.client().sendEvent("ZOMBIE_KILLED", "middle");
        awaitTrue(() -> partner.received("ZOMBIE_KILLED", "middle"), "partner never learned of the host's kill");
    }

    @Test
    @DisplayName("picking up the stairs key tells the other player only")
    void stairsKeyGoesToTheOtherPlayer() throws Exception {
        startServer();
        Peer host = join("host-1", "Host");
        Peer partner = join("partner-1", "Partner");

        partner.client().sendEvent("STAIRS_KEY_TAKEN", "");
        awaitTrue(() -> host.received("STAIRS_KEY_TAKEN", ""), "host never learned the key was taken");
        Thread.sleep(300);
        assertEquals(0, partner.count("STAIRS_KEY_TAKEN"), "picker must not get its own key event back");
    }

    @Test
    @DisplayName("a bite the host reports for the partner damages the partner only")
    void hostReportedBiteDamagesOnlyTheBittenPlayer() throws Exception {
        startServer();
        Peer host = join("host-1", "Host");
        Peer partner = join("partner-1", "Partner");

        float hostHpBefore = hpOf(partner, "host-1");
        float partnerHpBefore = hpOf(partner, "partner-1");

        // Exactly what the host's zombie AI sends when a zombie bites the other player.
        host.client().sendEvent("ZOMBIE_BITE_DAMAGE", "20.0:partner-1");

        awaitTrue(() -> hpOf(partner, "partner-1") <= partnerHpBefore - 20f + 0.01f,
                "partner's HP did not drop on the partner's screen");
        awaitTrue(() -> hpOf(host, "partner-1") <= partnerHpBefore - 20f + 0.01f,
                "partner's HP did not drop on the host's screen");
        assertEquals(hostHpBefore, hpOf(host, "host-1"), 0.01f, "the host must not take the partner's bite");

        // And a bite on the host (sent with no target) damages the host, seen on both screens.
        host.client().sendEvent("ZOMBIE_BITE_DAMAGE", "20.0");
        awaitTrue(() -> hpOf(partner, "host-1") <= hostHpBefore - 20f + 0.01f,
                "host's HP did not drop on the partner's screen");
        assertEquals(partnerHpBefore - 20f, hpOf(host, "partner-1"), 0.01f,
                "the partner must not take the host's bite");
    }

    // ------------------------------------------------------------------

    private void startServer() throws Exception {
        server = new GameServer();
        server.start("Test Host", "http://localhost:8080");
    }

    private Peer join(String playerId, String displayName) throws Exception {
        Queue<EventMessage> events = new ConcurrentLinkedQueue<>();
        CountDownLatch accepted = new CountDownLatch(1);
        GameClient client = new GameClient(new GameBridge());
        clients.add(client);
        client.setOnEvent(events::add);
        client.setOnJoinAccepted((JoinAccept accept) -> accepted.countDown());
        client.connect("localhost", playerId, displayName);
        assertTrue(accepted.await(AWAIT_SECONDS, TimeUnit.SECONDS), "join was never accepted for " + displayName);
        return new Peer(client, events);
    }

    /** Ticks the host simulation (which broadcasts snapshots) and reads a player's HP as this peer sees it. */
    private float hpOf(Peer peer, String playerId) throws InterruptedException {
        long deadline = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(AWAIT_SECONDS);
        while (System.currentTimeMillis() < deadline) {
            server.fixedTimestepUpdate(GameServer.SIM_STEP_SECONDS);
            WorldSnapshot snapshot = peer.client().getInterpolatedSnapshot(System.currentTimeMillis());
            if (snapshot != null && snapshot.players != null) {
                for (WorldSnapshot.PlayerState p : snapshot.players) {
                    if (playerId.equals(p.playerId)) return p.hp;
                }
            }
            Thread.sleep(10);
        }
        throw new AssertionError("no snapshot with " + playerId + " arrived");
    }

    private interface Check {
        boolean ok() throws Exception;
    }

    private void awaitTrue(Check check, String message) throws Exception {
        long deadline = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(AWAIT_SECONDS);
        while (System.currentTimeMillis() < deadline) {
            if (check.ok()) return;
            Thread.sleep(20);
        }
        assertNotNull(null, message);
    }
}
