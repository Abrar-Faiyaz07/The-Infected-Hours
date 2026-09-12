package com.infectedhour.shared.network;

import java.util.List;

/**
 * Host → Client(s), broadcast at SNAPSHOT_BROADCAST_HZ (20). Client renders
 * ALL entities (including its own player) via interpolation between
 * snapshots — see TRD §5 and core/net/GameClient's interpolation buffer.
 */
public class WorldSnapshot {
    public long serverTick;
    public List<PlayerState> players;
    public List<EnemyState> enemies;
    public List<CloudFrontierDelta> cloudDeltas;
    public float globalContaminationPct;
    public List<ObjectiveState> objectives;
    public List<ItemState> items;

    public WorldSnapshot() {
    }

    public static class PlayerState {
        public String playerId;
        public CharacterType character;
        public float x, y;
        public float hp;
        public float personalContaminationPct;
        public boolean downed;
        public int reviveSecondsRemaining;
        public String equippedWeapon = "NONE";

        public PlayerState() {
        }
    }

    public static class EnemyState {
        public String enemyId;
        public String type;
        public float x, y;
        public float hp;

        public EnemyState() {
        }
    }

    /** Incremental change to a contamination cloud's tile frontier (TRD §4) rather than the full tile set. */
    public static class CloudFrontierDelta {
        public String cloudId;
        public int[] addedTileIndices;

        public CloudFrontierDelta() {
        }
    }

    public static class ObjectiveState {
        public String objectiveId;
        public String type; // ISOLATE, SAMPLE, MEDICINE, RESCUE, SANITATION (PRD §6)
        public int progress;
        public int target;
        public boolean complete;

        public ObjectiveState() {
        }
    }

    public static class ItemState {
        public String id;
        public float x;
        public float y;
        public String type;

        public ItemState() {
        }
    }
}
