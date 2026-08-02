package com.infectedhour.shared.dto;

/** GET /health response — unauthenticated liveness check shown as a chip in the lobby. */
public record HealthStatus(String status) {
    public static HealthStatus up() {
        return new HealthStatus("UP");
    }
}
