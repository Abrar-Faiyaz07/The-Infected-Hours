package com.infectedhour.shared.network;

/** Sent when a second client tries to join a full lobby (App Flow §6). */
public class JoinReject {
    public String reason; // e.g. "LOBBY_FULL", "VERSION_MISMATCH"

    public JoinReject() {
    }

    public JoinReject(String reason) {
        this.reason = reason;
    }
}
