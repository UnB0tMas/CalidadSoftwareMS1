// src/main/java/com/upsjb/ms1/dto/auditoria/response/UsuarioIpHistorialResponseDto.java
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
        String motivoSospecha,
        String mensaje
) implements Serializable {

    public UsuarioIpHistorialResponseDto(
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
    ) {
        this(
                idUsuarioIpHistorial,
                idUsuario,
                username,
                ipAddress,
                ultimoUserAgent,
                primerUsoAt,
                ultimoUsoAt,
                cantidadUsos,
                sospechosa,
                bloqueada,
                motivoSospecha,
                null
        );
    }
}