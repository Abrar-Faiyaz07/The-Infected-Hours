package com.infectedhour.core.systems;

import java.util.HashMap;
import java.util.Map;

/**
 * State machine for the 6 PRD §6 mission types. Each objective is tracked
 * as progress/target so the same class drives isolate, sample, medicine,
 * rescue, and sanitation objectives without a subclass per type.
 */
public class ObjectiveSystem {

    public enum ObjectiveType {
        ISOLATE_ZONE, COLLECT_SAMPLE, DELIVER_MEDICINE, RESCUE_VILLAGER, ACTIVATE_SANITATION
    }

    public static class ObjectiveState {
        public final String id;
        public final ObjectiveType type;
        public int progress;
        public final int target;

        public ObjectiveState(String id, ObjectiveType type, int target) {
            this.id = id;
            this.type = type;
            this.target = target;
        }

        public boolean isComplete() {
            return progress >= target;
        }
    }

    private final Map<String, ObjectiveState> objectives = new HashMap<>();

    public void register(String id, ObjectiveType type, int target) {
        objectives.put(id, new ObjectiveState(id, type, target));
    }

    public void incrementProgress(String id) {
        ObjectiveState state = objectives.get(id);
        if (state == null) {
            throw new IllegalArgumentException("Unknown objective: " + id);
        }
        if (!state.isComplete()) {
            state.progress++;
        }
    }

    public boolean isObjectiveComplete(String id) {
        ObjectiveState state = objectives.get(id);
        return state != null && state.isComplete();
    }

    public boolean areAllObjectivesComplete() {
        return objectives.values().stream().allMatch(ObjectiveState::isComplete);
    }

    public Map<String, ObjectiveState> getObjectives() {
        return objectives;
    }
}
