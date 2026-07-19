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
    public static final int PROTOCOL_VERSION = 1;

    // --- Networking (TRD §5) ---
    /** UDP broadcast port used by clients to discover a host on the LAN. */
    public static final int DISCOVERY_UDP_PORT = 54777;
    /** KryoNet TCP port — reliable events (join, objective complete, inventory, story sync, chat). */
    public static final int KRYONET_TCP_PORT = 54555;
    /** KryoNet UDP port — world state snapshots. */
    public static final int KRYONET_UDP_PORT = 54777;

    public static final int SIMULATION_TICK_HZ = 60;
    public static final int SNAPSHOT_BROADCAST_HZ = 20;
    public static final int CLIENT_INPUT_SEND_HZ = 30;
    /** Client-side snapshot interpolation buffer, per TRD §5. */
    public static final int INTERPOLATION_BUFFER_MS = 100;

    public static final int RECONNECT_WAIT_SECONDS = 60;

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
