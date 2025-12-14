package com.cubecrush.gateway.config;

import com.cubecrush.gateway.filter.JwtAuthFilter;
import com.cubecrush.gateway.filter.InternalServiceFilter;
import com.cubecrush.gateway.filter.RequestLogger;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;

@Configuration
public class GatewayConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final InternalServiceFilter internalServiceFilter;
    private final RequestLogger requestLogger;

    public GatewayConfig(JwtAuthFilter jwtAuthFilter, 
                         InternalServiceFilter internalServiceFilter,
                         RequestLogger requestLogger) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.internalServiceFilter = internalServiceFilter;
        this.requestLogger = requestLogger;
    }

    @Bean
    public RouteLocator routes(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("auth-register", r -> r
                        .path("/api/v1/auth/register")
                        .and()
                        .method(HttpMethod.POST)
                        .filters(f -> f
                                .filter(requestLogger)
                                .filter(internalServiceFilter)
                        )
                        .uri("lb://auth-service")
                )
                .route("auth-login", r -> r
                        .path("/api/v1/auth/login")
                        .and()
                        .method(HttpMethod.POST)
                        .filters(f -> f
                                .filter(requestLogger)
                                .filter(internalServiceFilter)
                        )
                        .uri("lb://auth-service")
                )
                .route("auth-validate", r -> r
                        .path("/api/v1/auth/validate")
                        .and()
                        .method(HttpMethod.POST)
                        .filters(f -> f
                                .filter(requestLogger)
                                .filter(internalServiceFilter)
                        )
                        .uri("lb://auth-service")
                )

                .route("auth-logout", r -> r
                        .path("/api/v1/auth/logout")
                        .and()
                        .method(HttpMethod.POST)
                        .filters(f -> f
                                .filter(jwtAuthFilter)
                                .filter(requestLogger)
                                .filter(internalServiceFilter)
                        )
                        .uri("lb://auth-service")
                )
                .route("auth-refresh", r -> r
                        .path("/api/v1/auth/refresh")
                        .and()
                        .method(HttpMethod.POST)
                        .filters(f -> f
                                .filter(jwtAuthFilter)
                                .filter(requestLogger)
                                .filter(internalServiceFilter)
                        )
                        .uri("lb://auth-service")
                )

                .route("user-create", r -> r
                        .path("/api/v1/users", "/user-service/api/v1/users")
                        .and()
                        .method(HttpMethod.POST)
                        .filters(f -> f
                                .rewritePath("/user-service/(?<segment>.*)", "/${segment}")
                                .filter(requestLogger)
                                .filter(internalServiceFilter)
                        )
                        .uri("lb://user-service")
                )

                .route("user-profile", r -> r
                        .path("/api/v1/users/**", "/user-service/api/v1/users/**")
                        .and()
                        .method(HttpMethod.GET, HttpMethod.PUT, HttpMethod.PATCH, HttpMethod.DELETE)
                        .filters(f -> f
                                .rewritePath("/user-service/(?<segment>.*)", "/${segment}")
                                .filter(jwtAuthFilter)
                                .filter(requestLogger)
                                .filter(internalServiceFilter)
                        )
                        .uri("lb://user-service")
                )

                .route("gateway-health", r -> r
                        .path("/actuator/**")
                        .filters(f -> f
                                .filter(requestLogger)
                        )
                        .uri("lb://api-gateway")
                )

                .build();
    }

}
