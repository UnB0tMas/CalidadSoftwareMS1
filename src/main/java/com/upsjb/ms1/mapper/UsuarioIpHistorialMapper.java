// src/main/java/com/upsjb/ms1/mapper/UsuarioIpHistorialMapper.java
package com.upsjb.ms1.mapper;

import com.upsjb.ms1.domain.entity.Usuario;
import com.upsjb.ms1.domain.entity.UsuarioIpHistorial;
import com.upsjb.ms1.dto.auditoria.response.UsuarioIpHistorialResponseDto;
import org.springframework.stereotype.Component;

@Component
public class UsuarioIpHistorialMapper {

    public UsuarioIpHistorialResponseDto toResponse(UsuarioIpHistorial historial) {
        return toResponse(historial, null);
    }

    public UsuarioIpHistorialResponseDto toResponse(
            UsuarioIpHistorial historial,
            String mensaje
    ) {
        if (historial == null) {
            return null;
        }

        Usuario usuario = historial.getUsuario();

        return new UsuarioIpHistorialResponseDto(
                historial.getId(),
                usuario == null ? null : usuario.getId(),
                usuario == null || usuario.getUsername() == null ? null : usuario.getUsername().getValue(),
                historial.getIpAddress() == null ? null : historial.getIpAddress().getValue(),
                historial.getUltimoUserAgent() == null ? null : historial.getUltimoUserAgent().getValue(),
                historial.getPrimerUsoAt(),
                historial.getUltimoUsoAt(),
                historial.getCantidadUsos(),
                historial.isSospechosa(),
                historial.isBloqueada(),
                historial.getMotivoSospecha(),
                mensaje
        );
    }
}