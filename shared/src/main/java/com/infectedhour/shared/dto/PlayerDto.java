package com.infectedhour.shared.dto;

import java.util.UUID;

public record PlayerDto(UUID id, String username, String displayName) {
}
