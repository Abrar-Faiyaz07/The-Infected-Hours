package com.infectedhour.backend.controller;

import com.infectedhour.backend.service.AuthService;
import com.infectedhour.shared.dto.LoginRequest;
import com.infectedhour.shared.dto.LoginResponse;
import com.infectedhour.shared.dto.RegisterRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/** POST /auth/register, POST /auth/login (Backend Schema §4). Unauthenticated per SecurityConfig. */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, UUID>> register(@Valid @RequestBody RegisterRequest request) {
        UUID playerId = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("playerId", playerId));
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }
}
