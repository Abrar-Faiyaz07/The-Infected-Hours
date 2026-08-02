package com.infectedhour.shared.dto;

import java.util.UUID;

/** Exact shape per Backend Schema §5. */
public record ParticipantStats(
        UUID playerId,
        String character,
        int damageDealt,
        int villagersRescued,
        int samplesCollected,
        int medicineDelivered,
        int barricadesPlaced,
        int sanitationsDone,
        int timesDowned,
        int revivesDone
) {
}
