package com.upsjb.ms1.dto.auditoria.filter;

import com.upsjb.ms1.domain.enums.TipoAuditoriaSeguridad;
import java.io.Serializable;
import java.time.Instant;

public record AuditoriaSeguridadFilterDto(
        Long idUsuarioActor,
        Long idUsuarioAfectado,
        String username,
        TipoAuditoriaSeguridad tipoEvento,
        String resultado,
        String ipAddress,
        String requestId,
        String path,
        Instant fechaDesde,
        Instant fechaHasta
) implements Serializable {
}