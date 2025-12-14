package com.cubecrush.gateway.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/gateway")
public class GatewayController {

    @GetMapping("/status")
    public ResponseEntity<?> getStatus() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "API Gateway");
        response.put("port", 8080);
        response.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/info")
    public ResponseEntity<?> getInfo() {
        Map<String, Object> response = new HashMap<>();
        response.put("name", "Cube Crush API Gateway");
        response.put("version", "1.0.0");
        response.put("description", "Central API Gateway for Cube Crush Game");
        response.put("routes", new String[]{
                "POST /api/v1/auth/register - Register new user",
                "POST /api/v1/auth/login - Login user",
                "POST /api/v1/auth/validate - Validate JWT token",
                "POST /api/v1/auth/logout - Logout user (requires token)",
                "POST /api/v1/auth/refresh - Refresh JWT token (requires token)",
                "GET /api/v1/users/profile - Get user profile (requires token)",
                "PUT /api/v1/users/profile - Update user profile (requires token)",
                "POST /api/v1/users - Create user (requires token)",
                "DELETE /api/v1/users/{id} - Delete user (requires token)"
        });
        return ResponseEntity.ok(response);
    }

    @ExceptionHandler
    public ResponseEntity<?> handleException(Exception e) {
        Map<String, Object> response = new HashMap<>();
        response.put("error", e.getMessage());
        response.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

}
