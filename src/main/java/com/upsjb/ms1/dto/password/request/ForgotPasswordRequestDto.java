package com.upsjb.ms1.dto.password.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.io.Serializable;

public record ForgotPasswordRequestDto(
        @NotBlank(message = "El correo electrónico es obligatorio.")
        @Email(message = "El correo electrónico no tiene un formato válido.")
        @Size(max = 180, message = "El correo electrónico no puede superar 180 caracteres.")
        String email
) implements Serializable {
}