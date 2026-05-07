package com.upsjb.ms1.dto.auth.response;

import com.upsjb.ms1.domain.enums.EstadoSesion;
import com.upsjb.ms1.domain.enums.MotivoRevocacionSesion;
import com.upsjb.ms1.domain.enums.TipoLogin;
import java.io.Serializable;
import java.time.Instant;

public record SessionResponseDto(
        Long idSesion,
        EstadoSesion estado,
        TipoLogin tipoLogin,
        String ipAddress,
        String userAgent,
        String deviceFingerprint,
        Instant fechaCreacion,
        Instant fechaExpiracion,
        Instant ultimoUso,
        Instant fechaRevocacion,
        MotivoRevocacionSesion motivoRevocacion,
        String detalleRevocacion,
        boolean sesionActual
) implements Serializable {
}