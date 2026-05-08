package com.upsjb.ms1.util;

import java.text.Normalizer;
import java.util.Locale;

public final class StringNormalizer {

    private StringNormalizer() {
    }

    public static String trimToNull(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public static String normalizeSpaces(String value) {
        String trimmed = trimToNull(value);
        return trimmed == null ? null : trimmed.replaceAll("\\s+", " ");
    }

    public static String lower(String value) {
        String normalized = normalizeSpaces(value);
        return normalized == null ? null : normalized.toLowerCase(Locale.ROOT);
    }

    public static String upper(String value) {
        String normalized = normalizeSpaces(value);
        return normalized == null ? null : normalized.toUpperCase(Locale.ROOT);
    }

    public static String removeAccents(String value) {
        String normalized = normalizeSpaces(value);
        if (normalized == null) {
            return null;
        }

        return Normalizer.normalize(normalized, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
    }

    public static String normalizeSearch(String value) {
        String withoutAccents = removeAccents(value);
        return withoutAccents == null ? null : withoutAccents.toLowerCase(Locale.ROOT);
    }
}