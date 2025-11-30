package com.cubecrush.auth.repository;

import com.cubecrush.auth.model.UserSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserSessionRepository extends JpaRepository<UserSession, Long> {
    Optional<UserSession> findByJti(String jti);

    Optional<UserSession> findByRefreshTokenHash(String refreshTokenHash);

    List<UserSession> findByUserIdAndIsRevokedFalseAndExpiresAtAfter(Long userId, LocalDateTime now);

    @Modifying
    @Query("UPDATE UserSession us SET us.isRevoked = true WHERE us.user.id = :userId AND us.isRevoked = false")
    void revokeAllUserSessions(@Param("userId") Long userId);

    @Modifying
    @Query("DELETE FROM UserSession us WHERE us.expiresAt < :now")
    void deleteExpiredSessions(@Param("now") LocalDateTime now);

    boolean existsByJti(String jti);
}