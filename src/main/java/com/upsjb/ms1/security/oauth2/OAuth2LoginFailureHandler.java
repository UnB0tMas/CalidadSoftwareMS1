package com.upsjb.ms1.security.oauth2;

import com.upsjb.ms1.config.AppPropertiesConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

@Component
public class OAuth2LoginFailureHandler implements AuthenticationFailureHandler {

    private static final Logger log = LoggerFactory.getLogger(OAuth2LoginFailureHandler.class);

    private static final String DEFAULT_PROVIDER = "google";
    private static final String CALLBACK_PREFIX = "/login/oauth2/code/";
    private static final String AUTHORIZATION_PREFIX = "/oauth2/authorization/";

    private static final String REQUEST_ID_HEADER = "X-Request-Id";
    private static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
    private static final String FORWARDED_PROTO_HEADER = "X-Forwarded-Proto";
    private static final String FORWARDED_HOST_HEADER = "X-Forwarded-Host";
    private static final String FORWARDED_PORT_HEADER = "X-Forwarded-Port";

    private final AppPropertiesConfig appProperties;

    public OAuth2LoginFailureHandler(AppPropertiesConfig appProperties) {
        this.appProperties = appProperties;
    }

    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception
    ) throws IOException, ServletException {
        String errorId = generateErrorId();
        String requestId = resolveHeader(request, REQUEST_ID_HEADER, "UNKNOWN");
        String correlationId = resolveHeader(request, CORRELATION_ID_HEADER, requestId);
        String requestUri = safe(request == null ? null : request.getRequestURI(), 500, "UNKNOWN");
        String queryString = safe(request == null ? null : request.getQueryString(), 800, "WITHOUT_QUERY");
        String provider = resolveProvider(requestUri);
        String oauth2ErrorCode = resolveOAuth2ErrorCode(exception);
        String rootCause = rootCauseClass(exception);
        String rootMessage = rootCauseMessage(exception);

        if (isDirectCallbackWithoutGoogleResponse(request)) {
            String authorizationUrl = resolveGatewayBaseUrl(request) + AUTHORIZATION_PREFIX + provider;

            log.warn(
                    "oauth2_callback_direct_access errorId={} requestId={} correlationId={} provider={} requestUri={} queryString=\"{}\" oauth2ErrorCode={} exceptionType={} rootCause={} rootMessage=\"{}\" suggestedAction=\"No acceder directamente al callback. Iniciar OAuth2 desde {}\"",
                    errorId,
                    requestId,
                    correlationId,
                    provider,
                    requestUri,
                    queryString,
                    oauth2ErrorCode,
                    exception == null ? "UNKNOWN" : exception.getClass().getName(),
                    rootCause,
                    rootMessage,
                    authorizationUrl
            );

            response.sendRedirect(authorizationUrl);
            return;
        }

        log.warn(
                "oauth2_login_failure errorId={} requestId={} correlationId={} provider={} requestUri={} queryString=\"{}\" oauth2ErrorCode={} exceptionType={} rootCause={} rootMessage=\"{}\" suggestedAction=\"Validar redirect URI en Google Cloud, cookies/state OAuth2, X-Forwarded headers del Gateway y configuración del client registration.\"",
                errorId,
                requestId,
                correlationId,
                provider,
                requestUri,
                queryString,
                oauth2ErrorCode,
                exception == null ? "UNKNOWN" : exception.getClass().getName(),
                rootCause,
                rootMessage,
                exception
        );

        if (expectsJson(request)) {
            writeJsonError(
                    response,
                    errorId,
                    requestId,
                    correlationId,
                    oauth2ErrorCode,
                    "No se pudo completar el login OAuth2."
            );
            return;
        }

        if (isFrontendFlow(request)) {
            redirectToFrontendCallback(
                    response,
                    errorId,
                    oauth2ErrorCode,
                    "No se pudo autenticar con OAuth2."
            );
            return;
        }

        writeHtmlError(
                response,
                errorId,
                requestId,
                correlationId,
                provider,
                oauth2ErrorCode,
                "No se pudo completar el login OAuth2.",
                resolveGatewayBaseUrl(request) + AUTHORIZATION_PREFIX + provider
        );
    }

    private boolean isDirectCallbackWithoutGoogleResponse(HttpServletRequest request) {
        if (request == null) {
            return false;
        }

        String requestUri = request.getRequestURI();

        if (requestUri == null || !requestUri.startsWith(CALLBACK_PREFIX)) {
            return false;
        }

        String code = request.getParameter("code");
        String state = request.getParameter("state");
        String error = request.getParameter("error");

        return isBlank(code) && isBlank(state) && isBlank(error);
    }

    private String resolveProvider(String requestUri) {
        if (requestUri == null || requestUri.isBlank()) {
            return DEFAULT_PROVIDER;
        }

        int index = requestUri.indexOf(CALLBACK_PREFIX);

        if (index < 0) {
            return DEFAULT_PROVIDER;
        }

        String provider = requestUri.substring(index + CALLBACK_PREFIX.length());

        if (provider.contains("/")) {
            provider = provider.substring(0, provider.indexOf('/'));
        }

        provider = safe(provider, 60, DEFAULT_PROVIDER);

        if (provider.equals("UNKNOWN") || provider.isBlank()) {
            return DEFAULT_PROVIDER;
        }

        return provider;
    }

    private String resolveGatewayBaseUrl(HttpServletRequest request) {
        String configuredGatewayUrl = safe(appProperties.getGatewayUrl(), 200, null);

        if (configuredGatewayUrl != null && !"UNKNOWN".equals(configuredGatewayUrl)) {
            return removeTrailingSlash(configuredGatewayUrl);
        }

        if (request == null) {
            return "http://localhost:8080";
        }

        String proto = resolveHeader(request, FORWARDED_PROTO_HEADER, null);
        String host = resolveHeader(request, FORWARDED_HOST_HEADER, null);
        String port = resolveHeader(request, FORWARDED_PORT_HEADER, null);

        if (!isBlank(proto) && !isBlank(host)) {
            String baseUrl = proto + "://" + host;

            if (!isBlank(port) && !host.contains(":")) {
                baseUrl = baseUrl + ":" + port;
            }

            return removeTrailingSlash(baseUrl);
        }

        String scheme = safe(request.getScheme(), 20, "http");
        String serverName = safe(request.getServerName(), 200, "localhost");
        int serverPort = request.getServerPort();

        if (serverPort <= 0 || serverPort == 80 || serverPort == 443) {
            return scheme + "://" + serverName;
        }

        return scheme + "://" + serverName + ":" + serverPort;
    }

    private void redirectToFrontendCallback(
            HttpServletResponse response,
            String errorId,
            String error,
            String message
    ) throws IOException {
        String frontendUrl = removeTrailingSlash(appProperties.getFrontendUrl());

        String redirectUrl = frontendUrl
                + "/auth/oauth2/callback"
                + "?status=error"
                + "&errorId=" + encode(errorId)
                + "&error=" + encode(error)
                + "&message=" + encode(message);

        response.sendRedirect(redirectUrl);
    }

    private void writeJsonError(
            HttpServletResponse response,
            String errorId,
            String requestId,
            String correlationId,
            String error,
            String message
    ) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("application/json;charset=UTF-8");
        response.setHeader(REQUEST_ID_HEADER, requestId);
        response.setHeader(CORRELATION_ID_HEADER, correlationId);
        response.setHeader("X-Error-Id", errorId);
        response.setHeader("Cache-Control", "no-store");
        response.setHeader("Pragma", "no-cache");

        String json = """
                {
                  "success": false,
                  "status": 401,
                  "code": "OAUTH2_LOGIN_FAILED",
                  "message": "%s",
                  "oauth2Error": "%s",
                  "requestId": "%s",
                  "correlationId": "%s",
                  "errorId": "%s",
                  "timestamp": "%s"
                }
                """.formatted(
                escapeJson(message),
                escapeJson(error),
                escapeJson(requestId),
                escapeJson(correlationId),
                escapeJson(errorId),
                Instant.now()
        );

        response.getWriter().write(json);
    }

    private void writeHtmlError(
            HttpServletResponse response,
            String errorId,
            String requestId,
            String correlationId,
            String provider,
            String error,
            String message,
            String retryUrl
    ) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("text/html;charset=UTF-8");
        response.setHeader(REQUEST_ID_HEADER, requestId);
        response.setHeader(CORRELATION_ID_HEADER, correlationId);
        response.setHeader("X-Error-Id", errorId);
        response.setHeader("Cache-Control", "no-store");
        response.setHeader("Pragma", "no-cache");

        String html = """
                <!doctype html>
                <html lang="es">
                <head>
                    <meta charset="UTF-8">
                    <title>OAuth2 no completado</title>
                    <style>
                        body {
                            font-family: Arial, sans-serif;
                            background: #f6f7fb;
                            color: #1f2937;
                            padding: 40px;
                        }
                        .card {
                            max-width: 760px;
                            margin: 0 auto;
                            background: #ffffff;
                            border: 1px solid #e5e7eb;
                            border-radius: 14px;
                            padding: 28px;
                            box-shadow: 0 10px 30px rgba(15, 23, 42, 0.08);
                        }
                        h1 {
                            margin-top: 0;
                            font-size: 24px;
                        }
                        code {
                            background: #f3f4f6;
                            padding: 2px 6px;
                            border-radius: 6px;
                        }
                        .muted {
                            color: #6b7280;
                            font-size: 14px;
                        }
                        .button {
                            display: inline-block;
                            margin-top: 18px;
                            padding: 10px 16px;
                            border-radius: 8px;
                            background: #2563eb;
                            color: white;
                            text-decoration: none;
                        }
                    </style>
                </head>
                <body>
                    <div class="card">
                        <h1>OAuth2 no se completó correctamente</h1>
                        <p>%s</p>
                        <p>Proveedor: <code>%s</code></p>
                        <p>Error OAuth2: <code>%s</code></p>
                        <p class="muted">errorId: <code>%s</code></p>
                        <p class="muted">requestId: <code>%s</code></p>
                        <p class="muted">correlationId: <code>%s</code></p>
                        <p>Para iniciar sesión, no abras directamente el callback. Usa la URL de autorización OAuth2.</p>
                        <a class="button" href="%s">Iniciar login con Google</a>
                    </div>
                </body>
                </html>
                """.formatted(
                escapeHtml(message),
                escapeHtml(provider),
                escapeHtml(error),
                escapeHtml(errorId),
                escapeHtml(requestId),
                escapeHtml(correlationId),
                escapeHtml(retryUrl)
        );

        response.getWriter().write(html);
    }

    private boolean isFrontendFlow(HttpServletRequest request) {
        if (request == null) {
            return false;
        }

        String origin = resolveHeader(request, "Origin", "");
        String referer = resolveHeader(request, "Referer", "");
        String frontendUrl = removeTrailingSlash(appProperties.getFrontendUrl());

        return (!origin.isBlank() && origin.startsWith(frontendUrl))
                || (!referer.isBlank() && referer.startsWith(frontendUrl));
    }

    private boolean expectsJson(HttpServletRequest request) {
        if (request == null) {
            return false;
        }

        String accept = resolveHeader(request, "Accept", "");

        return accept.contains("application/json");
    }

    private String resolveOAuth2ErrorCode(AuthenticationException exception) {
        if (exception instanceof OAuth2AuthenticationException oauth2Exception
                && oauth2Exception.getError() != null
                && oauth2Exception.getError().getErrorCode() != null
                && !oauth2Exception.getError().getErrorCode().isBlank()) {
            return safe(oauth2Exception.getError().getErrorCode(), 120, "oauth2_error");
        }

        if (exception == null) {
            return "oauth2_error";
        }

        return safe(exception.getClass().getSimpleName(), 120, "oauth2_error");
    }

    private String resolveHeader(
            HttpServletRequest request,
            String headerName,
            String fallback
    ) {
        if (request == null || headerName == null || headerName.isBlank()) {
            return fallback;
        }

        return safe(request.getHeader(headerName), 300, fallback);
    }

    private String rootCauseClass(Throwable throwable) {
        Throwable root = rootCause(throwable);
        return root == null ? "UNKNOWN" : root.getClass().getName();
    }

    private String rootCauseMessage(Throwable throwable) {
        Throwable root = rootCause(throwable);

        if (root == null || root.getMessage() == null || root.getMessage().isBlank()) {
            return "WITHOUT_MESSAGE";
        }

        return safe(root.getMessage(), 500, "WITHOUT_MESSAGE");
    }

    private Throwable rootCause(Throwable throwable) {
        if (throwable == null) {
            return null;
        }

        Throwable current = throwable;
        Throwable previous = null;

        while (current != null && current != previous) {
            previous = current;

            if (current.getCause() == null) {
                return current;
            }

            current = current.getCause();
        }

        return previous;
    }

    private String safe(
            String value,
            int maxLength,
            String fallback
    ) {
        if (value == null || value.isBlank()) {
            return fallback;
        }

        String sanitized = value
                .trim()
                .replace("\r", "")
                .replace("\n", "")
                .replace("\t", " ");

        if (sanitized.isBlank()) {
            return fallback;
        }

        if (maxLength > 0 && sanitized.length() > maxLength) {
            return sanitized.substring(0, maxLength);
        }

        return sanitized;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String encode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }

    private String removeTrailingSlash(String value) {
        String normalized = safe(value, 300, "http://localhost:8080");

        while (normalized.endsWith("/") && normalized.length() > 1) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }

        return normalized;
    }

    private String generateErrorId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private String escapeHtml(String value) {
        if (value == null) {
            return "";
        }

        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private String escapeJson(String value) {
        if (value == null) {
            return "";
        }

        StringBuilder builder = new StringBuilder(value.length() + 16);

        for (int index = 0; index < value.length(); index++) {
            char current = value.charAt(index);

            switch (current) {
                case '"' -> builder.append("\\\"");
                case '\\' -> builder.append("\\\\");
                case '\b' -> builder.append("\\b");
                case '\f' -> builder.append("\\f");
                case '\n' -> builder.append("\\n");
                case '\r' -> builder.append("\\r");
                case '\t' -> builder.append("\\t");
                default -> {
                    if (current < 0x20) {
                        builder.append(String.format("\\u%04x", (int) current));
                    } else {
                        builder.append(current);
                    }
                }
            }
        }

        return builder.toString();
    }
}