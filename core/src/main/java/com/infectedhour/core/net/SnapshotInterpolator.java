package com.infectedhour.core.net;

import com.infectedhour.shared.network.WorldSnapshot;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Buffers incoming WorldSnapshots and returns an interpolated view ~100ms
 * behind real time (TRD §5), so the client renders smooth motion between
 * the host's 20Hz snapshot broadcasts without any prediction/reconciliation.
 *
 * Skeleton only: buffering is in place, actual lerp-between-two-snapshots
 * math is TODO (interpolate PlayerState/EnemyState fields between the two
 * snapshots that straddle renderTimeMs - bufferMs).
 */
public class SnapshotInterpolator {

    private final int bufferMs;
    private final Deque<TimedSnapshot> buffer = new ArrayDeque<>();
    private static final int MAX_BUFFERED = 30;

    public SnapshotInterpolator(int bufferMs) {
        this.bufferMs = bufferMs;
    }

    public void pushSnapshot(WorldSnapshot snapshot) {
        buffer.addLast(new TimedSnapshot(System.currentTimeMillis(), snapshot));
        while (buffer.size() > MAX_BUFFERED) {
            buffer.removeFirst();
        }
    }

    /**
     * Returns the most recent snapshot older than (renderTimeMs - bufferMs).
     *
     * TEAMMATE TASK (net): upgrade to TRUE interpolation for smooth motion.
     *  1. Find the two snapshots A (just before targetTime) and B (just after).
     *  2. alpha = (targetTime - A.time) / (B.time - A.time)   // 0..1
     *  3. Build a display snapshot where every entity position =
     *     lerp(A.pos, B.pos, alpha):  x = ax + (bx - ax) * alpha
     *  4. Non-positional fields (hp, downed, objectives) just take B's value.
     *  The current fallback (latest-older snapshot) works but visibly
     *  stutters at 20Hz — do this before the Day-5 playtest.
     */
    public WorldSnapshot getInterpolated(long renderTimeMs) {
        long targetTime = renderTimeMs - bufferMs;
        WorldSnapshot best = null;
        for (TimedSnapshot ts : buffer) {
            if (ts.receivedAtMs <= targetTime) {
                best = ts.snapshot;
            } else {
                break;
            }
        }
        return best != null ? best : (buffer.isEmpty() ? null : buffer.peekLast().snapshot);
    }

    private record TimedSnapshot(long receivedAtMs, WorldSnapshot snapshot) {
    }
}
