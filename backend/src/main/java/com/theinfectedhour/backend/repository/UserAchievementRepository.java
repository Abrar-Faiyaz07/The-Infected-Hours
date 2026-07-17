package com.theinfectedhour.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.theinfectedhour.backend.entity.UserAchievement;
import com.theinfectedhour.backend.entity.UserAchievementId;

/** Spring Data access for user_achievements rows. */
public interface UserAchievementRepository extends JpaRepository<UserAchievement, UserAchievementId> {
}
