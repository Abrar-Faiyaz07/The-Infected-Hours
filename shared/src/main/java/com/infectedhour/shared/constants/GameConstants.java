package com.infectedhour.shared.constants;

/**
 * Cross-module constants (ports, tick rates, protocol version) so core,
 * fx-launcher, and backend never hardcode magic numbers independently.
 * Values per TRD §5 (Networking Design) and §8 (Performance Targets).
 */
public final class GameConstants {

    private GameConstants() {
    }

    // --- Protocol / compatibility ---
    /** Bumped whenever a message class in shared/network changes shape. Checked in the join handshake. */
    public static final int PROTOCOL_VERSION = 1;

    // --- Networking (TRD §5) ---
    /**
     * UDP broadcast port used by clients to discover a host on the LAN.
     *
     * NOTE: TRD §5 lists 54777 for BOTH discovery and KryoNet's UDP channel.
     * That is an error in the doc: KryoNet's Server binds 54777 for snapshots,
     * so a discovery responder binding the same port on the same host throws
     * BindException("Address already in use"). Discovery therefore owns 54778.
     */
    public static final int DISCOVERY_UDP_PORT = 54778;
    /** KryoNet TCP port — reliable events (join, objective complete, inventory, story sync, chat). */
    public static final int KRYONET_TCP_PORT = 54555;
    /** KryoNet UDP port — world state snapshots, 20x/sec. */
    public static final int KRYONET_UDP_PORT = 54777;

    /** Datagram payload a client broadcasts to find hosts. */
    public static final String DISCOVERY_REQUEST = "IH_DISCOVER";
    /** Prefix of a host's reply: {@code IH_HOST:<displayName>:<protocolVersion>}. */
    public static final String DISCOVERY_RESPONSE_PREFIX = "IH_HOST:";
    /** How long a client listens for discovery replies before giving up. */
    public static final int DISCOVERY_TIMEOUT_MS = 2000;
    /** How long a client waits for the KryoNet TCP connection itself. */
    public static final int CONNECT_TIMEOUT_MS = 5000;

    public static final int SIMULATION_TICK_HZ = 60;
    public static final int SNAPSHOT_BROADCAST_HZ = 20;
    public static final int CLIENT_INPUT_SEND_HZ = 30;
    /** Client-side snapshot interpolation buffer, per TRD §5. */
    public static final int INTERPOLATION_BUFFER_MS = 100;

    public static final int RECONNECT_WAIT_SECONDS = 60;
    /** Host-authoritative session capacity: Elric + Jane (PRD §4). */
    public static final int MAX_PLAYERS = 2;

    // --- Reliable event types carried by EventMessage over TCP (TRD §5) ---
    public static final String EVENT_READY = "READY";
    public static final String EVENT_PARTNER_JOINED = "PARTNER_JOINED";
    public static final String EVENT_PARTNER_DISCONNECTED = "PARTNER_DISCONNECTED";
    public static final String EVENT_PARTNER_RECONNECTED = "PARTNER_RECONNECTED";
    public static final String EVENT_CONVERTED_TO_SOLO = "CONVERTED_TO_SOLO";
    public static final String EVENT_OBJECTIVE_COMPLETE = "OBJECTIVE_COMPLETE";
    public static final String EVENT_PAUSE = "PAUSE";
    public static final String EVENT_RESUME = "RESUME";

    // --- Join rejection reasons ---
    public static final String REJECT_LOBBY_FULL = "LOBBY_FULL";
    public static final String REJECT_VERSION_MISMATCH = "VERSION_MISMATCH";
    public static final String REJECT_TIMEOUT = "TIMEOUT";
    public static final String REJECT_HOST_LOST = "HOST_LOST";

    // --- Gameplay (PRD §7, §10) ---
    public static final int LEVEL_COUNT = 3;
    public static final int BOSS_LEVEL_NUMBER = 3;
    public static final float GLOBAL_CONTAMINATION_MAX = 100f;
    public static final float PERSONAL_CONTAMINATION_MAX = 100f;
    public static final int REVIVE_WINDOW_SECONDS = 30;
    public static final int INVENTORY_SLOTS = 4;

    // --- Backend (TRD §6, Backend Schema §6) ---
    public static final int DEFAULT_BACKEND_PORT = 8080;
    public static final long JWT_EXPIRY_HOURS = 12;
}
