package com.upsjb.ms1.dto.usuario.response;

import com.upsjb.ms1.domain.enums.EstadoRegistro;
import java.io.Serializable;
import java.time.Instant;

public record UsuarioDetailResponseDto(
        Long idUsuario,
        String username,
        String email,
        String nombres,
        String apellidos,
        Long idRol,
        String codigoRol,
        String nombreRol,
        EstadoRegistro estado,
        boolean emailVerificado,
        boolean requiereCambioPassword,
        Instant bloqueadoHasta,
        Instant ultimoLoginAt,
        Instant ultimoCambioPasswordAt,
        Instant createdAt,
        Instant updatedAt
) implements Serializable {
}