package com.theinfectedhour.backend.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

/** JPA mapping of the level_unlocks table — backs "finishing a map saves progress" (composite PK). */
@Entity
@Table(name = "level_unlocks")
@IdClass(LevelUnlockId.class)
public class LevelUnlock {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Id
    @Column(name = "level_id")
    private Integer levelId;

    @Column(name = "unlocked_at")
    private Instant unlockedAt;
}
