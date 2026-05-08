package com.upsjb.ms1.config;

import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerSecurityConfig {

    public static final String BEARER_AUTH_SCHEME = "bearerAuth";
    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String BEARER_FORMAT = "JWT";
    public static final String BEARER_SCHEME = "bearer";

    @Bean
    public SecurityScheme bearerAuthSecurityScheme() {
        return new SecurityScheme()
                .name(AUTHORIZATION_HEADER)
                .type(SecurityScheme.Type.HTTP)
                .scheme(BEARER_SCHEME)
                .bearerFormat(BEARER_FORMAT)
                .in(SecurityScheme.In.HEADER);
    }

    @Bean
    public SecurityRequirement bearerSecurityRequirement() {
        return new SecurityRequirement().addList(BEARER_AUTH_SCHEME);
    }
}