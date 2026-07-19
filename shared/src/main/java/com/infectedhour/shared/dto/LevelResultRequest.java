package com.infectedhour.shared.dto;

/** Body for POST /matches/{id}/level-result. Idempotent per level_number (Backend Schema §4). */
public record LevelResultRequest(
        int levelNumber,
        boolean cleared,
        Integer clearTimeSec,
        int finalContaminationPct,
        int retries
) {
}
