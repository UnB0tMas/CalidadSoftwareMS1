package com.upsjb.ms1.dto.rol.response;

import com.upsjb.ms1.domain.enums.EstadoRegistro;
import java.io.Serializable;
import java.time.Instant;

public record RolResponseDto(
        Long idRol,
        String codigo,
        String nombre,
        String descripcion,
        EstadoRegistro estado,
        boolean rolSistema,
        Instant createdAt,
        Instant updatedAt
) implements Serializable {
}