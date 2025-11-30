package com.cubecrush.auth.service;

import com.cubecrush.auth.client.UserServiceClient;
import com.cubecrush.auth.model.UserSession;
import com.cubecrush.auth.web.dto.CreateUserRequest;
import com.cubecrush.auth.web.dto.UserProfile;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.cubecrush.auth.web.dto.TokenValidationResponse;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserServiceClient userServiceClient; // ← ЗАМЕНИЛИ UserService
    private final TokenService tokenService;
    private final JwtService jwtService;

    @Transactional
    public AuthResult register(String nickname, String password) {
        UserProfile userProfile = userServiceClient.createUser(
                new CreateUserRequest(nickname, password)
        );

        String accessToken = jwtService.generateAccessToken(userProfile.id(), userProfile.nickname());
        String refreshToken = jwtService.generateRefreshToken(userProfile.id(), userProfile.nickname());

        UserSession session = tokenService.createSession(
                userProfile.id(),
                userProfile.nickname(),
                jwtService.getJtiFromToken(accessToken),
                jwtService.hashToken(accessToken),
                jwtService.hashToken(refreshToken),
                jwtService.getAccessTokenExpirationTime(),
                jwtService.getRefreshTokenExpirationTime()
        );

        log.info("User registered successfully: {}", nickname);
        return AuthResult.builder()
                .userProfile(userProfile)
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    @Transactional
    public AuthResult login(String nickname, String password) {

        boolean isValid = userServiceClient.validateCredentials(nickname, password);
        if (!isValid) {
            throw new IllegalArgumentException("Invalid credentials");
        }

        UserProfile userProfile = userServiceClient.getUserByNickname(nickname);

        tokenService.revokeAllUserSessions(userProfile.id());

        String accessToken = jwtService.generateAccessToken(userProfile.id(), userProfile.nickname());
        String refreshToken = jwtService.generateRefreshToken(userProfile.id(), userProfile.nickname());

        UserSession session = tokenService.createSession(
                userProfile.id(),
                userProfile.nickname(),
                jwtService.getJtiFromToken(accessToken),
                jwtService.hashToken(accessToken),
                jwtService.hashToken(refreshToken),
                jwtService.getAccessTokenExpirationTime(),
                jwtService.getRefreshTokenExpirationTime()
        );

        log.info("User logged in successfully: {}", nickname);
        return AuthResult.builder()
                .userProfile(userProfile)
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    @Transactional
    public AuthResult refreshTokens(String refreshToken) {
        String refreshTokenHash = jwtService.hashToken(refreshToken);

        UserSession session = tokenService.findValidSessionByRefreshToken(refreshTokenHash)
                .orElseThrow(() -> new IllegalArgumentException("Invalid refresh token"));

        tokenService.revokeSession(session.getJti());

        String newAccessToken = jwtService.generateAccessToken(session.getUserId(), session.getUserNickname());
        String newRefreshToken = jwtService.generateRefreshToken(session.getUserId(), session.getUserNickname());

        UserSession newSession = tokenService.createSession(
                session.getUserId(),
                session.getUserNickname(),
                jwtService.getJtiFromToken(newAccessToken),
                jwtService.hashToken(newAccessToken),
                jwtService.hashToken(newRefreshToken),
                jwtService.getAccessTokenExpirationTime(),
                jwtService.getRefreshTokenExpirationTime()
        );

        log.info("Tokens refreshed for user: {}", session.getUserNickname());

        UserProfile userProfile = userServiceClient.getUserById(session.getUserId());

        return AuthResult.builder()
                .userProfile(userProfile)
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .build();
    }

    public boolean validateToken(String token) {
        try {
            return jwtService.validateToken(token);
        } catch (Exception e) {
            return false;
        }
    }

    public TokenValidationResponse validateTokenWithUser(String token) {
        try {
            if (!jwtService.validateToken(token)) {
                return new TokenValidationResponse(false, null, null);
            }

            Long userId = jwtService.getUserIdFromToken(token);
            String username = jwtService.getUsernameFromToken(token);

            return new TokenValidationResponse(true, userId, username);
        } catch (Exception e) {
            return new TokenValidationResponse(false, null, null);
        }
    }

    public record AuthResult(UserProfile userProfile, String accessToken, String refreshToken) {
        public static AuthResultBuilder builder() {
            return new AuthResultBuilder();
        }

        public static class AuthResultBuilder {
            private UserProfile userProfile;
            private String accessToken;
            private String refreshToken;

            public AuthResultBuilder userProfile(UserProfile userProfile) {
                this.userProfile = userProfile;
                return this;
            }

            public AuthResultBuilder accessToken(String accessToken) {
                this.accessToken = accessToken;
                return this;
            }

            public AuthResultBuilder refreshToken(String refreshToken) {
                this.refreshToken = refreshToken;
                return this;
            }

            public AuthResult build() {
                return new AuthResult(userProfile, accessToken, refreshToken);
            }
        }
    }

    @Transactional
    public void logout(String authHeader) {
        String token = extractToken(authHeader);
        String jti = jwtService.getJtiFromToken(token);
        tokenService.revokeSession(jti);
        tokenService.revokeToken(jti, jwtService.getExpirationFromToken(token));
    }

    private String extractToken(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        throw new IllegalArgumentException("Invalid Authorization header");
    }
}