package com.infectedhour.fxlauncher.save;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.infectedhour.shared.constants.GameConstants;
import com.infectedhour.shared.dto.SaveSlotDto;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * On-disk mirror of the nine save slots at
 * {@code ~/.infectedhour/slots.json} (App Flow §6 offline cache).
 *
 * <p>Why this exists: the backend runs on the host laptop and is optional. A
 * player on the client machine — or anyone playing solo with no server at all —
 * must still be able to save and load. Without a local copy, "Load Game" would
 * be dead whenever the backend is unreachable, which is most of the time during
 * development.
 *
 * <p>The file is the fallback, not the truth. When the backend is reachable its
 * copy wins and is written through to disk; the local file is only read when
 * the network call fails or the player is in offline mode. That ordering
 * matches how {@code SaveState} is reconciled elsewhere.
 *
 * <p>Every method degrades quietly: a corrupt or unreadable file yields nine
 * empty slots rather than an exception, because a broken save file must never
 * prevent the player from reaching the menu.
 */
public final class LocalSaveSlots {

    private static final Logger LOG = Logger.getLogger(LocalSaveSlots.class.getName());
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private LocalSaveSlots() {
    }

    public static Path file() {
        return Paths.get(System.getProperty("user.home"), ".infectedhour", "slots.json");
    }

    /** Always exactly {@value GameConstants#SAVE_SLOT_COUNT} entries, in slot order. */
    public static List<SaveSlotDto> load() {
        Path path = file();
        if (!Files.exists(path)) {
            return emptySet();
        }
        try {
            List<SaveSlotDto> stored = MAPPER.readValue(
                    Files.readAllBytes(path),
                    MAPPER.getTypeFactory().constructCollectionType(List.class, SaveSlotDto.class));
            return normalise(stored);
        } catch (IOException | RuntimeException e) {
            // A corrupt cache is recoverable — the player just sees empty slots.
            LOG.log(Level.WARNING, "Could not read " + path + "; treating all slots as empty", e);
            return emptySet();
        }
    }

    public static void save(List<SaveSlotDto> slots) {
        Path path = file();
        try {
            Files.createDirectories(path.getParent());
            Files.write(path, MAPPER.writerWithDefaultPrettyPrinter()
                    .writeValueAsBytes(normalise(slots)));
        } catch (IOException | RuntimeException e) {
            LOG.log(Level.WARNING, "Could not write " + path, e);
        }
    }

    /** Write one slot into the cached set, leaving the other eight untouched. */
    public static void saveSlot(SaveSlotDto slot) {
        if (slot == null || !GameConstants.isValidSaveSlot(slot.slotNumber())) {
            return;
        }
        List<SaveSlotDto> slots = load();
        slots.set(slot.slotNumber() - 1, slot);
        save(slots);
    }

    public static void deleteSlot(int slotNumber) {
        if (!GameConstants.isValidSaveSlot(slotNumber)) {
            return;
        }
        List<SaveSlotDto> slots = load();
        slots.set(slotNumber - 1, SaveSlotDto.empty(slotNumber));
        save(slots);
    }

    /**
     * Force the list to exactly nine entries indexed by slot number.
     * Guards against a hand-edited or partially-written file producing a list
     * that the 3x3 grid would index out of bounds.
     */
    private static List<SaveSlotDto> normalise(List<SaveSlotDto> input) {
        List<SaveSlotDto> out = emptySet();
        if (input != null) {
            for (SaveSlotDto slot : input) {
                if (slot != null && GameConstants.isValidSaveSlot(slot.slotNumber())) {
                    out.set(slot.slotNumber() - 1, slot);
                }
            }
        }
        return out;
    }

    private static List<SaveSlotDto> emptySet() {
        List<SaveSlotDto> slots = new ArrayList<>(GameConstants.SAVE_SLOT_COUNT);
        for (int n = 1; n <= GameConstants.SAVE_SLOT_COUNT; n++) {
            slots.add(SaveSlotDto.empty(n));
        }
        return slots;
    }
}
