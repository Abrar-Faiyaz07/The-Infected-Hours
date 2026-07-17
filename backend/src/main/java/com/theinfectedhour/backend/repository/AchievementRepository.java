package com.theinfectedhour.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.theinfectedhour.backend.entity.Achievement;

/** Spring Data access for achievements rows. */
public interface AchievementRepository extends JpaRepository<Achievement, Long> {
}
