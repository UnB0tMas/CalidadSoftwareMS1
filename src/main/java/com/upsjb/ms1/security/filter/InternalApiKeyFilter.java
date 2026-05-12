// ruta: src/main/java/com/upsjb/ms1/security/filter/InternalApiKeyFilter.java
package com.upsjb.ms1.security.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.upsjb.ms1.dto.shared.ErrorResponseDto;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class InternalApiKeyFilter extends OncePerRequestFilter {

    private static final String INTERNAL_PATH_PREFIX = "/api/internal/";
    private static final String DEFAULT_HEADER = "X-Internal-Service-Key";
    private static final String REQUEST_ID_HEADER = "X-Request-Id";

    private final ObjectMapper objectMapper;
    private final String configuredHeader;
    private final String configuredKey;

    public InternalApiKeyFilter(
            ObjectMapper objectMapper,
            @Value("${internal.security.header-name:X-Internal-Service-Key}") String configuredHeader,
            @Value("${internal.security.service-key:}") String configuredKey
    ) {
        this.objectMapper = objectMapper;
        this.configuredHeader = normalize(configuredHeader, DEFAULT_HEADER);
        this.configuredKey = normalize(configuredKey, "");
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path == null || !path.startsWith(INTERNAL_PATH_PREFIX);
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        if (configuredKey.isBlank()) {
            writeError(
                    request,
                    response,
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "INTERNAL_SECURITY_NOT_CONFIGURED",
                    "La seguridad interna de MS1 no está configurada."
            );
            return;
        }

        String receivedKey = normalize(request.getHeader(configuredHeader), "");

        if (!constantTimeEquals(configuredKey, receivedKey)) {
            writeError(
                    request,
                    response,
                    HttpStatus.FORBIDDEN,
                    "INTERNAL_ACCESS_DENIED",
                    "No tiene permiso para consumir endpoints internos de MS1."
            );
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void writeError(
            HttpServletRequest request,
            HttpServletResponse response,
            HttpStatus status,
            String code,
            String message
    ) throws IOException {
        response.setStatus(status.value());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        ErrorResponseDto body = new ErrorResponseDto(
                false,
                message,
                new ErrorResponseDto.ErrorDetail(
                        status.name(),
                        code,
                        List.of()
                ),
                requestId(request),
                Instant.now()
        );

        objectMapper.writeValue(response.getWriter(), body);
    }

    private boolean constantTimeEquals(String expected, String actual) {
        if (expected == null || actual == null) {
            return false;
        }

        byte[] expectedBytes = expected.getBytes(StandardCharsets.UTF_8);
        byte[] actualBytes = actual.getBytes(StandardCharsets.UTF_8);

        int maxLength = Math.max(expectedBytes.length, actualBytes.length);
        int result = expectedBytes.length ^ actualBytes.length;

        for (int i = 0; i < maxLength; i++) {
            byte expectedByte = i < expectedBytes.length ? expectedBytes[i] : 0;
            byte actualByte = i < actualBytes.length ? actualBytes[i] : 0;
            result |= expectedByte ^ actualByte;
        }

        return result == 0;
    }

    private String requestId(HttpServletRequest request) {
        if (request == null) {
            return null;
        }

        return normalize(request.getHeader(REQUEST_ID_HEADER), null);
    }

    private String normalize(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }

        return value.trim();
    }
}