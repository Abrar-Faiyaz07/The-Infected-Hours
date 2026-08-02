package com.infectedhour.core.entities;

/**
 * NPC rescued via the "Rescue Villagers" objective (PRD §6). Has its own
 * infection timer independent of any player's contamination bar.
 */
public class Villager implements Collidable {

    private final String villagerId;
    private float x, y;
    private float infectionTimerSecondsRemaining;
    private boolean beingEscorted = false;
    private boolean rescued = false;

    public Villager(String villagerId, float infectionTimerSecondsRemaining) {
        this.villagerId = villagerId;
        this.infectionTimerSecondsRemaining = infectionTimerSecondsRemaining;
    }

    public String getVillagerId() {
        return villagerId;
    }

    public boolean isRescued() {
        return rescued;
    }

    public boolean isExpired() {
        return infectionTimerSecondsRemaining <= 0 && !rescued;
    }

    public void beginEscort() {
        beingEscorted = true;
    }

    public void markRescued() {
        rescued = true;
        beingEscorted = false;
    }

    /**
     * Raw displacement with no collision check. The escort-follow logic below
     * must go through {@code CollisionSystem.moveWithCollision}, or a villager
     * being led to safety will walk through walls.
     */
    @Override
    public void move(float dx, float dy) {
        this.x += dx;
        this.y += dy;
    }

    /** Absolute placement — used when seeding villagers at a level's spawn points. */
    public void setPosition(float x, float y) {
        this.x = x;
        this.y = y;
    }

    @Override
    public float getCollisionRadius() {
        return com.infectedhour.shared.constants.GameConstants.VILLAGER_COLLISION_RADIUS;
    }

    @Override
    public float getX() {
        return x;
    }

    @Override
    public float getY() {
        return y;
    }

    @Override
    public void update(float delta) {
        if (!rescued) {
            infectionTimerSecondsRemaining -= delta;
        }
        // ================ TEAMMATE TASK: ESCORT FOLLOW ================
        // TODO(villager): when beingEscorted, follow the escorting player.
        //  1. Store a reference to the escorting Player in beginEscort(...).
        //  2. Each tick, move toward them when farther than ~1 tile
        //     (simple "leash" follow — no pathfinding needed for v1).
        //  3. When the villager reaches the safe zone -> markRescued() and
        //     ObjectiveSystem.incrementProgress for the rescue objective.
        // ==============================================================
    }
}
