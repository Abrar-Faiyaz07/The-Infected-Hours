package com.theinfectedhour.network.packets;

/** Base for all wire messages: header (type, tick, senderId) plus subclass payload. */
public abstract class Packet implements SerializablePacket {

    protected PacketType type;
    protected long tick;
    protected String senderId;
}
