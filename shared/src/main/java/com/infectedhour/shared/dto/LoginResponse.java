package com.infectedhour.shared.dto;

public record LoginResponse(String token, long expiresIn, PlayerDto player) {
}
