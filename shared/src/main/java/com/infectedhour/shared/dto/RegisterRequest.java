package com.infectedhour.shared.dto;

/** POST /auth/register body. Username pattern validated server-side: ^[a-zA-Z0-9_]{3,24}$ */
public record RegisterRequest(String username, String displayName, String password) {
}
