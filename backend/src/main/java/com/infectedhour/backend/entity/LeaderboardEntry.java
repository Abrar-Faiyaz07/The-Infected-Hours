package com.infectedhour.backend.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/** `leaderboard_entry` table — denormalized bests. UNIQUE(player_id, board). Backend Schema §3. */
@Entity
@Table(name = "leaderboard_entry", uniqueConstraints = @UniqueConstraint(columnNames = {"player_id", "board"}))
public class LeaderboardEntry {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "player_id", nullable = false)
    private Player player;

    @Column(nullable = false, length = 24)
    private String board; // FASTEST_L1..L3, LOWEST_CONTAM_L1..L3, BOSS_TIME

    @Column(nullable = false)
    private int value; // seconds or percent — lower is better for ALL boards

    @ManyToOne
    @JoinColumn(name = "match_id", nullable = false)
    private Match match;

    @Column(nullable = false)
    private Instant achievedAt = Instant.now();

    protected LeaderboardEntry() {
        // JPA
    }

    public LeaderboardEntry(Player player, String board, int value, Match match) {
        this.player = player;
        this.board = board;
        this.value = value;
        this.match = match;
    }

    public UUID getId() {
        return id;
    }

    public Player getPlayer() {
        return player;
    }

    public String getBoard() {
        return board;
    }

    public int getValue() {
        return value;
    }

    public Match getMatch() {
        return match;
    }

    public Instant getAchievedAt() {
        return achievedAt;
    }

    /** Business rule 2 (Backend Schema §7): replace only if new value is better (lower). */
    public void replaceIfBetter(int newValue, Match newMatch) {
        if (newValue < this.value) {
            this.value = newValue;
            this.match = newMatch;
            this.achievedAt = Instant.now();
        }
    }
}
