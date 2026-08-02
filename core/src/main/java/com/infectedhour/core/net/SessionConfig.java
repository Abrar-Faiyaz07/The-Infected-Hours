package com.infectedhour.core.net;

import com.infectedhour.shared.constants.GameConstants;
import com.infectedhour.shared.net.LanDiscovery;

/**
 * Everything the game needs to know about the session it is booting into,
 * handed across the JavaFX → libGDX thread boundary as one immutable value.
 *
 * @param host        true when this machine runs the authoritative {@link GameServer}
 * @param hostAddress address to connect the {@link GameClient} to; ignored (and
 *                    replaced with loopback) when {@code host} is true
 * @param playerId    stable identity — the backend player id when logged in, a
 *                    generated one when playing offline
 * @param displayName name the partner sees in the lobby
 * @param backendUrl  Spring Boot instance BOTH laptops must write to; the host
 *                    ships it to the client inside JoinAccept (TRD §6)
 */
public record SessionConfig(boolean host, String hostAddress, String playerId,
                            String displayName, String backendUrl) {

    public SessionConfig {
        playerId = playerId == null || playerId.isBlank() ? "player-" + System.nanoTime() : playerId;
        displayName = displayName == null || displayName.isBlank() ? "Player" : displayName;
        backendUrl = backendUrl == null || backendUrl.isBlank()
                ? "http://localhost:" + GameConstants.DEFAULT_BACKEND_PORT
                : backendUrl;
    }

    /** This laptop hosts: the client connects to its own loopback. */
    public static SessionConfig hosting(String playerId, String displayName, String backendUrl) {
        return new SessionConfig(true, "localhost", playerId, displayName, backendUrl);
    }

    /** This laptop joins the host at {@code hostAddress}; the backend URL arrives in JoinAccept. */
    public static SessionConfig joining(String hostAddress, String playerId, String displayName) {
        return new SessionConfig(false, hostAddress, playerId, displayName, null);
    }

    /** Dev shortcut used by {@code ./gradlew lwjgl3:run} — host-solo, no launcher. */
    public static SessionConfig devSolo() {
        return hosting("dev-host", "Dev Host", null);
    }

    /** The address the client should actually dial. */
    public String effectiveHostAddress() {
        return host ? "localhost" : (hostAddress == null || hostAddress.isBlank() ? "localhost" : hostAddress.trim());
    }

    /** What the host advertises as the shared backend, with its LAN IP substituted for localhost. */
    public String advertisedBackendUrl() {
        if (!host) {
            return backendUrl;
        }
        return backendUrl.replace("localhost", LanDiscovery.resolveLanIpv4())
                .replace("127.0.0.1", LanDiscovery.resolveLanIpv4());
    }
}
