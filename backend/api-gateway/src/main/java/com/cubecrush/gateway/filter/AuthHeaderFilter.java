package com.cubecrush.gateway.filter;

import com.cubecrush.gateway.util.JwtUtil;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class AuthHeaderFilter implements GlobalFilter, Ordered {

    private final JwtUtil jwtUtil;

    public AuthHeaderFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                if (jwtUtil.validateToken(token)) {
                    String userId = jwtUtil.getUserId(token);
                    ServerWebExchange modifiedExchange = exchange.mutate()
                            .request(r -> r.header("X-User-Id", userId))
                            .build();
                    return chain.filter(modifiedExchange);
                }
            } catch (Exception e) {
            }
        }
        
        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return -1;
    }
}
