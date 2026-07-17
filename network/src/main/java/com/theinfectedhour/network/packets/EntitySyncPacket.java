package com.theinfectedhour.network.packets;

import java.util.List;

import com.theinfectedhour.shared.dto.EntityState;

/** Host -> client at a fixed tick rate (target 20Hz): authoritative positions/animations for all entities. */
public class EntitySyncPacket extends Packet {

    private List<EntityState> entityStates;

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
