package com.theinfectedhour.shared.math;

import java.io.Serializable;

/** Minimal engine-agnostic 2D vector for wire DTOs — the network module must stay libGDX-free. */
public class Vector2 implements Serializable {

    public float x;
    public float y;

    public Vector2() {
    }

    public Vector2(float x, float y) {
        this.x = x;
        this.y = y;
    }
}
