package com.infectedhour.shared.network;

/**
 * Reliable one-off event over KryoNet TCP: join, objective complete,
 * inventory change, story sync, chat pings (TRD §5).
 */
public class EventMessage {
    public String type;
    public String payload; // small JSON blob; keep events lightweight

    public EventMessage() {
    }

    public EventMessage(String type, String payload) {
        this.type = type;
        this.payload = payload;
    }
}
