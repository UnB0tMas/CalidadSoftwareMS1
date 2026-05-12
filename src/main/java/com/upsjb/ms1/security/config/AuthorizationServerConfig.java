package com.upsjb.ms1.security.config;

import com.upsjb.ms1.security.handler.RestAccessDeniedHandler;
import com.upsjb.ms1.security.handler.RestAuthenticationEntryPoint;
import com.upsjb.ms1.security.jwt.JwtProperties;
import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.oauth2.server.authorization.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.client.InMemoryRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class AuthorizationServerConfig {

    private static final Logger log = LoggerFactory.getLogger(AuthorizationServerConfig.class);

    private static final String CLIENT_PREFIX =
            "spring.security.oauth2.authorizationserver.client.angular-client";

    private static final String DEFAULT_CLIENT_ID = "angular-client";
    private static final String DEFAULT_CLIENT_SECRET = "{noop}ms1-local-angular-secret";

    private static final List<String> DEFAULT_REDIRECT_URIS = List.of(
            "http://localhost:4200/auth/callback",
            "http://127.0.0.1:4200/auth/callback"
    );

    private static final List<String> DEFAULT_POST_LOGOUT_REDIRECT_URIS = List.of(
            "http://localhost:4200",
            "http://127.0.0.1:4200"
    );

    private static final List<String> DEFAULT_CLIENT_AUTHENTICATION_METHODS = List.of(
            "client_secret_basic",
            "client_secret_post"
    );

    private static final List<String> DEFAULT_AUTHORIZATION_GRANT_TYPES = List.of(
            "authorization_code",
            "refresh_token",
            "client_credentials"
    );

    private static final List<String> DEFAULT_SCOPES = List.of(
            "openid",
            "profile",
            "email"
    );

    private final JwtProperties jwtProperties;
    private final Environment environment;

    public AuthorizationServerConfig(
            JwtProperties jwtProperties,
            Environment environment
    ) {
        this.jwtProperties = jwtProperties;
        this.environment = environment;
    }

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public SecurityFilterChain authorizationServerSecurityFilterChain(
            HttpSecurity http,
            RestAuthenticationEntryPoint authenticationEntryPoint,
            RestAccessDeniedHandler accessDeniedHandler
    ) throws Exception {
        OAuth2AuthorizationServerConfigurer authorizationServerConfigurer =
                new OAuth2AuthorizationServerConfigurer();

        http
                .securityMatcher(authorizationServerConfigurer.getEndpointsMatcher())
                .with(authorizationServerConfigurer, authorizationServer ->
                        authorizationServer.oidc(Customizer.withDefaults())
                )
                .csrf(AbstractHttpConfigurer::disable)
                .exceptionHandling(exceptionHandling -> exceptionHandling
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                )
                .authorizeHttpRequests(authorize -> authorize
                        .anyRequest().authenticated()
                );

        return http.build();
    }

    @Bean
    public RegisteredClientRepository registeredClientRepository() {
        RegisteredClient angularClient = buildAngularRegisteredClient();

        log.info(
                "OAuth2 RegisteredClient configurado correctamente. clientId={}, redirectUris={}, grants={}, scopes={}",
                angularClient.getClientId(),
                angularClient.getRedirectUris(),
                angularClient.getAuthorizationGrantTypes(),
                angularClient.getScopes()
        );

        return new InMemoryRegisteredClientRepository(angularClient);
    }

    @Bean
    public AuthorizationServerSettings authorizationServerSettings() {
        return AuthorizationServerSettings.builder()
                .issuer(jwtProperties.getIssuer())
                .authorizationEndpoint("/oauth2/authorize")
                .deviceAuthorizationEndpoint("/oauth2/device_authorization")
                .deviceVerificationEndpoint("/oauth2/device_verification")
                .tokenEndpoint("/oauth2/token")
                .tokenIntrospectionEndpoint("/oauth2/introspect")
                .tokenRevocationEndpoint("/oauth2/revoke")
                .jwkSetEndpoint("/oauth2/jwks")
                .oidcLogoutEndpoint("/connect/logout")
                .oidcUserInfoEndpoint("/userinfo")
                .oidcClientRegistrationEndpoint("/connect/register")
                .build();
    }

    private RegisteredClient buildAngularRegisteredClient() {
        String clientId = property(
                CLIENT_PREFIX + ".registration.client-id",
                DEFAULT_CLIENT_ID
        );

        String clientSecret = property(
                CLIENT_PREFIX + ".registration.client-secret",
                DEFAULT_CLIENT_SECRET
        );

        Set<ClientAuthenticationMethod> clientAuthenticationMethods = resolveClientAuthenticationMethods();
        Set<AuthorizationGrantType> authorizationGrantTypes = resolveAuthorizationGrantTypes();
        Set<String> redirectUris = resolveRedirectUris();
        Set<String> postLogoutRedirectUris = resolvePostLogoutRedirectUris();
        Set<String> scopes = resolveScopes();

        validateRegisteredClientConfiguration(
                clientId,
                clientSecret,
                clientAuthenticationMethods,
                authorizationGrantTypes,
                redirectUris,
                scopes
        );

        return RegisteredClient
                .withId(UUID.randomUUID().toString())
                .clientId(clientId)
                .clientSecret(clientSecret)
                .clientAuthenticationMethods(methods -> methods.addAll(clientAuthenticationMethods))
                .authorizationGrantTypes(grants -> grants.addAll(authorizationGrantTypes))
                .redirectUris(uris -> uris.addAll(redirectUris))
                .postLogoutRedirectUris(uris -> uris.addAll(postLogoutRedirectUris))
                .scopes(registeredScopes -> registeredScopes.addAll(scopes))
                .clientSettings(buildClientSettings())
                .tokenSettings(buildTokenSettings())
                .build();
    }

    private ClientSettings buildClientSettings() {
        boolean requireAuthorizationConsent = booleanProperty(
                CLIENT_PREFIX + ".require-authorization-consent",
                false
        );

        return ClientSettings.builder()
                .requireAuthorizationConsent(requireAuthorizationConsent)
                .requireProofKey(false)
                .build();
    }

    private TokenSettings buildTokenSettings() {
        long accessTokenMinutes = longProperty(
                "security.jwt.access-token-minutes",
                jwtProperties.getAccessTokenMinutes()
        );

        long refreshTokenDays = longProperty(
                "app.security.refresh-token-days",
                7L
        );

        return TokenSettings.builder()
                .accessTokenTimeToLive(Duration.ofMinutes(accessTokenMinutes))
                .refreshTokenTimeToLive(Duration.ofDays(refreshTokenDays))
                .authorizationCodeTimeToLive(Duration.ofMinutes(5))
                .reuseRefreshTokens(false)
                .build();
    }

    private Set<ClientAuthenticationMethod> resolveClientAuthenticationMethods() {
        List<String> values = listProperty(
                CLIENT_PREFIX + ".registration.client-authentication-methods",
                DEFAULT_CLIENT_AUTHENTICATION_METHODS
        );

        Set<ClientAuthenticationMethod> methods = new LinkedHashSet<>();

        for (String value : values) {
            String normalized = normalize(value);

            if (normalized == null) {
                continue;
            }

            switch (normalized.toLowerCase(Locale.ROOT)) {
                case "client_secret_basic" -> methods.add(ClientAuthenticationMethod.CLIENT_SECRET_BASIC);
                case "client_secret_post" -> methods.add(ClientAuthenticationMethod.CLIENT_SECRET_POST);
                case "none" -> methods.add(ClientAuthenticationMethod.NONE);
                default -> log.warn(
                        "OAuth2 client-authentication-method ignorado por no estar permitido. value={}",
                        normalized
                );
            }
        }

        if (methods.isEmpty()) {
            log.warn(
                    "No se encontró client-authentication-method válido. Se usarán defaults: {}",
                    DEFAULT_CLIENT_AUTHENTICATION_METHODS
            );
            methods.add(ClientAuthenticationMethod.CLIENT_SECRET_BASIC);
            methods.add(ClientAuthenticationMethod.CLIENT_SECRET_POST);
        }

        return methods;
    }

    private Set<AuthorizationGrantType> resolveAuthorizationGrantTypes() {
        List<String> values = listProperty(
                CLIENT_PREFIX + ".registration.authorization-grant-types",
                DEFAULT_AUTHORIZATION_GRANT_TYPES
        );

        Set<AuthorizationGrantType> grantTypes = new LinkedHashSet<>();

        for (String value : values) {
            String normalized = normalize(value);

            if (normalized == null) {
                continue;
            }

            switch (normalized.toLowerCase(Locale.ROOT)) {
                case "authorization_code" -> grantTypes.add(AuthorizationGrantType.AUTHORIZATION_CODE);
                case "refresh_token" -> grantTypes.add(AuthorizationGrantType.REFRESH_TOKEN);
                case "client_credentials" -> grantTypes.add(AuthorizationGrantType.CLIENT_CREDENTIALS);
                default -> log.warn(
                        "OAuth2 authorization-grant-type ignorado por no estar permitido. value={}. " +
                                "Revisa application.properties; probablemente hay una línea pegada o mal separada.",
                        normalized
                );
            }
        }

        if (grantTypes.isEmpty()) {
            log.warn(
                    "No se encontró authorization-grant-type válido. Se usarán defaults: {}",
                    DEFAULT_AUTHORIZATION_GRANT_TYPES
            );
            grantTypes.add(AuthorizationGrantType.AUTHORIZATION_CODE);
            grantTypes.add(AuthorizationGrantType.REFRESH_TOKEN);
            grantTypes.add(AuthorizationGrantType.CLIENT_CREDENTIALS);
        }

        return grantTypes;
    }

    private Set<String> resolveRedirectUris() {
        Set<String> redirectUris = new LinkedHashSet<>(listProperty(
                CLIENT_PREFIX + ".registration.redirect-uris",
                DEFAULT_REDIRECT_URIS
        ));

        redirectUris.removeIf(value -> !isValidHttpUrl(value));

        if (redirectUris.isEmpty()) {
            log.warn(
                    "No se encontró redirect-uri válido. Se usarán defaults: {}",
                    DEFAULT_REDIRECT_URIS
            );
            redirectUris.addAll(DEFAULT_REDIRECT_URIS);
        }

        return redirectUris;
    }

    private Set<String> resolvePostLogoutRedirectUris() {
        Set<String> uris = new LinkedHashSet<>(listProperty(
                CLIENT_PREFIX + ".registration.post-logout-redirect-uris",
                DEFAULT_POST_LOGOUT_REDIRECT_URIS
        ));

        uris.removeIf(value -> !isValidHttpUrl(value));

        if (uris.isEmpty()) {
            log.warn(
                    "No se encontró post-logout-redirect-uri válido. Se usarán defaults: {}",
                    DEFAULT_POST_LOGOUT_REDIRECT_URIS
            );
            uris.addAll(DEFAULT_POST_LOGOUT_REDIRECT_URIS);
        }

        return uris;
    }

    private Set<String> resolveScopes() {
        Set<String> scopes = new LinkedHashSet<>(listProperty(
                CLIENT_PREFIX + ".registration.scopes",
                DEFAULT_SCOPES
        ));

        scopes.removeIf(value -> value.contains("http://")
                || value.contains("https://")
                || value.contains("=")
                || value.length() > 80);

        if (scopes.isEmpty()) {
            log.warn(
                    "No se encontró scope válido. Se usarán defaults: {}",
                    DEFAULT_SCOPES
            );
            scopes.addAll(DEFAULT_SCOPES);
        }

        return scopes;
    }

    private void validateRegisteredClientConfiguration(
            String clientId,
            String clientSecret,
            Set<ClientAuthenticationMethod> clientAuthenticationMethods,
            Set<AuthorizationGrantType> authorizationGrantTypes,
            Set<String> redirectUris,
            Set<String> scopes
    ) {
        if (clientId == null || clientId.isBlank()) {
            throw new IllegalStateException("OAuth2 RegisteredClient inválido: clientId obligatorio.");
        }

        if (clientSecret == null || clientSecret.isBlank()) {
            throw new IllegalStateException("OAuth2 RegisteredClient inválido: clientSecret obligatorio.");
        }

        if (clientAuthenticationMethods == null || clientAuthenticationMethods.isEmpty()) {
            throw new IllegalStateException("OAuth2 RegisteredClient inválido: clientAuthenticationMethods obligatorio.");
        }

        if (authorizationGrantTypes == null || authorizationGrantTypes.isEmpty()) {
            throw new IllegalStateException("OAuth2 RegisteredClient inválido: authorizationGrantTypes obligatorio.");
        }

        if (redirectUris == null || redirectUris.isEmpty()) {
            throw new IllegalStateException(
                    "OAuth2 RegisteredClient inválido: redirectUris obligatorio. " +
                            "Configura " + CLIENT_PREFIX + ".registration.redirect-uris=http://localhost:4200/auth/callback,http://127.0.0.1:4200/auth/callback"
            );
        }

        if (scopes == null || scopes.isEmpty()) {
            throw new IllegalStateException("OAuth2 RegisteredClient inválido: scopes obligatorio.");
        }
    }

    private String property(
            String key,
            String defaultValue
    ) {
        String value = environment.getProperty(key);

        if (value == null || value.isBlank()) {
            return defaultValue;
        }

        return normalize(value);
    }

    private boolean booleanProperty(
            String key,
            boolean defaultValue
    ) {
        String value = environment.getProperty(key);

        if (value == null || value.isBlank()) {
            return defaultValue;
        }

        return Boolean.parseBoolean(value.trim());
    }

    private long longProperty(
            String key,
            long defaultValue
    ) {
        String value = environment.getProperty(key);

        if (value == null || value.isBlank()) {
            return defaultValue;
        }

        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException exception) {
            log.warn(
                    "Propiedad numérica inválida. key={}, value={}, defaultValue={}",
                    key,
                    value,
                    defaultValue
            );
            return defaultValue;
        }
    }

    private List<String> listProperty(
            String key,
            List<String> defaultValues
    ) {
        String[] indexedValues = environment.getProperty(key, String[].class);

        if (indexedValues != null && indexedValues.length > 0) {
            List<String> normalizedIndexedValues = normalizeList(List.of(indexedValues));

            if (!normalizedIndexedValues.isEmpty()) {
                return normalizedIndexedValues;
            }
        }

        String rawValue = environment.getProperty(key);

        if (rawValue != null && !rawValue.isBlank()) {
            List<String> normalizedRawValues = normalizeList(List.of(rawValue.split(",")));

            if (!normalizedRawValues.isEmpty()) {
                return normalizedRawValues;
            }
        }

        return normalizeList(defaultValues);
    }

    private List<String> normalizeList(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }

        return values.stream()
                .map(this::normalize)
                .filter(value -> value != null && !value.isBlank())
                .distinct()
                .toList();
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value
                .trim()
                .replace("\r", "")
                .replace("\n", "")
                .replace("\t", "");
    }

    private boolean isValidHttpUrl(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }

        String normalized = value.toLowerCase(Locale.ROOT);

        return normalized.startsWith("http://") || normalized.startsWith("https://");
    }
}