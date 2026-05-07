package com.upsjb.ms1.dto.auditoria.response;

import com.upsjb.ms1.domain.enums.TipoLogin;
import java.io.Serializable;
import java.time.Instant;

public record LoginAttemptResponseDto(
        Long idLoginAttempt,
        Long idUsuario,
        String username,
        String usernameOrEmail,
        TipoLogin tipoLogin,
        boolean exitoso,
        String failureCode,
        String failureReason,
        String ipAddress,
        String userAgent,
        Instant attemptedAt,
        String requestId
) implements Serializable {
}