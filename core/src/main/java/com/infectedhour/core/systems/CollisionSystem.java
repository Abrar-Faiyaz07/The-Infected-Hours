package com.infectedhour.core.systems;

import com.infectedhour.core.entities.Collidable;
import com.infectedhour.core.level.TileMap;

import java.util.List;

/**
 * Host-authoritative collision (TRD §4): circle-vs-tile blocking with wall
 * sliding, plus circle-vs-circle separation between entities.
 *
 * <p>This runs <b>only on the host</b>, inside the 60Hz simulation tick. Clients
 * render interpolated snapshots and never resolve collision themselves, so this
 * class stays out of the network path entirely — nothing here changes the wire
 * protocol.
 *
 * <p>Deliberately free of libGDX and KryoNet types so the whole thing is
 * headless-testable (TRD §10), and shared by {@link MovementSystem} and
 * {@link AISystem} so players and enemies obey exactly one collision rule.
 *
 * <p><b>Not thread-safe</b> — it is called from the simulation thread only.
 */
public class CollisionSystem {

    /**
     * Longest displacement resolved in one step, in tiles. Anything larger is
     * split so a fast mover cannot tunnel through a one-tile wall. At the sprint
     * speed of 6.4 tiles/s a 60Hz step is ~0.107 tiles, so normal movement never
     * substeps; this only matters for dash-style abilities.
     */
    private static final float MAX_STEP_TILES = 0.25f;

    /** Below this, two entities count as exactly coincident and need an arbitrary push direction. */
    private static final float COINCIDENT_EPSILON = 1e-4f;

    /**
     * How far, in tiles, an entity may be nudged sideways to slip past a corner
     * it has clipped. Without this, brushing a door frame stops movement dead
     * and the player has to shuffle until they are aligned to the gap — the
     * single biggest cause of movement feeling sticky in tight doorways.
     */
    private static final float CORNER_SLIP_TILES = 0.22f;

    /** Slip is attempted in this many increments, nearest offset first. */
    private static final int CORNER_SLIP_STEPS = 4;

    private TileMap tileMap;

    public CollisionSystem() {
    }

    public CollisionSystem(TileMap tileMap) {
        this.tileMap = tileMap;
    }

    /** Swaps in the grid for the current level. {@code null} disables tile blocking (entity separation still runs). */
    public void setTileMap(TileMap tileMap) {
        this.tileMap = tileMap;
    }

    public TileMap getTileMap() {
        return tileMap;
    }

    /**
     * Moves {@code entity} by up to {@code (dx, dy)}, stopping at walls.
     *
     * <p>The two axes are resolved separately, which is what produces wall
     * sliding: walking diagonally into a vertical wall keeps the vertical
     * component and drops only the horizontal one, instead of stopping dead.
     *
     * @return {@code true} if a wall absorbed part of the requested movement
     */
    public boolean moveWithCollision(Collidable entity, float dx, float dy) {
        if (!isFinite(dx) || !isFinite(dy) || (dx == 0f && dy == 0f)) {
            return false;
        }

        int steps = stepCountFor(dx, dy);
        float stepX = dx / steps;
        float stepY = dy / steps;
        float radius = entity.getCollisionRadius();
        boolean blocked = false;

        for (int i = 0; i < steps; i++) {
            float x = entity.getX();
            float y = entity.getY();

            float movedX = 0f;
            float movedY = 0f;
            float slipX = 0f;
            float slipY = 0f;

            if (stepX != 0f) {
                if (!overlapsBlockedTile(x + stepX, y, radius)) {
                    movedX = stepX;
                } else {
                    blocked = true;
                    // Clipped a corner while moving horizontally: look for a small
                    // vertical offset that clears it, so the entity rounds the
                    // frame instead of stopping against it.
                    slipY = findCornerSlip(x + stepX, y, radius, false);
                    if (slipY != 0f) {
                        movedX = stepX;
                    }
                }
            }

            // Test the vertical move from the already-resolved horizontal position,
            // otherwise the entity could slide into a corner it does not fit through.
            if (stepY != 0f) {
                if (!overlapsBlockedTile(x + movedX, y + slipY + stepY, radius)) {
                    movedY = stepY;
                } else {
                    blocked = true;
                    slipX = findCornerSlip(x + movedX, y + slipY + stepY, radius, true);
                    if (slipX != 0f) {
                        movedY = stepY;
                    }
                }
            }

            if (movedX == 0f && movedY == 0f && slipX == 0f && slipY == 0f) {
                break; // fully walled in on both axes; further substeps cannot help
            }
            entity.move(movedX + slipX, movedY + slipY);
        }
        return blocked;
    }

