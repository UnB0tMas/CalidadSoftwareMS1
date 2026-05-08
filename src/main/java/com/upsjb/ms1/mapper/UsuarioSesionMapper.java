package com.upsjb.ms1.mapper;

import com.upsjb.ms1.domain.entity.UsuarioSesion;
import com.upsjb.ms1.dto.auth.response.SessionResponseDto;
import org.springframework.stereotype.Component;

@Component
public class UsuarioSesionMapper {

    public SessionResponseDto toResponse(UsuarioSesion sesion) {
        return toResponse(sesion, null);
    }

    public SessionResponseDto toResponse(UsuarioSesion sesion, Long idSesionActual) {
        if (sesion == null) {
            return null;
        }

        return new SessionResponseDto(
                sesion.getId(),
                sesion.getEstado(),
                sesion.getTipoLogin(),
                sesion.getIpAddress() == null ? null : sesion.getIpAddress().getValue(),
                sesion.getUserAgent() == null ? null : sesion.getUserAgent().getValue(),
                sesion.getDeviceFingerprint(),
                sesion.getCreatedAt(),
                sesion.getExpiresAt(),
                sesion.getLastUsedAt(),
                sesion.getRevokedAt(),
                sesion.getMotivoRevocacion(),
                sesion.getRevocationReasonDetail(),
                idSesionActual != null && idSesionActual.equals(sesion.getId())
        );
    }
}