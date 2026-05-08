package com.upsjb.ms1.service.contract;

import com.upsjb.ms1.domain.entity.Usuario;
import com.upsjb.ms1.domain.enums.TipoLogin;
import com.upsjb.ms1.dto.auditoria.filter.LoginAttemptFilterDto;
import com.upsjb.ms1.dto.auditoria.response.LoginAttemptResponseDto;
import com.upsjb.ms1.dto.shared.PageRequestDto;
import com.upsjb.ms1.dto.shared.PageResponseDto;
import com.upsjb.ms1.security.principal.AuthenticatedUserContext;
import java.time.Instant;
import java.util.List;

public interface LoginAttemptService {

    LoginAttemptResponseDto recordSuccess(
            Usuario usuario,
            String usernameOrEmail,
            TipoLogin tipoLogin
    );

    LoginAttemptResponseDto recordFailure(
            Usuario usuario,
            String usernameOrEmail,
            TipoLogin tipoLogin,
            String failureCode,
            String failureReason
    );

    LoginAttemptResponseDto recordAttempt(
            Usuario usuario,
            String usernameOrEmail,
            TipoLogin tipoLogin,
            boolean exitoso,
            String failureCode,
            String failureReason
    );

    LoginAttemptResponseDto findById(
            AuthenticatedUserContext actor,
            Long idLoginAttempt
    );

    PageResponseDto<LoginAttemptResponseDto> findAll(
            AuthenticatedUserContext actor,
            LoginAttemptFilterDto filter,
            PageRequestDto pageRequest
    );

    List<LoginAttemptResponseDto> findRecentByUsernameOrEmail(
            AuthenticatedUserContext actor,
            String usernameOrEmail
    );

    long countRecentFailuresByUsernameOrEmail(
            String usernameOrEmail,
            Instant since
    );

    long countRecentFailuresByIp(
            String ipAddress,
            Instant since
    );
}