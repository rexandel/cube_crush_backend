package com.cubecrush.auth.web.dto;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        UserProfile userProfile
) {}