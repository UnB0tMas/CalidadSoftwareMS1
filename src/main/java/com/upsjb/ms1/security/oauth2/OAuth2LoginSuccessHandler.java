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

    private static final int TOKEN_VISIBLE_PREFIX = 18;
    private static final int TOKEN_VISIBLE_SUFFIX = 10;

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
        OAuth2UserInfo userInfo = resolveUserInfo(authentication);

        AuthTokenResponseDto tokenResponse = authService.loginOAuth2(
                userInfo,
                null,
                IpAddressUtil.extractClientIp(request),
                UserAgentUtil.extractUserAgent(request)
        );

        Map<String, Object> payload = buildFrontendPayload(tokenResponse);
        Map<String, Object> safePayload = buildSafeDiagnosticPayload(tokenResponse);

        writePostMessageResponse(response, payload, safePayload);
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
            Map<String, Object> safePayload
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
                jsonPayload,
                safeJsonPayload,
                objectMapper.writeValueAsString(frontendOrigin)
        ));
    }
}
