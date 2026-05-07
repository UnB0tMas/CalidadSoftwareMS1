package com.upsjb.ms1.dto.usuario.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.io.Serializable;

public record UsuarioChangeRolRequestDto(
        @NotNull(message = "El rol es obligatorio.")
        Long idRol,

        @Size(max = 250, message = "El motivo no puede superar 250 caracteres.")
        String motivo
) implements Serializable {
}