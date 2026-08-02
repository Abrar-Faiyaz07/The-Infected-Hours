package com.infectedhour.backend.controller;

import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

/**
 * Small helper: JwtAuthFilter puts the player's UUID (as a String) in the
 * SecurityContext principal. Every authenticated controller pulls "me"
 * from here instead of trusting a client-supplied id.
 */
final class CurrentPlayer {

    private CurrentPlayer() {
    }

    static UUID id() {
        String principal = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return UUID.fromString(principal);
    }
}
