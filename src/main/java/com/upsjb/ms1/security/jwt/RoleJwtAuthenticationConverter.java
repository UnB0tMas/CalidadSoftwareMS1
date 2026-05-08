package com.upsjb.ms1.security.jwt;

import com.upsjb.ms1.security.roles.SecurityRoles;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

@Component
public class RoleJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private final JwtProperties jwtProperties;
    private final JwtTokenValidator jwtTokenValidator;

    public RoleJwtAuthenticationConverter(
            JwtProperties jwtProperties,
            JwtTokenValidator jwtTokenValidator
    ) {
        this.jwtProperties = jwtProperties;
        this.jwtTokenValidator = jwtTokenValidator;
    }

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        jwtTokenValidator.validateAccessTokenOrThrow(jwt);

        Set<GrantedAuthority> authorities = new LinkedHashSet<>();
        authorities.addAll(resolveRoleAuthorities(jwt));
        authorities.addAll(resolveAuthorities(jwt));

        return new JwtAuthenticationToken(jwt, authorities, resolvePrincipalName(jwt));
    }

    private Set<GrantedAuthority> resolveRoleAuthorities(Jwt jwt) {
        Set<String> roles = new LinkedHashSet<>();

        String mainRole = jwt.getClaimAsString(jwtProperties.getRoleClaim());
        if (mainRole != null && !mainRole.isBlank()) {
            roles.add(mainRole);
        }

        Object rolesClaim = jwt.getClaim(jwtProperties.getRolesClaim());
        if (rolesClaim instanceof Collection<?> collection) {
            collection.stream()
                    .filter(String.class::isInstance)
                    .map(String.class::cast)
                    .forEach(roles::add);
        }

        return roles.stream()
                .map(SecurityRoles::toAuthority)
                .filter(Objects::nonNull)
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private Set<GrantedAuthority> resolveAuthorities(Jwt jwt) {
        Object claim = jwt.getClaim(jwtProperties.getAuthoritiesClaim());

        if (!(claim instanceof Collection<?> collection)) {
            return Set.of();
        }

        return collection.stream()
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .map(SecurityRoles::normalizeAuthority)
                .filter(Objects::nonNull)
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private String resolvePrincipalName(Jwt jwt) {
        String username = jwt.getClaimAsString(JwtClaimsFactory.USERNAME_CLAIM);

        if (username != null && !username.isBlank()) {
            return username;
        }

        return jwt.getSubject();
    }
}
