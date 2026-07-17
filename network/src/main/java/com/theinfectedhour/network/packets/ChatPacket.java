package com.theinfectedhour.network.packets;

/** Bidirectional: a chat message between the two players. */
public class ChatPacket extends Packet {

    private String message;

    @Override
    public byte[] serialize() {
        // TODO: implement — blocked on wire-format decision (Kryo vs manual, Architecture.md §7)
        return new byte[0];
    }

    @Override
    public void deserialize(byte[] data) {
        // TODO: implement
    }
}
