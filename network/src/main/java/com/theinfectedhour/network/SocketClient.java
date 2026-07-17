package com.theinfectedhour.network;

import com.theinfectedhour.network.packets.Packet;

/** Client-side (Player 2) TCP connection: sends input packets, receives sync/state packets. */
public class SocketClient {

    private final PacketManager packetManager = new PacketManager();

    public void connect(String host, int port) {
        // TODO: implement — no working socket I/O in the skeleton milestone
    }

    public void send(Packet packet) {
        // TODO: implement
    }
}
