package com.upsjb.ms1.dto.password.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.io.Serializable;

public record ResetPasswordRequestDto(
        @NotBlank(message = "El correo electrónico es obligatorio.")
        @Email(message = "El correo electrónico no tiene un formato válido.")
        @Size(max = 180, message = "El correo electrónico no puede superar 180 caracteres.")
        String email,

        @NotBlank(message = "El token o código de recuperación es obligatorio.")
        @Size(max = 4096, message = "El token o código no puede superar 4096 caracteres.")
        String token,

        @NotBlank(message = "La nueva contraseña es obligatoria.")
        @Size(min = 8, max = 120, message = "La nueva contraseña debe tener entre 8 y 120 caracteres.")
        String nuevaPassword,

        @NotBlank(message = "La confirmación de contraseña es obligatoria.")
        @Size(min = 8, max = 120, message = "La confirmación debe tener entre 8 y 120 caracteres.")
        String confirmarNuevaPassword
) implements Serializable {
}