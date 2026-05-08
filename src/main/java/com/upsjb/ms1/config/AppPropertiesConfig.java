package com.upsjb.ms1.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

@Validated
@Configuration
@ConfigurationProperties(prefix = "app")
public class AppPropertiesConfig {

    @NotBlank(message = "El nombre de la aplicación es obligatorio.")
    private String name = "MS Seguridad Usuarios";

    @NotBlank(message = "La versión de la aplicación es obligatoria.")
    private String version = "1.0.0";

    @NotBlank(message = "La descripción de la aplicación es obligatoria.")
    private String description = "Microservicio de seguridad, usuarios, autenticación, OAuth2, JWT, correo, sesiones y auditoría.";

    @NotBlank(message = "La URL del frontend es obligatoria.")
    private String frontendUrl = "http://localhost:4200";

    @NotBlank(message = "La URL del gateway es obligatoria.")
    private String gatewayUrl = "http://localhost:8080";

    @NotBlank(message = "La zona horaria de la aplicación es obligatoria.")
    private String zoneId = "UTC";

    @Valid
    private Security security = new Security();

    @Valid
    private Mail mail = new Mail();

    @Valid
    private Cors cors = new Cors();

    @Valid
    private Cache cache = new Cache();

    @Valid
    private OpenApi openApi = new OpenApi();

