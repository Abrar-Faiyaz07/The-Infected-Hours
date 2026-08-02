package com.infectedhour.shared.net;

import com.infectedhour.shared.constants.GameConstants;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Step 1 of the two-step LAN connect described in TRD §5.
 *
 * <pre>
 *   Join Lobby                              Host Lobby
 *   ----------                              ---------
 *   broadcast "IH_DISCOVER"  --UDP:54778-->  Responder
 *   collect  "IH_HOST:name:v" <--UDP:54778--  replies with its display name
 *      |
 *      +--> user picks a row -> KryoNet connects on TCP 54555 / UDP 54777
 * </pre>
 *
 * This is deliberately raw {@link DatagramSocket} code and lives in
 * {@code shared} (no libGDX, no Spring, no KryoNet) so that the JavaFX
 * lobby screens can run discovery long before a game session exists.
 *
 * <p>Broadcast is best-effort: some networks (campus Wi-Fi with client
 * isolation — see the TRD risk table) drop broadcast traffic entirely.
 * Manual IP entry in the Join Lobby is the supported fallback and is never
 * removed from the UI.
 */
public final class LanDiscovery {

    private static final Logger LOG = Logger.getLogger(LanDiscovery.class.getName());

    private LanDiscovery() {
    }

    /** One host that answered a discovery broadcast. */
    public record DiscoveredHost(String displayName, String address, int protocolVersion) {

        /** Row text for the Join Lobby list. */
        @Override
        public String toString() {
            return displayName + "  (" + address + ")"
                    + (protocolVersion == GameConstants.PROTOCOL_VERSION ? "" : "  [version mismatch]");
        }
    }

    // ------------------------------------------------------------------
    // Host side
    // ------------------------------------------------------------------

    /**
     * Answers discovery broadcasts while the Host Lobby is open.
     * Runs on one daemon thread and is safe to {@link #close()} twice.
     */
    public static final class Responder implements AutoCloseable {

        private final String hostDisplayName;
        private volatile DatagramSocket socket;
        private volatile Thread thread;

        public Responder(String hostDisplayName) {
            this.hostDisplayName = hostDisplayName == null || hostDisplayName.isBlank()
                    ? "Host"
                    : hostDisplayName;
        }

        /**
         * Binds {@link GameConstants#DISCOVERY_UDP_PORT} and starts replying.
         *
         * @throws SocketException if the port is already taken — surface this
         *                         to the lobby as "another host is already running
         *                         on this machine", never as a crash.
         */
        public void start() throws SocketException {
            if (thread != null) {
                return;
            }
            DatagramSocket s = new DatagramSocket(null);
            s.setReuseAddress(true);
            s.bind(new InetSocketAddress(GameConstants.DISCOVERY_UDP_PORT));
            this.socket = s;

            Thread t = new Thread(this::listenLoop, "lan-discovery-responder");
            t.setDaemon(true);
            this.thread = t;
            t.start();
            LOG.info(() -> "Discovery responder listening on UDP " + GameConstants.DISCOVERY_UDP_PORT
                    + " as \"" + hostDisplayName + "\"");
        }

        private void listenLoop() {
            byte[] buffer = new byte[256];
            String reply = GameConstants.DISCOVERY_RESPONSE_PREFIX
                    + hostDisplayName + ":" + GameConstants.PROTOCOL_VERSION;
            byte[] replyBytes = reply.getBytes(StandardCharsets.UTF_8);

            while (!Thread.currentThread().isInterrupted()) {
                DatagramSocket s = socket;
                if (s == null || s.isClosed()) {
                    return;
                }
                try {
                    DatagramPacket request = new DatagramPacket(buffer, buffer.length);
                    s.receive(request);

                    String payload = new String(request.getData(), request.getOffset(),
                            request.getLength(), StandardCharsets.UTF_8).trim();
                    if (!payload.startsWith(GameConstants.DISCOVERY_REQUEST)) {
                        continue;
                    }
                    s.send(new DatagramPacket(replyBytes, replyBytes.length,
                            request.getAddress(), request.getPort()));
                } catch (IOException e) {
                    if (socket == null || socket.isClosed()) {
                        return; // normal shutdown
                    }
                    LOG.log(Level.FINE, "Discovery responder ignored a bad packet", e);
                }
            }
        }

