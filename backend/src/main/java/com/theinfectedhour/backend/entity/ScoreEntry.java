package com.theinfectedhour.backend.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** JPA mapping of the score_entries table — the leaderboard source. */
@Entity
@Table(name = "score_entries")
public class ScoreEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "level_id")
    private Integer levelId;

    @Column(name = "time_seconds")
    private Integer timeSeconds;

    private Integer deaths;

    @Column(name = "completed_at")
    private Instant completedAt;
}
