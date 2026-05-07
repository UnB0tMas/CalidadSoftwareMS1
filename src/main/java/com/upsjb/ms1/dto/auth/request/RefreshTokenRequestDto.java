package com.upsjb.ms1.dto.auth.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.io.Serializable;

public record RefreshTokenRequestDto(
        @NotBlank(message = "El refresh token es obligatorio.")
        @Size(max = 4096, message = "El refresh token no puede superar 4096 caracteres.")
        String refreshToken
) implements Serializable {
}