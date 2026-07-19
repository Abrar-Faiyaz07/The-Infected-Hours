package com.infectedhour.backend.controller;

import com.infectedhour.backend.service.MatchService;
import com.infectedhour.shared.dto.*;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * POST /matches, POST /matches/{id}/level-result, POST /matches/{id}/complete,
 * GET /players/me/matches?limit=20 (Backend Schema §4). Host calls the
 * write endpoints (TRD §6 save sync flow).
 */
@RestController
@RequestMapping("/api/v1")
public class MatchController {

    private final MatchService matchService;

    public MatchController(MatchService matchService) {
        this.matchService = matchService;
    }

    @PostMapping("/matches")
    public Map<String, UUID> createMatch(@Valid @RequestBody MatchCreateRequest request) {
        UUID matchId = matchService.createMatch(CurrentPlayer.id(), request);
        return Map.of("matchId", matchId);
    }

    @PostMapping("/matches/{id}/level-result")
    public void submitLevelResult(@PathVariable("id") UUID matchId, @Valid @RequestBody LevelResultRequest request) {
        matchService.submitLevelResult(matchId, request);
    }

    @PostMapping("/matches/{id}/complete")
    public void completeMatch(@PathVariable("id") UUID matchId, @Valid @RequestBody MatchCompleteRequest request) {
        matchService.completeMatch(matchId, request);
    }

    @GetMapping("/players/me/matches")
    public List<MatchHistoryEntryDto> getMyMatches(@RequestParam(defaultValue = "20") int limit) {
        return matchService.getHistory(CurrentPlayer.id(), limit);
    }
}
