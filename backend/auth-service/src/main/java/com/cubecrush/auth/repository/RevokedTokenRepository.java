package com.cubecrush.auth.repository;

import com.cubecrush.auth.model.RevokedToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface RevokedTokenRepository extends JpaRepository<RevokedToken, Long> {

    boolean existsByJti(String jti);

    @Modifying
    @Query("DELETE FROM RevokedToken rt WHERE rt.expiresAt < :now")
    int deleteExpiredTokens(@Param("now") Instant now);

    @Modifying
    @Query("DELETE FROM RevokedToken rt WHERE rt.revokedAt < :threshold")
    int deleteByRevokedAtBefore(@Param("threshold") Instant threshold);

    @Query("SELECT COUNT(rt) FROM RevokedToken rt WHERE rt.expiresAt > :now")
    long countActiveRevokedTokens(@Param("now") Instant now);
}