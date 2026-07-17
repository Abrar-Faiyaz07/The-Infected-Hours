package com.theinfectedhour.network.packets;

import com.theinfectedhour.shared.math.Vector2;

/** Client -> host: the player's movement/action INTENT for one tick — never position (Architecture.md §7). */
public class PlayerInputPacket extends Packet {

    private Vector2 moveVector;
    private boolean actionPressed;

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
