package com.upsjb.ms1.dto.shared;

import java.io.Serializable;
import java.time.Instant;
import java.util.List;

public record ErrorResponseDto(
        boolean success,
        String message,
        ErrorDetail error,
        String requestId,
        Instant timestamp
) implements Serializable {

    public record ErrorDetail(
            String type,
            String code,
            List<FieldErrorDto> details
    ) implements Serializable {
    }
}