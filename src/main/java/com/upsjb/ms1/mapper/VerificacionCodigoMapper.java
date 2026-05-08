// src/main/java/com/upsjb/ms1/mapper/VerificacionCodigoMapper.java
package com.upsjb.ms1.mapper;

import com.upsjb.ms1.domain.entity.VerificacionCodigo;
import com.upsjb.ms1.domain.enums.EstadoVerificacionCodigo;
import com.upsjb.ms1.dto.verificacion.response.VerificationCodeResponseDto;
import com.upsjb.ms1.dto.verificacion.response.VerificationStatusResponseDto;
import com.upsjb.ms1.util.EmailMaskingUtil;
import java.time.Clock;
import java.time.Instant;
import org.springframework.stereotype.Component;

@Component
public class VerificacionCodigoMapper {

    private final Clock clock;

    public VerificacionCodigoMapper(Clock clock) {
        this.clock = clock;
    }

    public VerificationCodeResponseDto toResponse(VerificacionCodigo codigo) {
        return toResponse(codigo, resolveDefaultMessage(codigo));
    }

    public VerificationCodeResponseDto toResponse(
            VerificacionCodigo codigo,
            String mensaje
    ) {
        if (codigo == null) {
            return null;
        }

        return new VerificationCodeResponseDto(
                codigo.getId(),
                EmailMaskingUtil.mask(codigo.getEmail() == null ? null : codigo.getEmail().getValue()),
                codigo.getTipoCodigo(),
                codigo.getEstado(),
                mensaje,
                codigo.getExpiresAt(),
                codigo.getIntentosFallidos(),
                codigo.getMaxIntentos()
        );
    }

    public VerificationStatusResponseDto toStatusResponse(VerificacionCodigo codigo) {
        return toStatusResponse(codigo, resolveDefaultMessage(codigo));
    }

    public VerificationStatusResponseDto toStatusResponse(
            VerificacionCodigo codigo,
            String mensaje
    ) {
        if (codigo == null) {
            return null;
        }

        Instant ahora = Instant.now(clock);
        boolean valido = codigo.estaValidado() || codigo.estaVigente(ahora);
        int intentosRestantes = Math.max(0, codigo.getMaxIntentos() - codigo.getIntentosFallidos());

        return new VerificationStatusResponseDto(
                codigo.getTipoCodigo(),
                codigo.getEstado(),
                valido,
                mensaje,
                codigo.getValidatedAt(),
                codigo.getExpiresAt(),
                intentosRestantes
        );
    }

    private String resolveDefaultMessage(VerificacionCodigo codigo) {
        if (codigo == null || codigo.getEstado() == null) {
            return null;
        }

        if (EstadoVerificacionCodigo.PENDIENTE.equals(codigo.getEstado())) {
            return "Código de verificación pendiente.";
        }

        if (EstadoVerificacionCodigo.VALIDADO.equals(codigo.getEstado())) {
            return "Código de verificación validado correctamente.";
        }

        if (EstadoVerificacionCodigo.EXPIRADO.equals(codigo.getEstado())) {
            return "El código de verificación expiró.";
        }

        if (EstadoVerificacionCodigo.BLOQUEADO.equals(codigo.getEstado())) {
            return "El código de verificación fue bloqueado por intentos fallidos.";
        }

        if (EstadoVerificacionCodigo.REVOCADO.equals(codigo.getEstado())) {
            return "El código de verificación fue revocado.";
        }

        return "Estado de código de verificación actualizado.";
    }
}