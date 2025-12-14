package com.cubecrush.gateway.filter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class JwtAuthFilter implements GatewayFilter {

    @Value("${jwt.secret:mySuperSecretKeyForCubeCrushGameThatShouldBeVeryLongAndSecure123!@#4567890}")
    private String jwtSecret;

    private final SecretKey signingKey;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public JwtAuthFilter(@Value("${jwt.secret}") String jwtSecret) {
        this.signingKey = io.jsonwebtoken.security.Keys.hmacShaKeyFor(
                jwtSecret.getBytes(StandardCharsets.UTF_8)
        );
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        log.debug("JwtAuthFilter: Processing request for URI: {}", exchange.getRequest().getURI());

        String token = getTokenFromRequest(exchange.getRequest());

        if (!StringUtils.hasText(token)) {
            log.warn("JwtAuthFilter: No token provided in Authorization header");
            return onError(exchange, "Missing or invalid Authorization header", HttpStatus.UNAUTHORIZED);
        }

        try {
            Claims claims = validateToken(token);

            Object userIdObj = claims.get("userId");
            String userId = userIdObj != null ? String.valueOf(userIdObj) : null;
            String email = claims.getSubject();

            if (!StringUtils.hasText(userId)) {
                log.warn("JwtAuthFilter: userId not found in token claims");
                return onError(exchange, "Invalid token: missing userId", HttpStatus.UNAUTHORIZED);
            }

            log.debug("JwtAuthFilter: Token validated successfully. userId={}, email={}", userId, email);

            ServerHttpRequest mutatedRequest = exchange.getRequest()
                    .mutate()
                    .header("X-User-Id", userId)
                    .header("X-User-Email", email != null ? email : "")
                    .build();

            return chain.filter(exchange.mutate().request(mutatedRequest).build());

        } catch (JwtException e) {
            log.warn("JwtAuthFilter: Invalid JWT token - {} : {}", e.getClass().getSimpleName(), e.getMessage());
            String message;
            if (e instanceof io.jsonwebtoken.ExpiredJwtException) {
                message = "Token has expired";
            } else if (e instanceof io.jsonwebtoken.UnsupportedJwtException) {
                message = "Unsupported JWT token";
            } else if (e instanceof io.jsonwebtoken.MalformedJwtException) {
                message = "Malformed JWT token";
            } else if (e instanceof io.jsonwebtoken.security.SignatureException) {
                message = "Invalid JWT signature";
            } else {
                message = "Invalid JWT token";
            }
            return onError(exchange, message, HttpStatus.UNAUTHORIZED);
        } catch (Exception e) {
            log.error("JwtAuthFilter: Unexpected error during authentication", e);
            return onError(exchange, "Authentication failed", HttpStatus.UNAUTHORIZED);
        }
    }

    private String getTokenFromRequest(ServerHttpRequest request) {
        String authHeader = request.getHeaders().getFirst("Authorization");
        if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }

    private Claims validateToken(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private Mono<Void> onError(ServerWebExchange exchange, String message, HttpStatus status) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> error = new HashMap<>();
        error.put("error", "Authentication Error");
        error.put("message", message);
        error.put("status", status.value());
        error.put("timestamp", System.currentTimeMillis());

        byte[] bytes;
        try {
            bytes = objectMapper.writeValueAsBytes(error);
        } catch (JsonProcessingException e) {
            bytes = "{\"error\":\"Internal error\"}".getBytes(StandardCharsets.UTF_8);
        }

        DataBuffer buffer = response.bufferFactory().wrap(bytes);
        return response.writeWith(Mono.just(buffer));
    }
}