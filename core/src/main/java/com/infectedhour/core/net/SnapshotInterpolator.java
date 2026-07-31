package com.infectedhour.core.net;

import com.infectedhour.shared.network.WorldSnapshot;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

/**
 * Buffers incoming {@link WorldSnapshot}s and returns a view of the world
 * ~100 ms behind real time (TRD §5), so the client renders smooth motion
 * between the host's 20 Hz broadcasts with no prediction or reconciliation.
 *
 * <h2>Why a delay buffer at all</h2>
 * Snapshots land every ~50 ms. Drawing the newest one directly gives 20 FPS of
 * visible motion inside a 60 FPS render loop — entities teleport. Rendering
 * {@code now - 100ms} instead means there is almost always a snapshot on
 * <em>both</em> sides of the display time, so positions can be interpolated
 * between them. The cost is a fixed 100 ms of visual lag, which on a LAN is
 * far less objectionable than stutter.
 *
 * <h2>Thread safety</h2>
 * {@link #pushSnapshot} is called from KryoNet's listener thread while
 * {@link #getInterpolated} is called from the render thread, so the buffer is
 * guarded. The lock is held only for the deque scan, never for interpolation.
 */
public class SnapshotInterpolator {

    private final int bufferMs;
    private final Deque<TimedSnapshot> buffer = new ArrayDeque<>();
    private final Object lock = new Object();
    private static final int MAX_BUFFERED = 30;

    public SnapshotInterpolator(int bufferMs) {
        this.bufferMs = bufferMs;
    }

    public void pushSnapshot(WorldSnapshot snapshot) {
        if (snapshot == null) {
            return;
        }
        synchronized (lock) {
            // UDP can deliver out of order; serverTick is the authority on age.
            if (!buffer.isEmpty() && snapshot.serverTick <= buffer.peekLast().snapshot.serverTick) {
                return;
            }
            buffer.addLast(new TimedSnapshot(System.currentTimeMillis(), snapshot));
            while (buffer.size() > MAX_BUFFERED) {
                buffer.removeFirst();
            }
        }
    }

    /**
     * The world as it should be drawn right now: positions linearly
     * interpolated between the two snapshots straddling
     * {@code renderTimeMs - bufferMs}.
     *
     * @return an interpolated snapshot, or the newest one available when the
     *         buffer cannot straddle the target time (first packets, or a
     *         stall). Null only before the very first packet arrives.
     */
    public WorldSnapshot getInterpolated(long renderTimeMs) {
        long targetTime = renderTimeMs - bufferMs;

        TimedSnapshot before = null;
        TimedSnapshot after = null;
        TimedSnapshot newest;

        synchronized (lock) {
            if (buffer.isEmpty()) {
                return null;
            }
            newest = buffer.peekLast();
            for (TimedSnapshot candidate : buffer) {
                if (candidate.receivedAtMs <= targetTime) {
                    before = candidate;
                } else {
                    after = candidate;
                    break;
                }
            }
        }

        if (before == null) {
            // Target time predates everything buffered — still filling up.
            return after != null ? after.snapshot : newest.snapshot;
        }
        if (after == null) {
            // Nothing newer than the target: the stream stalled, so hold the last known state.
            return before.snapshot;
        }

        long span = after.receivedAtMs - before.receivedAtMs;
        float alpha = span <= 0 ? 1f : (float) (targetTime - before.receivedAtMs) / span;
        alpha = Math.max(0f, Math.min(1f, alpha));
        return blend(before.snapshot, after.snapshot, alpha);
    }

    /**
     * Positions lerp; everything else snaps to the newer snapshot.
     *
     * <p>Interpolating HP or objective progress would render values the host
     * never simulated — a half-completed pickup, 73.4 HP. Only continuous
     * spatial quantities are safe to blend.
     */
    private static WorldSnapshot blend(WorldSnapshot from, WorldSnapshot to, float alpha) {
        WorldSnapshot out = new WorldSnapshot();
        out.serverTick = to.serverTick;
        out.globalContaminationPct = lerp(from.globalContaminationPct, to.globalContaminationPct, alpha);
        out.objectives = to.objectives;
        out.cloudDeltas = to.cloudDeltas;

        out.players = new ArrayList<>();
        if (to.players != null) {
            Map<String, WorldSnapshot.PlayerState> previous = indexPlayers(from);
            for (WorldSnapshot.PlayerState target : to.players) {
                WorldSnapshot.PlayerState start = previous.get(target.playerId);
                WorldSnapshot.PlayerState blended = new WorldSnapshot.PlayerState();
                blended.playerId = target.playerId;
                blended.character = target.character;
                blended.hp = target.hp;
                blended.personalContaminationPct = target.personalContaminationPct;
                blended.downed = target.downed;
                blended.reviveSecondsRemaining = target.reviveSecondsRemaining;
                blended.x = start == null ? target.x : lerp(start.x, target.x, alpha);
                blended.y = start == null ? target.y : lerp(start.y, target.y, alpha);
                out.players.add(blended);
            }
        }

        out.enemies = new ArrayList<>();
        if (to.enemies != null) {
            Map<String, WorldSnapshot.EnemyState> previous = indexEnemies(from);
            for (WorldSnapshot.EnemyState target : to.enemies) {
                WorldSnapshot.EnemyState start = previous.get(target.enemyId);
                WorldSnapshot.EnemyState blended = new WorldSnapshot.EnemyState();
                blended.enemyId = target.enemyId;
                blended.type = target.type;
                blended.hp = target.hp;
                blended.x = start == null ? target.x : lerp(start.x, target.x, alpha);
                blended.y = start == null ? target.y : lerp(start.y, target.y, alpha);
                out.enemies.add(blended);
            }
        }
        return out;
    }

    private static Map<String, WorldSnapshot.PlayerState> indexPlayers(WorldSnapshot snapshot) {
        Map<String, WorldSnapshot.PlayerState> index = new HashMap<>();
        if (snapshot != null && snapshot.players != null) {
            for (WorldSnapshot.PlayerState state : snapshot.players) {
                index.put(state.playerId, state);
            }
        }
        return index;
    }

    private static Map<String, WorldSnapshot.EnemyState> indexEnemies(WorldSnapshot snapshot) {
        Map<String, WorldSnapshot.EnemyState> index = new HashMap<>();
        if (snapshot != null && snapshot.enemies != null) {
            for (WorldSnapshot.EnemyState state : snapshot.enemies) {
                index.put(state.enemyId, state);
            }
        }
        return index;
    }

    private static float lerp(float from, float to, float alpha) {
        return from + (to - from) * alpha;
    }

    /** Number of snapshots currently buffered — useful in tests and a net-debug overlay. */
    public int bufferedCount() {
        synchronized (lock) {
            return buffer.size();
        }
    }

    private record TimedSnapshot(long receivedAtMs, WorldSnapshot snapshot) {
    }
}
