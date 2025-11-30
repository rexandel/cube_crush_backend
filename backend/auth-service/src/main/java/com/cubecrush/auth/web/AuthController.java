package com.cubecrush.auth.web;

import com.cubecrush.auth.service.AuthService;
import com.cubecrush.auth.web.dto.*;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        var result = authService.register(request.nickname(), request.password());
        return new AuthResponse(
                result.accessToken(),
                result.refreshToken(),
                UserProfile.from(result.user())
        );
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        var result = authService.login(request.nickname(), request.password());
        return new AuthResponse(
                result.accessToken(),
                result.refreshToken(),
                UserProfile.from(result.user())
        );
    }

    @PostMapping("/logout")
    @SecurityRequirement(name = "bearerAuth")
    public void logout(@RequestHeader("Authorization") String authHeader) {
        authService.logout(authHeader);
    }

    @PostMapping("/refresh")
    public AuthResponse refreshTokens(@Valid @RequestBody RefreshTokenRequest request) {
        var result = authService.refreshTokens(request.refreshToken());
        return new AuthResponse(
                result.accessToken(),
                result.refreshToken(),
                UserProfile.from(result.user())
        );
    }

    private String extractToken(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        throw new IllegalArgumentException("Invalid Authorization header");
    }
}