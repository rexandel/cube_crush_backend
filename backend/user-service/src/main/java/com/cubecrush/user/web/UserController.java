package com.cubecrush.user.web;

import com.cubecrush.user.client.AuthServiceClient;
import com.cubecrush.user.service.UserService;
import com.cubecrush.user.web.dto.ChangePasswordRequest;
import com.cubecrush.user.web.dto.UpdateNicknameRequest;
import com.cubecrush.user.web.dto.UserProfile;
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
    private final AuthServiceClient authServiceClient;

    @GetMapping("/me")
    @SecurityRequirement(name = "bearerAuth")
    public UserProfile getCurrentUserProfile(@RequestHeader("Authorization") String token) {
        var validationResponse = authServiceClient.validateTokenWithUser(token);

        if (!validationResponse.valid()) {
            throw new IllegalArgumentException("Invalid token");
        }

        var user = userService.findById(validationResponse.userId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        return UserProfile.from(user);
    }

    @PatchMapping("/me/nickname")
    @SecurityRequirement(name = "bearerAuth")
    public UserProfile updateNickname(@RequestHeader("Authorization") String token,
                                      @Valid @RequestBody UpdateNicknameRequest request) {
        var validationResponse = authServiceClient.validateTokenWithUser(token);

        if (!validationResponse.valid()) {
            throw new IllegalArgumentException("Invalid token");
        }

        var updatedUser = userService.updateNickname(validationResponse.userId(), request.nickname());
        return UserProfile.from(updatedUser);
    }

    @PatchMapping("/me/password")
    @SecurityRequirement(name = "bearerAuth")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(@RequestHeader("Authorization") String token,
                               @Valid @RequestBody ChangePasswordRequest request) {
        var validationResponse = authServiceClient.validateTokenWithUser(token);

        if (!validationResponse.valid()) {
            throw new IllegalArgumentException("Invalid token");
        }

        userService.changePassword(validationResponse.userId(), request.currentPassword(), request.newPassword());
    }
}