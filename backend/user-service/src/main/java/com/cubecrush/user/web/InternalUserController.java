package com.cubecrush.user.web;

import com.cubecrush.user.model.User;
import com.cubecrush.user.service.UserService;
import com.cubecrush.user.web.dto.CreateUserRequest;
import com.cubecrush.user.web.dto.UserProfile;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/internal/users")
@RequiredArgsConstructor
public class InternalUserController {
    private final UserService userService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserProfile createUser(@Valid @RequestBody CreateUserRequest request) {
        User user = userService.createUser(request.nickname(), request.password());
        return UserProfile.from(user);
    }

    @GetMapping("/{userId}")
    public UserProfile getUserById(@PathVariable Long userId) {
        var user = userService.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        return UserProfile.from(user);
    }

    @GetMapping("/by-nickname/{nickname}")
    public UserProfile getUserByNickname(@PathVariable String nickname) {
        var user = userService.findByNickname(nickname)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        return UserProfile.from(user);
    }

    @PostMapping("/validate-credentials")
    public boolean validateCredentials(@RequestParam String nickname,
                                       @RequestParam String password) {
        var user = userService.findByNickname(nickname)
                .orElse(null);

        if (user == null) {
            return false;
        }

        return userService.validatePassword(password, user.getPasswordHash());
    }
}