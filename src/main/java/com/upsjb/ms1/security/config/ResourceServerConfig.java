package com.upsjb.ms1.security.config;

import com.upsjb.ms1.security.jwt.RoleJwtAuthenticationConverter;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ResourceServerConfig {

    private final RoleJwtAuthenticationConverter roleJwtAuthenticationConverter;

    public ResourceServerConfig(RoleJwtAuthenticationConverter roleJwtAuthenticationConverter) {
        this.roleJwtAuthenticationConverter = roleJwtAuthenticationConverter;
    }

    public RoleJwtAuthenticationConverter roleJwtAuthenticationConverter() {
        return roleJwtAuthenticationConverter;
    }
}