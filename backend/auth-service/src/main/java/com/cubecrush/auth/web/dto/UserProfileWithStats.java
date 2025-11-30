package com.cubecrush.auth.web.dto;

import java.time.Instant;

public record UserProfileWithStats(
        Long id,
        String nickname,
        Instant createdAt,
        UserStats stats
) {}