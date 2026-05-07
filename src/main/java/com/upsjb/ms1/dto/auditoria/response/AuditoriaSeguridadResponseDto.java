package com.upsjb.ms1.dto.auditoria.response;

import com.upsjb.ms1.domain.enums.TipoAuditoriaSeguridad;
import java.io.Serializable;
import java.time.Instant;

public record AuditoriaSeguridadResponseDto(
        Long idAuditoria,
        TipoAuditoriaSeguridad tipoEvento,
        Long idUsuarioActor,
        String usernameActor,
        Long idUsuarioAfectado,
        String usernameAfectado,
        String ipAddress,
        String userAgent,
        String requestId,
        String httpMethod,
        String path,
        String resultado,
        String descripcion,
        Instant eventAt
) implements Serializable {
}