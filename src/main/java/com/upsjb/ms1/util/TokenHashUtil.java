package com.upsjb.ms1.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public final class TokenHashUtil {

    private static final String HASH_ALGORITHM = "SHA-256";

    private TokenHashUtil() {
    }

    public static String hash(String rawToken) {
        String normalized = StringNormalizer.trimToNull(rawToken);

        if (normalized == null) {
            throw new IllegalArgumentException("El token/código es obligatorio.");
        }

        try {
            MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);
            byte[] hash = digest.digest(normalized.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("No se pudo inicializar el algoritmo de hash.", exception);
        }
    }

    public static boolean matches(String rawToken, String expectedHash) {
        String expected = StringNormalizer.trimToNull(expectedHash);

        if (expected == null) {
            return false;
        }

        String actual = hash(rawToken);
        return MessageDigest.isEqual(
                actual.getBytes(StandardCharsets.UTF_8),
                expected.getBytes(StandardCharsets.UTF_8)
        );
    }
}