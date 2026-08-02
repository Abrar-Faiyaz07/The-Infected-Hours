package com.infectedhour.shared.dto;

public record SaveStateDto(
        int highestLevelUnlocked,
        int storyProgress,
        long totalPlaytimeSec,
        String settingsJson,
        String updatedAt // ISO-8601; kept as String to avoid a java.time dep here if not needed elsewhere
) {
}