    @Valid
    private Jpa jpa = new Jpa();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = normalize(name, this.name);
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = normalize(version, this.version);
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = normalize(description, this.description);
    }

    public String getFrontendUrl() {
        return frontendUrl;
    }

    public void setFrontendUrl(String frontendUrl) {
        this.frontendUrl = normalize(frontendUrl, this.frontendUrl);
    }

    public String getGatewayUrl() {
        return gatewayUrl;
    }

    public void setGatewayUrl(String gatewayUrl) {
        this.gatewayUrl = normalize(gatewayUrl, this.gatewayUrl);
    }

    public String getZoneId() {
        return zoneId;
    }

    public void setZoneId(String zoneId) {
        this.zoneId = normalize(zoneId, this.zoneId);
    }

    public Security getSecurity() {
        return security;
    }

    public void setSecurity(Security security) {
        this.security = security == null ? new Security() : security;
    }

    public Mail getMail() {
        return mail;
    }

    public void setMail(Mail mail) {
        this.mail = mail == null ? new Mail() : mail;
    }

    public Cors getCors() {
        return cors;
    }

    public void setCors(Cors cors) {
        this.cors = cors == null ? new Cors() : cors;
    }

    public Cache getCache() {
        return cache;
    }

    public void setCache(Cache cache) {
        this.cache = cache == null ? new Cache() : cache;
    }

    public OpenApi getOpenApi() {
        return openApi;
    }

    public void setOpenApi(OpenApi openApi) {
        this.openApi = openApi == null ? new OpenApi() : openApi;
    }

    public Jpa getJpa() {
        return jpa;
    }

    public void setJpa(Jpa jpa) {
        this.jpa = jpa == null ? new Jpa() : jpa;
    }

    private static String normalize(String value, String defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }

        return value.trim();
    }

    private static List<String> normalizeList(List<String> values, List<String> defaults) {
        if (values == null || values.isEmpty()) {
            return new ArrayList<>(defaults);
        }

        return values.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .distinct()
                .toList();
    }

    public static class Security {

        @Positive(message = "Los minutos del access token deben ser positivos.")
        private long accessTokenMinutes = 15;

        @Positive(message = "Los días del refresh token deben ser positivos.")
        private long refreshTokenDays = 7;

        @Positive(message = "Los segundos de tolerancia JWT deben ser positivos.")
        private long jwtClockSkewSeconds = 60;

        @Positive(message = "Los minutos del código de cambio de contraseña deben ser positivos.")
        private long passwordChangeCodeMinutes = 10;

        @Positive(message = "Los minutos del token de recuperación deben ser positivos.")
        private long passwordResetTokenMinutes = 15;

        @Positive(message = "El máximo de intentos de verificación debe ser positivo.")
        private int verificationMaxAttempts = 5;

        @Positive(message = "El cooldown de códigos debe ser positivo.")
        private long verificationCodeCooldownSeconds = 60;

        @Positive(message = "El máximo de intentos de login debe ser positivo.")
        private int maxLoginAttempts = 5;

        @Positive(message = "Los minutos de bloqueo deben ser positivos.")
        private long lockMinutes = 15;

        public long getAccessTokenMinutes() {
            return accessTokenMinutes;
        }

        public void setAccessTokenMinutes(long accessTokenMinutes) {
            this.accessTokenMinutes = accessTokenMinutes;
        }

        public long getRefreshTokenDays() {
            return refreshTokenDays;
        }

        public void setRefreshTokenDays(long refreshTokenDays) {
            this.refreshTokenDays = refreshTokenDays;
        }

        public long getJwtClockSkewSeconds() {
            return jwtClockSkewSeconds;
        }

        public void setJwtClockSkewSeconds(long jwtClockSkewSeconds) {
            this.jwtClockSkewSeconds = jwtClockSkewSeconds;
        }

        public long getPasswordChangeCodeMinutes() {
            return passwordChangeCodeMinutes;
        }

        public void setPasswordChangeCodeMinutes(long passwordChangeCodeMinutes) {
            this.passwordChangeCodeMinutes = passwordChangeCodeMinutes;
        }

        public long getPasswordResetTokenMinutes() {
            return passwordResetTokenMinutes;
        }

        public void setPasswordResetTokenMinutes(long passwordResetTokenMinutes) {
            this.passwordResetTokenMinutes = passwordResetTokenMinutes;
        }

        public int getVerificationMaxAttempts() {
            return verificationMaxAttempts;
        }

        public void setVerificationMaxAttempts(int verificationMaxAttempts) {
            this.verificationMaxAttempts = verificationMaxAttempts;
        }

        public long getVerificationCodeCooldownSeconds() {
            return verificationCodeCooldownSeconds;
        }

        public void setVerificationCodeCooldownSeconds(long verificationCodeCooldownSeconds) {
            this.verificationCodeCooldownSeconds = verificationCodeCooldownSeconds;
        }

        public int getMaxLoginAttempts() {
            return maxLoginAttempts;
        }

        public void setMaxLoginAttempts(int maxLoginAttempts) {
            this.maxLoginAttempts = maxLoginAttempts;
        }

        public long getLockMinutes() {
            return lockMinutes;
        }

        public void setLockMinutes(long lockMinutes) {
            this.lockMinutes = lockMinutes;
        }
    }

    public static class Mail {

        @NotBlank(message = "El correo remitente es obligatorio.")
        @Email(message = "El correo remitente no tiene formato válido.")
        private String from = "no-reply@localhost.test";

        @NotBlank(message = "El nombre del remitente es obligatorio.")
        private String senderName = "MS Seguridad Usuarios";

        @Email(message = "El correo de soporte no tiene formato válido.")
        private String supportEmail = "soporte@localhost.test";

        private boolean enabled = true;

        public String getFrom() {
            return from;
        }

        public void setFrom(String from) {
            this.from = normalize(from, this.from);
        }

        public String getSenderName() {
            return senderName;
        }

        public void setSenderName(String senderName) {
            this.senderName = normalize(senderName, this.senderName);
        }

        public String getSupportEmail() {
            return supportEmail;
        }

        public void setSupportEmail(String supportEmail) {
            this.supportEmail = normalize(supportEmail, this.supportEmail);
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }

    public static class Cors {

        private List<String> allowedOrigins = new ArrayList<>(List.of("http://localhost:4200"));

        private List<String> allowedOriginPatterns = new ArrayList<>();

        private List<String> allowedMethods = new ArrayList<>(List.of(
                "GET",
                "POST",
                "PUT",
                "PATCH",
                "DELETE",
                "OPTIONS"
        ));

        private List<String> allowedHeaders = new ArrayList<>(List.of(
                "Authorization",
                "Content-Type",
                "Accept",
                "Origin",
                "X-Requested-With",
                "X-Request-Id",
                "X-Correlation-Id"
        ));

        private List<String> exposedHeaders = new ArrayList<>(List.of(
                "Authorization",
                "X-Request-Id",
                "X-Correlation-Id"
        ));

        private boolean allowCredentials = false;

        @Positive(message = "El maxAge de CORS debe ser positivo.")
        private long maxAgeSeconds = 3600;

        @NotBlank(message = "El patrón CORS es obligatorio.")
        private String pathPattern = "/**";

        public List<String> getAllowedOrigins() {
            return List.copyOf(allowedOrigins);
        }

        public void setAllowedOrigins(List<String> allowedOrigins) {
            this.allowedOrigins = new ArrayList<>(normalizeList(
                    allowedOrigins,
                    List.of("http://localhost:4200")
            ));
        }

        public List<String> getAllowedOriginPatterns() {
            return List.copyOf(allowedOriginPatterns);
        }

        public void setAllowedOriginPatterns(List<String> allowedOriginPatterns) {
            this.allowedOriginPatterns = new ArrayList<>(normalizeList(
                    allowedOriginPatterns,
                    List.of()
            ));
        }

        public List<String> getAllowedMethods() {
            return List.copyOf(allowedMethods);
        }

        public void setAllowedMethods(List<String> allowedMethods) {
            this.allowedMethods = new ArrayList<>(normalizeList(
                    allowedMethods,
                    List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
            ));
        }

        public List<String> getAllowedHeaders() {
            return List.copyOf(allowedHeaders);
        }

        public void setAllowedHeaders(List<String> allowedHeaders) {
            this.allowedHeaders = new ArrayList<>(normalizeList(
                    allowedHeaders,
                    List.of("Authorization", "Content-Type", "Accept", "Origin", "X-Requested-With")
            ));
        }

        public List<String> getExposedHeaders() {
            return List.copyOf(exposedHeaders);
        }

        public void setExposedHeaders(List<String> exposedHeaders) {
            this.exposedHeaders = new ArrayList<>(normalizeList(
                    exposedHeaders,
                    List.of("Authorization", "X-Request-Id", "X-Correlation-Id")
            ));
        }

        public boolean isAllowCredentials() {
            return allowCredentials;
        }

        public void setAllowCredentials(boolean allowCredentials) {
            this.allowCredentials = allowCredentials;
        }

        public long getMaxAgeSeconds() {
            return maxAgeSeconds;
        }

        public void setMaxAgeSeconds(long maxAgeSeconds) {
            this.maxAgeSeconds = maxAgeSeconds;
        }

        public String getPathPattern() {
            return pathPattern;
        }

        public void setPathPattern(String pathPattern) {
            this.pathPattern = normalize(pathPattern, this.pathPattern);
        }
    }

    public static class Cache {

        public static final String VERIFICATION_CODE_COOLDOWN = "verification-code-cooldown";
        public static final String PASSWORD_RESET_COOLDOWN = "password-reset-cooldown";
        public static final String LOGIN_ATTEMPTS = "login-attempts";
        public static final String OAUTH2_STATE = "oauth2-state";
        public static final String USER_SECURITY_CONTEXT = "user-security-context";

        private List<String> names = new ArrayList<>(List.of(
                VERIFICATION_CODE_COOLDOWN,
                PASSWORD_RESET_COOLDOWN,
                LOGIN_ATTEMPTS,
                OAUTH2_STATE,
                USER_SECURITY_CONTEXT
        ));

        @Positive(message = "El tiempo de expiración de cache debe ser positivo.")
        private long expireAfterWriteMinutes = 10;

        @Positive(message = "El tamaño máximo de cache debe ser positivo.")
        private long maximumSize = 10_000;

        private boolean recordStats = true;

        private boolean allowNullValues = false;

        public List<String> getNames() {
            return List.copyOf(names);
        }

        public void setNames(List<String> names) {
            this.names = new ArrayList<>(normalizeList(
                    names,
                    List.of(
                            VERIFICATION_CODE_COOLDOWN,
                            PASSWORD_RESET_COOLDOWN,
                            LOGIN_ATTEMPTS,
                            OAUTH2_STATE,
                            USER_SECURITY_CONTEXT
                    )
            ));
        }

        public long getExpireAfterWriteMinutes() {
            return expireAfterWriteMinutes;
        }

        public void setExpireAfterWriteMinutes(long expireAfterWriteMinutes) {
            this.expireAfterWriteMinutes = expireAfterWriteMinutes;
        }

        public long getMaximumSize() {
            return maximumSize;
        }

        public void setMaximumSize(long maximumSize) {
            this.maximumSize = maximumSize;
        }

        public boolean isRecordStats() {
            return recordStats;
        }

        public void setRecordStats(boolean recordStats) {
            this.recordStats = recordStats;
        }

        public boolean isAllowNullValues() {
            return allowNullValues;
        }

        public void setAllowNullValues(boolean allowNullValues) {
            this.allowNullValues = allowNullValues;
        }
    }

    public static class OpenApi {

        @NotBlank(message = "El título OpenAPI es obligatorio.")
        private String title = "MS Seguridad Usuarios API";

        @NotBlank(message = "La descripción OpenAPI es obligatoria.")
        private String description = "API REST del microservicio de seguridad, usuarios, autenticación, sesiones y auditoría.";

        private String contactName = "Equipo MS1";

        @Email(message = "El correo de contacto OpenAPI no tiene formato válido.")
        private String contactEmail = "soporte@localhost.test";

        private String contactUrl = "http://localhost:4200";

        private List<ApiServer> servers = new ArrayList<>();

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = normalize(title, this.title);
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = normalize(description, this.description);
        }

        public String getContactName() {
            return contactName;
        }

        public void setContactName(String contactName) {
            this.contactName = normalize(contactName, this.contactName);
        }

        public String getContactEmail() {
            return contactEmail;
        }

        public void setContactEmail(String contactEmail) {
            this.contactEmail = normalize(contactEmail, this.contactEmail);
        }

        public String getContactUrl() {
            return contactUrl;
        }

        public void setContactUrl(String contactUrl) {
            this.contactUrl = normalize(contactUrl, this.contactUrl);
        }

        public List<ApiServer> getServers() {
            return List.copyOf(servers);
        }

        public void setServers(List<ApiServer> servers) {
            this.servers = servers == null ? new ArrayList<>() : new ArrayList<>(servers);
        }

        public static class ApiServer {

            @NotBlank(message = "La URL del servidor OpenAPI es obligatoria.")
            private String url;

            @NotBlank(message = "La descripción del servidor OpenAPI es obligatoria.")
            private String description;

            public ApiServer() {
            }

            public ApiServer(String url, String description) {
                this.url = url;
                this.description = description;
            }

            public String getUrl() {
                return url;
            }

            public void setUrl(String url) {
                this.url = normalize(url, this.url);
            }

            public String getDescription() {
                return description;
            }

            public void setDescription(String description) {
                this.description = normalize(description, this.description);
            }
        }
    }

    public static class Jpa {

        @NotBlank(message = "El auditor técnico del sistema es obligatorio.")
        private String systemAuditor = "SYSTEM";

        @PositiveOrZero(message = "La longitud máxima del auditor no puede ser negativa.")
        private int auditorMaxLength = 120;

        public String getSystemAuditor() {
            return systemAuditor;
        }

        public void setSystemAuditor(String systemAuditor) {
            this.systemAuditor = normalize(systemAuditor, this.systemAuditor);
        }

        public int getAuditorMaxLength() {
            return auditorMaxLength;
        }

        public void setAuditorMaxLength(int auditorMaxLength) {
            this.auditorMaxLength = auditorMaxLength;
        }
    }
}