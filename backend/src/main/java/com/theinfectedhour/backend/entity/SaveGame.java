package com.theinfectedhour.backend.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** JPA mapping of the save_games table — one persisted save slot per user. */
@Entity
@Table(name = "save_games")
public class SaveGame {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "level_reached")
    private Integer levelReached;

    @Column(name = "contamination_stats", columnDefinition = "json")
    private String contaminationStats;

    @Column(name = "updated_at")
    private Instant updatedAt;
}
