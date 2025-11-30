package com.cubecrush.user.web.dto;

public record TokenValidationResponse(
        boolean valid,
        Long userId,
        String username
) {}