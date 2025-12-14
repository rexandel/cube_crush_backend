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
    public UserSession createSession(Long userId, String userNickname, String jti,
                                     String accessTokenHash, String refreshTokenHash,
                                     Instant accessTokenExpiresAt, Instant refreshTokenExpiresAt) {

        if (userSessionRepository.existsByJti(jti)) {
            log.warn("Session with jti: {} already exists, recreating", jti);
            revokeSession(jti);
        }

        UserSession session = UserSession.builder()
                .userId(userId)
                .userNickname(userNickname)
                .jti(jti)
                .accessTokenHash(accessTokenHash)
                .refreshTokenHash(refreshTokenHash)
                .accessTokenExpiresAt(accessTokenExpiresAt)
                .refreshTokenExpiresAt(refreshTokenExpiresAt)
                .isRevoked(false)
                .createdAt(Instant.now())
                .build();

        UserSession savedSession = userSessionRepository.save(session);
        log.info("Created session with jti: {} for user: {}", jti, userNickname);
        return savedSession;
    }

    public Optional<UserSession> findValidSessionByRefreshToken(String refreshTokenHash) {
        return userSessionRepository.findByRefreshTokenHashAndIsRevokedFalseAndRefreshTokenExpiresAtAfter(
                refreshTokenHash, Instant.now());
    }

    public Optional<UserSession> findValidSessionByAccessToken(String accessTokenHash) {
        return userSessionRepository.findByAccessTokenHashAndIsRevokedFalseAndAccessTokenExpiresAtAfter(
                accessTokenHash, Instant.now());
    }

    public Optional<UserSession> findValidSessionByJti(String jti) {
        return userSessionRepository.findByJtiAndIsRevokedFalseAndRefreshTokenExpiresAtAfter(
                jti, Instant.now());
    }

    @Transactional
    public void revokeSession(String jti) {
        int updatedCount = userSessionRepository.revokeSessionByJti(jti);
        if (updatedCount > 0) {
            log.info("Revoked session with jti: {}", jti);
        } else {
            log.debug("Session with jti: {} not found or already revoked", jti);
        }
    }

    @Transactional
    public void revokeAllUserSessions(Long userId) {
        int revokedCount = userSessionRepository.revokeAllUserSessions(userId);
        log.info("Revoked {} sessions for user id: {}", revokedCount, userId);
    }

    @Transactional
    public void revokeToken(String jti, Instant expiresAt) {
        if (!revokedTokenRepository.existsByJti(jti)) {
            RevokedToken revokedToken = RevokedToken.builder()
                    .jti(jti)
                    .expiresAt(expiresAt)
                    .revokedAt(Instant.now())
                    .build();

            revokedTokenRepository.save(revokedToken);
            log.info("Added token to blacklist, jti: {}", jti);
        } else {
            log.debug("Token with jti: {} already in blacklist", jti);
        }
    }

    public boolean isTokenRevoked(String jti) {
        return revokedTokenRepository.existsByJti(jti);
    }

    public List<UserSession> findActiveUserSessions(Long userId) {
        return userSessionRepository.findByUserIdAndIsRevokedFalseAndRefreshTokenExpiresAtAfter(
                userId, Instant.now());
    }

    public List<UserSession> findExpiringSessions(int hoursBeforeExpiration) {
        Instant threshold = Instant.now().plusSeconds(hoursBeforeExpiration * 3600L);
        return userSessionRepository.findByRefreshTokenExpiresAtBeforeAndIsRevokedFalse(threshold);
    }

    public boolean isValidSession(String jti) {
        if (isTokenRevoked(jti)) {
            return false;
        }
        return findValidSessionByJti(jti).isPresent();
    }

    @Scheduled(cron = "0 0 */6 * * ?")
    @Transactional
    public void cleanupExpiredData() {
        Instant now = Instant.now();

        int sessionsDeleted = userSessionRepository.deleteExpiredSessions(now);
        int tokensDeleted = revokedTokenRepository.deleteExpiredTokens(now);

        log.info("Cleaned up {} expired sessions and {} revoked tokens",
                sessionsDeleted, tokensDeleted);
    }

    @Scheduled(cron = "0 0 2 * * ?")
    @Transactional
    public void cleanupOldRevokedTokens() {
        Instant oldThreshold = Instant.now().minusSeconds(7 * 24 * 3600L); // 7 дней назад
        int deletedCount = revokedTokenRepository.deleteByRevokedAtBefore(oldThreshold);
        log.info("Cleaned up {} old revoked tokens", deletedCount);
    }

    public SessionStats getSessionStats() {
        Instant now = Instant.now();
        long activeSessions = userSessionRepository.countByIsRevokedFalseAndRefreshTokenExpiresAtAfter(now);
        long revokedSessions = userSessionRepository.countByIsRevokedTrue();
        long expiredSessions = userSessionRepository.countByRefreshTokenExpiresAtBefore(now);

        return new SessionStats(activeSessions, revokedSessions, expiredSessions);
    }

    public record SessionStats(long activeSessions, long revokedSessions, long expiredSessions) {}
}