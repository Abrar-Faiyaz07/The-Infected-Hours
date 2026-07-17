package com.theinfectedhour.network;

import java.util.HashMap;
import java.util.Map;

import com.theinfectedhour.network.packets.Packet;

/** Registry that routes each incoming Packet to the handler registered for its type. */
public class PacketManager {

    private final Map<Class<? extends Packet>, PacketHandler> handlers = new HashMap<>();

    public void register(Class<? extends Packet> type, PacketHandler handler) {
        // TODO: implement
    }

    public void dispatch(Packet packet) {
        // TODO: implement
    }
}
