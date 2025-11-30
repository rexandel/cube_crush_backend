package com.cubecrush.auth.service;

import com.cubecrush.auth.model.User;
import com.cubecrush.auth.model.UserSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserService userService;
    private final TokenService tokenService;
    private final JwtService jwtService;

    @Transactional
    public AuthResult register(String nickname, String password) {
        User user = userService.createUser(nickname, password);

        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        UserSession session = tokenService.createSession(
                user,
                jwtService.getJtiFromToken(accessToken),
                jwtService.hashToken(accessToken),
                jwtService.hashToken(refreshToken),
                jwtService.getAccessTokenExpirationTime(),
                jwtService.getRefreshTokenExpirationTime()
        );

        log.info("User registered successfully: {}", nickname);
        return AuthResult.builder()
                .user(user)
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    @Transactional
    public AuthResult login(String nickname, String password) {
        User user = userService.findByNickname(nickname)
                .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));

        if (!userService.validatePassword(password, user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid credentials");
        }

        tokenService.revokeAllUserSessions(user.getId());

        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        UserSession session = tokenService.createSession(
                user,
                jwtService.getJtiFromToken(accessToken),
                jwtService.hashToken(accessToken),
                jwtService.hashToken(refreshToken),
                jwtService.getAccessTokenExpirationTime(),
                jwtService.getRefreshTokenExpirationTime()
        );

        log.info("User logged in successfully: {}", nickname);
        return AuthResult.builder()
                .user(user)
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

        User user = session.getUser();
        String newAccessToken = jwtService.generateAccessToken(user);
        String newRefreshToken = jwtService.generateRefreshToken(user);

        UserSession newSession = tokenService.createSession(
                user,
                jwtService.getJtiFromToken(newAccessToken),
                jwtService.hashToken(newAccessToken),
                jwtService.hashToken(newRefreshToken),
                jwtService.getAccessTokenExpirationTime(),
                jwtService.getRefreshTokenExpirationTime()
        );

        log.info("Tokens refreshed for user: {}", user.getNickname());
        return AuthResult.builder()
                .user(user)
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .build();
    }

    public record AuthResult(User user, String accessToken, String refreshToken) {
        public static AuthResultBuilder builder() {
            return new AuthResultBuilder();
        }

        public static class AuthResultBuilder {
            private User user;
            private String accessToken;
            private String refreshToken;

            public AuthResultBuilder user(User user) {
                this.user = user;
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
                return new AuthResult(user, accessToken, refreshToken);
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