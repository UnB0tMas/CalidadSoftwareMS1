package com.upsjb.ms1.util;

import jakarta.servlet.http.HttpServletRequest;

public final class IpAddressUtil {

    private static final String UNKNOWN = "unknown";

    private IpAddressUtil() {
    }

    public static String extractClientIp(HttpServletRequest request) {
        if (request == null) {
            return "0.0.0.0";
        }

        String forwardedFor = firstValidHeader(request, "X-Forwarded-For");
        if (forwardedFor != null) {
            return forwardedFor.split(",")[0].trim();
        }

        String realIp = firstValidHeader(request, "X-Real-IP");
        if (realIp != null) {
            return realIp;
        }

        String gatewayIp = firstValidHeader(request, "X-Gateway-Client-IP");
        if (gatewayIp != null) {
            return gatewayIp;
        }

        String remoteAddress = request.getRemoteAddr();
        return remoteAddress == null || remoteAddress.isBlank() ? "0.0.0.0" : remoteAddress;
    }

    private static String firstValidHeader(HttpServletRequest request, String name) {
        String value = request.getHeader(name);
        return value == null || value.isBlank() || UNKNOWN.equalsIgnoreCase(value.trim()) ? null : value.trim();
    }
}