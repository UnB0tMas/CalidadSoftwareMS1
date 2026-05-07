package com.upsjb.ms1.dto.usuario.filter;

import com.upsjb.ms1.domain.enums.EstadoRegistro;
import java.io.Serializable;
import java.time.Instant;

public record UsuarioFilterDto(
        String search,
        String username,
        String email,
        String nombres,
        String apellidos,
        Long idRol,
        String codigoRol,
        EstadoRegistro estado,
        Boolean emailVerificado,
        Boolean requiereCambioPassword,
        Instant fechaDesde,
        Instant fechaHasta
) implements Serializable {
}