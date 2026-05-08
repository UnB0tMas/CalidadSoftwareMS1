package com.upsjb.ms1.security.jwt;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

@Validated
@Configuration
@ConfigurationProperties(prefix = "security.jwt")
public class JwtProperties {

    @NotBlank(message = "El issuer JWT es obligatorio.")
    private String issuer = "http://localhost:8080";

    private List<String> audiences = new ArrayList<>(List.of("ms-seguridad-usuarios"));

    @Positive(message = "Los minutos del access token deben ser positivos.")
    private long accessTokenMinutes = 15;

    @Positive(message = "Los segundos de tolerancia JWT deben ser positivos.")
    private long clockSkewSeconds = 60;

    @NotBlank(message = "El Key ID JWT es obligatorio.")
    private String keyId = "ms1-rsa-key";

    @NotBlank(message = "La ruta de la llave pública es obligatoria.")
    private String publicKeyLocation = "classpath:keys/public_key.pem";

    @NotBlank(message = "La ruta de la llave privada es obligatoria.")
    private String privateKeyLocation = "classpath:keys/private_key.pem";

    @NotBlank(message = "El algoritmo JWT es obligatorio.")
    private String jwsAlgorithm = "RS256";

    @NotBlank(message = "El claim de roles es obligatorio.")
    private String rolesClaim = "roles";

    @NotBlank(message = "El claim de authorities es obligatorio.")
    private String authoritiesClaim = "authorities";

    @NotBlank(message = "El claim de rol principal es obligatorio.")
    private String roleClaim = "rol";

    @NotBlank(message = "El claim de sesión es obligatorio.")
    private String sessionIdClaim = "sid";

    @NotBlank(message = "El claim de tipo de token es obligatorio.")
    private String tokenTypeClaim = "typ";

    @NotBlank(message = "El valor del tipo access token es obligatorio.")
    private String accessTokenType = "access";

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = normalize(issuer, this.issuer);
    }

    public List<String> getAudiences() {
        return List.copyOf(audiences);
    }

    public void setAudiences(List<String> audiences) {
        if (audiences == null || audiences.isEmpty()) {
            return;
        }

        this.audiences = audiences.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .distinct()
                .toList();
    }

    public long getAccessTokenMinutes() {
        return accessTokenMinutes;
    }

    public void setAccessTokenMinutes(long accessTokenMinutes) {
        this.accessTokenMinutes = accessTokenMinutes;
    }

    public long getClockSkewSeconds() {
        return clockSkewSeconds;
    }

    public void setClockSkewSeconds(long clockSkewSeconds) {
        this.clockSkewSeconds = clockSkewSeconds;
    }

    public String getKeyId() {
        return keyId;
    }

    public void setKeyId(String keyId) {
        this.keyId = normalize(keyId, this.keyId);
    }

    public String getPublicKeyLocation() {
        return publicKeyLocation;
    }

    public void setPublicKeyLocation(String publicKeyLocation) {
        this.publicKeyLocation = normalize(publicKeyLocation, this.publicKeyLocation);
    }

    public String getPrivateKeyLocation() {
        return privateKeyLocation;
    }

    public void setPrivateKeyLocation(String privateKeyLocation) {
        this.privateKeyLocation = normalize(privateKeyLocation, this.privateKeyLocation);
    }

    public String getJwsAlgorithm() {
        return jwsAlgorithm;
    }

    public void setJwsAlgorithm(String jwsAlgorithm) {
        this.jwsAlgorithm = normalize(jwsAlgorithm, this.jwsAlgorithm);
    }

    public String getRolesClaim() {
        return rolesClaim;
    }

    public void setRolesClaim(String rolesClaim) {
        this.rolesClaim = normalize(rolesClaim, this.rolesClaim);
    }

    public String getAuthoritiesClaim() {
        return authoritiesClaim;
    }

    public void setAuthoritiesClaim(String authoritiesClaim) {
        this.authoritiesClaim = normalize(authoritiesClaim, this.authoritiesClaim);
    }

    public String getRoleClaim() {
        return roleClaim;
    }

    public void setRoleClaim(String roleClaim) {
        this.roleClaim = normalize(roleClaim, this.roleClaim);
    }

    public String getSessionIdClaim() {
        return sessionIdClaim;
    }

    public void setSessionIdClaim(String sessionIdClaim) {
        this.sessionIdClaim = normalize(sessionIdClaim, this.sessionIdClaim);
    }

    public String getTokenTypeClaim() {
        return tokenTypeClaim;
    }

    public void setTokenTypeClaim(String tokenTypeClaim) {
        this.tokenTypeClaim = normalize(tokenTypeClaim, this.tokenTypeClaim);
    }

    public String getAccessTokenType() {
        return accessTokenType;
    }

    public void setAccessTokenType(String accessTokenType) {
        this.accessTokenType = normalize(accessTokenType, this.accessTokenType);
    }

    private static String normalize(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}