package com.infectedhour.core.entities;

/**
 * An entity that occupies space and can be pushed by
 * {@code systems.CollisionSystem}.
 *
 * <p>Kept separate from {@link Entity} on purpose: contamination clouds are
 * positioned by tile index and have no circular body, so forcing a radius onto
 * every entity would be a lie (Interface Segregation).
 *
 * <p>Colliders are <b>circles centred on the entity position</b>, in tile units.
 * A circle rather than the AABB named in TRD §4 because the renderer already
 * centres sprites on the position, and a circle slides along walls without
 * snagging on tile corners during diagonal movement.
 */
public interface Collidable extends Entity {

    /** Collider radius in tiles; must be positive and below 0.5 to fit a one-tile gap. */
    float getCollisionRadius();

    /** Applies a relative displacement. Callers should go through CollisionSystem, not this directly. */
    void move(float dx, float dy);
}
