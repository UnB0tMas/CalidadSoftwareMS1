// ruta: src/main/java/com/upsjb/ms1/dto/internal/response/InternalSessionValidationResponseDto.java
package com.upsjb.ms1.dto.internal.response;

import java.io.Serializable;

public record InternalSessionValidationResponseDto(
        Boolean valid,
        Boolean active,
        Long idUsuarioMs1,
        Long sessionId,
        String username,
        String email,
        String rol,
        String tokenType,
        String estado,
        String message
) implements Serializable {

    public static InternalSessionValidationResponseDto active(
            Long idUsuarioMs1,
            Long sessionId,
            String username,
            String email,
            String rol
    ) {
        return new InternalSessionValidationResponseDto(
                true,
                true,
                idUsuarioMs1,
                sessionId,
                username,
                email,
                rol,
                "access",
                "ACTIVA",
                "Sesión MS1 activa."
        );
    }

    public static InternalSessionValidationResponseDto inactive(
            Long idUsuarioMs1,
            Long sessionId,
            String estado,
            String message
    ) {
        return new InternalSessionValidationResponseDto(
                false,
                false,
                idUsuarioMs1,
                sessionId,
                null,
                null,
                null,
                "access",
                estado == null || estado.isBlank() ? "INVALIDA" : estado.trim(),
                message == null || message.isBlank() ? "Sesión MS1 inválida." : message.trim()
        );
    }
}