package com.infectedhour.backend.controller;

import com.infectedhour.backend.service.PlayerService;
import com.infectedhour.shared.dto.PlayerDto;
import com.infectedhour.shared.dto.SaveStateDto;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/**
 * GET /players/me, GET/PUT /players/me/save (Backend Schema §4).
 * GET /players/me/matches is on MatchController since it reads match history.
 */
@RestController
@RequestMapping("/api/v1/players/me")
public class PlayerController {

    private final PlayerService playerService;

    public PlayerController(PlayerService playerService) {
        this.playerService = playerService;
    }

    @GetMapping
    public PlayerDto getMe() {
        return playerService.getPlayer(CurrentPlayer.id());
    }

    @GetMapping("/save")
    public SaveStateDto getSave() {
        return playerService.getSave(CurrentPlayer.id());
    }

    @PutMapping("/save")
    public SaveStateDto putSave(@Valid @RequestBody SaveStateDto save) {
        return playerService.putSave(CurrentPlayer.id(), save);
    }
}
