package com.upsjb.ms1.service.contract;

import com.upsjb.ms1.domain.entity.Usuario;
import com.upsjb.ms1.domain.entity.UsuarioSesion;
import com.upsjb.ms1.domain.enums.EstadoSesion;
import com.upsjb.ms1.domain.enums.TipoLogin;
import com.upsjb.ms1.dto.auth.response.SessionResponseDto;
import com.upsjb.ms1.dto.shared.PageRequestDto;
import com.upsjb.ms1.dto.shared.PageResponseDto;
import com.upsjb.ms1.security.principal.AuthenticatedUserContext;

public interface SesionService {

    CreatedSession createSession(
            Usuario usuario,
            TipoLogin tipoLogin,
            String deviceFingerprint,
            String ipAddress,
            String userAgent
    );

    RotatedSession rotateRefreshToken(String refreshToken);

    SessionResponseDto findById(
            AuthenticatedUserContext actor,
            Long idSesion
    );

    PageResponseDto<SessionResponseDto> findOwnSessions(
            AuthenticatedUserContext actor,
            EstadoSesion estado,
            PageRequestDto pageRequest
    );

    PageResponseDto<SessionResponseDto> findUserSessions(
            AuthenticatedUserContext actor,
            Long idUsuario,
            EstadoSesion estado,
            PageRequestDto pageRequest
    );

    SessionResponseDto revokeSession(
            AuthenticatedUserContext actor,
            Long idSesion,
            String motivo
    );

    int revokeOwnActiveSessions(
            AuthenticatedUserContext actor,
            String motivo
    );

    int revokeUserActiveSessions(
            AuthenticatedUserContext actor,
            Long idUsuario,
            String motivo
    );

    int expireExpiredSessions();

    record CreatedSession(
            UsuarioSesion session,
            String refreshToken,
            SessionResponseDto response
    ) {
    }

    record RotatedSession(
            UsuarioSesion session,
            String refreshToken,
            SessionResponseDto response
    ) {
    }
}