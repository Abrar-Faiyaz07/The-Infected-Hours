package com.infectedhour.backend.service;

import com.infectedhour.backend.entity.Player;
import com.infectedhour.backend.entity.SaveSlot;
import com.infectedhour.backend.repository.PlayerRepository;
import com.infectedhour.backend.repository.SaveSlotRepository;
import com.infectedhour.shared.constants.GameConstants;
import com.infectedhour.shared.dto.SaveSlotDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The nine manual save slots (Backend Schema — save slots addendum).
 *
 * <p>Two rules shape this class:
 * <ul>
 *   <li><b>Always nine.</b> {@link #listSlots} returns a full set whether or not
 *       rows exist, so the launcher renders a fixed 3x3 grid with no null
 *       handling and no "create the row first" dance.</li>
 *   <li><b>Saving replaces.</b> A save to an occupied slot overwrites it in
 *       place rather than inserting, which is what the unique constraint on
 *       (player_id, slot_number) enforces at the database level too.</li>
 * </ul>
 *
 * <p>Loading a slot deliberately does <b>not</b> touch {@code SaveState}.
 * Account progression — highest level unlocked, story progress — is earned and
 * must not be rolled back just because the player reloaded an older save.
 */
@Service
public class SaveSlotService {

    private final SaveSlotRepository saveSlotRepository;
    private final PlayerRepository playerRepository;

    public SaveSlotService(SaveSlotRepository saveSlotRepository, PlayerRepository playerRepository) {
        this.saveSlotRepository = saveSlotRepository;
        this.playerRepository = playerRepository;
    }

    /** All {@value GameConstants#SAVE_SLOT_COUNT} slots, empty placeholders included, in slot order. */
    public List<SaveSlotDto> listSlots(UUID playerId) {
        Map<Integer, SaveSlot> stored = new HashMap<>();
        for (SaveSlot slot : saveSlotRepository.findByPlayerIdOrderBySlotNumberAsc(playerId)) {
            stored.put(slot.getSlotNumber(), slot);
        }

        List<SaveSlotDto> slots = new ArrayList<>(GameConstants.SAVE_SLOT_COUNT);
        for (int n = 1; n <= GameConstants.SAVE_SLOT_COUNT; n++) {
            SaveSlot slot = stored.get(n);
            slots.add(slot == null ? SaveSlotDto.empty(n) : toDto(slot));
        }
        return slots;
    }

    public SaveSlotDto getSlot(UUID playerId, int slotNumber) {
        requireValidSlot(slotNumber);
        return saveSlotRepository.findByPlayerIdAndSlotNumber(playerId, slotNumber)
                .map(SaveSlotService::toDto)
                .orElseGet(() -> SaveSlotDto.empty(slotNumber));
    }

    /** Write (or overwrite) a slot. The path's slot number wins over the body's. */
    @Transactional
    public SaveSlotDto saveSlot(UUID playerId, int slotNumber, SaveSlotDto incoming) {
        requireValidSlot(slotNumber);

        SaveSlot slot = saveSlotRepository.findByPlayerIdAndSlotNumber(playerId, slotNumber)
                .orElseGet(() -> {
                    Player player = playerRepository.findById(playerId)
                            .orElseThrow(() -> new IllegalArgumentException("Player not found"));
                    return new SaveSlot(player, slotNumber);
                });

        slot.overwrite(
                incoming.levelNumber(),
                incoming.levelName(),
                incoming.checkpointId(),
                incoming.checkpointName(),
                incoming.storyProgress(),
                incoming.playtimeSec(),
                incoming.characterType(),
                incoming.playerHp(),
                incoming.personalContaminationPct(),
                incoming.globalContaminationPct(),
                incoming.inventoryJson(),
                incoming.objectivesJson());

        return toDto(saveSlotRepository.save(slot));
    }

    /** Deleting an empty slot is a no-op, not an error — the UI need not check first. */
    @Transactional
    public void deleteSlot(UUID playerId, int slotNumber) {
        requireValidSlot(slotNumber);
        saveSlotRepository.deleteByPlayerIdAndSlotNumber(playerId, slotNumber);
    }

    private static void requireValidSlot(int slotNumber) {
        if (!GameConstants.isValidSaveSlot(slotNumber)) {
            throw new IllegalArgumentException(
                    "Slot must be between 1 and " + GameConstants.SAVE_SLOT_COUNT + ", got " + slotNumber);
        }
    }

    private static SaveSlotDto toDto(SaveSlot slot) {
        return new SaveSlotDto(
                slot.getSlotNumber(),
                true,
                slot.getLevelNumber(),
                slot.getLevelName(),
                slot.getCheckpointId(),
                slot.getCheckpointName(),
                slot.getStoryProgress(),
                slot.getPlaytimeSec(),
                slot.getCharacterType(),
                slot.getPlayerHp(),
                slot.getPersonalContaminationPct(),
                slot.getGlobalContaminationPct(),
                slot.getInventoryJson(),
                slot.getObjectivesJson(),
                slot.getSavedAt().toString());
    }
}
