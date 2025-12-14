package com.cubecrush.auth.service;

import com.cubecrush.auth.model.UserSession;
import com.cubecrush.auth.repository.UserSessionRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class JwtService {
    private final UserSessionRepository userSessionRepository;

    private SecretKey signingKey;

    @Value("${jwt.secret:mySuperSecretKeyForCubeCrushGameThatShouldBeVeryLongAndSecure}")
    private String jwtSecret;

    @Value("${jwt.expiration.access:900}")
    private long accessTokenExpirationSeconds;

    @Value("${jwt.expiration.refresh:604800}")
    private long refreshTokenExpirationSeconds;

    public JwtService(UserSessionRepository userSessionRepository) {
        this.userSessionRepository = userSessionRepository;
    }

    private SecretKey getSigningKey() {
        if (signingKey == null) {
            signingKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        }
        return signingKey;
    }

    public String generateAccessToken(Long userId, String nickname) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("nickname", nickname);
        claims.put("type", "access");

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(nickname)
                .setIssuedAt(new Date())
                .setExpiration(Date.from(Instant.now().plusSeconds(accessTokenExpirationSeconds)))
                .setId(UUID.randomUUID().toString())
                .signWith(getSigningKey())
                .compact();
    }

    public String generateRefreshToken(Long userId, String nickname) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("type", "refresh");

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(nickname)
                .setIssuedAt(new Date())
                .setExpiration(Date.from(Instant.now().plusSeconds(refreshTokenExpirationSeconds)))
                .setId(UUID.randomUUID().toString())
                .signWith(getSigningKey())
                .compact();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            log.debug("Invalid JWT token: {}", e.getMessage());
            return false;
        }
    }

    public String getUsernameFromToken(String token) {
        return getClaimsFromToken(token).getSubject();
    }

    public Long getUserIdFromToken(String token) {
        return getClaimsFromToken(token).get("userId", Long.class);
    }

    public String getJtiFromToken(String token) {
        return getClaimsFromToken(token).getId();
    }

    public Instant getExpirationFromToken(String token) {
        return getClaimsFromToken(token).getExpiration().toInstant();
    }

    public String getTokenTypeFromToken(String token) {
        return getClaimsFromToken(token).get("type", String.class);
    }

    private Claims getClaimsFromToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public Instant getExpirationFromJti(String jti) {
        return userSessionRepository.findByJti(jti)
                .map(UserSession::getRefreshTokenExpiresAt)
                .orElseThrow(() -> new IllegalArgumentException("Session not found for jti: " + jti));
    }

    public Duration getAccessTokenExpiration() {
        return Duration.ofSeconds(accessTokenExpirationSeconds);
    }

    public Duration getRefreshTokenExpiration() {
        return Duration.ofSeconds(refreshTokenExpirationSeconds);
    }

    public String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));

            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            log.error("SHA-256 algorithm not available", e);
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    public Instant getAccessTokenExpirationTime() {
        return Instant.now().plusSeconds(accessTokenExpirationSeconds);
    }

    public Instant getRefreshTokenExpirationTime() {
        return Instant.now().plusSeconds(refreshTokenExpirationSeconds);
    }

    public boolean isTokenValid(String token) {
        try {
            return validateToken(token) &&
                    "access".equals(getTokenTypeFromToken(token)) &&
                    getExpirationFromToken(token).isAfter(Instant.now());
        } catch (Exception e) {
            return false;
        }
    }
}