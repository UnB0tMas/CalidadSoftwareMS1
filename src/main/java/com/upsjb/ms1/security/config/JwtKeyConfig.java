package com.upsjb.ms1.security.config;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import com.upsjb.ms1.security.jwt.JwtProperties;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

@Configuration
public class JwtKeyConfig {

    private static final String RSA_ALGORITHM = "RSA";

    private final JwtProperties jwtProperties;
    private final ResourceLoader resourceLoader;

    public JwtKeyConfig(
            JwtProperties jwtProperties,
            ResourceLoader resourceLoader
    ) {
        this.jwtProperties = jwtProperties;
        this.resourceLoader = resourceLoader;
    }

    @Bean
    public RSAPublicKey rsaPublicKey() {
        return readPublicKey(jwtProperties.getPublicKeyLocation());
    }

    @Bean
    public RSAPrivateKey rsaPrivateKey() {
        return readPrivateKey(jwtProperties.getPrivateKeyLocation());
    }

    @Bean
    public RSAKey rsaKey(
            RSAPublicKey rsaPublicKey,
            RSAPrivateKey rsaPrivateKey
    ) {
        return new RSAKey.Builder(rsaPublicKey)
                .privateKey(rsaPrivateKey)
                .keyID(jwtProperties.getKeyId())
                .build();
    }

    @Bean
    public JWKSource<SecurityContext> jwkSource(RSAKey rsaKey) {
        return new ImmutableJWKSet<>(new JWKSet(rsaKey));
    }

    @Bean
    public JwtEncoder jwtEncoder(JWKSource<SecurityContext> jwkSource) {
        return new NimbusJwtEncoder(jwkSource);
    }

    @Bean
    public JwtDecoder jwtDecoder(RSAPublicKey rsaPublicKey) {
        return NimbusJwtDecoder
                .withPublicKey(rsaPublicKey)
                .signatureAlgorithm(SignatureAlgorithm.RS256)
                .build();
    }

    private RSAPublicKey readPublicKey(String location) {
        try {
            String pem = readPem(location);
            String normalized = cleanPem(
                    pem,
                    "-----BEGIN PUBLIC KEY-----",
                    "-----END PUBLIC KEY-----"
            );

            byte[] decoded = Base64.getDecoder().decode(normalized);
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(decoded);

            return (RSAPublicKey) KeyFactory
                    .getInstance(RSA_ALGORITHM)
                    .generatePublic(keySpec);
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "No se pudo cargar la llave pública JWT. Usa una llave pública RSA X.509 en formato PEM.",
                    exception
            );
        }
    }

    private RSAPrivateKey readPrivateKey(String location) {
        try {
            String pem = readPem(location);
            String normalized = cleanPem(
                    pem,
                    "-----BEGIN PRIVATE KEY-----",
                    "-----END PRIVATE KEY-----"
            );

            byte[] decoded = Base64.getDecoder().decode(normalized);
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(decoded);

            return (RSAPrivateKey) KeyFactory
                    .getInstance(RSA_ALGORITHM)
                    .generatePrivate(keySpec);
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "No se pudo cargar la llave privada JWT. Usa una llave privada RSA PKCS#8 en formato PEM.",
                    exception
            );
        }
    }

    private String readPem(String location) throws IOException {
        Resource resource = resourceLoader.getResource(location);

        if (!resource.exists()) {
            throw new IllegalStateException("No existe el recurso de llave JWT: " + location);
        }

        try (InputStream inputStream = resource.getInputStream()) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private String cleanPem(String pem, String begin, String end) {
        return pem
                .replace(begin, "")
                .replace(end, "")
                .replaceAll("\\s", "");
    }
}