// src/main/java/com/upsjb/ms1/dto/password/response/ChangePasswordVerificationResponseDto.java
package com.upsjb.ms1.dto.password.response;

import java.io.Serializable;
import java.time.Instant;

public record ChangePasswordVerificationResponseDto(
        String emailDestinoEnmascarado,
        Instant expiresAt,
        int maxIntentos,
        String mensaje
) implements Serializable {

    public ChangePasswordVerificationResponseDto(
            String emailDestinoEnmascarado,
            Instant expiresAt,
            int maxIntentos
    ) {
        this(
                emailDestinoEnmascarado,
                expiresAt,
                maxIntentos,
                "Código de verificación enviado correctamente."
        );
    }
}