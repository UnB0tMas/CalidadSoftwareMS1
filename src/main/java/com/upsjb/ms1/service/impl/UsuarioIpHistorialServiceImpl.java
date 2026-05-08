// src/main/java/com/upsjb/ms1/service/impl/UsuarioIpHistorialServiceImpl.java
package com.upsjb.ms1.service.impl;

import com.upsjb.ms1.domain.entity.Usuario;
import com.upsjb.ms1.domain.entity.UsuarioIpHistorial;
import com.upsjb.ms1.domain.enums.TipoAuditoriaSeguridad;
import com.upsjb.ms1.domain.value.IpAddressValue;
import com.upsjb.ms1.domain.value.UserAgentValue;
import com.upsjb.ms1.dto.auditoria.filter.UsuarioIpHistorialFilterDto;
import com.upsjb.ms1.dto.auditoria.response.UsuarioIpHistorialResponseDto;
import com.upsjb.ms1.dto.shared.PageRequestDto;
import com.upsjb.ms1.dto.shared.PageResponseDto;
import com.upsjb.ms1.mapper.UsuarioIpHistorialMapper;
import com.upsjb.ms1.policy.AuditoriaSeguridadPolicy;
import com.upsjb.ms1.repository.UsuarioIpHistorialRepository;
import com.upsjb.ms1.repository.UsuarioRepository;
import com.upsjb.ms1.security.principal.AuthenticatedUserContext;
import com.upsjb.ms1.service.contract.AuditoriaSeguridadService;
import com.upsjb.ms1.service.contract.UsuarioIpHistorialService;
import com.upsjb.ms1.shared.audit.AuditContextHolder;
import com.upsjb.ms1.shared.exception.BusinessException;
import com.upsjb.ms1.shared.exception.NotFoundException;
import com.upsjb.ms1.shared.exception.ValidationException;
import com.upsjb.ms1.shared.pagination.PaginationMapper;
import com.upsjb.ms1.shared.pagination.PaginationService;
import com.upsjb.ms1.specification.UsuarioIpHistorialSpecifications;
import com.upsjb.ms1.util.StringNormalizer;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UsuarioIpHistorialServiceImpl implements UsuarioIpHistorialService {

    private static final Logger log = LoggerFactory.getLogger(UsuarioIpHistorialServiceImpl.class);

    private static final String MSG_IP_NUEVA_REGISTRADA = "Nueva IP registrada correctamente.";
    private static final String MSG_USO_IP_ACTUALIZADO = "Uso de IP registrado correctamente.";
    private static final String MSG_HISTORIAL_CONSULTADO = "Historial de IP consultado correctamente.";
    private static final String MSG_IP_SOSPECHOSA = "IP marcada como sospechosa correctamente.";
    private static final String MSG_SOSPECHA_LIMPIADA = "Sospecha de IP limpiada correctamente.";
    private static final String MSG_IP_BLOQUEADA = "IP bloqueada correctamente.";
    private static final String MSG_IP_DESBLOQUEADA = "IP desbloqueada correctamente.";

    private static final Set<String> SORT_FIELDS = Set.of(
            "id",
            "primerUsoAt",
            "ultimoUsoAt",
            "cantidadUsos",
            "sospechosa",
            "bloqueada"
    );

    private final UsuarioIpHistorialRepository usuarioIpHistorialRepository;
    private final UsuarioRepository usuarioRepository;
    private final UsuarioIpHistorialMapper usuarioIpHistorialMapper;
    private final AuditoriaSeguridadPolicy auditoriaSeguridadPolicy;
    private final AuditoriaSeguridadService auditoriaSeguridadService;
    private final PaginationService paginationService;
    private final PaginationMapper paginationMapper;
    private final Clock clock;

    public UsuarioIpHistorialServiceImpl(
            UsuarioIpHistorialRepository usuarioIpHistorialRepository,
            UsuarioRepository usuarioRepository,
            UsuarioIpHistorialMapper usuarioIpHistorialMapper,
            AuditoriaSeguridadPolicy auditoriaSeguridadPolicy,
            AuditoriaSeguridadService auditoriaSeguridadService,
            PaginationService paginationService,
            PaginationMapper paginationMapper,
            Clock clock
    ) {
        this.usuarioIpHistorialRepository = usuarioIpHistorialRepository;
        this.usuarioRepository = usuarioRepository;
        this.usuarioIpHistorialMapper = usuarioIpHistorialMapper;
        this.auditoriaSeguridadPolicy = auditoriaSeguridadPolicy;
        this.auditoriaSeguridadService = auditoriaSeguridadService;
        this.paginationService = paginationService;
        this.paginationMapper = paginationMapper;
        this.clock = clock;
    }

    @Override
    @Transactional
    public UsuarioIpHistorialResponseDto registerUsage(
            Usuario usuario,
            String ipAddress,
            String userAgent
    ) {
        return executeWithTechnicalLog(
                "registerUsage",
                usernameOf(usuario),
                usuario == null ? null : usuario.getId(),
                () -> {
                    validateUsuario(usuario);
                    String normalizedIp = requireIpAddress(ipAddress);

                    Instant now = Instant.now(clock);

                    Optional<UsuarioIpHistorial> existingHistory = usuarioIpHistorialRepository
                            .findByUsuario_IdAndIpAddress_Value(usuario.getId(), normalizedIp);

                    boolean nuevoHistorial = existingHistory.isEmpty();

                    UsuarioIpHistorial historial = existingHistory
                            .map(existing -> {
                                existing.registrarUso(UserAgentValue.of(userAgent), now);
                                return existing;
                            })
                            .orElseGet(() -> createNewHistory(usuario, normalizedIp, userAgent, now));

                    UsuarioIpHistorial saved = usuarioIpHistorialRepository.save(historial);

                    if (nuevoHistorial) {
                        auditoriaSeguridadService.registerWarning(
                                TipoAuditoriaSeguridad.IP_NUEVA,
                                usuario,
                                usuario,
                                "Nueva IP registrada para el usuario. IP: " + normalizedIp + "."
                        );
                    }

                    if (saved.isBloqueada()) {
                        auditoriaSeguridadService.registerWarning(
                                TipoAuditoriaSeguridad.IP_SOSPECHOSA,
                                usuario,
                                usuario,
                                "Uso detectado desde una IP bloqueada. IP: " + normalizedIp + "."
                        );
                    }

                    return usuarioIpHistorialMapper.toResponse(
                            saved,
                            nuevoHistorial ? MSG_IP_NUEVA_REGISTRADA : MSG_USO_IP_ACTUALIZADO
                    );
                }
        );
    }

    @Override
    @Transactional(readOnly = true)
    public UsuarioIpHistorialResponseDto findById(
            AuthenticatedUserContext actor,
            Long idUsuarioIpHistorial
    ) {
        return executeWithTechnicalLog(
                "findById",
                actorRef(actor),
                idUsuarioIpHistorial,
                () -> {
                    UsuarioIpHistorial historial = requireById(idUsuarioIpHistorial);
                    Long idUsuario = historial.getUsuario() == null ? null : historial.getUsuario().getId();

                    auditoriaSeguridadPolicy.ensureCanViewOwnIpHistory(actor, idUsuario);

                    return usuarioIpHistorialMapper.toResponse(historial, MSG_HISTORIAL_CONSULTADO);
                }
        );
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponseDto<UsuarioIpHistorialResponseDto> findAll(
            AuthenticatedUserContext actor,
            UsuarioIpHistorialFilterDto filter,
            PageRequestDto pageRequest
    ) {
        return executeWithTechnicalLog(
                "findAll",
                actorRef(actor),
                filter == null ? null : filter.idUsuario(),
                () -> {
                    if (filter != null && filter.idUsuario() != null) {
                        validateUsuarioIdExists(filter.idUsuario());
                        auditoriaSeguridadPolicy.ensureCanViewOwnIpHistory(actor, filter.idUsuario());
                    } else {
                        auditoriaSeguridadPolicy.ensureCanListIpHistory(actor);
                    }

                    validateDateRange(
                            filter == null ? null : filter.fechaDesde(),
                            filter == null ? null : filter.fechaHasta()
                    );

                    Pageable pageable = paginationService.toPageable(pageRequest, "ultimoUsoAt", SORT_FIELDS);

                    Page<UsuarioIpHistorial> page = usuarioIpHistorialRepository.findAll(
                            UsuarioIpHistorialSpecifications.from(filter),
                            pageable
                    );

                    return paginationMapper.toPageResponse(page, usuarioIpHistorialMapper::toResponse);
                }
        );
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponseDto<UsuarioIpHistorialResponseDto> findByUsuario(
            AuthenticatedUserContext actor,
            Long idUsuario,
            PageRequestDto pageRequest
    ) {
        return executeWithTechnicalLog(
                "findByUsuario",
                actorRef(actor),
                idUsuario,
                () -> {
                    validateUsuarioIdExists(idUsuario);
                    auditoriaSeguridadPolicy.ensureCanViewOwnIpHistory(actor, idUsuario);

                    Pageable pageable = paginationService.toPageable(pageRequest, "ultimoUsoAt", SORT_FIELDS);

                    Page<UsuarioIpHistorial> page = usuarioIpHistorialRepository.findByUsuario_Id(idUsuario, pageable);

                    return paginationMapper.toPageResponse(page, usuarioIpHistorialMapper::toResponse);
                }
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<UsuarioIpHistorialResponseDto> findRecentByUsuario(
            AuthenticatedUserContext actor,
            Long idUsuario
    ) {
        return executeWithTechnicalLog(
                "findRecentByUsuario",
                actorRef(actor),
                idUsuario,
                () -> {
                    validateUsuarioIdExists(idUsuario);
                    auditoriaSeguridadPolicy.ensureCanViewOwnIpHistory(actor, idUsuario);

                    return usuarioIpHistorialRepository.findTop10ByUsuario_IdOrderByUltimoUsoAtDesc(idUsuario)
                            .stream()
                            .map(historial -> usuarioIpHistorialMapper.toResponse(historial, MSG_HISTORIAL_CONSULTADO))
                            .toList();
                }
        );
    }

    @Override
    @Transactional
    public UsuarioIpHistorialResponseDto markSuspicious(
            AuthenticatedUserContext actor,
            Long idUsuarioIpHistorial,
            String motivo
    ) {
        return executeWithTechnicalLog(
                "markSuspicious",
                actorRef(actor),
                idUsuarioIpHistorial,
                () -> {
                    auditoriaSeguridadPolicy.ensureCanListIpHistory(actor);

                    UsuarioIpHistorial historial = requireById(idUsuarioIpHistorial);
                    String normalizedReason = normalizeReason(
                            motivo,
                            "Marcada como sospechosa por revisión administrativa."
                    );

                    historial.marcarSospechosa(normalizedReason);

                    UsuarioIpHistorial saved = usuarioIpHistorialRepository.save(historial);

                    auditoriaSeguridadService.registerWarning(
                            TipoAuditoriaSeguridad.IP_SOSPECHOSA,
                            resolveActor(actor),
                            saved.getUsuario(),
                            "IP marcada como sospechosa. Motivo: " + normalizedReason
                    );

                    return usuarioIpHistorialMapper.toResponse(saved, MSG_IP_SOSPECHOSA);
                }
        );
    }

    @Override
    @Transactional
    public UsuarioIpHistorialResponseDto clearSuspicion(
            AuthenticatedUserContext actor,
            Long idUsuarioIpHistorial
    ) {
        return executeWithTechnicalLog(
                "clearSuspicion",
                actorRef(actor),
                idUsuarioIpHistorial,
                () -> {
                    auditoriaSeguridadPolicy.ensureCanListIpHistory(actor);

                    UsuarioIpHistorial historial = requireById(idUsuarioIpHistorial);

                    if (historial.isBloqueada()) {
                        throw new ValidationException(
                                "IP_HISTORIAL_BLOQUEADA_NO_LIMPIABLE",
                                "No se puede limpiar la sospecha de una IP bloqueada. Primero desbloquea la IP."
                        );
                    }

                    if (!historial.isSospechosa()) {
                        return usuarioIpHistorialMapper.toResponse(
                                historial,
                                "La IP no tenía sospecha activa."
                        );
                    }

                    historial.limpiarSospecha();

                    UsuarioIpHistorial saved = usuarioIpHistorialRepository.save(historial);

                    auditoriaSeguridadService.registerSuccess(
                            TipoAuditoriaSeguridad.IP_SOSPECHOSA,
                            resolveActor(actor),
                            saved.getUsuario(),
                            "Sospecha de IP limpiada por revisión administrativa."
                    );

                    return usuarioIpHistorialMapper.toResponse(saved, MSG_SOSPECHA_LIMPIADA);
                }
        );
    }

    @Override
    @Transactional
    public UsuarioIpHistorialResponseDto blockIp(
            AuthenticatedUserContext actor,
            Long idUsuarioIpHistorial,
            String motivo
    ) {
        return executeWithTechnicalLog(
                "blockIp",
                actorRef(actor),
                idUsuarioIpHistorial,
                () -> {
                    auditoriaSeguridadPolicy.ensureCanListIpHistory(actor);

                    UsuarioIpHistorial historial = requireById(idUsuarioIpHistorial);
                    String normalizedReason = normalizeReason(
                            motivo,
                            "IP bloqueada por revisión administrativa."
                    );

                    historial.bloquear();
                    historial.marcarSospechosa(normalizedReason);

                    UsuarioIpHistorial saved = usuarioIpHistorialRepository.save(historial);

                    auditoriaSeguridadService.registerWarning(
                            TipoAuditoriaSeguridad.IP_SOSPECHOSA,
                            resolveActor(actor),
                            saved.getUsuario(),
                            "IP bloqueada. Motivo: " + normalizedReason
                    );

                    return usuarioIpHistorialMapper.toResponse(saved, MSG_IP_BLOQUEADA);
                }
        );
    }

    @Override
    @Transactional
    public UsuarioIpHistorialResponseDto unblockIp(
            AuthenticatedUserContext actor,
            Long idUsuarioIpHistorial
    ) {
        return executeWithTechnicalLog(
                "unblockIp",
                actorRef(actor),
                idUsuarioIpHistorial,
                () -> {
                    auditoriaSeguridadPolicy.ensureCanListIpHistory(actor);

                    UsuarioIpHistorial historial = requireById(idUsuarioIpHistorial);

                    if (!historial.isBloqueada()) {
                        return usuarioIpHistorialMapper.toResponse(
                                historial,
                                "La IP no estaba bloqueada."
                        );
                    }

                    historial.desbloquear();

                    UsuarioIpHistorial saved = usuarioIpHistorialRepository.save(historial);

                    auditoriaSeguridadService.registerSuccess(
                            TipoAuditoriaSeguridad.IP_SOSPECHOSA,
                            resolveActor(actor),
                            saved.getUsuario(),
                            "IP desbloqueada por revisión administrativa."
                    );

                    return usuarioIpHistorialMapper.toResponse(saved, MSG_IP_DESBLOQUEADA);
                }
        );
    }

    private UsuarioIpHistorial createNewHistory(
            Usuario usuario,
            String ipAddress,
            String userAgent,
            Instant now
    ) {
        return UsuarioIpHistorial.builder()
                .usuario(usuario)
                .ipAddress(IpAddressValue.of(ipAddress))
                .ultimoUserAgent(UserAgentValue.of(userAgent))
                .primerUsoAt(now)
                .ultimoUsoAt(now)
                .cantidadUsos(1)
                .sospechosa(false)
                .bloqueada(false)
                .build();
    }

    private UsuarioIpHistorial requireById(Long idUsuarioIpHistorial) {
        if (idUsuarioIpHistorial == null) {
            throw new ValidationException(
                    "IP_HISTORIAL_ID_OBLIGATORIO",
                    "El identificador del historial de IP es obligatorio."
            );
        }

        return usuarioIpHistorialRepository.findById(idUsuarioIpHistorial)
                .orElseThrow(() -> new NotFoundException(
                        "IP_HISTORIAL_NO_ENCONTRADO",
                        "No se encontró el historial de IP solicitado."
                ));
    }

    private void validateUsuario(Usuario usuario) {
        if (usuario == null || usuario.getId() == null) {
            throw new ValidationException(
                    "IP_HISTORIAL_USUARIO_OBLIGATORIO",
                    "El usuario es obligatorio para registrar historial de IP."
            );
        }

        if (!usuarioRepository.existsById(usuario.getId())) {
            throw new NotFoundException(
                    "USUARIO_NO_ENCONTRADO",
                    "No se encontró el usuario asociado al historial de IP."
            );
        }
    }

    private void validateUsuarioIdExists(Long idUsuario) {
        if (idUsuario == null) {
            throw new ValidationException(
                    "IP_HISTORIAL_USUARIO_ID_OBLIGATORIO",
                    "El usuario es obligatorio para consultar historial de IP."
            );
        }

        if (!usuarioRepository.existsById(idUsuario)) {
            throw new NotFoundException(
                    "USUARIO_NO_ENCONTRADO",
                    "No se encontró el usuario asociado al historial de IP."
            );
        }
    }

    private String requireIpAddress(String ipAddress) {
        String normalized = StringNormalizer.trimToNull(ipAddress);

        if (normalized == null) {
            throw new ValidationException(
                    "IP_HISTORIAL_IP_OBLIGATORIA",
                    "La dirección IP es obligatoria."
            );
        }

        try {
            return IpAddressValue.of(normalized).getValue();
        } catch (IllegalArgumentException exception) {
            throw new ValidationException(
                    "IP_HISTORIAL_IP_INVALIDA",
                    exception.getMessage()
            );
        }
    }

    private void validateDateRange(
            Instant desde,
            Instant hasta
    ) {
        if (desde != null && hasta != null && hasta.isBefore(desde)) {
            throw new ValidationException(
                    "IP_HISTORIAL_RANGO_FECHAS_INVALIDO",
                    "La fecha hasta no puede ser anterior a la fecha desde."
            );
        }
    }

    private String normalizeReason(
            String motivo,
            String defaultValue
    ) {
        String normalized = StringNormalizer.normalizeSpaces(motivo);

        if (normalized == null) {
            return defaultValue;
        }

        return normalized.length() <= 250 ? normalized : normalized.substring(0, 250);
    }

    private Usuario resolveActor(AuthenticatedUserContext actor) {
        if (actor == null || actor.idUsuario() == null) {
            return null;
        }

        return usuarioRepository.findById(actor.idUsuario()).orElse(null);
    }

    private String usernameOf(Usuario usuario) {
        return usuario == null || usuario.getUsername() == null ? "USUARIO_DESCONOCIDO" : usuario.getUsername().getValue();
    }

    private String actorRef(AuthenticatedUserContext actor) {
        if (actor == null || actor.username() == null) {
            return "ANONIMO";
        }

        return actor.username();
    }

    private <T> T executeWithTechnicalLog(
            String method,
            String actor,
            Long idRecurso,
            Supplier<T> action
    ) {
        try {
            return action.get();
        } catch (BusinessException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            log.error(
                    "Error inesperado en UsuarioIpHistorialServiceImpl.{} - actor={}, idRecurso={}, requestId={}",
                    method,
                    actor,
                    idRecurso,
                    requestId(),
                    exception
            );
            throw exception;
        }
    }

    private String requestId() {
        return AuditContextHolder.getRequestIdOrNull();
    }
}