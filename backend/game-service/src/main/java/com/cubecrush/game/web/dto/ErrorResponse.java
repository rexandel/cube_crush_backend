package com.cubecrush.game.web.dto;

import java.time.Instant;

public record ErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        String localizationKey
) {
    public ErrorResponse(int status, String error, String message, String localizationKey) {
        this(Instant.now(), status, error, message, localizationKey);
    }
}
