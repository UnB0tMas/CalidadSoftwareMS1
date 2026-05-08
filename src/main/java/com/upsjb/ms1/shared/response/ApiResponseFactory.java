package com.upsjb.ms1.shared.response;

import com.upsjb.ms1.dto.shared.ApiResponseDto;
import com.upsjb.ms1.shared.audit.AuditContextHolder;
import java.time.Clock;
import java.time.Instant;
import org.springframework.stereotype.Component;

@Component
public class ApiResponseFactory {

    private final Clock clock;

    public ApiResponseFactory(Clock clock) {
        this.clock = clock;
    }

    public <T> ApiResponseDto<T> ok(String message, T data) {
        return new ApiResponseDto<>(
                true,
                message,
                data,
                AuditContextHolder.getRequestIdOrNull(),
                Instant.now(clock)
        );
    }

    public <T> ApiResponseDto<T> created(String message, T data) {
        return ok(message, data);
    }

    public ApiResponseDto<Void> ok(String message) {
        return ok(message, null);
    }
}