        public boolean isRunning() {
            DatagramSocket s = socket;
            return s != null && !s.isClosed();
        }

        @Override
        public void close() {
            Thread t = thread;
            thread = null;
            DatagramSocket s = socket;
            socket = null;
            if (s != null) {
                s.close(); // unblocks receive()
            }
            if (t != null) {
                t.interrupt();
            }
        }
    }

    // ------------------------------------------------------------------
    // Client side
    // ------------------------------------------------------------------

    /**
     * Broadcasts a discovery request and collects replies for
     * {@link GameConstants#DISCOVERY_TIMEOUT_MS}.
     *
     * <p>Blocking — call it off the JavaFX Application Thread and hand the
     * result back with {@code Platform.runLater}.
     *
     * @return hosts that answered, de-duplicated by address, in reply order.
     *         Empty (never null) when nothing answered.
     */
    public static List<DiscoveredHost> probe() {
        return probe(GameConstants.DISCOVERY_TIMEOUT_MS);
    }

    public static List<DiscoveredHost> probe(int timeoutMs) {
        Map<String, DiscoveredHost> found = new LinkedHashMap<>();

        try (DatagramSocket socket = new DatagramSocket()) {
            socket.setBroadcast(true);
            socket.setSoTimeout(200);

            byte[] request = GameConstants.DISCOVERY_REQUEST.getBytes(StandardCharsets.UTF_8);
            for (InetAddress target : broadcastTargets()) {
                try {
                    socket.send(new DatagramPacket(request, request.length,
                            target, GameConstants.DISCOVERY_UDP_PORT));
                } catch (IOException e) {
                    LOG.log(Level.FINE, () -> "Broadcast to " + target + " failed: " + e.getMessage());
                }
            }

            byte[] buffer = new byte[256];
            long deadline = System.currentTimeMillis() + timeoutMs;
            while (System.currentTimeMillis() < deadline) {
                try {
                    DatagramPacket reply = new DatagramPacket(buffer, buffer.length);
                    socket.receive(reply);

                    String payload = new String(reply.getData(), reply.getOffset(),
                            reply.getLength(), StandardCharsets.UTF_8).trim();
                    if (!payload.startsWith(GameConstants.DISCOVERY_RESPONSE_PREFIX)) {
                        continue;
                    }
                    String address = reply.getAddress().getHostAddress();
                    found.putIfAbsent(address, parseReply(payload, address));
                } catch (SocketTimeoutException expected) {
                    // keep polling until the overall deadline
                } catch (IOException e) {
                    LOG.log(Level.FINE, "Discovery reply dropped", e);
                }
            }
        } catch (SocketException e) {
            LOG.log(Level.WARNING, "Could not open a discovery socket; use manual IP entry", e);
        }
        return new ArrayList<>(found.values());
    }

    /** {@code IH_HOST:<name>:<version>} — tolerates a missing version field. */
    private static DiscoveredHost parseReply(String payload, String address) {
        String body = payload.substring(GameConstants.DISCOVERY_RESPONSE_PREFIX.length());
        int lastColon = body.lastIndexOf(':');

        String name = body;
        int version = 0;
        if (lastColon >= 0) {
            try {
                version = Integer.parseInt(body.substring(lastColon + 1).trim());
                name = body.substring(0, lastColon);
            } catch (NumberFormatException ignored) {
                // Older/foreign responder without a version suffix — keep the whole body as the name.
            }
        }
        return new DiscoveredHost(name.isBlank() ? "Host" : name, address, version);
    }

