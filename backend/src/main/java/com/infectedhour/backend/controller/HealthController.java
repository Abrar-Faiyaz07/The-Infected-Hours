package com.infectedhour.backend.controller;

import com.infectedhour.shared.dto.HealthStatus;
import org.springframework.web.bind.annotation.*;

/** GET /health — unauthenticated liveness check (lobby shows backend status chip, Backend Schema §4). */
@RestController
@RequestMapping("/api/v1/health")
public class HealthController {

    @GetMapping
    public HealthStatus health() {
        return HealthStatus.up();
    }
}
