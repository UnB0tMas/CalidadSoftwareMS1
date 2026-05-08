package com.upsjb.ms1.service.contract;

import com.upsjb.ms1.domain.entity.Usuario;
import com.upsjb.ms1.domain.enums.TipoAuditoriaSeguridad;
import com.upsjb.ms1.dto.auditoria.filter.AuditoriaSeguridadFilterDto;
import com.upsjb.ms1.dto.auditoria.filter.LoginAttemptFilterDto;
import com.upsjb.ms1.dto.auditoria.filter.UsuarioIpHistorialFilterDto;
import com.upsjb.ms1.dto.auditoria.response.AuditoriaSeguridadResponseDto;
import com.upsjb.ms1.dto.auditoria.response.LoginAttemptResponseDto;
import com.upsjb.ms1.dto.auditoria.response.UsuarioIpHistorialResponseDto;
import com.upsjb.ms1.dto.shared.PageRequestDto;
import com.upsjb.ms1.dto.shared.PageResponseDto;
import com.upsjb.ms1.security.principal.AuthenticatedUserContext;

public interface AuditoriaSeguridadService {

    AuditoriaSeguridadResponseDto registerSuccess(
            TipoAuditoriaSeguridad tipo,
            Usuario actor,
            Usuario afectado,
            String descripcion
    );

    AuditoriaSeguridadResponseDto registerSuccess(
            TipoAuditoriaSeguridad tipo,
            Usuario actor,
            Usuario afectado,
            String descripcion,
            String metadataJson
    );

    AuditoriaSeguridadResponseDto registerFailure(
            TipoAuditoriaSeguridad tipo,
            Usuario actor,
            Usuario afectado,
            String descripcion
    );

    AuditoriaSeguridadResponseDto registerFailure(
            TipoAuditoriaSeguridad tipo,
            Usuario actor,
            Usuario afectado,
            String descripcion,
            String metadataJson
    );

    AuditoriaSeguridadResponseDto registerWarning(
            TipoAuditoriaSeguridad tipo,
            Usuario actor,
            Usuario afectado,
            String descripcion
    );

    AuditoriaSeguridadResponseDto registerWarning(
            TipoAuditoriaSeguridad tipo,
            Usuario actor,
            Usuario afectado,
            String descripcion,
            String metadataJson
    );

    AuditoriaSeguridadResponseDto findById(
            AuthenticatedUserContext actor,
            Long idAuditoria
    );

    PageResponseDto<AuditoriaSeguridadResponseDto> findSecurityAudit(
            AuthenticatedUserContext actor,
            AuditoriaSeguridadFilterDto filter,
            PageRequestDto pageRequest
    );

    PageResponseDto<LoginAttemptResponseDto> findLoginAttempts(
            AuthenticatedUserContext actor,
            LoginAttemptFilterDto filter,
            PageRequestDto pageRequest
    );

    PageResponseDto<UsuarioIpHistorialResponseDto> findIpHistory(
            AuthenticatedUserContext actor,
            UsuarioIpHistorialFilterDto filter,
            PageRequestDto pageRequest
    );
}