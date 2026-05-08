package com.upsjb.ms1.security.jwt;

import com.upsjb.ms1.domain.entity.Rol;
import com.upsjb.ms1.domain.entity.Usuario;
import com.upsjb.ms1.domain.entity.UsuarioSesion;
import com.upsjb.ms1.security.principal.CustomUserDetails;
import com.upsjb.ms1.security.roles.SecurityRoles;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.stereotype.Component;

@Component
public class JwtClaimsFactory {

    public static final String USER_ID_CLAIM = "userId";
    public static final String USERNAME_CLAIM = "username";
    public static final String EMAIL_CLAIM = "email";
    public static final String ROLE_ID_CLAIM = "idRol";
    public static final String ROLE_NAME_CLAIM = "nombreRol";
    public static final String SESSION_ID_COMPAT_CLAIM = "sessionId";

    private final JwtProperties jwtProperties;
    private final Clock clock;

    public JwtClaimsFactory(
            JwtProperties jwtProperties,
            Clock clock
    ) {
        this.jwtProperties = jwtProperties;
        this.clock = clock;
    }

    public JwtClaimsSet fromUsuario(
            Usuario usuario,
            UsuarioSesion sesion
    ) {
        Rol rol = usuario.getRol();
        Instant issuedAt = Instant.now(clock);
        Instant expiresAt = issuedAt.plusSeconds(jwtProperties.getAccessTokenMinutes() * 60);

        String username = usuario.getUsername() == null ? null : usuario.getUsername().getValue();
        String email = usuario.getEmail() == null ? null : usuario.getEmail().getValue();
        String codigoRol = SecurityRoles.normalizeRoleCode(rol == null ? null : rol.getCodigo());
        String nombreRol = rol == null ? null : rol.getNombre();
        Long idRol = rol == null ? null : rol.getId();
        Long idSesion = sesion == null ? null : sesion.getId();

        return baseBuilder(username, issuedAt, expiresAt)
                .claim(USER_ID_CLAIM, usuario.getId())
                .claim(USERNAME_CLAIM, username)
                .claim(EMAIL_CLAIM, email)
                .claim(ROLE_ID_CLAIM, idRol)
                .claim(jwtProperties.getRoleClaim(), codigoRol)
                .claim(ROLE_NAME_CLAIM, nombreRol)
                .claim(jwtProperties.getRolesClaim(), codigoRol == null ? List.of() : List.of(codigoRol))
                .claim(jwtProperties.getAuthoritiesClaim(), codigoRol == null ? List.of() : List.of(SecurityRoles.toAuthority(codigoRol)))
                .claim(jwtProperties.getSessionIdClaim(), idSesion)
                .claim(SESSION_ID_COMPAT_CLAIM, idSesion)
                .claim(jwtProperties.getTokenTypeClaim(), jwtProperties.getAccessTokenType())
                .build();
    }

    public JwtClaimsSet fromUserDetails(
            CustomUserDetails userDetails,
            Long sessionId
    ) {
        Instant issuedAt = Instant.now(clock);
        Instant expiresAt = issuedAt.plusSeconds(jwtProperties.getAccessTokenMinutes() * 60);
        String codigoRol = SecurityRoles.normalizeRoleCode(userDetails.codigoRol());

        return baseBuilder(userDetails.getUsername(), issuedAt, expiresAt)
                .claim(USER_ID_CLAIM, userDetails.idUsuario())
                .claim(USERNAME_CLAIM, userDetails.getUsername())
                .claim(EMAIL_CLAIM, userDetails.email())
                .claim(ROLE_ID_CLAIM, userDetails.idRol())
                .claim(jwtProperties.getRoleClaim(), codigoRol)
                .claim(ROLE_NAME_CLAIM, userDetails.nombreRol())
                .claim(jwtProperties.getRolesClaim(), codigoRol == null ? List.of() : List.of(codigoRol))
                .claim(jwtProperties.getAuthoritiesClaim(), userDetails.authorityNames())
                .claim(jwtProperties.getSessionIdClaim(), sessionId)
                .claim(SESSION_ID_COMPAT_CLAIM, sessionId)
                .claim(jwtProperties.getTokenTypeClaim(), jwtProperties.getAccessTokenType())
                .build();
    }

    private JwtClaimsSet.Builder baseBuilder(
            String subject,
            Instant issuedAt,
            Instant expiresAt
    ) {
        return JwtClaimsSet.builder()
                .id(UUID.randomUUID().toString())
                .issuer(jwtProperties.getIssuer())
                .audience(jwtProperties.getAudiences())
                .subject(subject)
                .issuedAt(issuedAt)
                .notBefore(issuedAt)
                .expiresAt(expiresAt);
    }
}
