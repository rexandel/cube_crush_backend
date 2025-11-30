package com.cubecrush.auth.repository;

import com.cubecrush.auth.model.UserSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserSessionRepository extends JpaRepository<UserSession, Long> {
    Optional<UserSession> findByJti(String jti);
    Optional<UserSession> findByRefreshTokenHash(String refreshTokenHash);
    Optional<UserSession> findByAccessTokenHash(String accessTokenHash);
    boolean existsByJti(String jti);

    List<UserSession> findByUserIdAndIsRevokedFalseAndAccessTokenExpiresAtAfter(Long userId, Instant time);
    List<UserSession> findByUserIdAndIsRevokedFalseAndRefreshTokenExpiresAtAfter(Long userId, Instant time);

    @Modifying
    @Query("UPDATE UserSession us SET us.isRevoked = true WHERE us.userId = :userId AND us.isRevoked = false") // ← ИСПРАВИЛ: us.userId
    void revokeAllUserSessions(@Param("userId") Long userId);

    @Modifying
    @Query("DELETE FROM UserSession us WHERE us.refreshTokenExpiresAt < :now")
    void deleteExpiredSessions(@Param("now") Instant now);
}