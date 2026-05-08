package com.upsjb.ms1.security.jwt;

import com.upsjb.ms1.repository.UsuarioSesionRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenValidator {

    private final JwtProperties jwtProperties;
    private final UsuarioSesionRepository usuarioSesionRepository;
    private final Clock clock;

    public JwtTokenValidator(
            JwtProperties jwtProperties,
            UsuarioSesionRepository usuarioSesionRepository,
            Clock clock
    ) {
        this.jwtProperties = jwtProperties;
        this.usuarioSesionRepository = usuarioSesionRepository;
        this.clock = clock;
    }

    public boolean isValidAccessToken(Jwt jwt) {
        if (jwt == null) {
            return false;
        }

        return hasValidIssuer(jwt)
                && hasValidAudience(jwt)
                && hasValidType(jwt)
                && !isExpired(jwt)
                && !isUsedBeforeValidTime(jwt)
                && hasValidUserClaims(jwt)
                && hasActiveSession(jwt);
    }

    public void validateAccessTokenOrThrow(Jwt jwt) {
        if (!isValidAccessToken(jwt)) {
            throw new JwtException("JWT inválido, expirado o asociado a una sesión no activa.");
        }
    }

    private boolean hasValidIssuer(Jwt jwt) {
        return jwt.getIssuer() != null
                && jwtProperties.getIssuer().equals(jwt.getIssuer().toString());
    }

    private boolean hasValidAudience(Jwt jwt) {
        List<String> tokenAudience = jwt.getAudience();

        if (tokenAudience == null || tokenAudience.isEmpty()) {
            return false;
        }

        return tokenAudience.stream().anyMatch(jwtProperties.getAudiences()::contains);
    }

    private boolean hasValidType(Jwt jwt) {
        String tokenType = jwt.getClaimAsString(jwtProperties.getTokenTypeClaim());
        return jwtProperties.getAccessTokenType().equals(tokenType);
    }

    private boolean hasValidUserClaims(Jwt jwt) {
        return asLong(jwt.getClaim(JwtClaimsFactory.USER_ID_CLAIM)) != null
                && hasText(jwt.getClaimAsString(JwtClaimsFactory.USERNAME_CLAIM))
                && hasText(jwt.getClaimAsString(jwtProperties.getRoleClaim()))
                && asLong(jwt.getClaim(jwtProperties.getSessionIdClaim())) != null;
    }

    private boolean hasActiveSession(Jwt jwt) {
        Long idSesion = asLong(jwt.getClaim(jwtProperties.getSessionIdClaim()));

        if (idSesion == null) {
            return false;
        }

        Instant now = Instant.now(clock);

        return usuarioSesionRepository.findById(idSesion)
                .filter(session -> session.estaActiva() && session.estaVigente(now))
                .isPresent();
    }

    private boolean isExpired(Jwt jwt) {
        Instant expiresAt = jwt.getExpiresAt();

        if (expiresAt == null) {
            return true;
        }

        Instant now = Instant.now(clock).minusSeconds(jwtProperties.getClockSkewSeconds());
        return expiresAt.isBefore(now);
    }

    private boolean isUsedBeforeValidTime(Jwt jwt) {
        Instant notBefore = jwt.getNotBefore();

        if (notBefore == null) {
            return false;
        }

        Instant now = Instant.now(clock).plusSeconds(jwtProperties.getClockSkewSeconds());
        return notBefore.isAfter(now);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private Long asLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }

        if (value instanceof String text && !text.isBlank()) {
            try {
                return Long.valueOf(text);
            } catch (NumberFormatException exception) {
                return null;
            }
        }

        return null;
    }
}
