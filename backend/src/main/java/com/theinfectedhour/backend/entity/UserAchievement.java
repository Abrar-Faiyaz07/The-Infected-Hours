package com.theinfectedhour.backend.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

/** JPA mapping of the user_achievements table — which user earned which achievement (composite PK). */
@Entity
@Table(name = "user_achievements")
@IdClass(UserAchievementId.class)
public class UserAchievement {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Id
    @Column(name = "achievement_id")
    private Long achievementId;

    @Column(name = "earned_at")
    private Instant earnedAt;
}
