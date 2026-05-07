package com.upsjb.ms1.dto.usuario.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.io.Serializable;

public record UsuarioCreateRequestDto(
        @NotBlank(message = "El username es obligatorio.")
        @Size(min = 3, max = 60, message = "El username debe tener entre 3 y 60 caracteres.")
        String username,

        @NotBlank(message = "El correo electrónico es obligatorio.")
        @Email(message = "El correo electrónico no tiene un formato válido.")
        @Size(max = 180, message = "El correo electrónico no puede superar 180 caracteres.")
        String email,

        @NotBlank(message = "La contraseña es obligatoria.")
        @Size(min = 8, max = 120, message = "La contraseña debe tener entre 8 y 120 caracteres.")
        String password,

        @NotBlank(message = "Los nombres son obligatorios.")
        @Size(max = 120, message = "Los nombres no pueden superar 120 caracteres.")
        String nombres,

        @NotBlank(message = "Los apellidos son obligatorios.")
        @Size(max = 120, message = "Los apellidos no pueden superar 120 caracteres.")
        String apellidos,

        @NotNull(message = "El rol es obligatorio.")
        Long idRol,

        Boolean emailVerificado,
        Boolean requiereCambioPassword
) implements Serializable {
}