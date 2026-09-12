package com.infectedhour.backend.service;

import com.infectedhour.backend.entity.Player;
import com.infectedhour.backend.repository.PlayerRepository;
import com.infectedhour.shared.constants.GameConstants;
import com.infectedhour.shared.dto.SaveSlotDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Rules for the nine manual save slots. Runs against H2 (MySQL mode) so no
 * MySQL server is needed — same arrangement as the other backend tests.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class SaveSlotServiceTest {

    @Autowired private SaveSlotService saveSlotService;
    @Autowired private PlayerRepository playerRepository;

    private UUID playerId;

    @BeforeEach
    void createPlayer() {
        // Player.username is length = 24, so a full UUID suffix overflows the
        // column and H2 fails the insert with a DataException before any slot
        // logic runs. Eight hex characters are plenty to keep tests unique.
        String unique = UUID.randomUUID().toString().substring(0, 8);
        Player player = playerRepository.save(new Player("slot-" + unique, "Slot Tester", "hash"));
        playerId = player.getId();
    }

    @Test
    @DisplayName("a new player already has nine empty slots")
    void listReturnsNineEmptySlots() {
        List<SaveSlotDto> slots = saveSlotService.listSlots(playerId);

        assertEquals(GameConstants.SAVE_SLOT_COUNT, slots.size());
        for (int i = 0; i < slots.size(); i++) {
            assertEquals(i + 1, slots.get(i).slotNumber(), "slots must be in order");
            assertFalse(slots.get(i).occupied(), "a fresh player has nothing saved");
        }
    }

    @Test
    @DisplayName("saving stores the checkpoint and the carried resources")
    void saveStoresCheckpointAndResources() {
        saveSlotService.saveSlot(playerId, 4, sample(2, "Market District", "cp_sanitation", 62f));

        SaveSlotDto stored = saveSlotService.getSlot(playerId, 4);
        assertTrue(stored.occupied());
        assertEquals(2, stored.levelNumber());
        assertEquals("Market District", stored.levelName());
        assertEquals("cp_sanitation", stored.checkpointId());
        assertEquals(62f, stored.playerHp());
        assertEquals("[{\"item\":\"MEDICINE\",\"qty\":2}]", stored.inventoryJson());
        assertEquals("JANE", stored.characterType());
    }

    @Test
    @DisplayName("saving twice to one slot overwrites rather than duplicating")
    void saveOverwritesInPlace() {
        saveSlotService.saveSlot(playerId, 1, sample(1, "Village Outskirts", "cp_gate", 100f));
        saveSlotService.saveSlot(playerId, 1, sample(3, "The Virus Heart", "cp_arena", 25f));

        SaveSlotDto stored = saveSlotService.getSlot(playerId, 1);
        assertEquals(3, stored.levelNumber());
        assertEquals(25f, stored.playerHp());

        long occupied = saveSlotService.listSlots(playerId).stream()
                .filter(SaveSlotDto::occupied)
                .count();
        assertEquals(1, occupied, "the second save must replace the first, not add a row");
    }

    @Test
    @DisplayName("slots are independent of each other")
    void slotsDoNotInterfere() {
        saveSlotService.saveSlot(playerId, 2, sample(1, "Village Outskirts", "cp_gate", 90f));
        saveSlotService.saveSlot(playerId, 7, sample(2, "Market District", "cp_clinic", 40f));

        assertEquals(1, saveSlotService.getSlot(playerId, 2).levelNumber());
        assertEquals(2, saveSlotService.getSlot(playerId, 7).levelNumber());
        assertFalse(saveSlotService.getSlot(playerId, 5).occupied());
    }

    @Test
    @DisplayName("deleting frees the slot and leaves the others alone")
    void deleteClearsOnlyThatSlot() {
        saveSlotService.saveSlot(playerId, 3, sample(1, "Village Outskirts", "cp_gate", 80f));
        saveSlotService.saveSlot(playerId, 6, sample(2, "Market District", "cp_clinic", 55f));

        saveSlotService.deleteSlot(playerId, 3);

        assertFalse(saveSlotService.getSlot(playerId, 3).occupied());
        assertTrue(saveSlotService.getSlot(playerId, 6).occupied());
    }

    @Test
    @DisplayName("deleting an empty slot is a no-op, not an error")
    void deletingEmptySlotIsHarmless() {
        saveSlotService.deleteSlot(playerId, 9);
        assertFalse(saveSlotService.getSlot(playerId, 9).occupied());
    }

    @Test
    @DisplayName("slot numbers outside 1..9 are rejected")
    void slotNumberIsValidated() {
        SaveSlotDto payload = sample(1, "Village Outskirts", "cp_gate", 100f);
        assertThrows(IllegalArgumentException.class, () -> saveSlotService.saveSlot(playerId, 0, payload));
        assertThrows(IllegalArgumentException.class, () -> saveSlotService.saveSlot(playerId, 10, payload));
        assertThrows(IllegalArgumentException.class, () -> saveSlotService.getSlot(playerId, -1));
    }

    private static SaveSlotDto sample(int level, String levelName, String checkpointId, float hp) {
        return new SaveSlotDto(0, true, level, levelName, checkpointId, "Checkpoint",
                level, 1830L, "JANE", hp, 12f, 34f,
                "[{\"item\":\"MEDICINE\",\"qty\":2}]", "{\"l2_rescue\":2}", null);
    }
}