    /**
     * 255.255.255.255 plus every interface-specific broadcast address.
     * The global address alone is dropped by some Windows/driver combinations,
     * so the per-interface addresses are what actually make discovery reliable.
     */
    private static List<InetAddress> broadcastTargets() {
        List<InetAddress> targets = new ArrayList<>();
        try {
            targets.add(InetAddress.getByName("255.255.255.255"));
        } catch (IOException e) {
            LOG.log(Level.FINE, "Global broadcast address unavailable", e);
        }
        try {
            for (NetworkInterface nic : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                if (!nic.isUp() || nic.isLoopback()) {
                    continue;
                }
                for (InterfaceAddress ia : nic.getInterfaceAddresses()) {
                    InetAddress broadcast = ia.getBroadcast();
                    if (broadcast != null) {
                        targets.add(broadcast);
                    }
                }
            }
        } catch (SocketException e) {
            LOG.log(Level.FINE, "Could not enumerate network interfaces", e);
        }
        return targets;
    }

    /**
     * This machine's LAN IPv4 address — what the Host Lobby shows so the other
     * player can type it in manually.
     *
     * <p>Two approaches that look correct and are not:
     * <ul>
     *   <li>{@code InetAddress.getLocalHost()} resolves to 127.0.0.1 on many
     *       Windows and Linux setups via the hosts file. Useless to the other
     *       laptop.</li>
     *   <li>"first non-loopback IPv4 from {@link NetworkInterface}" picks
     *       whichever adapter the OS happens to enumerate first. On a developer
     *       machine that is usually a VirtualBox/VMware/Hyper-V/WSL adapter —
     *       observed here as {@code 192.168.19.1} while the real Wi-Fi address
     *       was {@code 192.168.0.206}. The lobby would then display an address
     *       the other laptop can never reach. {@code isVirtual()} does not help:
     *       it flags sub-interfaces like {@code eth0:1}, not virtual NICs.</li>
     * </ul>
     *
     * <p>So ask the routing table instead: "connect" a UDP socket to an
     * off-link address and read back the local address the OS chose. UDP
     * connect only fixes the peer for later sends — <b>no packet is
     * transmitted</b> and nothing needs to be reachable. Interface enumeration
     * stays as the fallback for a machine with no default route.
     */
    public static String resolveLanIpv4() {
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.connect(InetAddress.getByName("8.8.8.8"), 53);
            InetAddress local = socket.getLocalAddress();
            if (local instanceof Inet4Address && !local.isLoopbackAddress() && !local.isAnyLocalAddress()) {
                return local.getHostAddress();
            }
        } catch (IOException e) {
            LOG.log(Level.FINE, "Routing-table lookup failed; falling back to interface scan", e);
        }
        return scanInterfacesForIpv4();
    }

    /** Fallback for a machine with no default route; prefers physical-looking adapters. */
    private static String scanInterfacesForIpv4() {
        String firstFound = null;
        try {
            for (NetworkInterface nic : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                if (!nic.isUp() || nic.isLoopback() || nic.isVirtual()) {
                    continue;
                }
                for (InetAddress address : Collections.list(nic.getInetAddresses())) {
                    if (!(address instanceof Inet4Address) || address.isLoopbackAddress()) {
                        continue;
                    }
                    if (firstFound == null) {
                        firstFound = address.getHostAddress();
                    }
                    if (!looksVirtual(nic)) {
                        return address.getHostAddress();
                    }
                }
            }
        } catch (SocketException e) {
            LOG.log(Level.FINE, "Could not resolve a LAN address", e);
        }
        return firstFound != null ? firstFound : "127.0.0.1";
    }

    private static boolean looksVirtual(NetworkInterface nic) {
        String name = (nic.getDisplayName() == null ? nic.getName() : nic.getDisplayName()).toLowerCase();
        return name.contains("virtual") || name.contains("vmware") || name.contains("vbox")
                || name.contains("virtualbox") || name.contains("hyper-v") || name.contains("wsl")
                || name.contains("docker") || name.contains("loopback") || name.contains("tap")
                || name.contains("tunnel") || name.contains("bluetooth");
    }
}
