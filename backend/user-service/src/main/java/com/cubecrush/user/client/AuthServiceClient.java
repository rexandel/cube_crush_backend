package com.cubecrush.user.client;

import com.cubecrush.user.web.dto.TokenValidationResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "auth-service", url = "http://localhost:8081")
public interface AuthServiceClient {

    @PostMapping("/api/v1/auth/validate")
    boolean validateToken(@RequestHeader("Authorization") String token);

    @PostMapping("/api/v1/auth/validate-token")
    TokenValidationResponse validateTokenWithUser(@RequestHeader("Authorization") String token);
}