    /**
     * Is a circle of {@code radius} at {@code (centreX, centreY)} intersecting a
     * blocked tile? Out-of-bounds counts as blocked, so the map edge needs no
     * explicit wall ring.
     */
    public boolean overlapsBlockedTile(float centreX, float centreY, float radius) {
        if (tileMap == null) {
            return false;
        }
        // Tested against collision CELLS, not gameplay tiles: the level art's
        // walls do not align to the tile grid, so a tile-resolution test would
        // block walkable corridor or let players clip into walls.
        float cellSize = tileMap.getCellSize();
        int minCellX = tileMap.toCell(centreX - radius);
        int maxCellX = tileMap.toCell(centreX + radius);
        int minCellY = tileMap.toCell(centreY - radius);
        int maxCellY = tileMap.toCell(centreY + radius);

        for (int cellY = minCellY; cellY <= maxCellY; cellY++) {
            for (int cellX = minCellX; cellX <= maxCellX; cellX++) {
                if (tileMap.isCellWalkable(cellX, cellY)) {
                    continue;
                }
                // Closest point on the cell rectangle to the circle centre.
                float cellMinX = cellX * cellSize;
                float cellMinY = cellY * cellSize;
                float nearestX = clamp(centreX, cellMinX, cellMinX + cellSize);
                float nearestY = clamp(centreY, cellMinY, cellMinY + cellSize);
                float offsetX = centreX - nearestX;
                float offsetY = centreY - nearestY;
                // Strictly-less: resting flush against a wall is contact, not overlap,
                // so a stopped entity never reads as stuck inside geometry.
                if (offsetX * offsetX + offsetY * offsetY < radius * radius) {
                    return true;
                }
            }
        }
        return false;
    }

    /** Convenience overload for callers that only care whether a point is on a walkable tile. */
    public boolean isWalkableAt(float worldX, float worldY) {
        return tileMap == null || tileMap.isWalkableAt(worldX, worldY);
    }

    /**
     * Pushes every overlapping pair in {@code entities} apart, so players and
     * enemies cannot stack on the same spot.
     *
     * <p>O(n²), which is correct for the handful of entities this build spawns.
     * TRD §4 calls for a spatial hash above ~50 enemies — do that only if a
     * profiler says the tick is actually suffering.
     */
    public void separateAll(List<? extends Collidable> entities) {
        for (int i = 0; i < entities.size(); i++) {
            for (int j = i + 1; j < entities.size(); j++) {
                separate(entities.get(i), entities.get(j));
            }
        }
    }

    /**
     * Resolves one overlapping pair by pushing both halfway out along the line
     * between them. The push itself goes through
     * {@link #moveWithCollision}, so separating two entities can never shove
     * either of them through a wall.
     *
     * @return {@code true} if the pair overlapped and was pushed apart
     */
    public boolean separate(Collidable a, Collidable b) {
        if (a == b) {
            return false;
        }
        float offsetX = b.getX() - a.getX();
        float offsetY = b.getY() - a.getY();
        float distanceSq = offsetX * offsetX + offsetY * offsetY;
        float minDistance = a.getCollisionRadius() + b.getCollisionRadius();

        if (distanceSq >= minDistance * minDistance) {
            return false;
        }

        float distance = (float) Math.sqrt(distanceSq);
        if (distance < COINCIDENT_EPSILON) {
            // Exactly stacked (e.g. two players spawned on one tile). Pick a fixed
            // axis rather than a random one: the host simulation must stay
            // reproducible, since its output is what every client renders.
            offsetX = 1f;
            offsetY = 0f;
            distance = 1f;
        }

        float overlap = minDistance - distance;
        float pushX = (offsetX / distance) * overlap * 0.5f;
        float pushY = (offsetY / distance) * overlap * 0.5f;

        moveWithCollision(a, -pushX, -pushY);
        moveWithCollision(b, pushX, pushY);
        return true;
    }

    /**
     * Finds the smallest sideways offset that frees a blocked position, or 0 if
     * none within {@link #CORNER_SLIP_TILES} does.
     *
     * <p>Offsets are tried nearest-first and in both directions, so an entity is
     * pulled toward whichever side of the gap is actually open and never jumps
     * further than it must.
     *
     * @param horizontal {@code true} to slip along X (when vertical movement was
     *                   blocked), {@code false} to slip along Y
     */
    private float findCornerSlip(float x, float y, float radius, boolean horizontal) {
        float increment = CORNER_SLIP_TILES / CORNER_SLIP_STEPS;
        for (int step = 1; step <= CORNER_SLIP_STEPS; step++) {
            float offset = increment * step;
            for (float direction : new float[] { offset, -offset }) {
                float testX = horizontal ? x + direction : x;
                float testY = horizontal ? y : y + direction;
                if (!overlapsBlockedTile(testX, testY, radius)) {
                    return direction;
                }
            }
        }
        return 0f;
    }

    /** Splits a long displacement into substeps no longer than {@link #MAX_STEP_TILES}. */
    private static int stepCountFor(float dx, float dy) {
        float distance = (float) Math.sqrt(dx * dx + dy * dy);
        return Math.max(1, (int) Math.ceil(distance / MAX_STEP_TILES));
    }

    private static float clamp(float value, float min, float max) {
        return value < min ? min : Math.min(value, max);
    }

    private static boolean isFinite(float value) {
        return !Float.isNaN(value) && !Float.isInfinite(value);
    }
}
