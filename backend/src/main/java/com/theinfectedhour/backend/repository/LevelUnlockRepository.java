package com.theinfectedhour.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.theinfectedhour.backend.entity.LevelUnlock;
import com.theinfectedhour.backend.entity.LevelUnlockId;

/** Spring Data access for level_unlocks rows. */
public interface LevelUnlockRepository extends JpaRepository<LevelUnlock, LevelUnlockId> {
}
