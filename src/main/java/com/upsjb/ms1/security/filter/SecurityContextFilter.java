package com.upsjb.ms1.security.filter;

import com.upsjb.ms1.security.jwt.JwtProperties;
import com.upsjb.ms1.security.principal.AuthenticatedUserContext;
import com.upsjb.ms1.security.principal.CustomUserDetails;
import com.upsjb.ms1.shared.audit.AuditContext;
import com.upsjb.ms1.shared.audit.AuditContextHolder;
import com.upsjb.ms1.util.IpAddressUtil;
import com.upsjb.ms1.util.UserAgentUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Set;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class SecurityContextFilter extends OncePerRequestFilter {

    private final JwtProperties jwtProperties;

    public SecurityContextFilter(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken)) {
            AuthenticatedUserContext actor = resolveActor(authentication, request);
            enrichAuditContext(actor);
        }

        filterChain.doFilter(request, response);
    }

    private AuthenticatedUserContext resolveActor(
            Authentication authentication,
            HttpServletRequest request
    ) {
        Object principal = authentication.getPrincipal();
        String ip = IpAddressUtil.extractClientIp(request);
        String userAgent = UserAgentUtil.extractUserAgent(request);

        if (principal instanceof AuthenticatedUserContext actor) {
            return actor;
        }

        if (principal instanceof CustomUserDetails userDetails) {
            return userDetails.toAuthenticatedUserContext(null, ip, userAgent);
        }

        if (principal instanceof Jwt jwt) {
            return fromJwt(jwt, authentication, ip, userAgent);
        }

        return AuthenticatedUserContext.anonymous();
    }

    private AuthenticatedUserContext fromJwt(
            Jwt jwt,
            Authentication authentication,
            String ip,
            String userAgent
    ) {
        return new AuthenticatedUserContext(
                asLong(jwt.getClaim("userId")),
                jwt.getClaimAsString("username"),
                jwt.getClaimAsString("email"),
                asLong(jwt.getClaim("idRol")),
                jwt.getClaimAsString(jwtProperties.getRoleClaim()),
                jwt.getClaimAsString("nombreRol"),
                authorityNames(authentication),
                asLong(jwt.getClaim(jwtProperties.getSessionIdClaim())),
                ip,
                userAgent
        );
    }

    private Set<String> authorityNames(Authentication authentication) {
        Set<String> values = new LinkedHashSet<>();

        for (GrantedAuthority authority : authentication.getAuthorities()) {
            values.add(authority.getAuthority());
        }

        return values;
    }

    private Long asLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }

        if (value instanceof String text && !text.isBlank()) {
            return Long.valueOf(text);
        }

        return null;
    }

    private void enrichAuditContext(AuthenticatedUserContext actor) {
        if (actor == null || !actor.authenticated()) {
            return;
        }

        AuditContext current = AuditContextHolder.get();
        AuditContextHolder.set(current.withUser(actor.idUsuario(), actor.username()));
    }
}