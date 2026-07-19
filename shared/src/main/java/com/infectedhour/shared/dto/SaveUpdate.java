package com.infectedhour.shared.dto;

import java.util.UUID;

/** Exact shape per Backend Schema §5. */
public record SaveUpdate(
        UUID playerId,
        int highestLevelUnlocked,
        int storyProgress,
        long playtimeDeltaSec
) {
}
