package com.upsjb.ms1.shared.exception;

import com.upsjb.ms1.dto.shared.ErrorResponseDto;
import com.upsjb.ms1.dto.shared.FieldErrorDto;
import com.upsjb.ms1.shared.audit.AuditContext;
import com.upsjb.ms1.shared.audit.AuditContextHolder;
import com.upsjb.ms1.shared.response.ErrorResponseFactory;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.util.List;
import java.util.UUID;
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
        String errorId = errorId();

        log.warn(
                "ms1_business_error errorId={} requestId={} correlationId={} method={} path={} status={} code={} message=\"{}\" clientIp={} userAgent=\"{}\" suggestedAction=\"Validar regla de negocio, datos enviados y estado del recurso.\"",
                errorId,
                requestId(),
                correlationId(),
                safeMethod(request),
                safePath(request),
                exception.getStatus().value(),
                exception.getCode(),
                exception.getMessage(),
                clientIp(),
                userAgent()
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
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {
        String errorId = errorId();

        List<FieldErrorDto> details = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(this::toFieldErrorDto)
                .toList();

        log.warn(
                "ms1_validation_error errorId={} requestId={} correlationId={} method={} path={} code={} fieldErrors={} suggestedAction=\"Revisar DTO de request, Bean Validation y payload enviado.\"",
                errorId,
                requestId(),
                correlationId(),
                safeMethod(request),
                safePath(request),
                "REQUEST_VALIDATION_FAILED",
                details
        );

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
            HandlerMethodValidationException exception,
            HttpServletRequest request
    ) {
        String errorId = errorId();

        log.warn(
                "ms1_parameter_validation_error errorId={} requestId={} correlationId={} method={} path={} code={} rootCause={} rootMessage=\"{}\" suggestedAction=\"Revisar parámetros de path/query y restricciones de validación.\"",
                errorId,
                requestId(),
                correlationId(),
                safeMethod(request),
                safePath(request),
                "REQUEST_PARAMETER_VALIDATION_FAILED",
                rootCauseClass(exception),
                rootCauseMessage(exception)
        );

        ErrorResponseDto response = errorResponseFactory.error(
                "La solicitud contiene parámetros inválidos.",
                "VALIDATION_ERROR",
                "REQUEST_PARAMETER_VALIDATION_FAILED"
        );

        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponseDto> handleConstraintViolation(
            ConstraintViolationException exception,
            HttpServletRequest request
    ) {
        String errorId = errorId();

        List<FieldErrorDto> details = exception.getConstraintViolations()
                .stream()
                .map(violation -> new FieldErrorDto(
                        violation.getPropertyPath() == null ? null : violation.getPropertyPath().toString(),
                        violation.getMessage()
                ))
                .toList();

        log.warn(
                "ms1_constraint_validation_error errorId={} requestId={} correlationId={} method={} path={} code={} violations={} suggestedAction=\"Revisar constraints de validación y parámetros recibidos.\"",
                errorId,
                requestId(),
                correlationId(),
                safeMethod(request),
                safePath(request),
                "CONSTRAINT_VALIDATION_FAILED",
                details
        );

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
    public ResponseEntity<ErrorResponseDto> handleAuthentication(
            RuntimeException exception,
            HttpServletRequest request
    ) {
        String errorId = errorId();

        log.warn(
                "ms1_authentication_error errorId={} requestId={} correlationId={} method={} path={} code={} exceptionType={} rootCause={} rootMessage=\"{}\" suggestedAction=\"Validar token, credenciales, issuer, expiración y configuración JWT/JWKS.\"",
                errorId,
                requestId(),
                correlationId(),
                safeMethod(request),
                safePath(request),
                "AUTHENTICATION_REQUIRED",
                exception.getClass().getName(),
                rootCauseClass(exception),
                rootCauseMessage(exception)
        );

        ErrorResponseDto response = errorResponseFactory.error(
                "No autenticado.",
                "UNAUTHORIZED",
                "AUTHENTICATION_REQUIRED"
        );

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    @ExceptionHandler({
            AuthorizationDeniedException.class,
            org.springframework.security.access.AccessDeniedException.class
    })
    public ResponseEntity<ErrorResponseDto> handleAccessDenied(
            RuntimeException exception,
            HttpServletRequest request
    ) {
        String errorId = errorId();

        log.warn(
                "ms1_access_denied errorId={} requestId={} correlationId={} method={} path={} code={} exceptionType={} rootCause={} rootMessage=\"{}\" suggestedAction=\"Validar roles, authorities y policy contextual del recurso.\"",
                errorId,
                requestId(),
                correlationId(),
                safeMethod(request),
                safePath(request),
                "ACCESS_DENIED",
                exception.getClass().getName(),
                rootCauseClass(exception),
                rootCauseMessage(exception)
        );

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
        String path = safePath(request);

        if (path.startsWith("/.well-known/appspecific/") || path.equals("/favicon.ico")) {
            return ResponseEntity.notFound().build();
        }

        String errorId = errorId();

        log.warn(
                "ms1_route_not_found errorId={} requestId={} correlationId={} method={} path={} code={} suggestedAction=\"Validar ruta solicitada, mapping del controller o route del Gateway.\"",
                errorId,
                requestId(),
                correlationId(),
                safeMethod(request),
                path,
                "RESOURCE_NOT_FOUND"
        );

        ErrorResponseDto response = errorResponseFactory.error(
                "El recurso solicitado no existe.",
                "NOT_FOUND",
                "RESOURCE_NOT_FOUND"
        );

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDto> handleUnexpectedException(
            Exception exception,
            HttpServletRequest request
    ) {
        String errorId = errorId();

        log.error(
                "ms1_unexpected_error errorId={} requestId={} correlationId={} method={} path={} code={} exceptionType={} rootCause={} rootMessage=\"{}\" clientIp={} userAgent=\"{}\" suggestedAction=\"Revisar stacktrace completo con errorId, validar base de datos, transacciones, configuración y dependencias externas.\"",
                errorId,
                requestId(),
                correlationId(),
                safeMethod(request),
                safePath(request),
                "UNEXPECTED_ERROR",
                exception.getClass().getName(),
                rootCauseClass(exception),
                rootCauseMessage(exception),
                clientIp(),
                userAgent(),
                exception
        );

        ErrorResponseDto response = errorResponseFactory.error(
                "Ocurrió un error interno al procesar la solicitud.",
                "INTERNAL_SERVER_ERROR",
                "UNEXPECTED_ERROR"
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    private FieldErrorDto toFieldErrorDto(FieldError error) {
        return new FieldErrorDto(
                error.getField(),
                error.getDefaultMessage()
        );
    }

    private String requestId() {
        String requestId = AuditContextHolder.getRequestIdOrNull();
        return requestId == null || requestId.isBlank() ? "UNKNOWN" : requestId;
    }

    private String correlationId() {
        AuditContext context = AuditContextHolder.get();

        if (context == null || context.correlationId() == null || context.correlationId().isBlank()) {
            return "UNKNOWN";
        }

        return context.correlationId();
    }

    private String clientIp() {
        AuditContext context = AuditContextHolder.get();

        if (context == null || context.ipAddress() == null || context.ipAddress().isBlank()) {
            return "UNKNOWN";
        }

        return sanitize(context.ipAddress(), 45);
    }

    private String userAgent() {
        AuditContext context = AuditContextHolder.get();

        if (context == null || context.userAgent() == null || context.userAgent().isBlank()) {
            return "UNKNOWN";
        }

        return sanitize(context.userAgent(), 180);
    }

    private String safeMethod(HttpServletRequest request) {
        return request == null || request.getMethod() == null
                ? "UNKNOWN"
                : sanitize(request.getMethod(), 20);
    }

    private String safePath(HttpServletRequest request) {
        return request == null
                ? "UNKNOWN"
                : sanitize(request.getRequestURI(), 500);
    }

    private String errorId() {
        return UUID.randomUUID().toString().replace("-", "");
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

        return sanitize(root.getMessage(), 500);
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

    private String sanitize(
            String value,
            int maxLength
    ) {
        if (value == null || value.isBlank()) {
            return "UNKNOWN";
        }

        String sanitized = value
                .trim()
                .replace("\r", "")
                .replace("\n", "")
                .replace("\t", " ");

        if (sanitized.isBlank()) {
            return "UNKNOWN";
        }

        if (maxLength > 0 && sanitized.length() > maxLength) {
            return sanitized.substring(0, maxLength);
        }

        return sanitized;
    }
}