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
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

@Component
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final AppPropertiesConfig appProperties;
    private final AuthService authService;
    private final ObjectMapper objectMapper;

    public OAuth2LoginSuccessHandler(
            AppPropertiesConfig appProperties,
            AuthService authService,
            ObjectMapper objectMapper
    ) {
        this.appProperties = appProperties;
        this.authService = authService;
        this.objectMapper = objectMapper;
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

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("status", "success");
        payload.put("tokenType", tokenResponse.tokenType());
        payload.put("accessToken", tokenResponse.accessToken());
        payload.put("refreshToken", tokenResponse.refreshToken());
        payload.put("accessTokenExpiresAt", tokenResponse.accessTokenExpiresAt());
        payload.put("refreshTokenExpiresAt", tokenResponse.refreshTokenExpiresAt());
        payload.put("user", tokenResponse.user());
        payload.put("session", tokenResponse.session());

        writePostMessageResponse(response, payload);
    }

    private OAuth2UserInfo resolveUserInfo(Authentication authentication) {
        Object principal = authentication == null ? null : authentication.getPrincipal();

        if (!(principal instanceof OAuth2User oAuth2User)) {
            throw new org.springframework.security.authentication.BadCredentialsException(
                    "Principal OAuth2 inválido."
            );
        }

        Object normalized = oAuth2User.getAttribute(CustomOAuth2UserService.NORMALIZED_USER_INFO_ATTRIBUTE);

        if (!(normalized instanceof OAuth2UserInfo userInfo)) {
            throw new org.springframework.security.authentication.BadCredentialsException(
                    "No se encontró información OAuth2 normalizada."
            );
        }

        return userInfo;
    }

    private void writePostMessageResponse(
            HttpServletResponse response,
            Map<String, Object> payload
    ) throws IOException {
        String jsonPayload = objectMapper.writeValueAsString(payload);
        String frontendOrigin = appProperties.getFrontendUrl();
        String frontendCallback = appProperties.getFrontendUrl() + "/auth/oauth2/callback?status=success";

        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType(MediaType.TEXT_HTML_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store, no-cache, must-revalidate, max-age=0");
        response.setHeader(HttpHeaders.PRAGMA, "no-cache");
        response.setHeader("X-Content-Type-Options", "nosniff");

        response.getWriter().write("""
                <!doctype html>
                <html lang=\"es\">
                <head>
                    <meta charset=\"UTF-8\">
                    <title>OAuth2 completado</title>
                </head>
                <body>
                    <script>
                        const payload = %s;
                        const targetOrigin = %s;
                        if (window.opener && !window.opener.closed) {
                            window.opener.postMessage(payload, targetOrigin);
                            window.close();
                        } else {
                            sessionStorage.setItem('oauth2_result', JSON.stringify(payload));
                            window.location.replace(%s);
                        }
                    </script>
                </body>
                </html>
                """.formatted(
                jsonPayload,
                objectMapper.writeValueAsString(frontendOrigin),
                objectMapper.writeValueAsString(frontendCallback)
        ));
    }
}
