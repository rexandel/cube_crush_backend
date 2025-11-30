package com.cubecrush.auth.web.dto;

public record ScoreSubmitResponse(
        Long scoreId,
        boolean newBestScore,
        Integer previousBest
) {}
