package com.cubecrush.auth.web.dto;

public record TokenValidationResponse(
        boolean valid,
        Long userId,
        String username
) {}