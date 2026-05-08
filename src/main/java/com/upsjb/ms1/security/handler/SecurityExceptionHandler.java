package com.upsjb.ms1.security.handler;

import com.upsjb.ms1.dto.shared.ErrorResponseDto;
import com.upsjb.ms1.shared.response.ErrorResponseFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class SecurityExceptionHandler {

    private final ErrorResponseFactory errorResponseFactory;

    public SecurityExceptionHandler(ErrorResponseFactory errorResponseFactory) {
        this.errorResponseFactory = errorResponseFactory;
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponseDto> handleAuthentication(AuthenticationException exception) {
        ErrorResponseDto response = errorResponseFactory.error(
                "No autenticado.",
                "UNAUTHORIZED",
                "AUTHENTICATION_REQUIRED"
        );

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponseDto> handleAccessDenied(AccessDeniedException exception) {
        ErrorResponseDto response = errorResponseFactory.error(
                "No tienes permisos para realizar esta acción.",
                "FORBIDDEN",
                "ACCESS_DENIED"
        );

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    @ExceptionHandler(JwtException.class)
    public ResponseEntity<ErrorResponseDto> handleJwtException(JwtException exception) {
        ErrorResponseDto response = errorResponseFactory.error(
                "El token de acceso no es válido.",
                "UNAUTHORIZED",
                "INVALID_ACCESS_TOKEN"
        );

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }
}