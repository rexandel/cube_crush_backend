package com.cubecrush.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
public class AuthServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthServiceApplication.class, args);
    }
}

@RestController
class HealthController {

    @GetMapping("/health")
    public String health() {
        return """
            {
                "status": "OK",
                "service": "auth-service",
                "timestamp": "%s"
            }
            """.formatted(java.time.LocalDateTime.now());
    }

    @GetMapping("/")
    public String home() {
        return """
            <div style="text-align: center; padding: 50px;">
                <h1>🚀 Cube Crush - Auth Service</h1>
                <p>Service is running successfully!</p>
                <ul style="list-style: none; padding: 0;">
                    <li><a href="/health">Health Check</a></li>
                    <li><a href="/actuator/health">Spring Boot Actuator</a></li>
                </ul>
            </div>
            """;
    }
}