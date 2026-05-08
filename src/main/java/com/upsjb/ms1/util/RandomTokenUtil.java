package com.upsjb.ms1.util;

import java.security.SecureRandom;
import java.util.Base64;

public final class RandomTokenUtil {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private RandomTokenUtil() {
    }

    public static String secureToken() {
        return secureToken(64);
    }

    public static String secureToken(int bytesLength) {
        if (bytesLength < 32) {
            throw new IllegalArgumentException("El token seguro debe tener al menos 32 bytes.");
        }

        byte[] bytes = new byte[bytesLength];
        SECURE_RANDOM.nextBytes(bytes);

        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(bytes);
    }
}