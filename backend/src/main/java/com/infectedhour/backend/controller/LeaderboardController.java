package com.infectedhour.backend.controller;

import com.infectedhour.backend.service.LeaderboardService;
import com.infectedhour.shared.dto.LeaderboardEntryDto;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** GET /leaderboards/{board}?limit=20 (Backend Schema §4). */
@RestController
@RequestMapping("/api/v1/leaderboards")
public class LeaderboardController {

    private final LeaderboardService leaderboardService;

    public LeaderboardController(LeaderboardService leaderboardService) {
        this.leaderboardService = leaderboardService;
    }

    @GetMapping("/{board}")
    public List<LeaderboardEntryDto> getBoard(@PathVariable String board,
                                               @RequestParam(defaultValue = "20") int limit) {
        return leaderboardService.getBoard(board, limit);
    }
}
