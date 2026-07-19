package com.infectedhour.shared.dto;

import java.util.UUID;

/** Row shape for GET /players/me/matches (Backend Schema §4, Profile screen). */
public record MatchHistoryEntryDto(
        UUID matchId,
        String mode,
        String result,
        int finalLevelReached,
        String startedAt,
        String endedAt
) {
}
