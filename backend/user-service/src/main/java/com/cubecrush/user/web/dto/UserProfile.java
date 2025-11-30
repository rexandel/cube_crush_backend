package com.cubecrush.user.web.dto;

import com.cubecrush.user.model.User;
import java.time.Instant;

public record UserProfile(
        Long id,
        String nickname,
        Instant createdAt
) {
    public static UserProfile from(User user) {
        return new UserProfile(user.getId(), user.getNickname(), user.getCreatedAt());
    }
}