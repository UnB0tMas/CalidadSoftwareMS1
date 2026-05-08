package com.upsjb.ms1.security.oauth2;

import com.upsjb.ms1.config.AppPropertiesConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

@Component
public class OAuth2LoginFailureHandler implements AuthenticationFailureHandler {

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
        String message = URLEncoder.encode(
                "No se pudo autenticar con OAuth2.",
                StandardCharsets.UTF_8
        );

        response.sendRedirect(appProperties.getFrontendUrl() + "/auth/oauth2/callback?status=error&message=" + message);
    }
}