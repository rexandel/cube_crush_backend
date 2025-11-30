package com.cubecrush.auth.web.dto;

import java.time.Instant;

public record ScoreHistoryEntry(
        int score,
        Instant achievedAt
) {}