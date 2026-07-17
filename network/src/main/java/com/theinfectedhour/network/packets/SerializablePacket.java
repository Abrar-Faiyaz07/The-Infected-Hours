package com.theinfectedhour.network.packets;

/** Contract for turning a packet into wire bytes and back (Architecture.md §5). */
public interface SerializablePacket {

    byte[] serialize();

    void deserialize(byte[] data);
}
