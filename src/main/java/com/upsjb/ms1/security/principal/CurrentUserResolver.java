package com.upsjb.ms1.security.principal;

import com.upsjb.ms1.security.jwt.JwtProperties;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashSet;
import java.util.Set;
import org.springframework.core.MethodParameter;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@Component
public class CurrentUserResolver implements HandlerMethodArgumentResolver {

    private final JwtProperties jwtProperties;

    public CurrentUserResolver(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return AuthenticatedUserContext.class.isAssignableFrom(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(
            MethodParameter parameter,
            ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest,
            WebDataBinderFactory binderFactory
    ) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return AuthenticatedUserContext.anonymous();
        }

        HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
        String ip = request == null ? null : request.getRemoteAddr();
        String userAgent = request == null ? null : request.getHeader("User-Agent");

        Object principal = authentication.getPrincipal();

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
        Long userId = asLong(jwt.getClaim("userId"));
        Long idRol = asLong(jwt.getClaim("idRol"));
        Long sessionId = asLong(jwt.getClaim(jwtProperties.getSessionIdClaim()));

        return new AuthenticatedUserContext(
                userId,
                jwt.getClaimAsString("username"),
                jwt.getClaimAsString("email"),
                idRol,
                jwt.getClaimAsString(jwtProperties.getRoleClaim()),
                jwt.getClaimAsString("nombreRol"),
                authorityNames(authentication),
                sessionId,
                ip,
                userAgent
        );
    }

    private Set<String> authorityNames(Authentication authentication) {
        Set<String> authorities = new LinkedHashSet<>();

        for (GrantedAuthority authority : authentication.getAuthorities()) {
            authorities.add(authority.getAuthority());
        }

        return authorities;
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
}