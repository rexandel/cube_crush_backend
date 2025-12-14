package com.cubecrush.auth.web;

import com.cubecrush.auth.exception.AuthException;
import com.cubecrush.auth.service.AuthService;
import com.cubecrush.auth.web.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Register new user", description = "Creates new user and returns authentication tokens")
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        log.info("Registration attempt for nickname: {}", request.nickname());
        var result = authService.register(request.nickname(), request.password());
        log.info("User registered successfully: {}", request.nickname());
        return new AuthResponse(
                result.accessToken(),
                result.refreshToken(),
                result.userProfile()
        );
    }

    @PostMapping("/login")
    @Operation(summary = "User login", description = "Authenticates user and returns tokens")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        log.info("Login attempt for nickname: {}", request.nickname());
        var result = authService.login(request.nickname(), request.password());
        log.info("User logged in successfully: {}", request.nickname());
        return new AuthResponse(
                result.accessToken(),
                result.refreshToken(),
                result.userProfile()
        );
    }

    @PostMapping("/logout")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "User logout", description = "Invalidates user's authentication tokens")
    public void logout(@Parameter(hidden = true) @RequestHeader("Authorization") String authHeader) {
        String token = extractToken(authHeader);
        log.info("Logout request for token: {}", maskToken(token));
        authService.logout(authHeader);
        log.info("User logged out successfully");
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh tokens", description = "Generates new access and refresh tokens using valid refresh token")
    public AuthResponse refreshTokens(@Valid @RequestBody RefreshTokenRequest request) {
        log.info("Token refresh attempt");
        var result = authService.refreshTokens(request.refreshToken());
        log.info("Tokens refreshed successfully");
        return new AuthResponse(
                result.accessToken(),
                result.refreshToken(),
                result.userProfile()
        );
    }

    @PostMapping("/validate")
    @Operation(summary = "Validate token", description = "Checks if the provided token is valid")
    @SecurityRequirement(name = "bearerAuth")
    public TokenValidationResponse validateToken(@Parameter(hidden = true) @RequestHeader("Authorization") String token) {
        String cleanToken = extractToken(token);
        log.debug("Token validation request: {}", maskToken(cleanToken));

        return authService.validateTokenWithUser(cleanToken);
    }

    @GetMapping("/health")
    @Operation(summary = "Health check", description = "Check if authentication service is healthy")
    public String health() {
        return "Auth Service is UP!";
    }

    private String extractToken(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        throw new AuthException("AUTH_INVALID_HEADER", HttpStatus.UNAUTHORIZED);
    }

    private String maskToken(String token) {
        if (token == null || token.length() <= 8) {
            return "***";
        }
        return token.substring(0, 4) + "..." + token.substring(token.length() - 4);
    }
}