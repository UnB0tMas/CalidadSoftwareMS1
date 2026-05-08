package com.upsjb.ms1.mapper;

import com.upsjb.ms1.domain.entity.Rol;
import com.upsjb.ms1.domain.enums.EstadoRegistro;
import com.upsjb.ms1.dto.rol.request.RolCreateRequestDto;
import com.upsjb.ms1.dto.rol.request.RolUpdateRequestDto;
import com.upsjb.ms1.dto.rol.response.RolLookupDto;
import com.upsjb.ms1.dto.rol.response.RolResponseDto;
import com.upsjb.ms1.util.StringNormalizer;
import org.springframework.stereotype.Component;

@Component
public class RolMapper {

    public RolResponseDto toResponse(Rol rol) {
        if (rol == null) {
            return null;
        }

        return new RolResponseDto(
                rol.getId(),
                rol.getCodigo(),
                rol.getNombre(),
                rol.getDescripcion(),
                rol.getEstado(),
                rol.isRolSistema(),
                rol.getCreatedAt(),
                rol.getUpdatedAt()
        );
    }

    public RolLookupDto toLookup(Rol rol) {
        if (rol == null) {
            return null;
        }

        return new RolLookupDto(
                rol.getId(),
                rol.getCodigo(),
                rol.getNombre()
        );
    }

    public Rol toEntity(RolCreateRequestDto request) {
        if (request == null) {
            return null;
        }

        return Rol.builder()
                .codigo(StringNormalizer.upper(request.codigo()))
                .nombre(StringNormalizer.normalizeSpaces(request.nombre()))
                .descripcion(StringNormalizer.normalizeSpaces(request.descripcion()))
                .estado(EstadoRegistro.ACTIVO)
                .rolSistema(false)
                .build();
    }

    public void applyUpdate(Rol rol, RolUpdateRequestDto request) {
        if (rol == null || request == null) {
            return;
        }

        if (request.nombre() != null) {
            rol.setNombre(StringNormalizer.normalizeSpaces(request.nombre()));
        }

        if (request.descripcion() != null) {
            rol.setDescripcion(StringNormalizer.normalizeSpaces(request.descripcion()));
        }
    }
}