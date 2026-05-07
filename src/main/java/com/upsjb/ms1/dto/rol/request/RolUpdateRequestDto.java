package com.upsjb.ms1.dto.rol.request;

import jakarta.validation.constraints.Size;
import java.io.Serializable;

public record RolUpdateRequestDto(
        @Size(min = 2, max = 80, message = "El nombre del rol debe tener entre 2 y 80 caracteres.")
        String nombre,

        @Size(max = 250, message = "La descripción no puede superar 250 caracteres.")
        String descripcion
) implements Serializable {
}