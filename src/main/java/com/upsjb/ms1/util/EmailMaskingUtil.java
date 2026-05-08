package com.upsjb.ms1.util;

public final class EmailMaskingUtil {

    private EmailMaskingUtil() {
    }

    public static String mask(String email) {
        String normalized = StringNormalizer.lower(email);

        if (normalized == null || !normalized.contains("@")) {
            return "correo no disponible";
        }

        String[] parts = normalized.split("@", 2);
        String local = parts[0];
        String domain = parts[1];

        String maskedLocal = switch (local.length()) {
            case 0 -> "***";
            case 1 -> local.charAt(0) + "***";
            case 2 -> local.charAt(0) + "***" + local.charAt(1);
            default -> local.charAt(0) + "***" + local.charAt(local.length() - 1);
        };

        return maskedLocal + "@" + domain;
    }
}