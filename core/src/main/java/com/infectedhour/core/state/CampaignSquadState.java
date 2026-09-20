package com.infectedhour.core.state;

import java.util.ArrayList;
import java.util.List;

/**
 * Tracks persistent squad progression, allies, and inventory across levels
 * so that Jane, rescued villagers, collected coins, and items (Machete, Grenades)
 * seamlessly transition across Level 1 -> Level 2 -> Level 3.
 */
public final class CampaignSquadState {

    public static int coins = 0;
    public static boolean isJaneRevived = false;
    public static float janeHp = 100f;
    public static boolean hasMachete = true;
    public static boolean isMacheteEquipped = true;
    public static boolean hasBomb = false;
    public static boolean isBombEquipped = false;

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
        janeHp = 100f;
        hasMachete = true;
        isMacheteEquipped = true;
        hasBomb = false;
        isBombEquipped = false;
        rescuedVillagers.clear();
    }
}
