package com.upsjb.ms1.shared.exception;

import com.upsjb.ms1.dto.shared.ErrorResponseDto;
import com.upsjb.ms1.dto.shared.FieldErrorDto;
import com.upsjb.ms1.shared.audit.AuditContextHolder;
import com.upsjb.ms1.shared.response.ErrorResponseFactory;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final ErrorResponseFactory errorResponseFactory;

    public GlobalExceptionHandler(ErrorResponseFactory errorResponseFactory) {
        this.errorResponseFactory = errorResponseFactory;
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponseDto> handleBusinessException(
            BusinessException exception,
            HttpServletRequest request
    ) {
        log.warn(
                "Error de negocio. requestId={}, method={}, path={}, code={}, message={}",
                AuditContextHolder.getRequestIdOrNull(),
                request.getMethod(),
                request.getRequestURI(),
                exception.getCode(),
                exception.getMessage()
        );

        ErrorResponseDto response = errorResponseFactory.error(
                exception.getMessage(),
                exception.getStatus().name(),
                exception.getCode()
        );

        return ResponseEntity.status(exception.getStatus()).body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDto> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception
    ) {
        List<FieldErrorDto> details = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(this::toFieldErrorDto)
                .toList();

        ErrorResponseDto response = errorResponseFactory.error(
                "La solicitud contiene datos inválidos.",
                "VALIDATION_ERROR",
                "REQUEST_VALIDATION_FAILED",
                details
        );

        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ErrorResponseDto> handleHandlerMethodValidation(
            HandlerMethodValidationException exception
    ) {
        ErrorResponseDto response = errorResponseFactory.error(
                "La solicitud contiene parámetros inválidos.",
                "VALIDATION_ERROR",
                "REQUEST_PARAMETER_VALIDATION_FAILED"
        );

        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponseDto> handleConstraintViolation(
            ConstraintViolationException exception
    ) {
        List<FieldErrorDto> details = exception.getConstraintViolations()
                .stream()
                .map(violation -> new FieldErrorDto(
                        violation.getPropertyPath() == null ? null : violation.getPropertyPath().toString(),
                        violation.getMessage()
                ))
                .toList();

        ErrorResponseDto response = errorResponseFactory.error(
                "La solicitud contiene datos inválidos.",
                "VALIDATION_ERROR",
                "CONSTRAINT_VALIDATION_FAILED",
                details
        );

        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler({
            AuthenticationException.class,
            BadCredentialsException.class
    })
    public ResponseEntity<ErrorResponseDto> handleAuthentication(Exception exception) {
        ErrorResponseDto response = errorResponseFactory.error(
                "No autenticado.",
                "UNAUTHORIZED",
                "AUTHENTICATION_REQUIRED"
        );

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    @ExceptionHandler(AuthorizationDeniedException.class)
    public ResponseEntity<ErrorResponseDto> handleAuthorizationDenied(
            AuthorizationDeniedException exception
    ) {
        ErrorResponseDto response = errorResponseFactory.error(
                "No tienes permisos para realizar esta acción.",
                "FORBIDDEN",
                "ACCESS_DENIED"
        );

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponseDto> handleNoResourceFound(
            NoResourceFoundException exception,
            HttpServletRequest request
    ) {
        String path = request.getRequestURI();

        if (path != null && path.startsWith("/.well-known/appspecific/")) {
            log.debug(
                    "Recurso técnico de navegador no encontrado. requestId={}, method={}, path={}",
                    AuditContextHolder.getRequestIdOrNull(),
                    request.getMethod(),
                    path
            );
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        log.warn(
                "Recurso no encontrado. requestId={}, method={}, path={}",
                AuditContextHolder.getRequestIdOrNull(),
                request.getMethod(),
                path
        );

        ErrorResponseDto response = errorResponseFactory.error(
                "El recurso solicitado no existe.",
                "NOT_FOUND",
                "RESOURCE_NOT_FOUND"
        );

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDto> handleUnexpected(
            Exception exception,
            HttpServletRequest request
    ) {
        log.error(
                "Error inesperado. requestId={}, method={}, path={}",
                AuditContextHolder.getRequestIdOrNull(),
                request.getMethod(),
                request.getRequestURI(),
                exception
        );

        ErrorResponseDto response = errorResponseFactory.error(
                "Ocurrió un error interno al procesar la solicitud.",
                "INTERNAL_SERVER_ERROR",
                "UNEXPECTED_ERROR"
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    private FieldErrorDto toFieldErrorDto(FieldError fieldError) {
        return new FieldErrorDto(
                fieldError.getField(),
                fieldError.getDefaultMessage()
        );
    }
}
