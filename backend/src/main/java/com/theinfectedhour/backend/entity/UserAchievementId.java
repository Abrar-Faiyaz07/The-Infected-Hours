package com.theinfectedhour.backend.entity;

import java.io.Serializable;
import java.util.Objects;

/** Composite key (user_id, achievement_id) for UserAchievement. */
public class UserAchievementId implements Serializable {

    private Long userId;
    private Long achievementId;

    public UserAchievementId() {
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof UserAchievementId other)) {
            return false;
        }
        return Objects.equals(userId, other.userId) && Objects.equals(achievementId, other.achievementId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, achievementId);
    }
}
