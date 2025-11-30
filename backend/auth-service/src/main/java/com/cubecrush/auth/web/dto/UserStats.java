package com.cubecrush.auth.web.dto;

public record UserStats(
        int gamesPlayed,
        int bestScore,
        int averageScore
) {}