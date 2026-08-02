package com.infectedhour.backend.entity;

import jakarta.persistence.*;

import java.util.UUID;

/** `level_result` table — Backend Schema §3. UNIQUE(match_id, level_number). */
@Entity
@Table(name = "level_result", uniqueConstraints = @UniqueConstraint(columnNames = {"match_id", "level_number"}))
public class LevelResult {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "match_id", nullable = false)
    private Match match;

    @Column(nullable = false)
    private int levelNumber;

    @Column(nullable = false)
    private boolean cleared;

    private Integer clearTimeSec;

    @Column(nullable = false)
    private int finalContaminationPct;

    @Column(nullable = false)
    private int retries = 0;

    protected LevelResult() {
        // JPA
    }

    public LevelResult(Match match, int levelNumber, boolean cleared, Integer clearTimeSec,
                        int finalContaminationPct, int retries) {
        this.match = match;
        this.levelNumber = levelNumber;
        this.cleared = cleared;
        this.clearTimeSec = clearTimeSec;
        this.finalContaminationPct = finalContaminationPct;
        this.retries = retries;
    }

    public UUID getId() {
        return id;
    }

    public Match getMatch() {
        return match;
    }

    public int getLevelNumber() {
        return levelNumber;
    }

    public boolean isCleared() {
        return cleared;
    }

    public Integer getClearTimeSec() {
        return clearTimeSec;
    }

    public int getFinalContaminationPct() {
        return finalContaminationPct;
    }

    public int getRetries() {
        return retries;
    }
}
