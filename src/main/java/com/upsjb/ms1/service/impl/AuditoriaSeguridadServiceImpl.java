package com.upsjb.ms1.service.impl;

import com.upsjb.ms1.domain.entity.AuditoriaSeguridad;
import com.upsjb.ms1.domain.entity.LoginAttempt;
import com.upsjb.ms1.domain.entity.Usuario;
import com.upsjb.ms1.domain.entity.UsuarioIpHistorial;
import com.upsjb.ms1.domain.enums.TipoAuditoriaSeguridad;
import com.upsjb.ms1.dto.auditoria.filter.AuditoriaSeguridadFilterDto;
import com.upsjb.ms1.dto.auditoria.filter.LoginAttemptFilterDto;
import com.upsjb.ms1.dto.auditoria.filter.UsuarioIpHistorialFilterDto;
import com.upsjb.ms1.dto.auditoria.response.AuditoriaSeguridadResponseDto;
import com.upsjb.ms1.dto.auditoria.response.LoginAttemptResponseDto;
import com.upsjb.ms1.dto.auditoria.response.UsuarioIpHistorialResponseDto;
import com.upsjb.ms1.dto.shared.PageRequestDto;
import com.upsjb.ms1.dto.shared.PageResponseDto;
import com.upsjb.ms1.mapper.AuditoriaSeguridadMapper;
import com.upsjb.ms1.mapper.LoginAttemptMapper;
import com.upsjb.ms1.mapper.UsuarioIpHistorialMapper;
import com.upsjb.ms1.policy.AuditoriaSeguridadPolicy;
import com.upsjb.ms1.repository.AuditoriaSeguridadRepository;
import com.upsjb.ms1.repository.LoginAttemptRepository;
import com.upsjb.ms1.repository.UsuarioIpHistorialRepository;
import com.upsjb.ms1.security.principal.AuthenticatedUserContext;
import com.upsjb.ms1.service.contract.AuditoriaSeguridadService;
import com.upsjb.ms1.shared.audit.AuditContextHolder;
import com.upsjb.ms1.shared.audit.AuditEventFactory;
import com.upsjb.ms1.shared.audit.AuditEventType;
import com.upsjb.ms1.shared.exception.BusinessException;
import com.upsjb.ms1.shared.exception.NotFoundException;
import com.upsjb.ms1.shared.exception.ValidationException;
import com.upsjb.ms1.shared.pagination.PaginationMapper;
import com.upsjb.ms1.shared.pagination.PaginationService;
import com.upsjb.ms1.specification.AuditoriaSeguridadSpecifications;
import com.upsjb.ms1.specification.LoginAttemptSpecifications;
import com.upsjb.ms1.specification.UsuarioIpHistorialSpecifications;
import com.upsjb.ms1.util.StringNormalizer;
import java.time.Instant;
import java.util.Set;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditoriaSeguridadServiceImpl implements AuditoriaSeguridadService {

    private static final Logger log = LoggerFactory.getLogger(AuditoriaSeguridadServiceImpl.class);

    private static final Set<String> AUDITORIA_SORT_FIELDS = Set.of(
            "eventAt",
            "tipo",
            "resultado",
            "requestId",
            "usernameActor",
            "usernameAfectado"
    );

    private static final Set<String> LOGIN_ATTEMPT_SORT_FIELDS = Set.of(
            "attemptedAt",
            "usernameOrEmail",
            "tipoLogin",
            "exitoso",
            "failureCode"
    );

    private static final Set<String> IP_HISTORY_SORT_FIELDS = Set.of(
            "primerUsoAt",
            "ultimoUsoAt",
            "cantidadUsos",
            "sospechosa",
            "bloqueada"
    );

    private final AuditoriaSeguridadRepository auditoriaSeguridadRepository;
    private final LoginAttemptRepository loginAttemptRepository;
    private final UsuarioIpHistorialRepository usuarioIpHistorialRepository;
    private final AuditoriaSeguridadMapper auditoriaSeguridadMapper;
    private final LoginAttemptMapper loginAttemptMapper;
    private final UsuarioIpHistorialMapper usuarioIpHistorialMapper;
    private final AuditoriaSeguridadPolicy auditoriaSeguridadPolicy;
    private final AuditEventFactory auditEventFactory;
    private final PaginationService paginationService;
    private final PaginationMapper paginationMapper;

    public AuditoriaSeguridadServiceImpl(
            AuditoriaSeguridadRepository auditoriaSeguridadRepository,
            LoginAttemptRepository loginAttemptRepository,
            UsuarioIpHistorialRepository usuarioIpHistorialRepository,
            AuditoriaSeguridadMapper auditoriaSeguridadMapper,
            LoginAttemptMapper loginAttemptMapper,
            UsuarioIpHistorialMapper usuarioIpHistorialMapper,
            AuditoriaSeguridadPolicy auditoriaSeguridadPolicy,
            AuditEventFactory auditEventFactory,
            PaginationService paginationService,
            PaginationMapper paginationMapper
    ) {
        this.auditoriaSeguridadRepository = auditoriaSeguridadRepository;
        this.loginAttemptRepository = loginAttemptRepository;
        this.usuarioIpHistorialRepository = usuarioIpHistorialRepository;
        this.auditoriaSeguridadMapper = auditoriaSeguridadMapper;
        this.loginAttemptMapper = loginAttemptMapper;
        this.usuarioIpHistorialMapper = usuarioIpHistorialMapper;
        this.auditoriaSeguridadPolicy = auditoriaSeguridadPolicy;
        this.auditEventFactory = auditEventFactory;
        this.paginationService = paginationService;
        this.paginationMapper = paginationMapper;
    }

    @Override
    @Transactional
    public AuditoriaSeguridadResponseDto registerSuccess(
            TipoAuditoriaSeguridad tipo,
            Usuario actor,
            Usuario afectado,
            String descripcion
    ) {
        return registerSuccess(tipo, actor, afectado, descripcion, null);
    }

    @Override
    @Transactional
    public AuditoriaSeguridadResponseDto registerSuccess(
            TipoAuditoriaSeguridad tipo,
            Usuario actor,
            Usuario afectado,
            String descripcion,
            String metadataJson
    ) {
        return registerAuditEvent(
                AuditEventType.SUCCESS,
                tipo,
                actor,
                afectado,
                descripcion,
                metadataJson
        );
    }

    @Override
    @Transactional
    public AuditoriaSeguridadResponseDto registerFailure(
            TipoAuditoriaSeguridad tipo,
            Usuario actor,
            Usuario afectado,
            String descripcion
    ) {
        return registerFailure(tipo, actor, afectado, descripcion, null);
    }

    @Override
    @Transactional
    public AuditoriaSeguridadResponseDto registerFailure(
            TipoAuditoriaSeguridad tipo,
            Usuario actor,
            Usuario afectado,
            String descripcion,
            String metadataJson
    ) {
        return registerAuditEvent(
                AuditEventType.FAILURE,
                tipo,
                actor,
                afectado,
                descripcion,
                metadataJson
        );
    }

    @Override
    @Transactional
    public AuditoriaSeguridadResponseDto registerWarning(
            TipoAuditoriaSeguridad tipo,
            Usuario actor,
            Usuario afectado,
            String descripcion
    ) {
        return registerWarning(tipo, actor, afectado, descripcion, null);
    }

    @Override
    @Transactional
    public AuditoriaSeguridadResponseDto registerWarning(
            TipoAuditoriaSeguridad tipo,
            Usuario actor,
            Usuario afectado,
            String descripcion,
            String metadataJson
    ) {
        return registerAuditEvent(
                AuditEventType.SECURITY_WARNING,
                tipo,
                actor,
                afectado,
                descripcion,
                metadataJson
        );
    }

    @Override
    @Transactional(readOnly = true)
    public AuditoriaSeguridadResponseDto findById(
            AuthenticatedUserContext actor,
            Long idAuditoria
    ) {
        return executeWithTechnicalLog(
                "findById",
                actorRef(actor),
                idAuditoria,
                () -> {
                    auditoriaSeguridadPolicy.ensureCanViewSecurityAuditDetail(actor);

                    if (idAuditoria == null) {
                        throw new ValidationException(
                                "AUDITORIA_ID_OBLIGATORIO",
                                "El identificador de auditoría es obligatorio."
                        );
                    }

                    AuditoriaSeguridad auditoria = auditoriaSeguridadRepository.findById(idAuditoria)
                            .orElseThrow(() -> new NotFoundException(
                                    "AUDITORIA_NO_ENCONTRADA",
                                    "No se encontró el evento de auditoría solicitado."
                            ));

                    return auditoriaSeguridadMapper.toResponse(auditoria);
                }
        );
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponseDto<AuditoriaSeguridadResponseDto> findSecurityAudit(
            AuthenticatedUserContext actor,
            AuditoriaSeguridadFilterDto filter,
            PageRequestDto pageRequest
    ) {
        return executeWithTechnicalLog(
                "findSecurityAudit",
                actorRef(actor),
                filter,
                () -> {
                    auditoriaSeguridadPolicy.ensureCanListSecurityAudit(actor);
                    validateDateRange(
                            filter == null ? null : filter.fechaDesde(),
                            filter == null ? null : filter.fechaHasta()
                    );

                    Pageable pageable = paginationService.toPageable(
                            pageRequest,
                            "eventAt",
                            AUDITORIA_SORT_FIELDS
                    );

                    Page<AuditoriaSeguridad> page = auditoriaSeguridadRepository.findAll(
                            AuditoriaSeguridadSpecifications.from(filter),
                            pageable
                    );

                    return paginationMapper.toPageResponse(page, auditoriaSeguridadMapper::toResponse);
                }
        );
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponseDto<LoginAttemptResponseDto> findLoginAttempts(
            AuthenticatedUserContext actor,
            LoginAttemptFilterDto filter,
            PageRequestDto pageRequest
    ) {
        return executeWithTechnicalLog(
                "findLoginAttempts",
                actorRef(actor),
                filter,
                () -> {
                    auditoriaSeguridadPolicy.ensureCanListLoginAttempts(actor);
                    validateDateRange(
                            filter == null ? null : filter.fechaDesde(),
                            filter == null ? null : filter.fechaHasta()
                    );

                    Pageable pageable = paginationService.toPageable(
                            pageRequest,
                            "attemptedAt",
                            LOGIN_ATTEMPT_SORT_FIELDS
                    );

                    Page<LoginAttempt> page = loginAttemptRepository.findAll(
                            LoginAttemptSpecifications.from(filter),
                            pageable
                    );

                    return paginationMapper.toPageResponse(page, loginAttemptMapper::toResponse);
                }
        );
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponseDto<UsuarioIpHistorialResponseDto> findIpHistory(
            AuthenticatedUserContext actor,
            UsuarioIpHistorialFilterDto filter,
            PageRequestDto pageRequest
    ) {
        return executeWithTechnicalLog(
                "findIpHistory",
                actorRef(actor),
                filter,
                () -> {
                    if (filter != null && filter.idUsuario() != null) {
                        auditoriaSeguridadPolicy.ensureCanViewOwnIpHistory(actor, filter.idUsuario());
                    } else {
                        auditoriaSeguridadPolicy.ensureCanListIpHistory(actor);
                    }

                    validateDateRange(
                            filter == null ? null : filter.fechaDesde(),
                            filter == null ? null : filter.fechaHasta()
                    );

                    Pageable pageable = paginationService.toPageable(
                            pageRequest,
                            "ultimoUsoAt",
                            IP_HISTORY_SORT_FIELDS
                    );

                    Page<UsuarioIpHistorial> page = usuarioIpHistorialRepository.findAll(
                            UsuarioIpHistorialSpecifications.from(filter),
                            pageable
                    );

                    return paginationMapper.toPageResponse(page, usuarioIpHistorialMapper::toResponse);
                }
        );
    }

    private AuditoriaSeguridadResponseDto registerAuditEvent(
            AuditEventType eventType,
            TipoAuditoriaSeguridad tipo,
            Usuario actor,
            Usuario afectado,
            String descripcion,
            String metadataJson
    ) {
        return executeWithTechnicalLog(
                "registerAuditEvent",
                usuarioRef(actor),
                tipo,
                () -> {
                    validateEvent(tipo, descripcion);

                    AuditoriaSeguridad evento = buildAuditEvent(
                            eventType,
                            tipo,
                            actor,
                            afectado,
                            descripcion,
                            StringNormalizer.trimToNull(metadataJson)
                    );

                    AuditoriaSeguridad saved = auditoriaSeguridadRepository.save(evento);
                    return auditoriaSeguridadMapper.toResponse(saved);
                }
        );
    }

    private AuditoriaSeguridad buildAuditEvent(
            AuditEventType eventType,
            TipoAuditoriaSeguridad tipo,
            Usuario actor,
            Usuario afectado,
            String descripcion,
            String metadataJson
    ) {
        return switch (eventType) {
            case SUCCESS -> auditEventFactory.success(
                    tipo,
                    actor,
                    afectado,
                    descripcion,
                    metadataJson
            );
            case FAILURE -> auditEventFactory.failure(
                    tipo,
                    actor,
                    afectado,
                    descripcion,
                    metadataJson
            );
            case SECURITY_WARNING -> auditEventFactory.warning(
                    tipo,
                    actor,
                    afectado,
                    descripcion,
                    metadataJson
            );
        };
    }

    private void validateEvent(
            TipoAuditoriaSeguridad tipo,
            String descripcion
    ) {
        if (tipo == null) {
            throw new ValidationException(
                    "AUDITORIA_TIPO_OBLIGATORIO",
                    "El tipo de auditoría es obligatorio."
            );
        }

        if (StringNormalizer.trimToNull(descripcion) == null) {
            throw new ValidationException(
                    "AUDITORIA_DESCRIPCION_OBLIGATORIA",
                    "La descripción de auditoría es obligatoria."
            );
        }
    }

    private void validateDateRange(
            Instant desde,
            Instant hasta
    ) {
        if (desde != null && hasta != null && hasta.isBefore(desde)) {
            throw new ValidationException(
                    "AUDITORIA_RANGO_FECHAS_INVALIDO",
                    "La fecha hasta no puede ser anterior a la fecha desde."
            );
        }
    }

    private <T> T executeWithTechnicalLog(
            String method,
            String actor,
            Object resource,
            Supplier<T> operation
    ) {
        try {
            return operation.get();
        } catch (BusinessException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            log.error(
                    "Error inesperado en AuditoriaSeguridadServiceImpl.{} - actor={}, recurso={}, requestId={}",
                    method,
                    actor,
                    resource,
                    AuditContextHolder.getRequestIdOrNull(),
                    exception
            );
            throw exception;
        }
    }

    private String actorRef(AuthenticatedUserContext actor) {
        if (actor == null || !actor.authenticated()) {
            return "ANONIMO";
        }

        String username = StringNormalizer.trimToNull(actor.username());
        return username == null ? "ID_" + actor.idUsuario() : username;
    }

    private String usuarioRef(Usuario usuario) {
        if (usuario == null) {
            return "ANONIMO";
        }

        if (usuario.getUsername() != null
                && StringNormalizer.trimToNull(usuario.getUsername().getValue()) != null) {
            return usuario.getUsername().getValue();
        }

        return usuario.getId() == null ? "USUARIO_SIN_ID" : "ID_" + usuario.getId();
    }
}