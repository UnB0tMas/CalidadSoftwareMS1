package com.upsjb.ms1.security.jwt;

import com.upsjb.ms1.domain.entity.Usuario;
import com.upsjb.ms1.domain.entity.UsuarioSesion;
import com.upsjb.ms1.security.principal.CustomUserDetails;
import java.time.Instant;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;

@Service
public class JwtTokenService {

    private final JwtEncoder jwtEncoder;
    private final JwtDecoder jwtDecoder;
    private final JwtClaimsFactory jwtClaimsFactory;
    private final JwtProperties jwtProperties;

    public JwtTokenService(
            JwtEncoder jwtEncoder,
            JwtDecoder jwtDecoder,
            JwtClaimsFactory jwtClaimsFactory,
            JwtProperties jwtProperties
    ) {
        this.jwtEncoder = jwtEncoder;
        this.jwtDecoder = jwtDecoder;
        this.jwtClaimsFactory = jwtClaimsFactory;
        this.jwtProperties = jwtProperties;
    }

    public String generateAccessToken(
            Usuario usuario,
            UsuarioSesion sesion
    ) {
        JwtClaimsSet claims = jwtClaimsFactory.fromUsuario(usuario, sesion);
        return encode(claims);
    }

    public String generateAccessToken(
            CustomUserDetails userDetails,
            Long sessionId
    ) {
        JwtClaimsSet claims = jwtClaimsFactory.fromUserDetails(userDetails, sessionId);
        return encode(claims);
    }

    public Jwt decode(String token) {
        return jwtDecoder.decode(token);
    }

    public Instant getExpiration(String token) {
        Jwt jwt = decode(token);
        return jwt.getExpiresAt();
    }

    public String getTokenId(String token) {
        Jwt jwt = decode(token);
        return jwt.getId();
    }

    public Long getSessionId(String token) {
        Jwt jwt = decode(token);
        Object value = jwt.getClaim(jwtProperties.getSessionIdClaim());

        if (value instanceof Number number) {
            return number.longValue();
        }

        if (value instanceof String text && !text.isBlank()) {
            return Long.valueOf(text);
        }

        return null;
    }

    private String encode(JwtClaimsSet claims) {
        JwsHeader header = JwsHeader
                .with(SignatureAlgorithm.RS256)
                .keyId(jwtProperties.getKeyId())
                .build();

        return jwtEncoder
                .encode(JwtEncoderParameters.from(header, claims))
                .getTokenValue();
    }
}