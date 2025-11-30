package com.cubecrush.auth.web.dto;

import jakarta.validation.constraints.Min;

public record ScoreSubmitRequest(
        @Min(0) int score
) {}