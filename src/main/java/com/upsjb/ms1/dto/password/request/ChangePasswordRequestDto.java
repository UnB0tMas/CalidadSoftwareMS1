package com.upsjb.ms1.dto.password.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.io.Serializable;

public record ChangePasswordRequestDto(
        @NotBlank(message = "La contraseña actual es obligatoria.")
        @Size(max = 120, message = "La contraseña actual no puede superar 120 caracteres.")
        String passwordActual,

        @NotBlank(message = "La nueva contraseña es obligatoria.")
        @Size(min = 8, max = 120, message = "La nueva contraseña debe tener entre 8 y 120 caracteres.")
        String nuevaPassword,

        @NotBlank(message = "La confirmación de contraseña es obligatoria.")
        @Size(min = 8, max = 120, message = "La confirmación debe tener entre 8 y 120 caracteres.")
        String confirmarNuevaPassword
) implements Serializable {
}