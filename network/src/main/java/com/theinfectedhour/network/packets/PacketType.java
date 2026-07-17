package com.theinfectedhour.network.packets;

/** Header discriminator so the receiver knows which packet class to decode (Architecture.md §5: header = type, tick, senderId). */
public enum PacketType {
    PLAYER_INPUT,
    ENTITY_SYNC,
    GAME_STATE,
    MISSION_UPDATE,
    CHAT,
    DISCONNECT,
    HEARTBEAT
}
