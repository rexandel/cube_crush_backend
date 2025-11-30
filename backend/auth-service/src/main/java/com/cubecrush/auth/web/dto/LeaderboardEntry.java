package com.cubecrush.auth.web.dto;

import java.time.Instant;

public record LeaderboardEntry(
        int position,
        String nickname,
        int score,
        Instant achievedAt
) {}