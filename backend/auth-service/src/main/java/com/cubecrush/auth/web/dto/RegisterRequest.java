package com.cubecrush.auth.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Size(min = 3, max = 50) String nickname,
        @NotBlank @Size(min = 6) String password
) {}