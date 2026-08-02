package com.infectedhour.backend.service;

import com.infectedhour.backend.entity.Player;
import com.infectedhour.backend.entity.SaveState;
import com.infectedhour.backend.repository.PlayerRepository;
import com.infectedhour.backend.repository.SaveStateRepository;
import com.infectedhour.shared.dto.PlayerDto;
import com.infectedhour.shared.dto.SaveStateDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/** GET /players/me, GET/PUT /players/me/save (Backend Schema §4). */
@Service
public class PlayerService {

    private final PlayerRepository playerRepository;
    private final SaveStateRepository saveStateRepository;

    public PlayerService(PlayerRepository playerRepository, SaveStateRepository saveStateRepository) {
        this.playerRepository = playerRepository;
        this.saveStateRepository = saveStateRepository;
    }

    public PlayerDto getPlayer(UUID playerId) {
        Player player = playerRepository.findById(playerId)
                .orElseThrow(() -> new IllegalArgumentException("Player not found"));
        return new PlayerDto(player.getId(), player.getUsername(), player.getDisplayName());
    }

    public SaveStateDto getSave(UUID playerId) {
        SaveState save = saveStateRepository.findByPlayerId(playerId)
                .orElseThrow(() -> new IllegalArgumentException("Save state not found"));
        return toDto(save);
    }

    /** PUT /players/me/save — offline-mode reconciliation, server keeps latest updatedAt (Backend Schema §4). */
    @Transactional
    public SaveStateDto putSave(UUID playerId, SaveStateDto incoming) {
        SaveState save = saveStateRepository.findByPlayerId(playerId)
                .orElseThrow(() -> new IllegalArgumentException("Save state not found"));

        Instant incomingUpdatedAt = Instant.parse(incoming.updatedAt());
        if (incomingUpdatedAt.isAfter(save.getUpdatedAt())) {
            save.applyUpdate(incoming.highestLevelUnlocked(), incoming.storyProgress(), 0);
            save.setSettingsJson(incoming.settingsJson());
        }
        // else: server copy wins (App Flow §6) — silently keep existing save

        return toDto(save);
    }

    private SaveStateDto toDto(SaveState save) {
        return new SaveStateDto(save.getHighestLevelUnlocked(), save.getStoryProgress(),
                save.getTotalPlaytimeSec(), save.getSettingsJson(), save.getUpdatedAt().toString());
    }
}
