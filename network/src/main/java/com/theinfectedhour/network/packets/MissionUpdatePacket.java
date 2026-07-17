package com.theinfectedhour.network.packets;

/** Host -> client: a mission objective progressed or completed. */
public class MissionUpdatePacket extends Packet {

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
