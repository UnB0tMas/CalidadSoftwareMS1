package com.upsjb.ms1.security.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.upsjb.ms1.dto.shared.ApiResponseDto;
import com.upsjb.ms1.shared.response.ApiResponseFactory;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.stereotype.Component;

@Component
public class RestLogoutSuccessHandler implements LogoutSuccessHandler {

    private final ObjectMapper objectMapper;
    private final ApiResponseFactory apiResponseFactory;

    public RestLogoutSuccessHandler(
            ObjectMapper objectMapper,
            ApiResponseFactory apiResponseFactory
    ) {
        this.objectMapper = objectMapper;
        this.apiResponseFactory = apiResponseFactory;
    }

    @Override
    public void onLogoutSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException {
        ApiResponseDto<Void> body = apiResponseFactory.ok("Sesión cerrada correctamente.");

        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        objectMapper.writeValue(response.getWriter(), body);
    }
}