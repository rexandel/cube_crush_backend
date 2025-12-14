package com.cubecrush.user.web;

import com.cubecrush.user.model.User;
import com.cubecrush.user.service.UserService;
import com.cubecrush.user.web.dto.CreateUserRequest;
import com.cubecrush.user.web.dto.UserProfile;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/system/users")
@RequiredArgsConstructor
public class UserManagementController {
    private final UserService userService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserProfile createUser(@Valid @RequestBody CreateUserRequest request) {
        try {
            User user = userService.createUser(request.nickname(), request.password());
            return UserProfile.from(user);
        } catch (IllegalArgumentException e) {
            if (e.getMessage().contains("already exists")) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "User already exists", e);
            }
            throw e;
        }
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