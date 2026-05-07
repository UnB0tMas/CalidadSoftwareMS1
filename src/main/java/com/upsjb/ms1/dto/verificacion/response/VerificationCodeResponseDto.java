package com.upsjb.ms1.dto.verificacion.response;

import com.upsjb.ms1.domain.enums.EstadoVerificacionCodigo;
import com.upsjb.ms1.domain.enums.TipoCodigoVerificacion;
import java.io.Serializable;
import java.time.Instant;

public record VerificationCodeResponseDto(
        Long idVerificacionCodigo,
        String emailDestinoEnmascarado,
        TipoCodigoVerificacion tipoCodigo,
        EstadoVerificacionCodigo estado,
        Instant expiresAt,
        int intentosUsados,
        int maxIntentos
) implements Serializable {
}