package com.upsjb.ms1.dto.usuario.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import java.io.Serializable;

public record UsuarioUpdateRequestDto(
        @Email(message = "El correo electrónico no tiene un formato válido.")
        @Size(max = 180, message = "El correo electrónico no puede superar 180 caracteres.")
        String email,

        @Size(max = 120, message = "Los nombres no pueden superar 120 caracteres.")
        String nombres,

        @Size(max = 120, message = "Los apellidos no pueden superar 120 caracteres.")
        String apellidos
) implements Serializable {
}