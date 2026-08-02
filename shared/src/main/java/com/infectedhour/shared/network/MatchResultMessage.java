package com.infectedhour.shared.network;

/**
 * Host → Client: final in-session result, distinct from the backend DTO
 * (dto.MatchCompleteRequest) that the host POSTs to the server — this one
 * is just for driving the client's local Results screen immediately.
 */
public class MatchResultMessage {
    public String result; // VICTORY | DEFEAT | ABORTED
    public int finalLevelReached;

    public MatchResultMessage() {
    }

    public MatchResultMessage(String result, int finalLevelReached) {
        this.result = result;
        this.finalLevelReached = finalLevelReached;
    }
}
