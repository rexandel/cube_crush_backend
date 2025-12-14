package com.cubecrush.user.web;

import com.cubecrush.user.service.UserService;
import com.cubecrush.user.web.dto.ChangePasswordRequest;
import com.cubecrush.user.web.dto.UpdateNicknameRequest;
import com.cubecrush.user.web.dto.UserProfile;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @GetMapping("/me")
    @SecurityRequirement(name = "bearerAuth")
    public UserProfile getCurrentUserProfile(@Parameter(hidden = true) @RequestHeader("X-User-Id") Long userId) {
        var user = userService.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        return UserProfile.from(user);
    }

    @PatchMapping("/me/nickname")
    @SecurityRequirement(name = "bearerAuth")
    public UserProfile updateNickname(@Parameter(hidden = true) @RequestHeader("X-User-Id") Long userId,
                                      @Valid @RequestBody UpdateNicknameRequest request) {
        var updatedUser = userService.updateNickname(userId, request.nickname());
        return UserProfile.from(updatedUser);
    }

    @PatchMapping("/me/password")
    @SecurityRequirement(name = "bearerAuth")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(@Parameter(hidden = true) @RequestHeader("X-User-Id") Long userId,
                               @Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(userId, request.currentPassword(), request.newPassword());
    }
}