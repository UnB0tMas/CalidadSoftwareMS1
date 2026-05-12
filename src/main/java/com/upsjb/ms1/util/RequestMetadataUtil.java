package com.upsjb.ms1.util;

import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;

public final class RequestMetadataUtil {

    public static final String REQUEST_ID_HEADER = "X-Request-Id";
    public static final String CORRELATION_ID_HEADER = "X-Correlation-Id";

    public static final String FORWARDED_FOR_HEADER = "X-Forwarded-For";
    public static final String FORWARDED_HOST_HEADER = "X-Forwarded-Host";
    public static final String FORWARDED_PROTO_HEADER = "X-Forwarded-Proto";
    public static final String FORWARDED_PORT_HEADER = "X-Forwarded-Port";
    public static final String REAL_IP_HEADER = "X-Real-IP";
    public static final String GATEWAY_SOURCE_HEADER = "X-Gateway-Source";
    public static final String USER_AGENT_HEADER = "User-Agent";

    private static final int MAX_TRACE_HEADER_LENGTH = 128;
    private static final int MAX_PATH_LENGTH = 600;
    private static final String UNKNOWN = "UNKNOWN";

    private RequestMetadataUtil() {
    }

    public static String resolveRequestId(HttpServletRequest request) {
        String requestId = safeHeader(request, REQUEST_ID_HEADER, MAX_TRACE_HEADER_LENGTH);

        if (requestId != null) {
            return requestId;
        }

        return UUID.randomUUID().toString();
    }

    public static String resolveCorrelationId(
            HttpServletRequest request,
            String requestId
    ) {
        String correlationId = safeHeader(request, CORRELATION_ID_HEADER, MAX_TRACE_HEADER_LENGTH);

        if (correlationId != null) {
            return correlationId;
        }

        return requestId == null || requestId.isBlank()
                ? UUID.randomUUID().toString()
                : requestId;
    }

    public static String resolvePath(HttpServletRequest request) {
        if (request == null) {
            return UNKNOWN;
        }

        String uri = sanitize(request.getRequestURI(), MAX_PATH_LENGTH);

        if (uri == null) {
            uri = UNKNOWN;
        }

        String query = sanitize(request.getQueryString(), MAX_PATH_LENGTH);

        if (query == null) {
            return uri;
        }

        String fullPath = uri + "?" + query;

        if (fullPath.length() > MAX_PATH_LENGTH) {
            return fullPath.substring(0, MAX_PATH_LENGTH);
        }

        return fullPath;
    }

    public static String resolveGatewaySource(HttpServletRequest request) {
        String source = safeHeader(request, GATEWAY_SOURCE_HEADER, 120);
        return source == null ? "DIRECT_OR_UNKNOWN" : source;
    }

    public static boolean cameThroughGateway(HttpServletRequest request) {
        if (request == null) {
            return false;
        }

        return safeHeader(request, FORWARDED_HOST_HEADER, 180) != null
                || safeHeader(request, FORWARDED_PROTO_HEADER, 40) != null
                || safeHeader(request, FORWARDED_PORT_HEADER, 20) != null
                || safeHeader(request, GATEWAY_SOURCE_HEADER, 120) != null;
    }

    public static String safeHeader(
            HttpServletRequest request,
            String headerName,
            int maxLength
    ) {
        if (request == null || headerName == null || headerName.isBlank()) {
            return null;
        }

        String value = request.getHeader(headerName);
        return sanitize(value, maxLength);
    }

    public static String sanitize(
            String value,
            int maxLength
    ) {
        if (value == null || value.isBlank()) {
            return null;
        }

        String sanitized = value
                .trim()
                .replace("\r", "")
                .replace("\n", "")
                .replace("\t", " ");

        if (sanitized.isBlank()) {
            return null;
        }

        if (maxLength > 0 && sanitized.length() > maxLength) {
            return sanitized.substring(0, maxLength);
        }

        return sanitized;
    }
}