package com.upsjb.ms1.dto.auth.response;

import com.upsjb.ms1.domain.enums.EstadoRegistro;
import java.io.Serializable;

public record AuthUserResponseDto(
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
        boolean requiereCambioPassword
) implements Serializable {
}