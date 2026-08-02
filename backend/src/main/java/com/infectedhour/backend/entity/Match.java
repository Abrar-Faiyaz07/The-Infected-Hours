package com.infectedhour.backend.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/** `match` table — Backend Schema §3. */
@Entity
@Table(name = "match")
public class Match {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "host_player_id", nullable = false)
    private Player hostPlayer;

    @Column(nullable = false, length = 8)
    private String mode; // SOLO | COOP

    @Column(nullable = false)
    private Instant startedAt = Instant.now();

    private Instant endedAt;

    @Column(length = 10)
    private String result; // VICTORY | DEFEAT | ABORTED

    private Integer finalLevelReached;

    protected Match() {
        // JPA
    }

    public Match(Player hostPlayer, String mode) {
        this.hostPlayer = hostPlayer;
        this.mode = mode;
    }

    public UUID getId() {
        return id;
    }

    public Player getHostPlayer() {
        return hostPlayer;
    }

    public String getMode() {
        return mode;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getEndedAt() {
        return endedAt;
    }

    public String getResult() {
        return result;
    }

    public Integer getFinalLevelReached() {
        return finalLevelReached;
    }

    public void complete(String result, int finalLevelReached) {
        this.result = result;
        this.finalLevelReached = finalLevelReached;
        this.endedAt = Instant.now();
    }
}
