package com.upsjb.ms1.shared.response;

import com.upsjb.ms1.dto.shared.ErrorResponseDto;
import com.upsjb.ms1.dto.shared.FieldErrorDto;
import com.upsjb.ms1.shared.audit.AuditContextHolder;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ErrorResponseFactory {

    private final Clock clock;

    public ErrorResponseFactory(Clock clock) {
        this.clock = clock;
    }

    public ErrorResponseDto error(
            String message,
            String type,
            String code,
            List<FieldErrorDto> details
    ) {
        return new ErrorResponseDto(
                false,
                message,
                new ErrorResponseDto.ErrorDetail(
                        safe(type, "ERROR"),
                        safe(code, "UNEXPECTED_ERROR"),
                        details == null ? List.of() : List.copyOf(details)
                ),
                AuditContextHolder.getRequestIdOrNull(),
                Instant.now(clock)
        );
    }

    public ErrorResponseDto error(String message, String type, String code) {
        return error(message, type, code, List.of());
    }

    private String safe(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}