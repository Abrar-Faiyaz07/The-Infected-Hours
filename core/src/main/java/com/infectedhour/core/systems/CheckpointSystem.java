package com.infectedhour.core.systems;

import com.infectedhour.core.entities.Player;
import com.infectedhour.shared.level.Checkpoint;
import com.infectedhour.shared.level.CheckpointRegistry;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Tracks which checkpoint the run is currently at, on the HOST only.
 *
 * <p>Two separate questions, deliberately kept apart:
 * <ul>
 *   <li><b>"Which checkpoint has the run reached?"</b> — {@link #getLastReachedCheckpoint()}.
 *       This only ever moves forward, so walking back to an earlier checkpoint
 *       does not rewind the save point.</li>
 *   <li><b>"Is a player standing on one right now?"</b> — {@link #checkpointInRange}.
 *       This is what gates the "hold E to save" prompt, and it comes and goes as
 *       the player moves.</li>
 * </ul>
 *
 * <p>Both matter: the first is what gets written into a save slot, the second is
 * what tells the player they <em>can</em> save. Conflating them would either let
 * the player save anywhere, or lose their progress marker the moment they
 * stepped off the tile.
 *
 * <p>Kept free of any libGDX dependency so it stays headless-testable.
 */
public class CheckpointSystem {

    /** How close, in tiles, a player must be for the save prompt to appear. */
    private static final float ACTIVATION_RADIUS_TILES = 1.5f;

    private final Set<String> reached = new LinkedHashSet<>();
    private Checkpoint lastReached;
    private int currentLevel = 1;

    public CheckpointSystem() {
        this(1);
    }

    public CheckpointSystem(int startingLevel) {
        enterLevel(startingLevel);
    }

    /** Called when a level begins fresh — the run starts at that level's first checkpoint. */
    public void enterLevel(int levelNumber) {
        this.currentLevel = levelNumber;
        markReached(CheckpointRegistry.firstOf(levelNumber));
    }

    /**
     * Called when a save is loaded: drop the run straight onto a stored checkpoint.
     * Everything at or before it in the same level counts as already reached, so
     * campaign progress is not understated after a load.
     */
    public void restoreTo(String checkpointId) {
        Checkpoint target = CheckpointRegistry.byId(checkpointId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown checkpoint: " + checkpointId));
        this.currentLevel = target.levelNumber();
        for (Checkpoint checkpoint : CheckpointRegistry.forLevel(target.levelNumber())) {
            if (checkpoint.orderInLevel() <= target.orderInLevel()) {
                reached.add(checkpoint.id());
            }
        }
        this.lastReached = target;
    }

    /**
     * Advance the run's save point if this checkpoint is further in than the
     * current one. Backtracking is ignored.
     *
     * @return true if this was genuinely new progress
     */
    public boolean markReached(Checkpoint checkpoint) {
        if (checkpoint == null) {
            return false;
        }
        boolean isNew = reached.add(checkpoint.id());
        if (lastReached == null
                || checkpoint.levelNumber() > lastReached.levelNumber()
                || (checkpoint.levelNumber() == lastReached.levelNumber()
                    && checkpoint.orderInLevel() > lastReached.orderInLevel())) {
            lastReached = checkpoint;
        }
        return isNew;
    }

    /**
     * The checkpoint a player is close enough to save at, if any.
     * Nearest wins when two overlap.
     */
    public Optional<Checkpoint> checkpointInRange(Player player) {
        if (player == null) {
            return Optional.empty();
        }
        Checkpoint nearest = null;
        float nearestDistSq = Float.MAX_VALUE;

        for (Checkpoint checkpoint : CheckpointRegistry.forLevel(currentLevel)) {
            float dx = checkpoint.spawnTileX() - player.getX();
            float dy = checkpoint.spawnTileY() - player.getY();
            float distSq = dx * dx + dy * dy;
            if (distSq <= ACTIVATION_RADIUS_TILES * ACTIVATION_RADIUS_TILES && distSq < nearestDistSq) {
                nearest = checkpoint;
                nearestDistSq = distSq;
            }
        }
        return Optional.ofNullable(nearest);
    }

    /**
     * Walk every player past the checkpoints of the current level and record any
     * they have entered. Call once per simulation tick on the host.
     *
     * @return a newly reached checkpoint, if this tick produced one — the caller
     *         uses it to fire the "Checkpoint reached" toast
     */
    public Optional<Checkpoint> updateAndDetectNew(List<Player> players) {
        Checkpoint newlyReached = null;
        for (Player player : players) {
            Optional<Checkpoint> inRange = checkpointInRange(player);
            if (inRange.isPresent() && markReached(inRange.get())) {
                newlyReached = inRange.get();
            }
        }
        return Optional.ofNullable(newlyReached);
    }

    /** What a save slot records — where the run had got to. */
    public Checkpoint getLastReachedCheckpoint() {
        return lastReached;
    }

    public String getLastReachedId() {
        return lastReached == null ? null : lastReached.id();
    }

    public int getCurrentLevel() {
        return currentLevel;
    }

    public boolean hasReached(String checkpointId) {
        return reached.contains(checkpointId);
    }

    public int reachedCount() {
        return reached.size();
    }

    /** 0..100 across all {@value CheckpointRegistry#TOTAL_CHECKPOINTS} checkpoints. */
    public int campaignProgressPct() {
        return lastReached == null ? 0 : CheckpointRegistry.campaignProgressPct(lastReached.id());
    }
}
