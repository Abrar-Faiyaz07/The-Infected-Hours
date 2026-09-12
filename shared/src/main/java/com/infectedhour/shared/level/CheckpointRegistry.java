package com.infectedhour.shared.level;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The 22 checkpoints of the campaign, in play order.
 *
 * <p>Split 7 / 8 / 7 across the three levels. The ids are a permanent contract:
 * they are written into {@code save_slot.checkpoint_id}, so renaming one
 * invalidates every save that references it. Add new checkpoints at the end of
 * a level's block rather than renumbering the existing ones.
 *
 * <p>The registry is the single source of truth for "where can the game be
 * saved and resumed", which is what makes a save slot restorable: a slot stores
 * only the checkpoint <b>id</b>, and everything else — spawn position, level,
 * display name — is looked up here at load time. Storing coordinates in the
 * save row instead would freeze them, so moving a checkpoint during development
 * would silently strand old saves in walls.
 */
public final class CheckpointRegistry {

    public static final int TOTAL_CHECKPOINTS = 22;

    /**
     * Spawn coordinates are <b>tile centres</b> (x.5, y.5) and every one is
     * validated against the collision grids in {@code core/resources/maps/} by
     * {@code CheckpointPlacementTest}.
     *
     * <p>The first version used round numbers spread over an assumed 60x40 grid.
     * Level 1 is actually 45x33, so two checkpoints sat outside the map and two
     * inside walls. Nothing crashed — they simply never triggered, which is a
     * far harder failure to notice than a crash.
     *
     * <p>Integer coordinates are avoided deliberately: an integer sits on the
     * boundary between two tiles, so a 0.25-radius collider straddles both and
     * can clip a wall that touches only one of them.
     */
    private static final List<Checkpoint> ORDERED = List.of(
            // ---- Level 1 — Village Outskirts, 45x33 (7) ----
            new Checkpoint("l1_cp01_gate",        "Outskirts Gate",           1, 1,  9.5f,  8.5f),
            new Checkpoint("l1_cp02_well",        "Village Well",             1, 2,  1.5f, 19.5f),
            new Checkpoint("l1_cp03_barricade",   "First Barricade Line",     1, 3, 15.5f, 22.5f),
            new Checkpoint("l1_cp04_farmhouse",   "Abandoned Farmhouse",      1, 4, 20.5f,  5.5f),
            new Checkpoint("l1_cp05_sample",      "Sample Collection Point",  1, 5, 25.5f, 23.5f),
            new Checkpoint("l1_cp06_chapel",      "Chapel Safe Zone",         1, 6, 30.5f, 14.5f),
            new Checkpoint("l1_cp07_crossing",    "River Crossing",           1, 7, 35.5f, 17.5f),

            // ---- Level 2 — Market District, 60x40 (8) ----
            new Checkpoint("l2_cp01_checkpoint",  "District Checkpoint",      2, 1,  1.5f,  1.5f),
            new Checkpoint("l2_cp02_stalls",      "Market Stalls",            2, 2,  7.5f, 21.5f),
            new Checkpoint("l2_cp03_clinic",      "Clinic Sanitation Post",   2, 3, 14.5f,  4.5f),
            new Checkpoint("l2_cp04_pharmacy",    "Ransacked Pharmacy",       2, 4, 22.5f, 34.5f),
            new Checkpoint("l2_cp05_shelter",     "Civilian Shelter",         2, 5, 31.5f, 27.5f),
            new Checkpoint("l2_cp06_depot",       "Medicine Depot",           2, 6, 39.5f, 21.5f),
            new Checkpoint("l2_cp07_rooftops",    "Rooftop Route",            2, 7, 46.5f,  4.5f),
            new Checkpoint("l2_cp08_tunnel",      "Service Tunnel Mouth",     2, 8, 53.5f, 23.5f),

            // ---- Level 3 — The Virus Heart, 60x40 (7) ----
            new Checkpoint("l3_cp01_descent",     "The Descent",              3, 1,  1.5f,  1.5f),
            new Checkpoint("l3_cp02_labs",        "Flooded Laboratories",     3, 2,  8.5f, 26.5f),
            new Checkpoint("l3_cp03_containment", "Containment Ring",         3, 3, 16.5f, 32.5f),
            new Checkpoint("l3_cp04_antechamber", "Heart Antechamber",        3, 4, 25.5f, 10.5f),
            new Checkpoint("l3_cp05_shield",      "Shield Phase Arena",       3, 5, 33.5f, 25.5f),
            new Checkpoint("l3_cp06_exposure",    "Exposure Window",          3, 6, 42.5f,  3.5f),
            new Checkpoint("l3_cp07_core",        "The Core",                 3, 7, 50.5f,  9.5f)
    );

    private static final Map<String, Checkpoint> BY_ID = buildIndex();

    private CheckpointRegistry() {
    }

    private static Map<String, Checkpoint> buildIndex() {
        Map<String, Checkpoint> index = new LinkedHashMap<>();
        for (Checkpoint checkpoint : ORDERED) {
            if (index.put(checkpoint.id(), checkpoint) != null) {
                // A duplicate id would make saves ambiguous — fail at class-load,
                // not silently at 3am during the demo.
                throw new IllegalStateException("Duplicate checkpoint id: " + checkpoint.id());
            }
        }
        return index;
    }

    /** All 22, in play order. */
    public static List<Checkpoint> all() {
        return ORDERED;
    }

    /** The checkpoints of one level, in order. */
    public static List<Checkpoint> forLevel(int levelNumber) {
        List<Checkpoint> result = new ArrayList<>();
        for (Checkpoint checkpoint : ORDERED) {
            if (checkpoint.levelNumber() == levelNumber) {
                result.add(checkpoint);
            }
        }
        return result;
    }

    public static Optional<Checkpoint> byId(String id) {
        return Optional.ofNullable(id == null ? null : BY_ID.get(id));
    }

    public static boolean exists(String id) {
        return id != null && BY_ID.containsKey(id);
    }

    /** Where a level begins when starting fresh rather than loading a save. */
    public static Checkpoint firstOf(int levelNumber) {
        return forLevel(levelNumber).stream()
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No checkpoints for level " + levelNumber));
    }

    /** The next checkpoint after this one, or empty at the end of the campaign. */
    public static Optional<Checkpoint> next(String id) {
        int index = indexOf(id);
        return (index < 0 || index + 1 >= ORDERED.size())
                ? Optional.empty()
                : Optional.of(ORDERED.get(index + 1));
    }

    /**
     * 0..100 — how far through the whole campaign this checkpoint is.
     * Drives the "42% complete" line on a save slot card.
     */
    public static int campaignProgressPct(String id) {
        int index = indexOf(id);
        return index < 0 ? 0 : Math.round((index + 1) * 100f / ORDERED.size());
    }

    private static int indexOf(String id) {
        for (int i = 0; i < ORDERED.size(); i++) {
            if (ORDERED.get(i).id().equals(id)) {
                return i;
            }
        }
        return -1;
    }
}
