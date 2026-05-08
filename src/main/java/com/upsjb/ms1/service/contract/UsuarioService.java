// src/main/java/com/upsjb/ms1/service/contract/UsuarioService.java
package com.upsjb.ms1.service.contract;

import com.upsjb.ms1.dto.shared.PageRequestDto;
import com.upsjb.ms1.dto.shared.PageResponseDto;
import com.upsjb.ms1.dto.usuario.filter.UsuarioFilterDto;
import com.upsjb.ms1.dto.usuario.request.UsuarioChangeEstadoRequestDto;
import com.upsjb.ms1.dto.usuario.request.UsuarioChangeRolRequestDto;
import com.upsjb.ms1.dto.usuario.request.UsuarioCreateRequestDto;
import com.upsjb.ms1.dto.usuario.request.UsuarioUpdateRequestDto;
import com.upsjb.ms1.dto.usuario.response.UsuarioDetailResponseDto;
import com.upsjb.ms1.dto.usuario.response.UsuarioLookupDto;
import com.upsjb.ms1.dto.usuario.response.UsuarioResponseDto;
import com.upsjb.ms1.security.principal.AuthenticatedUserContext;
import java.util.List;

public interface UsuarioService {

    UsuarioResponseDto create(
            AuthenticatedUserContext actor,
            UsuarioCreateRequestDto request
    );

    UsuarioResponseDto update(
            AuthenticatedUserContext actor,
            Long idUsuario,
            UsuarioUpdateRequestDto request
    );

    UsuarioResponseDto changeEstado(
            AuthenticatedUserContext actor,
            Long idUsuario,
            UsuarioChangeEstadoRequestDto request
    );

    UsuarioResponseDto changeRol(
            AuthenticatedUserContext actor,
            Long idUsuario,
            UsuarioChangeRolRequestDto request
    );

    UsuarioDetailResponseDto findById(
            AuthenticatedUserContext actor,
            Long idUsuario
    );

    PageResponseDto<UsuarioResponseDto> findAll(
            AuthenticatedUserContext actor,
            UsuarioFilterDto filter,
            PageRequestDto pageRequest
    );

    List<UsuarioLookupDto> findLookup(AuthenticatedUserContext actor);
}