package com.upsjb.ms1.security.oauth2;

import com.upsjb.ms1.config.AppPropertiesConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

@Component
public class OAuth2LoginFailureHandler implements AuthenticationFailureHandler {

    private static final Logger log = LoggerFactory.getLogger(OAuth2LoginFailureHandler.class);

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
        log.warn(
                "Fallo OAuth2. requestUri={}, errorClass={}, message={}",
                request.getRequestURI(),
                exception.getClass().getName(),
                exception.getMessage(),
                exception
        );

        String message = URLEncoder.encode(
                "No se pudo autenticar con OAuth2.",
                StandardCharsets.UTF_8
        );

        String error = URLEncoder.encode(
                exception.getClass().getSimpleName(),
                StandardCharsets.UTF_8
        );

        response.sendRedirect(
                appProperties.getFrontendUrl()
                        + "/auth/oauth2/callback?status=error"
                        + "&error=" + error
                        + "&message=" + message
        );
    }
}