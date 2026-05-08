package com.upsjb.ms1.mapper;

import com.upsjb.ms1.domain.entity.LoginAttempt;
import com.upsjb.ms1.domain.entity.Usuario;
import com.upsjb.ms1.dto.auditoria.response.LoginAttemptResponseDto;
import org.springframework.stereotype.Component;

@Component
public class LoginAttemptMapper {

    public LoginAttemptResponseDto toResponse(LoginAttempt attempt) {
        if (attempt == null) {
            return null;
        }

        Usuario usuario = attempt.getUsuario();

        return new LoginAttemptResponseDto(
                attempt.getId(),
                usuario == null ? null : usuario.getId(),
                usuario == null || usuario.getUsername() == null ? null : usuario.getUsername().getValue(),
                attempt.getUsernameOrEmail(),
                attempt.getTipoLogin(),
                attempt.isExitoso(),
                attempt.getFailureCode(),
                attempt.getFailureReason(),
                attempt.getIpAddress() == null ? null : attempt.getIpAddress().getValue(),
                attempt.getUserAgent() == null ? null : attempt.getUserAgent().getValue(),
                attempt.getAttemptedAt(),
                attempt.getRequestId()
        );
    }
}