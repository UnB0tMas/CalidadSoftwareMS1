package com.upsjb.ms1.dto.rol.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.io.Serializable;

public record RolCreateRequestDto(
        @NotBlank(message = "El código del rol es obligatorio.")
        @Size(min = 2, max = 40, message = "El código del rol debe tener entre 2 y 40 caracteres.")
        String codigo,

        @NotBlank(message = "El nombre del rol es obligatorio.")
        @Size(min = 2, max = 80, message = "El nombre del rol debe tener entre 2 y 80 caracteres.")
        String nombre,

        @Size(max = 250, message = "La descripción no puede superar 250 caracteres.")
        String descripcion
) implements Serializable {
}