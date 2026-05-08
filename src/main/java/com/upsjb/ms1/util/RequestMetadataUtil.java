package com.upsjb.ms1.util;

import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;

public final class RequestMetadataUtil {

    public static final String REQUEST_ID_HEADER = "X-Request-Id";
    public static final String CORRELATION_ID_HEADER = "X-Correlation-Id";

    private RequestMetadataUtil() {
    }

    public static String resolveRequestId(HttpServletRequest request) {
        String requestId = request == null ? null : request.getHeader(REQUEST_ID_HEADER);
        return requestId == null || requestId.isBlank() ? UUID.randomUUID().toString() : requestId.trim();
    }

    public static String resolveCorrelationId(HttpServletRequest request, String requestId) {
        String correlationId = request == null ? null : request.getHeader(CORRELATION_ID_HEADER);
        return correlationId == null || correlationId.isBlank() ? requestId : correlationId.trim();
    }

    public static String resolvePath(HttpServletRequest request) {
        if (request == null) {
            return null;
        }

        String query = request.getQueryString();
        return query == null || query.isBlank()
                ? request.getRequestURI()
                : request.getRequestURI() + "?" + query;
    }
}