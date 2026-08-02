package com.infectedhour.backend.repository;

import com.infectedhour.backend.entity.Match;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MatchRepository extends JpaRepository<Match, UUID> {
    List<Match> findByHostPlayerIdOrderByStartedAtDesc(UUID hostPlayerId);
}
