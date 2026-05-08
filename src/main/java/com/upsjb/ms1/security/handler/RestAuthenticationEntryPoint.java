package com.upsjb.ms1.security.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.upsjb.ms1.dto.shared.ErrorResponseDto;
import com.upsjb.ms1.shared.response.ErrorResponseFactory;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;
    private final ErrorResponseFactory errorResponseFactory;

    public RestAuthenticationEntryPoint(
            ObjectMapper objectMapper,
            ErrorResponseFactory errorResponseFactory
    ) {
        this.objectMapper = objectMapper;
        this.errorResponseFactory = errorResponseFactory;
    }

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException {
        ErrorResponseDto body = errorResponseFactory.error(
                "No autenticado.",
                "UNAUTHORIZED",
                "AUTHENTICATION_REQUIRED"
        );

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        objectMapper.writeValue(response.getWriter(), body);
    }
}