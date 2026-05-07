package com.upsjb.ms1.dto.password.response;

import java.io.Serializable;
import java.time.Instant;

public record ChangePasswordVerificationResponseDto(
        String emailDestinoEnmascarado,
        Instant expiresAt,
        int maxIntentos
) implements Serializable {
}