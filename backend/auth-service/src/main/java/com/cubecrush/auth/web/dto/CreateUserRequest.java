package com.cubecrush.auth.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(
        @NotBlank
        @Size(min = 3, max = 20)
        String nickname,

        @NotBlank
        @Size(min = 6)
        String password
) {}