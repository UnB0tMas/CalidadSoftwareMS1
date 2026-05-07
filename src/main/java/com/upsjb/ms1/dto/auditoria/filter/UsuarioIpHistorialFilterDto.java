package com.upsjb.ms1.dto.auditoria.filter;

import java.io.Serializable;
import java.time.Instant;

public record UsuarioIpHistorialFilterDto(
        Long idUsuario,
        String username,
        String ipAddress,
        Boolean sospechosa,
        Boolean bloqueada,
        Instant fechaDesde,
        Instant fechaHasta
) implements Serializable {
}