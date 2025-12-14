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

    Optional<UserSession> findByRefreshTokenHashAndIsRevokedFalseAndRefreshTokenExpiresAtAfter(
            String refreshTokenHash, Instant time);

    Optional<UserSession> findByAccessTokenHashAndIsRevokedFalseAndAccessTokenExpiresAtAfter(
            String accessTokenHash, Instant time);

    Optional<UserSession> findByJtiAndIsRevokedFalseAndRefreshTokenExpiresAtAfter(
            String jti, Instant time);

    List<UserSession> findByUserIdAndIsRevokedFalseAndAccessTokenExpiresAtAfter(Long userId, Instant time);
    List<UserSession> findByUserIdAndIsRevokedFalseAndRefreshTokenExpiresAtAfter(Long userId, Instant time);

    List<UserSession> findByRefreshTokenExpiresAtBeforeAndIsRevokedFalse(Instant time);

    @Modifying
    @Query("UPDATE UserSession us SET us.isRevoked = true WHERE us.userId = :userId AND us.isRevoked = false")
    int revokeAllUserSessions(@Param("userId") Long userId);

    @Modifying
    @Query("UPDATE UserSession us SET us.isRevoked = true WHERE us.jti = :jti AND us.isRevoked = false")
    int revokeSessionByJti(@Param("jti") String jti);

    @Modifying
    @Query("DELETE FROM UserSession us WHERE us.refreshTokenExpiresAt < :now")
    int deleteExpiredSessions(@Param("now") Instant now);

    @Query("SELECT COUNT(us) FROM UserSession us WHERE us.isRevoked = false AND us.refreshTokenExpiresAt > :now")
    long countByIsRevokedFalseAndRefreshTokenExpiresAtAfter(@Param("now") Instant now);

    @Query("SELECT COUNT(us) FROM UserSession us WHERE us.isRevoked = true")
    long countByIsRevokedTrue();

    @Query("SELECT COUNT(us) FROM UserSession us WHERE us.refreshTokenExpiresAt < :now")
    long countByRefreshTokenExpiresAtBefore(@Param("now") Instant now);
}