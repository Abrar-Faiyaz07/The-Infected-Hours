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

    private static final List<Checkpoint> ORDERED = List.of(
            // ---- Level 1 — Village Outskirts (7) ----
            new Checkpoint("l1_cp01_gate",        "Outskirts Gate",           1, 1,  4f,  4f),
            new Checkpoint("l1_cp02_well",        "Village Well",             1, 2, 14f,  9f),
            new Checkpoint("l1_cp03_barricade",   "First Barricade Line",     1, 3, 24f, 12f),
            new Checkpoint("l1_cp04_farmhouse",   "Abandoned Farmhouse",      1, 4, 33f, 18f),
            new Checkpoint("l1_cp05_sample",      "Sample Collection Point",  1, 5, 41f, 22f),
            new Checkpoint("l1_cp06_chapel",      "Chapel Safe Zone",         1, 6, 48f, 28f),
            new Checkpoint("l1_cp07_crossing",    "River Crossing",           1, 7, 55f, 34f),

            // ---- Level 2 — Market District (8) ----
            new Checkpoint("l2_cp01_checkpoint",  "District Checkpoint",      2, 1,  5f,  5f),
            new Checkpoint("l2_cp02_stalls",      "Market Stalls",            2, 2, 12f, 11f),
            new Checkpoint("l2_cp03_clinic",      "Clinic Sanitation Post",   2, 3, 20f, 15f),
            new Checkpoint("l2_cp04_pharmacy",    "Ransacked Pharmacy",       2, 4, 28f, 19f),
            new Checkpoint("l2_cp05_shelter",     "Civilian Shelter",         2, 5, 35f, 23f),
            new Checkpoint("l2_cp06_depot",       "Medicine Depot",           2, 6, 42f, 27f),
            new Checkpoint("l2_cp07_rooftops",    "Rooftop Route",            2, 7, 49f, 31f),
            new Checkpoint("l2_cp08_tunnel",      "Service Tunnel Mouth",     2, 8, 56f, 36f),

            // ---- Level 3 — The Virus Heart (7) ----
            new Checkpoint("l3_cp01_descent",     "The Descent",              3, 1,  6f,  6f),
            new Checkpoint("l3_cp02_labs",        "Flooded Laboratories",     3, 2, 15f, 12f),
            new Checkpoint("l3_cp03_containment", "Containment Ring",         3, 3, 25f, 17f),
            new Checkpoint("l3_cp04_antechamber", "Heart Antechamber",        3, 4, 34f, 22f),
            new Checkpoint("l3_cp05_shield",      "Shield Phase Arena",       3, 5, 42f, 26f),
            new Checkpoint("l3_cp06_exposure",    "Exposure Window",          3, 6, 49f, 30f),
            new Checkpoint("l3_cp07_core",        "The Core",                 3, 7, 55f, 34f)
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
