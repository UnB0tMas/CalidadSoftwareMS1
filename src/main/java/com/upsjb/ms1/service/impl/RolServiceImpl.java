package com.upsjb.ms1.service.impl;

import com.upsjb.ms1.domain.entity.Rol;
import com.upsjb.ms1.domain.enums.EstadoRegistro;
import com.upsjb.ms1.domain.enums.TipoAuditoriaSeguridad;
import com.upsjb.ms1.dto.rol.filter.RolFilterDto;
import com.upsjb.ms1.dto.rol.request.RolCreateRequestDto;
import com.upsjb.ms1.dto.rol.request.RolUpdateRequestDto;
import com.upsjb.ms1.dto.rol.response.RolLookupDto;
import com.upsjb.ms1.dto.rol.response.RolResponseDto;
import com.upsjb.ms1.dto.shared.PageRequestDto;
import com.upsjb.ms1.dto.shared.PageResponseDto;
import com.upsjb.ms1.mapper.RolMapper;
import com.upsjb.ms1.policy.RolPolicy;
import com.upsjb.ms1.repository.RolRepository;
import com.upsjb.ms1.security.principal.AuthenticatedUserContext;
import com.upsjb.ms1.service.contract.AuditoriaSeguridadService;
import com.upsjb.ms1.service.contract.RolService;
import com.upsjb.ms1.shared.exception.ValidationException;
import com.upsjb.ms1.shared.pagination.PaginationMapper;
import com.upsjb.ms1.shared.pagination.PaginationService;
import com.upsjb.ms1.specification.RolSpecifications;
import com.upsjb.ms1.validator.RolValidator;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RolServiceImpl implements RolService {

    private static final Set<String> SORT_FIELDS = Set.of(
            "id",
            "codigo",
            "nombre",
            "estado",
            "rolSistema",
            "createdAt",
            "updatedAt"
    );

    private final RolRepository rolRepository;
    private final RolMapper rolMapper;
    private final RolValidator rolValidator;
    private final RolPolicy rolPolicy;
    private final AuditoriaSeguridadService auditoriaSeguridadService;
    private final PaginationService paginationService;
    private final PaginationMapper paginationMapper;
    private final Clock clock;

    public RolServiceImpl(
            RolRepository rolRepository,
            RolMapper rolMapper,
            RolValidator rolValidator,
            RolPolicy rolPolicy,
            AuditoriaSeguridadService auditoriaSeguridadService,
            PaginationService paginationService,
            PaginationMapper paginationMapper,
            Clock clock
    ) {
        this.rolRepository = rolRepository;
        this.rolMapper = rolMapper;
        this.rolValidator = rolValidator;
        this.rolPolicy = rolPolicy;
        this.auditoriaSeguridadService = auditoriaSeguridadService;
        this.paginationService = paginationService;
        this.paginationMapper = paginationMapper;
        this.clock = clock;
    }

    @Override
    @Transactional
    public RolResponseDto create(
            AuthenticatedUserContext actor,
            RolCreateRequestDto request
    ) {
        rolPolicy.ensureCanCreate(actor);

        if (request == null) {
            throw new ValidationException(
                    "ROL_CREATE_REQUEST_OBLIGATORIO",
                    "La solicitud de creación de rol es obligatoria."
            );
        }

        rolValidator.validateCreate(request.codigo(), request.nombre());

        Rol rol = rolMapper.toEntity(request);
        Rol saved = rolRepository.save(rol);

        auditoriaSeguridadService.registerSuccess(
                TipoAuditoriaSeguridad.ROL_CREADO,
                null,
                null,
                "Rol creado: " + saved.getCodigo() + "."
        );

        return rolMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public RolResponseDto update(
            AuthenticatedUserContext actor,
            Long idRol,
            RolUpdateRequestDto request
    ) {
        if (request == null) {
            throw new ValidationException(
                    "ROL_UPDATE_REQUEST_OBLIGATORIO",
                    "La solicitud de actualización de rol es obligatoria."
            );
        }

        Rol rol = rolValidator.requireById(idRol);

        rolPolicy.ensureCanUpdate(actor, rol);
        rolValidator.validateUpdate(idRol, request.nombre());

        rolMapper.applyUpdate(rol, request);

        Rol saved = rolRepository.save(rol);

        auditoriaSeguridadService.registerSuccess(
                TipoAuditoriaSeguridad.ROL_ACTUALIZADO,
                null,
                null,
                "Rol actualizado: " + saved.getCodigo() + "."
        );

        return rolMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public RolResponseDto changeEstado(
            AuthenticatedUserContext actor,
            Long idRol,
            EstadoRegistro nuevoEstado,
            String motivo
    ) {
        Rol rol = rolValidator.requireById(idRol);

        rolPolicy.ensureCanChangeEstado(actor, rol);
        validateEstadoPermitido(nuevoEstado);

        if (nuevoEstado.equals(rol.getEstado())) {
            return rolMapper.toResponse(rol);
        }

        TipoAuditoriaSeguridad tipoAuditoria = applyEstadoChange(actor, rol, nuevoEstado);

        Rol saved = rolRepository.save(rol);

        auditoriaSeguridadService.registerSuccess(
                tipoAuditoria,
                null,
                null,
                buildEstadoAuditMessage(saved, nuevoEstado, motivo)
        );

        return rolMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public RolResponseDto findById(
            AuthenticatedUserContext actor,
            Long idRol
    ) {
        rolPolicy.ensureCanFindById(actor);

        Rol rol = rolValidator.requireById(idRol);

        return rolMapper.toResponse(rol);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponseDto<RolResponseDto> findAll(
            AuthenticatedUserContext actor,
            RolFilterDto filter,
            PageRequestDto pageRequest
    ) {
        rolPolicy.ensureCanList(actor);

        Pageable pageable = paginationService.toPageable(pageRequest, "nombre", SORT_FIELDS);

        Page<Rol> page = rolRepository.findAll(
                RolSpecifications.from(filter),
                pageable
        );

        return paginationMapper.toPageResponse(page, rolMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RolLookupDto> findLookup(AuthenticatedUserContext actor) {
        rolPolicy.ensureCanLookup(actor);

        return rolRepository.findByEstadoOrderByNombreAsc(EstadoRegistro.ACTIVO)
                .stream()
                .map(rolMapper::toLookup)
                .toList();
    }

    private TipoAuditoriaSeguridad applyEstadoChange(
            AuthenticatedUserContext actor,
            Rol rol,
            EstadoRegistro nuevoEstado
    ) {
        return switch (nuevoEstado) {
            case ACTIVO -> {
                rol.activar();
                yield TipoAuditoriaSeguridad.ROL_ACTIVADO;
            }
            case INACTIVO -> {
                rolValidator.validatePuedeInactivar(rol);
                rol.inactivar();
                yield TipoAuditoriaSeguridad.ROL_INACTIVADO;
            }
            case ELIMINADO -> {
                rolPolicy.ensureCanDelete(actor, rol);
                rolValidator.validateEditable(rol);
                rol.eliminarLogicamente(Instant.now(clock));
                yield TipoAuditoriaSeguridad.ROL_ELIMINADO;
            }
            case BLOQUEADO -> throw new ValidationException(
                    "ROL_ESTADO_NO_SOPORTADO",
                    "El estado BLOQUEADO no aplica para roles."
            );
        };
    }

    private void validateEstadoPermitido(EstadoRegistro nuevoEstado) {
        if (nuevoEstado == null) {
            throw new ValidationException(
                    "ROL_ESTADO_OBLIGATORIO",
                    "El nuevo estado del rol es obligatorio."
            );
        }
    }

    private String buildEstadoAuditMessage(
            Rol rol,
            EstadoRegistro nuevoEstado,
            String motivo
    ) {
        String message = "Rol " + rol.getCodigo() + " cambió a estado " + nuevoEstado + ".";

        if (motivo == null || motivo.isBlank()) {
            return message;
        }

        return message + " Motivo: " + motivo.trim();
    }
}