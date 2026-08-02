package com.infectedhour.shared.network;

/**
 * Sent by a discovered/manually-entered host as part of TCP session setup,
 * before KryoNet is fully connected in-game. Kryo requires a no-arg
 * constructor on every registered message class.
 */
public class JoinRequest {
    public String playerId;
    public String displayName;
    public int protocolVersion;

    public JoinRequest() {
    }

    public JoinRequest(String playerId, String displayName, int protocolVersion) {
        this.playerId = playerId;
        this.displayName = displayName;
        this.protocolVersion = protocolVersion;
    }
}
