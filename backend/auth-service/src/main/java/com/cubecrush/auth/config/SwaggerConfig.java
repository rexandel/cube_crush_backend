package com.cubecrush.auth.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI cubeCrushOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Cube Crush API")
                        .description("API для мобильной игры Cube Crush")
                        .version("1.5.0"))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8081")
                                .description("Сервер разработки")
                ))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}