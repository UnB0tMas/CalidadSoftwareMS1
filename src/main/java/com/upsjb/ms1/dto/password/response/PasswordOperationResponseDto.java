package com.upsjb.ms1.dto.password.response;

import com.upsjb.ms1.domain.enums.EstadoVerificacionCodigo;
import java.io.Serializable;
import java.time.Instant;

public record PasswordOperationResponseDto(
        EstadoVerificacionCodigo estado,
        String mensaje,
        Instant expiresAt
) implements Serializable {
}