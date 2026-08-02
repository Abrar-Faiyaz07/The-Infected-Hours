package com.infectedhour.backend.service;

import com.infectedhour.backend.repository.LeaderboardEntryRepository;
import com.infectedhour.shared.dto.LeaderboardEntryDto;
import org.springframework.stereotype.Service;

import java.util.List;

/** GET /leaderboards/{board} (Backend Schema §4). */
@Service
public class LeaderboardService {

    private final LeaderboardEntryRepository leaderboardRepository;

    public LeaderboardService(LeaderboardEntryRepository leaderboardRepository) {
        this.leaderboardRepository = leaderboardRepository;
    }

    public List<LeaderboardEntryDto> getBoard(String board, int limit) {
        var entries = leaderboardRepository.findByBoardOrderByValueAsc(board);

        int rank = 1;
        return entries.stream()
                .limit(limit)
                .map(entry -> new LeaderboardEntryDto(
                        entryRank(entries, entry),
                        entry.getPlayer().getDisplayName(),
                        entry.getValue(),
                        entry.getAchievedAt().toString()))
                .toList();
    }

    private int entryRank(List<com.infectedhour.backend.entity.LeaderboardEntry> sortedEntries,
                           com.infectedhour.backend.entity.LeaderboardEntry entry) {
        return sortedEntries.indexOf(entry) + 1;
    }
}
