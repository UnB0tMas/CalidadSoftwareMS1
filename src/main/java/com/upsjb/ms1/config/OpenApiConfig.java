package com.upsjb.ms1.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import java.util.ArrayList;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private final AppPropertiesConfig appProperties;
    private final SecurityScheme bearerAuthSecurityScheme;
    private final SecurityRequirement bearerSecurityRequirement;

    public OpenApiConfig(
            AppPropertiesConfig appProperties,
            SecurityScheme bearerAuthSecurityScheme,
            SecurityRequirement bearerSecurityRequirement
    ) {
        this.appProperties = appProperties;
        this.bearerAuthSecurityScheme = bearerAuthSecurityScheme;
        this.bearerSecurityRequirement = bearerSecurityRequirement;
    }

    @Bean
    public OpenAPI openAPI() {
        AppPropertiesConfig.OpenApi openApiProperties = appProperties.getOpenApi();

        return new OpenAPI()
                .info(buildInfo(openApiProperties))
                .servers(buildServers(openApiProperties))
                .components(new Components().addSecuritySchemes(
                        SwaggerSecurityConfig.BEARER_AUTH_SCHEME,
                        bearerAuthSecurityScheme
                ))
                .addSecurityItem(bearerSecurityRequirement);
    }

    private Info buildInfo(AppPropertiesConfig.OpenApi openApiProperties) {
        Contact contact = new Contact()
                .name(openApiProperties.getContactName())
                .email(openApiProperties.getContactEmail())
                .url(openApiProperties.getContactUrl());

        return new Info()
                .title(openApiProperties.getTitle())
                .description(openApiProperties.getDescription())
                .version(appProperties.getVersion())
                .contact(contact);
    }

    private List<Server> buildServers(AppPropertiesConfig.OpenApi openApiProperties) {
        List<Server> servers = new ArrayList<>();

        if (openApiProperties.getServers().isEmpty()) {
            servers.add(new Server()
                    .url(appProperties.getGatewayUrl())
                    .description("API Gateway"));

            servers.add(new Server()
                    .url("http://localhost:8080")
                    .description("Servidor local del MS1"));

            return servers;
        }

        for (AppPropertiesConfig.OpenApi.ApiServer configuredServer : openApiProperties.getServers()) {
            servers.add(new Server()
                    .url(configuredServer.getUrl())
                    .description(configuredServer.getDescription()));
        }

        return servers;
    }
}