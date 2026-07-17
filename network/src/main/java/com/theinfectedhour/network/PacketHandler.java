package com.theinfectedhour.network;

import com.theinfectedhour.network.packets.Packet;

/** Callback invoked by PacketManager when a packet of a registered type arrives. */
@FunctionalInterface
public interface PacketHandler {

    void handle(Packet packet);
}
