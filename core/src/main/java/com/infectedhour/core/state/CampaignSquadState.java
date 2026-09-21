package com.infectedhour.core.state;

import java.util.ArrayList;
import java.util.List;

/**
 * Tracks persistent squad progression, inventory, and final operation statistics.
 * Rescued villagers travel with the squad through Level 3, where their outcome
 * is finalized; Levels 4 onward retain only the after-action report totals.
 */
public final class CampaignSquadState {

    public static final int TOTAL_RESCUABLE_NPCS = 4;

    public static int coins = 0;
    public static boolean isJaneRevived = false;
    public static float janeHp = com.infectedhour.shared.constants.GameConstants.PLAYER_MAX_HP;
    public static boolean hasMachete = true;
    public static boolean isMacheteEquipped = true;
    public static boolean hasBomb = false;
    public static boolean isBombEquipped = false;
    public static int npcsSaved = 0;
    public static int npcsFailed = 0;
    public static boolean survivorReportFinalized = false;
    private static long campaignStartedAtMillis = System.currentTimeMillis();
    private static long priorPlaytimeSeconds = 0L;

    public record RescuedVillagerInfo(String id, String name, float hp) {}
    public static final List<RescuedVillagerInfo> rescuedVillagers = new ArrayList<>();

    private CampaignSquadState() {
    }

    /**
     * Resets squad state when starting a fresh Story mode campaign.
     */
    public static void reset() {
        coins = 0;
        isJaneRevived = false;
        janeHp = com.infectedhour.shared.constants.GameConstants.PLAYER_MAX_HP;
        hasMachete = true;
        isMacheteEquipped = true;
        hasBomb = false;
        isBombEquipped = false;
        npcsSaved = 0;
        npcsFailed = 0;
        survivorReportFinalized = false;
        priorPlaytimeSeconds = 0L;
        campaignStartedAtMillis = System.currentTimeMillis();
        rescuedVillagers.clear();
    }

    public static void resumeTimer(long savedPlaytimeSeconds) {
        priorPlaytimeSeconds = Math.max(0L, savedPlaytimeSeconds);
        campaignStartedAtMillis = System.currentTimeMillis();
    }

    public static long totalGameTimeSeconds() {
        long currentSessionSeconds = Math.max(0L,
                (System.currentTimeMillis() - campaignStartedAtMillis) / 1000L);
        return priorPlaytimeSeconds + currentSessionSeconds;
    }

    public static void finalizeSurvivorReport(int livingRescuedNpcs) {
        npcsSaved = Math.max(0, Math.min(TOTAL_RESCUABLE_NPCS, livingRescuedNpcs));
        npcsFailed = TOTAL_RESCUABLE_NPCS - npcsSaved;
        survivorReportFinalized = true;
        rescuedVillagers.clear();
    }
}
