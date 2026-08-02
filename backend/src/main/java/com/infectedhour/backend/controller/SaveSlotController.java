package com.infectedhour.backend.controller;

import com.infectedhour.backend.service.SaveSlotService;
import com.infectedhour.shared.dto.SaveSlotDto;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * The nine manual save slots for the signed-in player.
 *
 * <pre>
 *   GET    /api/v1/players/me/slots        -> all 9, empty ones included
 *   GET    /api/v1/players/me/slots/{n}    -> one slot
 *   PUT    /api/v1/players/me/slots/{n}    -> save / overwrite
 *   DELETE /api/v1/players/me/slots/{n}    -> clear
 * </pre>
 *
 * <p>The player id always comes from {@link CurrentPlayer} (the JWT), never from
 * the request body — otherwise one player could read or overwrite another's
 * saves simply by sending a different id.
 */
@RestController
@RequestMapping("/api/v1/players/me/slots")
public class SaveSlotController {

    private final SaveSlotService saveSlotService;

    public SaveSlotController(SaveSlotService saveSlotService) {
        this.saveSlotService = saveSlotService;
    }

    @GetMapping
    public List<SaveSlotDto> listSlots() {
        return saveSlotService.listSlots(CurrentPlayer.id());
    }

    @GetMapping("/{slotNumber}")
    public SaveSlotDto getSlot(@PathVariable int slotNumber) {
        return saveSlotService.getSlot(CurrentPlayer.id(), slotNumber);
    }

    @PutMapping("/{slotNumber}")
    public SaveSlotDto saveSlot(@PathVariable int slotNumber, @Valid @RequestBody SaveSlotDto slot) {
        return saveSlotService.saveSlot(CurrentPlayer.id(), slotNumber, slot);
    }

    @DeleteMapping("/{slotNumber}")
    public ResponseEntity<Void> deleteSlot(@PathVariable int slotNumber) {
        saveSlotService.deleteSlot(CurrentPlayer.id(), slotNumber);
        return ResponseEntity.noContent().build();
    }
}
