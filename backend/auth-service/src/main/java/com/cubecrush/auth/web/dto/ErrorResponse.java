package com.cubecrush.auth.web.dto;

import java.time.Instant;

public record ErrorResponse(
        String error,
        String localizationKey,
        Integer retryAfter,
        Instant timestamp
) {
    public ErrorResponse(String error, String localizationKey) {
        this(error, localizationKey, null, Instant.now());
    }
}