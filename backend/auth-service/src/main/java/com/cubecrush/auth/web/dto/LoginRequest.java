package com.cubecrush.auth.web.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank String nickname,
        @NotBlank String password
) {}