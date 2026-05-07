package com.upsjb.ms1.dto.verificacion.response;

import com.upsjb.ms1.domain.enums.EstadoVerificacionCodigo;
import com.upsjb.ms1.domain.enums.TipoCodigoVerificacion;
import java.io.Serializable;
import java.time.Instant;

public record VerificationStatusResponseDto(
        TipoCodigoVerificacion tipoCodigo,
        EstadoVerificacionCodigo estado,
        boolean valido,
        Instant validatedAt,
        Instant expiresAt,
        int intentosRestantes
) implements Serializable {
}