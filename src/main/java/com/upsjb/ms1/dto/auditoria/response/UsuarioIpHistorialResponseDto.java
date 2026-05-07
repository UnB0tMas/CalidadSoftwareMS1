package com.upsjb.ms1.dto.auditoria.response;

import java.io.Serializable;
import java.time.Instant;

public record UsuarioIpHistorialResponseDto(
        Long idUsuarioIpHistorial,
        Long idUsuario,
        String username,
        String ipAddress,
        String ultimoUserAgent,
        Instant primerUsoAt,
        Instant ultimoUsoAt,
        int cantidadUsos,
        boolean sospechosa,
        boolean bloqueada,
        String motivoSospecha
) implements Serializable {
}