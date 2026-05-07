package com.upsjb.ms1.dto.usuario.request;

import com.upsjb.ms1.domain.enums.EstadoRegistro;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.io.Serializable;

public record UsuarioChangeEstadoRequestDto(
        @NotNull(message = "El estado es obligatorio.")
        EstadoRegistro estado,

        @Size(max = 250, message = "El motivo no puede superar 250 caracteres.")
        String motivo
) implements Serializable {
}