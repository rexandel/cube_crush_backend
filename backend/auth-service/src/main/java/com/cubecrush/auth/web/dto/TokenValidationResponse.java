package com.cubecrush.auth.web.dto;

import java.util.List;

public record TokenValidationResponse(
        boolean valid,
        Long userId,
        String nickname,
        List<String> authorities
) {}