package com.infectedhour.shared.dto;

import java.util.List;

/**
 * Exact shape per Backend Schema §5. This is the single-transaction request
 * that writes match + participants, updates both SaveStates, and upserts
 * leaderboard bests (Backend Schema §7 business rule 1).
 */
public record MatchCompleteRequest(
        String result,               // VICTORY | DEFEAT | ABORTED
        int finalLevelReached,
        List<ParticipantStats> participants,
        List<SaveUpdate> saves
) {
}
