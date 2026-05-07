package com.upsjb.ms1.dto.rol.response;

import java.io.Serializable;

public record RolLookupDto(
        Long idRol,
        String codigo,
        String nombre
) implements Serializable {
}