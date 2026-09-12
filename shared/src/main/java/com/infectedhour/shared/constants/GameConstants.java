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
    /** Host → clients when the run reaches a new checkpoint; payload is the checkpoint id. */
    public static final String EVENT_CHECKPOINT_REACHED = "CHECKPOINT_REACHED";
    /** Host → clients when a slot has been written; payload is the slot number. */
    public static final String EVENT_GAME_SAVED = "GAME_SAVED";

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

    // --- Movement & collision (TRD §4) ---
    /** Walk speed in tiles/second. One tile is one world unit. */
    public static final float PLAYER_WALK_SPEED = 4.0f;
    /** Sprint multiplier applied while the ability key is held. */
    public static final float PLAYER_SPRINT_MULTIPLIER = 1.6f;
    /** Enemy chase speed — deliberately below {@link #PLAYER_WALK_SPEED} so players can always disengage. */
    public static final float ENEMY_WALK_SPEED = 2.0f;

    /**
     * Collider radii in tiles. Colliders are circles centred on the entity
     * position — a foot-print, not the sprite's outline. Sprites are roughly a
     * tile wide, but a collider that wide cannot fit through a one-tile doorway
     * with any margin: the player has to line up exactly, movement catches on
     * door frames, and the level has to be carved open to compensate. A radius
     * near a quarter-tile leaves half a tile of clearance in a normal doorway,
     * which is what makes movement feel smooth.
     *
     * <p>Players and enemies share a radius on purpose, so anywhere a player can
     * go an enemy can follow — otherwise chasing enemies wedge in doorways.
     */
    public static final float PLAYER_COLLISION_RADIUS = 0.25f;
    public static final float ENEMY_COLLISION_RADIUS = 0.25f;
    public static final float VILLAGER_COLLISION_RADIUS = 0.22f;

    /**
     * Manual save slots per player, Resident Evil style: the Load Game screen
     * always shows exactly this many cards, filled or empty, in a 3x3 grid.
     */
    public static final int SAVE_SLOT_COUNT = 9;

    public static boolean isValidSaveSlot(int slotNumber) {
        return slotNumber >= 1 && slotNumber <= SAVE_SLOT_COUNT;
    }

    // --- Backend (TRD §6, Backend Schema §6) ---
    public static final int DEFAULT_BACKEND_PORT = 8080;
    public static final long JWT_EXPIRY_HOURS = 12;
}
