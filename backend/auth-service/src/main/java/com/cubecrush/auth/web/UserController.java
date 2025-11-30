package com.cubecrush.auth.web;

import com.cubecrush.auth.config.JwtAuthenticationFilter;
import com.cubecrush.auth.service.UserService;
import com.cubecrush.auth.web.dto.*;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @GetMapping("/me")
    @SecurityRequirement(name = "bearerAuth")
    public UserProfile getCurrentUserProfile(Authentication authentication) {
        JwtAuthenticationFilter.UserPrincipal userPrincipal =
                (JwtAuthenticationFilter.UserPrincipal) authentication.getPrincipal();

        var user = userService.findById(userPrincipal.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        return UserProfile.from(user);
    }

    @PatchMapping("/me/nickname")
    @SecurityRequirement(name = "bearerAuth")
    public UserProfile updateNickname(@Valid @RequestBody UpdateNicknameRequest request,
                                      Authentication authentication) {
        String username = authentication.getName();
        var user = userService.findByNickname(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        var updatedUser = userService.updateNickname(user.getId(), request.nickname());
        return UserProfile.from(updatedUser);
    }

    @PatchMapping("/me/password")
    @SecurityRequirement(name = "bearerAuth")
    public void changePassword(@Valid @RequestBody ChangePasswordRequest request,
                               Authentication authentication) {
        String username = authentication.getName();
        var user = userService.findByNickname(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        userService.changePassword(user.getId(), request.currentPassword(), request.newPassword());
    }
}