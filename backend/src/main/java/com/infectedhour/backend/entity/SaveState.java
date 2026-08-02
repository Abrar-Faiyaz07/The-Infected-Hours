package com.infectedhour.backend.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/** `save_state` table — one per player. Backend Schema §3. */
@Entity
@Table(name = "save_state")
public class SaveState {

    @Id
    @GeneratedValue
    private UUID id;

    @OneToOne
    @JoinColumn(name = "player_id", nullable = false, unique = true)
    private Player player;

    @Column(nullable = false)
    private int highestLevelUnlocked = 1;

    @Column(nullable = false)
    private int storyProgress = 0;

    @Column(nullable = false)
    private long totalPlaytimeSec = 0;

    @Column(columnDefinition = "TEXT")
    private String settingsJson;

    @Column(nullable = false)
    private Instant updatedAt = Instant.now();

    protected SaveState() {
        // JPA
    }

    public SaveState(Player player) {
        this.player = player;
    }

    public UUID getId() {
        return id;
    }

    public Player getPlayer() {
        return player;
    }

    public int getHighestLevelUnlocked() {
        return highestLevelUnlocked;
    }

    public int getStoryProgress() {
        return storyProgress;
    }

    public long getTotalPlaytimeSec() {
        return totalPlaytimeSec;
    }

    public String getSettingsJson() {
        return settingsJson;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    /** Business rule 3 (Backend Schema §7): highest_level_unlocked only increases. */
    public void applyUpdate(int newHighestLevel, int newStoryProgress, long playtimeDeltaSec) {
        this.highestLevelUnlocked = Math.max(this.highestLevelUnlocked, newHighestLevel);
        this.storyProgress = Math.max(this.storyProgress, newStoryProgress);
        this.totalPlaytimeSec += playtimeDeltaSec;
        this.updatedAt = Instant.now();
    }

    /** Conflict resolution: latest updatedAt wins (Backend Schema §3 note, App Flow §6 offline reconciliation). */
    public boolean isNewerThan(Instant other) {
        return this.updatedAt.isAfter(other);
    }

    public void setSettingsJson(String settingsJson) {
        this.settingsJson = settingsJson;
        this.updatedAt = Instant.now();
    }
}
