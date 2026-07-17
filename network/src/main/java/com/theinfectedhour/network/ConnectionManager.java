package com.theinfectedhour.network;

import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import com.theinfectedhour.network.packets.Packet;

/** Tracks live sockets and fans outgoing packets out to all of them. */
public class ConnectionManager {

    private final List<Socket> connections = new ArrayList<>();

    public void broadcast(Packet packet) {
        // TODO: implement — no working socket I/O in the skeleton milestone
    }
}
