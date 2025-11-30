package com.cubecrush.auth.service;

import com.cubecrush.auth.model.RevokedToken;
import com.cubecrush.auth.model.UserSession;
import com.cubecrush.auth.repository.RevokedTokenRepository;
import com.cubecrush.auth.repository.UserSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenService {
    private final UserSessionRepository userSessionRepository;
    private final RevokedTokenRepository revokedTokenRepository;

    @Transactional
    public UserSession createSession(Long userId, String userNickname, String jti, String accessTokenHash, String refreshTokenHash,
                                     Instant accessTokenExpiresAt, Instant refreshTokenExpiresAt) {
        UserSession session = UserSession.builder()
                .userId(userId)
                .userNickname(userNickname)
                .jti(jti)
                .accessTokenHash(accessTokenHash)
                .refreshTokenHash(refreshTokenHash)
                .accessTokenExpiresAt(accessTokenExpiresAt)
                .refreshTokenExpiresAt(refreshTokenExpiresAt)
                .isRevoked(false)
                .build();

        UserSession savedSession = userSessionRepository.save(session);
        log.info("Created session with jti: {} for user: {}", jti, userNickname);
        return savedSession;
    }

    public Optional<UserSession> findValidSessionByRefreshToken(String refreshTokenHash) {
        return userSessionRepository.findByRefreshTokenHash(refreshTokenHash)
                .filter(session -> !session.getIsRevoked())
                .filter(session -> session.getRefreshTokenExpiresAt().isAfter(Instant.now()));
    }

    public Optional<UserSession> findValidSessionByAccessToken(String accessTokenHash) {
        return userSessionRepository.findByAccessTokenHash(accessTokenHash)
                .filter(session -> !session.getIsRevoked())
                .filter(session -> session.getAccessTokenExpiresAt().isAfter(Instant.now()));
    }

    public Optional<UserSession> findValidSessionByJti(String jti) {
        return userSessionRepository.findByJti(jti)
                .filter(session -> !session.getIsRevoked())
                .filter(session -> session.getRefreshTokenExpiresAt().isAfter(Instant.now()));
    }

    @Transactional
    public void revokeSession(String jti) {
        userSessionRepository.findByJti(jti).ifPresent(session -> {
            session.setIsRevoked(true);
            userSessionRepository.save(session);
            log.info("Revoked session with jti: {}", jti);
        });
    }

    @Transactional
    public void revokeAllUserSessions(Long userId) {
        userSessionRepository.revokeAllUserSessions(userId);
        log.info("Revoked all sessions for user id: {}", userId);
    }

    @Transactional
    public void revokeToken(String jti, Instant expiresAt) {
        RevokedToken revokedToken = RevokedToken.builder()
                .jti(jti)
                .expiresAt(expiresAt)
                .build();

        revokedTokenRepository.save(revokedToken);
        log.info("Added token to blacklist, jti: {}", jti);
    }

    public boolean isTokenRevoked(String jti) {
        return revokedTokenRepository.existsByJti(jti);
    }

    public List<UserSession> findActiveUserSessions(Long userId) {
        return userSessionRepository.findByUserIdAndIsRevokedFalseAndRefreshTokenExpiresAtAfter(
                userId, Instant.now());
    }

    public List<UserSession> findSessionsWithExpiringAccessTokens(Long userId) {
        return userSessionRepository.findByUserIdAndIsRevokedFalseAndAccessTokenExpiresAtAfter(
                userId, Instant.now());
    }

    @Scheduled(cron = "0 0 3 * * ?")
    @Transactional
    public void cleanupExpiredData() {
        Instant now = Instant.now();

        userSessionRepository.deleteExpiredSessions(now);
        revokedTokenRepository.deleteExpiredTokens(now);

        log.info("Cleaned up expired sessions and revoked tokens");
    }
}