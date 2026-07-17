package com.theinfectedhour.network;

import com.theinfectedhour.network.packets.Packet;

/** Encodes/decodes packets to wire bytes; format is an OPEN DECISION — Kryo vs hand-rolled binary (Architecture.md §7). */
public class PacketSerializer {

    public byte[] encode(Packet packet) {
        // TODO: implement — blocked on wire-format decision
        return new byte[0];
    }

    public Packet decode(byte[] data) {
        // TODO: implement — blocked on wire-format decision
        return null;
    }
}
