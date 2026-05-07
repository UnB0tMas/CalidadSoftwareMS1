package com.upsjb.ms1.dto.verificacion.request;

import com.upsjb.ms1.domain.enums.TipoCodigoVerificacion;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.io.Serializable;

public record ResendVerificationCodeRequestDto(
        @NotBlank(message = "El correo electrónico es obligatorio.")
        @Email(message = "El correo electrónico no tiene un formato válido.")
        @Size(max = 180, message = "El correo electrónico no puede superar 180 caracteres.")
        String email,

        @NotNull(message = "El tipo de código es obligatorio.")
        TipoCodigoVerificacion tipoCodigo
) implements Serializable {
}