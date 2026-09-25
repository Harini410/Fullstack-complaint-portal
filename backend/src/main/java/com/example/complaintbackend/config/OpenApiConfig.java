package com.example.complaintbackend.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "bearer-jwt";

    @Bean
    public OpenAPI complaintPortalOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Smart Complaint Management Portal API")
                        .description("Production-grade RESTful API for the Smart Complaint Management Portal. " +
                                     "Supports full complaint lifecycle, role-based authorization (USER, ADMIN, SUPPORT_AGENT), " +
                                     "Kafka event dispatch, and Redis caching.")
                        .version("v1.0.0")
                        .contact(new Contact()
                                .name("Engineering Team")
                                .email("support@complaintportal.com"))
                        .license(new License().name("MIT License")))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME, new SecurityScheme()
                                .name(SECURITY_SCHEME_NAME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Enter the JWT Bearer token obtained from POST /api/auth/login or /api/auth/register")));
    }
}
