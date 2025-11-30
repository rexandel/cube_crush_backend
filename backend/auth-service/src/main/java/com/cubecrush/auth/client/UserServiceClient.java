package com.cubecrush.auth.client;

import com.cubecrush.auth.web.dto.CreateUserRequest;
import com.cubecrush.auth.web.dto.UserProfile;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "user-service", url = "http://localhost:8082")
public interface UserServiceClient {
    @PostMapping("/api/v1/internal/users")
    UserProfile createUser(@RequestBody CreateUserRequest request);

    @GetMapping("/api/v1/internal/users/{userId}")
    UserProfile getUserById(@PathVariable Long userId);

    @GetMapping("/api/v1/internal/users/by-nickname/{nickname}")
    UserProfile getUserByNickname(@PathVariable String nickname);

    @PostMapping("/api/v1/internal/users/validate-credentials")
    boolean validateCredentials(@RequestParam String nickname, @RequestParam String password);
}