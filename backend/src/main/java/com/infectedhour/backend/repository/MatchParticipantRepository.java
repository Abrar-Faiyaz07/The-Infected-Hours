package com.infectedhour.backend.repository;

import com.infectedhour.backend.entity.MatchParticipant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface MatchParticipantRepository extends JpaRepository<MatchParticipant, UUID> {
    Optional<MatchParticipant> findByMatchIdAndPlayerId(UUID matchId, UUID playerId);
}
