package com.upsjb.ms1.dto.auth.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.io.Serializable;

public record RevokeSessionRequestDto(
        @NotNull(message = "La sesión es obligatoria.")
        Long idSesion,

        @Size(max = 250, message = "El motivo no puede superar 250 caracteres.")
        String motivo
) implements Serializable {
}