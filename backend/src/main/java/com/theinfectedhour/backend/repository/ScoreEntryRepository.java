package com.theinfectedhour.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.theinfectedhour.backend.entity.ScoreEntry;

/** Spring Data access for score_entries rows (leaderboard queries). */
public interface ScoreEntryRepository extends JpaRepository<ScoreEntry, Long> {
}
