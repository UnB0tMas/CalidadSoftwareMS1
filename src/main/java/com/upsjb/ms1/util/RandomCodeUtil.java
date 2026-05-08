package com.upsjb.ms1.util;

import java.security.SecureRandom;

public final class RandomCodeUtil {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final String DIGITS = "0123456789";

    private RandomCodeUtil() {
    }

    public static String numericCode(int length) {
        if (length < 4 || length > 12) {
            throw new IllegalArgumentException("La longitud del código debe estar entre 4 y 12.");
        }

        StringBuilder code = new StringBuilder(length);

        for (int index = 0; index < length; index++) {
            code.append(DIGITS.charAt(SECURE_RANDOM.nextInt(DIGITS.length())));
        }

        return code.toString();
    }

    public static String defaultCode() {
        return numericCode(6);
    }
}