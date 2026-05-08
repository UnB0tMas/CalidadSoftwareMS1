// src/main/java/com/upsjb/ms1/mapper/PasswordResetTokenMapper.java
package com.upsjb.ms1.mapper;

import com.upsjb.ms1.domain.entity.PasswordResetToken;
import com.upsjb.ms1.domain.enums.EstadoVerificacionCodigo;
import com.upsjb.ms1.dto.password.response.PasswordOperationResponseDto;
import org.springframework.stereotype.Component;

@Component
public class PasswordResetTokenMapper {

    private static final String GENERIC_FORGOT_MESSAGE =
            "Si el correo está registrado, se enviaron instrucciones de recuperación.";

    public PasswordOperationResponseDto toResponse(PasswordResetToken token) {
        if (token == null) {
            return null;
        }

        return new PasswordOperationResponseDto(
                token.getEstado(),
                resolveMensaje(token.getEstado()),
                token.getExpiresAt()
        );
    }

    public PasswordOperationResponseDto toRequestedResponse(PasswordResetToken token) {
        if (token == null) {
            return null;
        }

        return new PasswordOperationResponseDto(
                EstadoVerificacionCodigo.PENDIENTE,
                GENERIC_FORGOT_MESSAGE,
                null
        );
    }

    public PasswordOperationResponseDto toCompletedResponse(PasswordResetToken token) {
        if (token == null) {
            return null;
        }

        return new PasswordOperationResponseDto(
                token.getEstado(),
                "La contraseña fue restablecida correctamente.",
                null
        );
    }

    private String resolveMensaje(EstadoVerificacionCodigo estado) {
        if (estado == null) {
            return "Operación de contraseña procesada.";
        }

        return switch (estado) {
            case PENDIENTE -> "La operación de contraseña está pendiente de confirmación.";
            case VALIDADO -> "La operación de contraseña fue validada correctamente.";
            case EXPIRADO -> "La operación de contraseña expiró.";
            case BLOQUEADO -> "La operación de contraseña fue bloqueada por intentos fallidos.";
            case REVOCADO -> "La operación de contraseña fue revocada.";
        };
    }
}