package com.cubecrush.auth.service;

import com.cubecrush.auth.exception.AuthException;
import com.cubecrush.auth.model.UserSession;
import com.cubecrush.auth.web.dto.CreateUserRequest;
import com.cubecrush.auth.web.dto.TokenValidationResponse;
import com.cubecrush.auth.web.dto.UserProfile;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final RestTemplate restTemplate;
    private final TokenService tokenService;
    private final JwtService jwtService;

    private static final String USER_SERVICE = "http://user-service/api/v1/system/users";

    @Transactional
    public AuthResult register(String nickname, String password) {
        try {
            UserProfile userProfile = restTemplate.postForObject(
                    USER_SERVICE,
                    new CreateUserRequest(nickname, password),
                    UserProfile.class
            );

            return createTokensAndSession(userProfile);

        } catch (HttpClientErrorException.Conflict e) {
            throw new AuthException("NICKNAME_ALREADY_TAKEN", HttpStatus.CONFLICT);
        } catch (Exception e) {
            log.error("Error registering user: {}", nickname, e);
            throw new AuthException("AUTH_REGISTRATION_FAILED", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Transactional
    public AuthResult login(String nickname, String password) {
        try {
            URI validateUri = UriComponentsBuilder
                    .fromHttpUrl(USER_SERVICE + "/validate-credentials")
                    .queryParam("nickname", nickname)
                    .queryParam("password", password)
                    .build()
                    .toUri();

            Boolean isValid = restTemplate.postForObject(validateUri, null, Boolean.class);
            if (!Boolean.TRUE.equals(isValid)) {
                throw new AuthException("AUTH_INVALID_CREDENTIALS", HttpStatus.UNAUTHORIZED);
            }

            UserProfile userProfile = restTemplate.getForObject(
                    USER_SERVICE + "/by-nickname/{nickname}",
                    UserProfile.class,
                    nickname
            );

            tokenService.revokeAllUserSessions(userProfile.id());

            return createTokensAndSession(userProfile);

        } catch (AuthException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error logging in user: {}", nickname, e);
            throw new AuthException("AUTH_LOGIN_FAILED", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Transactional
    public AuthResult refreshTokens(String refreshToken) {
        try {
            String refreshTokenHash = jwtService.hashToken(refreshToken);

            UserSession session = tokenService.findValidSessionByRefreshToken(refreshTokenHash)
                    .orElseThrow(() -> new AuthException("AUTH_INVALID_TOKEN", HttpStatus.UNAUTHORIZED));

            tokenService.revokeSession(session.getJti());

            String newAccessToken = jwtService.generateAccessToken(session.getUserId(), session.getUserNickname());
            String newRefreshToken = jwtService.generateRefreshToken(session.getUserId(), session.getUserNickname());

            tokenService.createSession(
                    session.getUserId(),
                    session.getUserNickname(),
                    jwtService.getJtiFromToken(newAccessToken),
                    jwtService.hashToken(newAccessToken),
                    jwtService.hashToken(newRefreshToken),
                    jwtService.getAccessTokenExpirationTime(),
                    jwtService.getRefreshTokenExpirationTime()
            );

            UserProfile userProfile = restTemplate.getForObject(
                    USER_SERVICE + "/{userId}",
                    UserProfile.class,
                    session.getUserId()
            );

            log.info("Tokens refreshed for user: {}", session.getUserNickname());

            return AuthResult.builder()
                    .userProfile(userProfile)
                    .accessToken(newAccessToken)
                    .refreshToken(newRefreshToken)
                    .build();
        } catch (AuthException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error refreshing tokens", e);
            throw new AuthException("AUTH_REFRESH_FAILED", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private AuthResult createTokensAndSession(UserProfile userProfile) {
        String accessToken = jwtService.generateAccessToken(userProfile.id(), userProfile.nickname());
        String refreshToken = jwtService.generateRefreshToken(userProfile.id(), userProfile.nickname());

        tokenService.createSession(
                userProfile.id(),
                userProfile.nickname(),
                jwtService.getJtiFromToken(accessToken),
                jwtService.hashToken(accessToken),
                jwtService.hashToken(refreshToken),
                jwtService.getAccessTokenExpirationTime(),
                jwtService.getRefreshTokenExpirationTime()
        );

        log.info("User {} successfully authenticated", userProfile.nickname());

        return AuthResult.builder()
                .userProfile(userProfile)
                .accessToken(accessToken)
                .refreshToken(refreshToken)
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
                return new TokenValidationResponse(false, null, null, null);
            }
            return new TokenValidationResponse(
                    true,
                    jwtService.getUserIdFromToken(token),
                    jwtService.getUsernameFromToken(token),
                    java.util.List.of("USER")
            );
        } catch (Exception e) {
            return new TokenValidationResponse(false, null, null, null);
        }
    }

    @Transactional
    public void logout(String authHeader) {
        String token = extractToken(authHeader);
        try {
            String jti = jwtService.getJtiFromToken(token);
            tokenService.revokeSession(jti);
        } catch (Exception e) {
            log.warn("Error during logout: {}", e.getMessage());
        }
    }

    private String extractToken(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        throw new AuthException("AUTH_INVALID_HEADER", HttpStatus.UNAUTHORIZED);
    }

    @Builder
    public record AuthResult(UserProfile userProfile, String accessToken, String refreshToken) {}
}