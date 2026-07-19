package com.infectedhour.core.entities;

import com.infectedhour.shared.network.CharacterType;

/**
 * A playable character. Per-character abilities (Elric heal / Jane dash,
 * PRD §4) are intentionally NOT branched with if/else here — see
 * systems/CombatSystem and the ability hook below; prefer Strategy if
 * ability logic grows complex.
 */
public class Player implements Entity {

    private final String playerId;
    private final CharacterType character;

    private float x, y;
    private float hp = 100f;
    private float personalContaminationPct = 0f;
    private boolean downed = false;
    private int reviveSecondsRemaining = 0;

    // ==================== TEAMMATE TASK: INVENTORY ====================
    // TODO(inventory): replace Object with a real Item type.
    //  1. Create core/entities/Item.java — enum ItemType { SAMPLE, MEDICINE,
    //     BARRICADE_PART, MEDKIT, QUEST_ITEM } (+ optional quantity).
    //  2. Add pickUp(Item), dropSelected(slot), useSelected(slot) here.
    //  3. InventoryHotbar reads this array to draw the 4 slots (PRD par.10).
    // ==================================================================
    private final Object[] inventory = new Object[4];
    private float abilityCooldownRemaining = 0f;

    public Player(String playerId, CharacterType character) {
        this.playerId = playerId;
        this.character = character;
    }

    public String getPlayerId() {
        return playerId;
    }

    public CharacterType getCharacter() {
        return character;
    }

    public float getHp() {
        return hp;
    }

    public float getPersonalContaminationPct() {
        return personalContaminationPct;
    }

    public boolean isDowned() {
        return downed;
    }

    public void applyDamage(float amount) {
        hp = Math.max(0, hp - amount);
        if (hp <= 0 && !downed) {
            downed = true;
            reviveSecondsRemaining = com.infectedhour.shared.constants.GameConstants.REVIVE_WINDOW_SECONDS;
        }
    }

    public void heal(float amount) {
        hp = Math.min(100f, hp + amount);
    }

    public void revive() {
        downed = false;
        hp = 50f;
        reviveSecondsRemaining = 0;
    }

    public void addContamination(float amount) {
        personalContaminationPct = Math.min(
                com.infectedhour.shared.constants.GameConstants.PERSONAL_CONTAMINATION_MAX,
                personalContaminationPct + amount);
    }

    public void cleanseContamination() {
        personalContaminationPct = 0f;
    }

    public void move(float dx, float dy) {
        this.x += dx;
        this.y += dy;
    }

    @Override
    public float getX() {
        return x;
    }

    @Override
    public float getY() {
        return y;
    }

    @Override
    public void update(float delta) {
        if (abilityCooldownRemaining > 0) abilityCooldownRemaining -= delta;
        if (downed && reviveSecondsRemaining > 0) {
            // ================ TEAMMATE TASK: REVIVE TIMER ================
            // TODO(player): count down the 30s revive window properly.
            //  - Accumulate delta into a float field; every full 1.0s,
            //    decrement reviveSecondsRemaining by 1.
            //  - Reaches 0 while still downed -> level fails (GameServer's
            //    win/lose check reads this).
            // =============================================================
        }
        // TODO(player, optional): slow contamination decay while standing in
        // a cleansed safe zone — nice touch, not demo-critical.
    }
}
