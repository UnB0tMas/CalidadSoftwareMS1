package com.upsjb.ms1.util;

import jakarta.servlet.http.HttpServletRequest;

public final class UserAgentUtil {

    private static final int MAX_LENGTH = 512;

    private UserAgentUtil() {
    }

    public static String extractUserAgent(HttpServletRequest request) {
        if (request == null) {
            return "UNKNOWN";
        }

        return sanitize(request.getHeader("User-Agent"));
    }

    public static String sanitize(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) {
            return "UNKNOWN";
        }

        String normalized = userAgent.trim().replaceAll("\\s+", " ");
        return normalized.length() > MAX_LENGTH ? normalized.substring(0, MAX_LENGTH) : normalized;
    }
}