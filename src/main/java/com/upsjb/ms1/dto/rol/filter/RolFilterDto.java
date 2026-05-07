package com.upsjb.ms1.dto.rol.filter;

import com.upsjb.ms1.domain.enums.EstadoRegistro;
import java.io.Serializable;

public record RolFilterDto(
        String search,
        String codigo,
        String nombre,
        EstadoRegistro estado,
        Boolean rolSistema
) implements Serializable {
}