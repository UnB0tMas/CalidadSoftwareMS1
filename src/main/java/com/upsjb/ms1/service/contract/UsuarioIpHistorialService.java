// src/main/java/com/upsjb/ms1/service/contract/UsuarioIpHistorialService.java
package com.upsjb.ms1.service.contract;

import com.upsjb.ms1.domain.entity.Usuario;
import com.upsjb.ms1.dto.auditoria.filter.UsuarioIpHistorialFilterDto;
import com.upsjb.ms1.dto.auditoria.response.UsuarioIpHistorialResponseDto;
import com.upsjb.ms1.dto.shared.PageRequestDto;
import com.upsjb.ms1.dto.shared.PageResponseDto;
import com.upsjb.ms1.security.principal.AuthenticatedUserContext;
import java.util.List;

public interface UsuarioIpHistorialService {

    UsuarioIpHistorialResponseDto registerUsage(
            Usuario usuario,
            String ipAddress,
            String userAgent
    );

    UsuarioIpHistorialResponseDto findById(
            AuthenticatedUserContext actor,
            Long idUsuarioIpHistorial
    );

    PageResponseDto<UsuarioIpHistorialResponseDto> findAll(
            AuthenticatedUserContext actor,
            UsuarioIpHistorialFilterDto filter,
            PageRequestDto pageRequest
    );

    PageResponseDto<UsuarioIpHistorialResponseDto> findByUsuario(
            AuthenticatedUserContext actor,
            Long idUsuario,
            PageRequestDto pageRequest
    );

    List<UsuarioIpHistorialResponseDto> findRecentByUsuario(
            AuthenticatedUserContext actor,
            Long idUsuario
    );

    UsuarioIpHistorialResponseDto markSuspicious(
            AuthenticatedUserContext actor,
            Long idUsuarioIpHistorial,
            String motivo
    );

    UsuarioIpHistorialResponseDto clearSuspicion(
            AuthenticatedUserContext actor,
            Long idUsuarioIpHistorial
    );

    UsuarioIpHistorialResponseDto blockIp(
            AuthenticatedUserContext actor,
            Long idUsuarioIpHistorial,
            String motivo
    );

    UsuarioIpHistorialResponseDto unblockIp(
            AuthenticatedUserContext actor,
            Long idUsuarioIpHistorial
    );
}