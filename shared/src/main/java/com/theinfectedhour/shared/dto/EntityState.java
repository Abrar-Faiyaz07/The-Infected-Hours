package com.theinfectedhour.shared.dto;

import java.io.Serializable;

import com.theinfectedhour.shared.math.Vector2;

/** Wire snapshot of one entity for EntitySyncPacket: id, position, animation. */
public class EntityState implements Serializable {

    public String entityId;
    public Vector2 position;
    public String animationState;
}
