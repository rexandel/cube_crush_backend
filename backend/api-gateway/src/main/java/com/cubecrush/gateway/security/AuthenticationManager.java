package com.cubecrush.gateway.security;

import com.cubecrush.gateway.util.JwtUtil;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;

@Component
@RequiredArgsConstructor
public class AuthenticationManager implements ReactiveAuthenticationManager {

    private final JwtUtil jwtUtil;

    @Override
    public Mono<Authentication> authenticate(Authentication authentication) {
        String authToken = authentication.getCredentials().toString();
        
        try {
            if (!jwtUtil.validateToken(authToken)) {
                return Mono.empty();
            }
            
            Claims claims = jwtUtil.getAllClaimsFromToken(authToken);
            String username = claims.getSubject();
            List<SimpleGrantedAuthority> authorities = Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"));
            
            return Mono.just(new UsernamePasswordAuthenticationToken(
                username,
                authToken,
                authorities
            ));
        } catch (Exception e) {
            return Mono.empty();
        }
    }
}
