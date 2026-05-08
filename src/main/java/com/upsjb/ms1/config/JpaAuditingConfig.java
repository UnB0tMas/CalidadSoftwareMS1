package com.upsjb.ms1.config;

import com.upsjb.ms1.security.principal.AuthenticatedUserContext;
import com.upsjb.ms1.util.StringNormalizer;
import java.util.Optional;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.jwt.Jwt;

@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
public class JpaAuditingConfig {

    private final AppPropertiesConfig appProperties;

    public JpaAuditingConfig(AppPropertiesConfig appProperties) {
        this.appProperties = appProperties;
    }

    @Bean
    public AuditorAware<String> auditorAware() {
        return () -> Optional.of(resolveCurrentAuditor());
    }

    private String resolveCurrentAuditor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return systemAuditor();
        }

        Object principal = authentication.getPrincipal();

        String auditor = switch (principal) {
            case AuthenticatedUserContext actor -> actor.username();
            case UserDetails userDetails -> userDetails.getUsername();
            case Jwt jwt -> resolveJwtAuditor(jwt);
            case String value -> value;
            default -> authentication.getName();
        };

        return truncate(
                StringNormalizer.normalizeSpaces(auditor),
                appProperties.getJpa().getAuditorMaxLength(),
                systemAuditor()
        );
    }

    private String resolveJwtAuditor(Jwt jwt) {
        String username = jwt.getClaimAsString("username");

        if (username != null && !username.isBlank()) {
            return username;
        }

        String email = jwt.getClaimAsString("email");

        if (email != null && !email.isBlank()) {
            return email;
        }

        return jwt.getSubject();
    }

    private String systemAuditor() {
        return truncate(
                appProperties.getJpa().getSystemAuditor(),
                appProperties.getJpa().getAuditorMaxLength(),
                "SYSTEM"
        );
    }

    private String truncate(String value, int maxLength, String fallback) {
        String normalized = StringNormalizer.normalizeSpaces(value);

        if (normalized == null) {
            normalized = fallback;
        }

        if (maxLength <= 0) {
            return normalized;
        }

        return normalized.length() <= maxLength
                ? normalized
                : normalized.substring(0, maxLength);
    }
}