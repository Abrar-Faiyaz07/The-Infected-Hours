package com.theinfectedhour.network.packets;

/** Either direction: orderly notice that a peer is leaving, or emitted after N missed heartbeats. */
public class DisconnectPacket extends Packet {

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
