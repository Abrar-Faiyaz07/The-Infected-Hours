package com.infectedhour.shared.dto;

/**
 * One of the nine manual save slots (Resident Evil style): where the player
 * was, and what they were carrying.
 *
 * <p>Distinct from {@link SaveStateDto}, and the two must not be confused:
 * <ul>
 *   <li>{@code SaveStateDto} is the player's <b>permanent account progression</b> —
 *       highest level unlocked, story progress, total playtime. There is exactly
 *       one per player and it only ever moves forward.</li>
 *   <li>{@code SaveSlotDto} is a <b>restorable point in time</b>. There are nine
 *       per player, each can be overwritten or deleted, and loading one rewinds
 *       the run without touching account progression.</li>
 * </ul>
 *
 * <p>{@code occupied == false} means the slot exists in the UI but has never
 * been written; every other field should be ignored. The API always returns all
 * nine so the launcher can render a fixed 3x3 grid without null handling.
 *
 * @param inventoryJson  serialised carried items — JSON so the item model can
 *                       evolve without a database migration
 * @param objectivesJson serialised objective progress at the moment of saving
 * @param savedAt        ISO-8601 instant, or null when the slot is empty
 */
public record SaveSlotDto(
        int slotNumber,
        boolean occupied,
        int levelNumber,
        String levelName,
        String checkpointId,
        String checkpointName,
        int storyProgress,
        long playtimeSec,
        String characterType,
        float playerHp,
        float personalContaminationPct,
        float globalContaminationPct,
        String inventoryJson,
        String objectivesJson,
        String savedAt
) {

    /** Placeholder for a slot that has never been written. */
    public static SaveSlotDto empty(int slotNumber) {
        return new SaveSlotDto(slotNumber, false, 0, null, null, null,
                0, 0L, null, 0f, 0f, 0f, null, null, null);
    }

    /** "1h 04m" / "12m 30s" — what the slot card shows. */
    public String formattedPlaytime() {
        long hours = playtimeSec / 3600;
        long minutes = (playtimeSec % 3600) / 60;
        long seconds = playtimeSec % 60;
        if (hours > 0) {
            return String.format("%dh %02dm", hours, minutes);
        }
        return String.format("%dm %02ds", minutes, seconds);
    }
}
