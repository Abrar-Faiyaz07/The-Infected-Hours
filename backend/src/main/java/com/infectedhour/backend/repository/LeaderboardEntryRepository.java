package com.infectedhour.backend.repository;

import com.infectedhour.backend.entity.LeaderboardEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LeaderboardEntryRepository extends JpaRepository<LeaderboardEntry, UUID> {
    Optional<LeaderboardEntry> findByPlayerIdAndBoard(UUID playerId, String board);

    /** Lower value = better for all boards (Backend Schema §4). */
    List<LeaderboardEntry> findByBoardOrderByValueAsc(String board);
}
