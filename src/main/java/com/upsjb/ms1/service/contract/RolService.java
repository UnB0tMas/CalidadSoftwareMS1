package com.upsjb.ms1.service.contract;

import com.upsjb.ms1.domain.enums.EstadoRegistro;
import com.upsjb.ms1.dto.rol.filter.RolFilterDto;
import com.upsjb.ms1.dto.rol.request.RolCreateRequestDto;
import com.upsjb.ms1.dto.rol.request.RolUpdateRequestDto;
import com.upsjb.ms1.dto.rol.response.RolLookupDto;
import com.upsjb.ms1.dto.rol.response.RolResponseDto;
import com.upsjb.ms1.dto.shared.PageRequestDto;
import com.upsjb.ms1.dto.shared.PageResponseDto;
import com.upsjb.ms1.security.principal.AuthenticatedUserContext;
import java.util.List;

public interface RolService {

    RolResponseDto create(
            AuthenticatedUserContext actor,
            RolCreateRequestDto request
    );

    RolResponseDto update(
            AuthenticatedUserContext actor,
            Long idRol,
            RolUpdateRequestDto request
    );

    RolResponseDto changeEstado(
            AuthenticatedUserContext actor,
            Long idRol,
            EstadoRegistro nuevoEstado,
            String motivo
    );

    RolResponseDto findById(
            AuthenticatedUserContext actor,
            Long idRol
    );

    PageResponseDto<RolResponseDto> findAll(
            AuthenticatedUserContext actor,
            RolFilterDto filter,
            PageRequestDto pageRequest
    );

    List<RolLookupDto> findLookup(AuthenticatedUserContext actor);
}