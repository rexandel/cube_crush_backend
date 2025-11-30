package com.cubecrush.auth.web.dto;

import java.time.Instant;

public record UserProfile(
        Long id,
        String nickname,
        Instant createdAt
) {}