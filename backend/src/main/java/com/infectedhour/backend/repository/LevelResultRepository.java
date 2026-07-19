package com.infectedhour.backend.repository;

import com.infectedhour.backend.entity.LevelResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface LevelResultRepository extends JpaRepository<LevelResult, UUID> {
    /** Idempotency check per Backend Schema §4 ("idempotent per level_number"). */
    Optional<LevelResult> findByMatchIdAndLevelNumber(UUID matchId, int levelNumber);
}
