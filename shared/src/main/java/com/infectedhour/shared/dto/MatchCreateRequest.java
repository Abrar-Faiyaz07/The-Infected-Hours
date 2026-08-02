package com.infectedhour.shared.dto;

import java.util.List;
import java.util.UUID;

/** Body for POST /matches at session start (Backend Schema §4). */
public record MatchCreateRequest(
        String mode, // SOLO | COOP
        List<Participant> participants
) {
    public record Participant(UUID playerId, String character) {
    }
}
