package com.theinfectedhour.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.theinfectedhour.backend.entity.SaveGame;

/** Spring Data access for save_games rows. */
public interface SaveGameRepository extends JpaRepository<SaveGame, Long> {
}
