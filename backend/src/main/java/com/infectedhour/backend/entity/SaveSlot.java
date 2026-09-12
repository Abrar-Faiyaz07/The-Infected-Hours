package com.infectedhour.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.UUID;

/**
 * `save_slot` table — one row per (player, slot number), nine slots per player.
 *
 * <p>Deliberately separate from {@link SaveState}. {@code SaveState} is the
 * player's permanent account progression and only ever moves forward;
 * a {@code SaveSlot} is a restorable point in time that can be overwritten,
 * deleted, and loaded to rewind a run. Storing both in one table would make it
 * impossible to load an old slot without also rolling back unlocks the player
 * has legitimately earned.
 *
 * <p>The unique constraint on (player_id, slot_number) is what makes "save to
 * slot 4" idempotent: the service looks the row up and overwrites it rather
 * than accumulating duplicates.
 *
 * <p>Inventory and objective progress are stored as JSON text rather than
 * child tables. At this scale a relational breakdown buys nothing — the data is
 * only ever read and written as a whole — and it would force a schema migration
 * every time an item type is added.
 */
@Entity
@Table(
        name = "save_slot",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_save_slot_player_slot",
                columnNames = {"player_id", "slot_number"})
)
public class SaveSlot {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "player_id", nullable = false)
    private Player player;

    /** 1..9 — validated in the service against GameConstants.SAVE_SLOT_COUNT. */
    @Column(name = "slot_number", nullable = false)
    private int slotNumber;

    @Column(nullable = false)
    private int levelNumber = 1;

    @Column(length = 64)
    private String levelName;

    /** Which checkpoint within the level, so loading resumes in the right place. */
    @Column(length = 64)
    private String checkpointId;

    @Column(length = 96)
    private String checkpointName;

    @Column(nullable = false)
    private int storyProgress = 0;

    @Column(nullable = false)
    private long playtimeSec = 0;

    /** ELRIC or JANE — stored as text so the enum can gain values safely. */
    @Column(length = 16)
    private String characterType;

    // --- carried resources at the moment of saving ---
    @Column(nullable = false)
    private float playerHp = 100f;

    @Column(nullable = false)
    private float personalContaminationPct = 0f;

    @Column(nullable = false)
    private float globalContaminationPct = 0f;

    @Column(columnDefinition = "TEXT")
    private String inventoryJson;

    @Column(columnDefinition = "TEXT")
    private String objectivesJson;

    @Column(nullable = false)
    private Instant savedAt = Instant.now();

    protected SaveSlot() {
        // JPA
    }

    public SaveSlot(Player player, int slotNumber) {
        this.player = player;
        this.slotNumber = slotNumber;
    }

    /** Overwrite this slot wholesale — a save always replaces, never merges. */
    public void overwrite(int levelNumber, String levelName, String checkpointId, String checkpointName,
                          int storyProgress, long playtimeSec, String characterType,
                          float playerHp, float personalContaminationPct, float globalContaminationPct,
                          String inventoryJson, String objectivesJson) {
        this.levelNumber = levelNumber;
        this.levelName = levelName;
        this.checkpointId = checkpointId;
        this.checkpointName = checkpointName;
        this.storyProgress = storyProgress;
        this.playtimeSec = playtimeSec;
        this.characterType = characterType;
        this.playerHp = playerHp;
        this.personalContaminationPct = personalContaminationPct;
        this.globalContaminationPct = globalContaminationPct;
        this.inventoryJson = inventoryJson;
        this.objectivesJson = objectivesJson;
        this.savedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public Player getPlayer() { return player; }
    public int getSlotNumber() { return slotNumber; }
    public int getLevelNumber() { return levelNumber; }
    public String getLevelName() { return levelName; }
    public String getCheckpointId() { return checkpointId; }
    public String getCheckpointName() { return checkpointName; }
    public int getStoryProgress() { return storyProgress; }
    public long getPlaytimeSec() { return playtimeSec; }
    public String getCharacterType() { return characterType; }
    public float getPlayerHp() { return playerHp; }
    public float getPersonalContaminationPct() { return personalContaminationPct; }
    public float getGlobalContaminationPct() { return globalContaminationPct; }
    public String getInventoryJson() { return inventoryJson; }
    public String getObjectivesJson() { return objectivesJson; }
    public Instant getSavedAt() { return savedAt; }
}
