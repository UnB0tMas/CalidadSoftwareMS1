package com.upsjb.ms1.security.oauth2;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.upsjb.ms1.config.AppPropertiesConfig;
import com.upsjb.ms1.dto.auth.response.AuthTokenResponseDto;
import com.upsjb.ms1.service.contract.AuthService;
import com.upsjb.ms1.util.IpAddressUtil;
import com.upsjb.ms1.util.UserAgentUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

@Component
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private static final Logger log = LoggerFactory.getLogger(OAuth2LoginSuccessHandler.class);

    private static final int TOKEN_VISIBLE_PREFIX = 18;
    private static final int TOKEN_VISIBLE_SUFFIX = 10;

    private static final String REQUEST_ID_HEADER = "X-Request-Id";
    private static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
    private static final String ERROR_ID_HEADER = "X-Error-Id";
    private static final String FORWARDED_HOST_HEADER = "X-Forwarded-Host";
    private static final String FORWARDED_PROTO_HEADER = "X-Forwarded-Proto";
    private static final String FORWARDED_PORT_HEADER = "X-Forwarded-Port";

    private final AppPropertiesConfig appProperties;
    private final AuthService authService;
    private final ObjectMapper objectMapper;
    private final OAuth2UserInfoFactory userInfoFactory;

    public OAuth2LoginSuccessHandler(
            AppPropertiesConfig appProperties,
            AuthService authService,
            ObjectMapper objectMapper,
            OAuth2UserInfoFactory userInfoFactory
    ) {
        this.appProperties = appProperties;
        this.authService = authService;
        this.objectMapper = objectMapper;
        this.userInfoFactory = userInfoFactory;
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException, ServletException {
        String requestId = resolveRequestId(request);
        String correlationId = resolveCorrelationId(request, requestId);
        String traceId = UUID.randomUUID().toString().replace("-", "");

        OAuth2UserInfo userInfo = resolveUserInfo(authentication);
        String registrationId = resolveRegistrationId(authentication);
        String ipAddress = IpAddressUtil.extractClientIp(request);
        String userAgent = UserAgentUtil.extractUserAgent(request);

        log.info(
                "oauth2_login_success_start traceId={} requestId={} correlationId={} provider={} email={} providerUserId={} ipAddress={} callbackUri={} forwardedHost={} forwardedProto={} forwardedPort={}",
                traceId,
                requestId,
                correlationId,
                userInfo.proveedor(),
                maskEmail(userInfo.email()),
                safe(userInfo.providerUserId(), 120, "UNKNOWN"),
                ipAddress,
                safe(request == null ? null : request.getRequestURI(), 300, "UNKNOWN"),
                resolveHeader(request, FORWARDED_HOST_HEADER, "UNKNOWN"),
                resolveHeader(request, FORWARDED_PROTO_HEADER, "UNKNOWN"),
                resolveHeader(request, FORWARDED_PORT_HEADER, "UNKNOWN")
        );

        AuthTokenResponseDto tokenResponse = authService.loginOAuth2(
                userInfo,
                null,
                ipAddress,
                userAgent
        );

        log.info(
                "oauth2_login_success_completed traceId={} requestId={} correlationId={} provider={} userId={} username={} email={} rol={} sessionId={} tokenType={} accessTokenExpiresAt={} refreshTokenExpiresAt={}",
                traceId,
                requestId,
                correlationId,
                registrationId,
                tokenResponse.user() == null ? null : tokenResponse.user().idUsuario(),
                tokenResponse.user() == null ? "UNKNOWN" : safe(tokenResponse.user().username(), 120, "UNKNOWN"),
                tokenResponse.user() == null ? "UNKNOWN" : maskEmail(tokenResponse.user().email()),
                tokenResponse.user() == null ? "UNKNOWN" : safe(tokenResponse.user().codigoRol(), 80, "UNKNOWN"),
                tokenResponse.session() == null ? null : tokenResponse.session().idSesion(),
                safe(tokenResponse.tokenType(), 30, "UNKNOWN"),
                tokenResponse.accessTokenExpiresAt(),
                tokenResponse.refreshTokenExpiresAt()
        );

        Map<String, Object> payload = buildFrontendPayload(tokenResponse);
        Map<String, Object> safePayload = buildSafeDiagnosticPayload(tokenResponse);

        writePostMessageResponse(
                response,
                payload,
                safePayload,
                requestId,
                correlationId,
                traceId
        );
    }

    private OAuth2UserInfo resolveUserInfo(Authentication authentication) {
        Object principal = authentication == null ? null : authentication.getPrincipal();

        if (!(principal instanceof OAuth2User oAuth2User)) {
            throw new BadCredentialsException("Principal OAuth2 inválido.");
        }

        Object normalized = oAuth2User.getAttribute(CustomOAuth2UserService.NORMALIZED_USER_INFO_ATTRIBUTE);

        if (normalized instanceof OAuth2UserInfo userInfo) {
            return userInfo;
        }

        String registrationId = resolveRegistrationId(authentication);

        try {
            return userInfoFactory.from(
                    registrationId,
                    oAuth2User.getAttributes()
            );
        } catch (RuntimeException exception) {
            throw new BadCredentialsException(
                    "No se pudo normalizar la información OAuth2 del proveedor.",
                    exception
            );
        }
    }

    private String resolveRegistrationId(Authentication authentication) {
        if (authentication instanceof OAuth2AuthenticationToken token
                && token.getAuthorizedClientRegistrationId() != null
                && !token.getAuthorizedClientRegistrationId().isBlank()) {
            return token.getAuthorizedClientRegistrationId();
        }

        throw new BadCredentialsException("No se encontró el registrationId OAuth2.");
    }

    private Map<String, Object> buildFrontendPayload(AuthTokenResponseDto tokenResponse) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("status", "success");
        payload.put("message", "Login OAuth2 completado correctamente.");
        payload.put("tokenType", tokenResponse.tokenType());
        payload.put("accessToken", tokenResponse.accessToken());
        payload.put("refreshToken", tokenResponse.refreshToken());
        payload.put("accessTokenExpiresAt", tokenResponse.accessTokenExpiresAt());
        payload.put("refreshTokenExpiresAt", tokenResponse.refreshTokenExpiresAt());
        payload.put("user", tokenResponse.user());
        payload.put("session", tokenResponse.session());
        return payload;
    }

    private Map<String, Object> buildSafeDiagnosticPayload(AuthTokenResponseDto tokenResponse) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("status", "success");
        payload.put("message", "Login OAuth2 completado correctamente. Los tokens no se muestran en esta vista directa.");
        payload.put("tokenType", tokenResponse.tokenType());
        payload.put("accessTokenPreview", maskToken(tokenResponse.accessToken()));
        payload.put("refreshTokenPreview", maskToken(tokenResponse.refreshToken()));
        payload.put("accessTokenExpiresAt", tokenResponse.accessTokenExpiresAt());
        payload.put("refreshTokenExpiresAt", tokenResponse.refreshTokenExpiresAt());
        payload.put("user", tokenResponse.user());
        payload.put("session", tokenResponse.session());
        return payload;
    }

    private String maskToken(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }

        if (token.length() <= TOKEN_VISIBLE_PREFIX + TOKEN_VISIBLE_SUFFIX) {
            return "***";
        }

        return token.substring(0, TOKEN_VISIBLE_PREFIX)
                + "..."
                + token.substring(token.length() - TOKEN_VISIBLE_SUFFIX);
    }

    private void writePostMessageResponse(
            HttpServletResponse response,
            Map<String, Object> payload,
            Map<String, Object> safePayload,
            String requestId,
            String correlationId,
            String traceId
    ) throws IOException {
        String jsonPayload = objectMapper.writeValueAsString(payload);
        String safeJsonPayload = objectMapper.writeValueAsString(safePayload);
        String frontendOrigin = appProperties.getFrontendUrl();

        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType(MediaType.TEXT_HTML_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store, no-cache, must-revalidate, max-age=0");
        response.setHeader(HttpHeaders.PRAGMA, "no-cache");
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("X-Frame-Options", "DENY");
        response.setHeader("Referrer-Policy", "no-referrer");
        response.setHeader("Robots", "noindex, nofollow");
        response.setHeader(REQUEST_ID_HEADER, requestId);
        response.setHeader(CORRELATION_ID_HEADER, correlationId);
        response.setHeader(ERROR_ID_HEADER, traceId);

        response.getWriter().write("""
                <!doctype html>
                <html lang="es">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <meta name="robots" content="noindex,nofollow">
                    <title>OAuth2 completado</title>
                    <style>
                        body {
                            font-family: Arial, sans-serif;
                            margin: 32px;
                            background: #f7f7f7;
                            color: #17202a;
                        }
                        .box {
                            background: white;
                            border: 1px solid #ddd;
                            border-radius: 8px;
                            padding: 20px;
                            max-width: 1100px;
                        }
                        .ok {
                            color: #1b5e20;
                            font-weight: 700;
                        }
                        pre {
                            white-space: pre-wrap;
                            word-break: break-word;
                            background: #111;
                            color: #f1f1f1;
                            padding: 16px;
                            border-radius: 6px;
                            overflow: auto;
                        }
                        .meta {
                            color: #5f6b7a;
                            font-size: 13px;
                        }
                    </style>
                </head>
                <body>
                    <div class="box">
                        <h1 class="ok">OAuth2 completado correctamente</h1>
                        <p>
                            Si esta ventana fue abierta como popup desde Angular,
                            los tokens fueron enviados con postMessage al origen configurado.
                        </p>
                        <p>
                            Esta vista directa no muestra tokens completos por seguridad.
                            Para consumirlos en producción usa el frontend mediante postMessage.
                        </p>
                        <p class="meta">requestId: %s</p>
                        <p class="meta">correlationId: %s</p>
                        <p class="meta">traceId: %s</p>
                        <pre id="payload"></pre>
                    </div>

                    <script>
                        const payload = %s;
                        const safePayload = %s;
                        const targetOrigin = %s;

                        if (window.opener && !window.opener.closed) {
                            window.opener.postMessage(payload, targetOrigin);
                            window.close();
                        } else {
                            document.getElementById('payload').textContent =
                                JSON.stringify(safePayload, null, 2);
                        }
                    </script>
                </body>
                </html>
                """.formatted(
                escapeHtml(requestId),
                escapeHtml(correlationId),
                escapeHtml(traceId),
                jsonPayload,
                safeJsonPayload,
                objectMapper.writeValueAsString(frontendOrigin)
        ));
    }

    private String resolveRequestId(HttpServletRequest request) {
        String value = resolveHeader(request, REQUEST_ID_HEADER, null);

        if (value != null && !value.isBlank() && !"UNKNOWN".equalsIgnoreCase(value)) {
            return value;
        }

        return UUID.randomUUID().toString();
    }

    private String resolveCorrelationId(
            HttpServletRequest request,
            String requestId
    ) {
        String value = resolveHeader(request, CORRELATION_ID_HEADER, null);

        if (value != null && !value.isBlank() && !"UNKNOWN".equalsIgnoreCase(value)) {
            return value;
        }

        return requestId;
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

    private String maskEmail(String email) {
        String value = safe(email, 180, "UNKNOWN");

        if ("UNKNOWN".equals(value) || !value.contains("@")) {
            return value;
        }

        int at = value.indexOf('@');

        if (at <= 2) {
            return "***" + value.substring(at);
        }

        return value.substring(0, 2) + "***" + value.substring(at);
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
}