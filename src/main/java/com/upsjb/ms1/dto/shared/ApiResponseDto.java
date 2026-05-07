package com.upsjb.ms1.dto.shared;

import java.io.Serializable;
import java.time.Instant;

public record ApiResponseDto<T>(
        boolean success,
        String message,
        T data,
        String requestId,
        Instant timestamp
) implements Serializable {
}