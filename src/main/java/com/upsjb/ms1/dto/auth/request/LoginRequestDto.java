package com.upsjb.ms1.dto.auth.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.io.Serializable;

public record LoginRequestDto(
        @NotBlank(message = "El username o email es obligatorio.")
        @Size(max = 180, message = "El username o email no puede superar 180 caracteres.")
        String usernameOrEmail,

        @NotBlank(message = "La contraseña es obligatoria.")
        @Size(max = 120, message = "La contraseña no puede superar 120 caracteres.")
        String password,

        Boolean rememberMe,

        @Size(max = 160, message = "El identificador del dispositivo no puede superar 160 caracteres.")
        String deviceFingerprint
) implements Serializable {
}