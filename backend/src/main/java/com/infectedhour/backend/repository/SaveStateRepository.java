package com.infectedhour.backend.repository;

import com.infectedhour.backend.entity.SaveState;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SaveStateRepository extends JpaRepository<SaveState, UUID> {
    Optional<SaveState> findByPlayerId(UUID playerId);
}
