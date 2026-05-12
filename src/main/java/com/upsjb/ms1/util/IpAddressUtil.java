package com.upsjb.ms1.util;

import jakarta.servlet.http.HttpServletRequest;

public final class IpAddressUtil {

    private static final String UNKNOWN = "unknown";
    private static final String DEFAULT_IP = "0.0.0.0";
    private static final int MAX_IP_LENGTH = 45;

    private IpAddressUtil() {
    }

    public static String extractClientIp(HttpServletRequest request) {
        if (request == null) {
            return DEFAULT_IP;
        }

        String forwardedFor = firstValidHeader(request, RequestMetadataUtil.FORWARDED_FOR_HEADER);
        if (forwardedFor != null) {
            String firstIp = firstForwardedIp(forwardedFor);
            if (firstIp != null) {
                return firstIp;
            }
        }

        String realIp = firstValidHeader(request, RequestMetadataUtil.REAL_IP_HEADER);
        if (realIp != null) {
            return realIp;
        }

        String gatewayIp = firstValidHeader(request, "X-Gateway-Client-IP");
        if (gatewayIp != null) {
            return gatewayIp;
        }

        String remoteAddress = request.getRemoteAddr();
        String sanitizedRemote = sanitizeIp(remoteAddress);

        return sanitizedRemote == null ? DEFAULT_IP : sanitizedRemote;
    }

    private static String firstForwardedIp(String forwardedFor) {
        if (forwardedFor == null || forwardedFor.isBlank()) {
            return null;
        }

        String[] parts = forwardedFor.split(",");

        for (String part : parts) {
            String ip = sanitizeIp(part);

            if (ip != null) {
                return ip;
            }
        }

        return null;
    }

    private static String firstValidHeader(
            HttpServletRequest request,
            String name
    ) {
        String value = request.getHeader(name);
        return sanitizeIp(value);
    }

    private static String sanitizeIp(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        String sanitized = value
                .trim()
                .replace("\r", "")
                .replace("\n", "")
                .replace("\t", "");

        if (sanitized.isBlank()
                || UNKNOWN.equalsIgnoreCase(sanitized)
                || "null".equalsIgnoreCase(sanitized)) {
            return null;
        }

        if (sanitized.length() > MAX_IP_LENGTH) {
            return sanitized.substring(0, MAX_IP_LENGTH);
        }

        return sanitized;
    }
}