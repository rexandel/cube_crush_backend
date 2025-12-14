package com.cubecrush.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class InternalServiceFilter implements GatewayFilter {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        log.debug("InternalServiceFilter: Adding internal service headers");

        ServerHttpRequest mutatedRequest = exchange.getRequest()
                .mutate()
                .header("X-API-Gateway", "true")
                .header("X-Request-Id", generateRequestId())
                .build();

        ServerWebExchange mutatedExchange = exchange.mutate().request(mutatedRequest).build();

        log.debug("InternalServiceFilter: Internal service headers added. Request-Id: {}", 
                generateRequestId());

        return chain.filter(mutatedExchange);
    }

    private String generateRequestId() {
        return "REQ-" + System.currentTimeMillis() + "-" + 
               Thread.currentThread().getId();
    }

}
