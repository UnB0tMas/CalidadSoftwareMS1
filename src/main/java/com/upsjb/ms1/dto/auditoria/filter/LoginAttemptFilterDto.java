package com.upsjb.ms1.dto.auditoria.filter;

import com.upsjb.ms1.domain.enums.TipoLogin;
import java.io.Serializable;
import java.time.Instant;

public record LoginAttemptFilterDto(
        String usernameOrEmail,
        TipoLogin tipoLogin,
        Boolean exitoso,
        String failureCode,
        String ipAddress,
        Instant fechaDesde,
        Instant fechaHasta
) implements Serializable {
}