package com.theinfectedhour.network;

/** Liveness ping in both directions every tick; N consecutive misses triggers disconnect handling (Architecture.md §7). */
public class Heartbeat {

    private int intervalMs;

    public void tick() {
        // TODO: implement
    }

    public void onTimeout(Runnable listener) {
        // TODO: implement
    }
}
