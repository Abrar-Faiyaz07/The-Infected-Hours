package com.theinfectedhour.network;

/** Host-side (Player 1) TCP listener: accepts the client and owns the connection/packet managers. */
public class SocketServer {

    private final ConnectionManager connectionManager = new ConnectionManager();
    private final PacketManager packetManager = new PacketManager();

    public void start(int port) {
        // TODO: implement — no working socket I/O in the skeleton milestone
    }

    public void onClientConnected(ClientHandle session) {
        // TODO: implement
    }
}
