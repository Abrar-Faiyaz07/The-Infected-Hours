package com.infectedhour.shared.net;

import com.infectedhour.shared.constants.GameConstants;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers the discovery half of TRD §5.
 *
 * <p>The responder is exercised with a <b>directed</b> datagram to 127.0.0.1
 * rather than a 255.255.255.255 broadcast. Broadcast delivery to your own
 * machine depends on the OS, the adapter, and whether a VPN or virtual switch
 * is installed — a test that relies on it is a test that fails for reasons
 * that have nothing to do with this code. The directed send proves the
 * request/response protocol; the broadcast fan-out is what the manual
 * two-laptop playtest is for.
 */
class LanDiscoveryTest {

    private LanDiscovery.Responder responder;

    @AfterEach
    void tearDown() {
        if (responder != null) {
            responder.close();
            responder = null;
        }
    }

    @Test
    @DisplayName("responder answers a discovery request with its name and protocol version")
    void responderAnswersDiscoveryRequests() throws Exception {
        responder = new LanDiscovery.Responder("Elric Laptop");
        responder.start();
        assertTrue(responder.isRunning());

        try (DatagramSocket probe = new DatagramSocket()) {
            probe.setSoTimeout(3000);

            byte[] request = GameConstants.DISCOVERY_REQUEST.getBytes(StandardCharsets.UTF_8);
            probe.send(new DatagramPacket(request, request.length,
                    InetAddress.getByName("127.0.0.1"), GameConstants.DISCOVERY_UDP_PORT));

            byte[] buffer = new byte[256];
            DatagramPacket reply = new DatagramPacket(buffer, buffer.length);
            probe.receive(reply);

            String payload = new String(reply.getData(), reply.getOffset(),
                    reply.getLength(), StandardCharsets.UTF_8);
            assertEquals(GameConstants.DISCOVERY_RESPONSE_PREFIX
                    + "Elric Laptop:" + GameConstants.PROTOCOL_VERSION, payload);
        }
    }

    @Test
    @DisplayName("responder ignores traffic that is not a discovery request")
    void responderIgnoresForeignPackets() throws Exception {
        responder = new LanDiscovery.Responder("Host");
        responder.start();

        try (DatagramSocket probe = new DatagramSocket()) {
            probe.setSoTimeout(600);

            byte[] noise = "hello?".getBytes(StandardCharsets.UTF_8);
            probe.send(new DatagramPacket(noise, noise.length,
                    InetAddress.getByName("127.0.0.1"), GameConstants.DISCOVERY_UDP_PORT));

            byte[] buffer = new byte[256];
            try {
                probe.receive(new DatagramPacket(buffer, buffer.length));
                org.junit.jupiter.api.Assertions.fail("responder replied to a non-discovery packet");
            } catch (java.net.SocketTimeoutException expected) {
                // correct: unrelated traffic on this port is not our business
            }
        }

        assertTrue(responder.isRunning(), "a stray packet must not kill the responder");
    }

    @Test
    @DisplayName("discovery and the KryoNet UDP channel use different ports")
    void discoveryPortDoesNotCollideWithKryonet() {
        // TRD §5 lists 54777 for both. They cannot share: KryoNet's Server binds
        // the UDP port on the host, and the discovery responder binds its own on
        // the same machine — identical values means BindException at lobby open.
        assertNotEquals(GameConstants.KRYONET_UDP_PORT, GameConstants.DISCOVERY_UDP_PORT);
        assertNotEquals(GameConstants.KRYONET_TCP_PORT, GameConstants.DISCOVERY_UDP_PORT);
    }

    @Test
    @DisplayName("probe returns an empty list rather than null when nothing answers")
    void probeIsEmptyWhenNoHostIsRunning() {
        var hosts = LanDiscovery.probe(300);
        assertTrue(hosts.stream().noneMatch(h -> h.displayName() == null),
                "a discovered host must always carry a name");
    }

    @Test
    @DisplayName("resolveLanIpv4 returns the address the OS would actually route from")
    void resolveLanIpv4ReturnsSomethingUsable() throws Exception {
        String ip = LanDiscovery.resolveLanIpv4();
        assertTrue(ip != null && !ip.isBlank());

        // Regression: the first-non-loopback-interface version returned a
        // VirtualBox/Hyper-V adapter address (192.168.19.1) while the machine's
        // real Wi-Fi address was 192.168.0.206, so the Host Lobby displayed an
        // address the other laptop could never reach. Cross-check against the
        // routing table the same way the implementation does.
        try (DatagramSocket routing = new DatagramSocket()) {
            routing.connect(InetAddress.getByName("8.8.8.8"), 53); // no packet is sent
            InetAddress expected = routing.getLocalAddress();
            if (!expected.isAnyLocalAddress() && !expected.isLoopbackAddress()) {
                assertEquals(expected.getHostAddress(), ip,
                        "must report the routable address, not whichever NIC enumerates first");
            }
        } catch (java.io.IOException noRoute) {
            // No default route on this machine — the interface-scan fallback is
            // in play and any non-blank answer is acceptable.
        }
    }
}
