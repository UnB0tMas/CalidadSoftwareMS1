// src/main/java/com/upsjb/ms1/service/impl/UsuarioServiceImpl.java
package com.upsjb.ms1.service.impl;

import com.upsjb.ms1.config.AppPropertiesConfig;
import com.upsjb.ms1.domain.entity.Rol;
import com.upsjb.ms1.domain.entity.Usuario;
import com.upsjb.ms1.domain.enums.EstadoRegistro;
import com.upsjb.ms1.domain.enums.TipoAuditoriaSeguridad;
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
import com.upsjb.ms1.mapper.UsuarioMapper;
import com.upsjb.ms1.policy.UsuarioPolicy;
import com.upsjb.ms1.repository.UsuarioRepository;
import com.upsjb.ms1.security.principal.AuthenticatedUserContext;
import com.upsjb.ms1.service.contract.AuditoriaSeguridadService;
import com.upsjb.ms1.service.contract.EmailService;
import com.upsjb.ms1.service.contract.SesionService;
import com.upsjb.ms1.service.contract.UsuarioService;
import com.upsjb.ms1.shared.audit.AuditContextHolder;
import com.upsjb.ms1.shared.exception.BusinessException;
import com.upsjb.ms1.shared.exception.ValidationException;
import com.upsjb.ms1.shared.pagination.PaginationMapper;
import com.upsjb.ms1.shared.pagination.PaginationService;
import com.upsjb.ms1.specification.UsuarioSpecifications;
import com.upsjb.ms1.util.StringNormalizer;
import com.upsjb.ms1.validator.RolValidator;
import com.upsjb.ms1.validator.UsuarioValidator;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class UsuarioServiceImpl implements UsuarioService {

    private static final Logger log = LoggerFactory.getLogger(UsuarioServiceImpl.class);

    private static final Set<String> SORT_FIELDS = Set.of(
            "id",
            "username",
            "email",
            "nombres",
            "apellidos",
            "estado",
            "emailVerificado",
            "requiereCambioPassword",
            "ultimoLoginAt",
            "ultimoCambioPasswordAt",
            "createdAt",
            "updatedAt"
    );

    private final UsuarioRepository usuarioRepository;
    private final UsuarioMapper usuarioMapper;
    private final UsuarioValidator usuarioValidator;
    private final RolValidator rolValidator;
    private final UsuarioPolicy usuarioPolicy;
    private final PasswordEncoder passwordEncoder;
    private final AuditoriaSeguridadService auditoriaSeguridadService;
    private final SesionService sesionService;
    private final EmailService emailService;
    private final PaginationService paginationService;
    private final PaginationMapper paginationMapper;
    private final AppPropertiesConfig appProperties;
    private final Clock clock;

    public UsuarioServiceImpl(
            UsuarioRepository usuarioRepository,
            UsuarioMapper usuarioMapper,
            UsuarioValidator usuarioValidator,
            RolValidator rolValidator,
            UsuarioPolicy usuarioPolicy,
            PasswordEncoder passwordEncoder,
            AuditoriaSeguridadService auditoriaSeguridadService,
            SesionService sesionService,
            EmailService emailService,
            PaginationService paginationService,
            PaginationMapper paginationMapper,
            AppPropertiesConfig appProperties,
            Clock clock
    ) {
        this.usuarioRepository = usuarioRepository;
        this.usuarioMapper = usuarioMapper;
        this.usuarioValidator = usuarioValidator;
        this.rolValidator = rolValidator;
        this.usuarioPolicy = usuarioPolicy;
        this.passwordEncoder = passwordEncoder;
        this.auditoriaSeguridadService = auditoriaSeguridadService;
        this.sesionService = sesionService;
        this.emailService = emailService;
        this.paginationService = paginationService;
        this.paginationMapper = paginationMapper;
        this.appProperties = appProperties;
        this.clock = clock;
    }

    @Override
    @Transactional
    public UsuarioResponseDto create(
            AuthenticatedUserContext actor,
            UsuarioCreateRequestDto request
    ) {
        return executeWithTechnicalLog("create", actor, null, () -> {
            usuarioPolicy.ensureCanCreate(actor);
            validateCreateRequest(request);

            usuarioValidator.validateCreate(
                    request.username(),
                    request.email(),
                    request.idRol(),
                    request.password(),
                    request.password(),
                    request.nombres(),
                    request.apellidos()
            );

            Rol rol = rolValidator.requireActivoById(request.idRol());
            String passwordHash = passwordEncoder.encode(request.password());

            Usuario usuario = usuarioMapper.toEntity(request, rol, passwordHash);
            Usuario saved = usuarioRepository.save(usuario);

            auditoriaSeguridadService.registerSuccess(
                    TipoAuditoriaSeguridad.USUARIO_CREADO,
                    resolveActor(actor),
                    saved,
                    "Usuario creado: " + usernameOf(saved) + "."
            );

            sendWelcomeAfterCommit(saved);

            return usuarioMapper.toResponse(saved);
        });
    }

    @Override
    @Transactional
    public UsuarioResponseDto update(
            AuthenticatedUserContext actor,
            Long idUsuario,
            UsuarioUpdateRequestDto request
    ) {
        return executeWithTechnicalLog("update", actor, idUsuario, () -> {
            if (request == null) {
                throw new ValidationException(
                        "USUARIO_UPDATE_REQUEST_OBLIGATORIO",
                        "La solicitud de actualización de usuario es obligatoria."
                );
            }

            usuarioPolicy.ensureCanUpdate(actor, idUsuario);

            Usuario usuario = usuarioValidator.requireWithRolById(idUsuario);
            usuarioValidator.validateUpdate(
                    usuario,
                    request.email(),
                    request.nombres(),
                    request.apellidos()
            );

            String emailAnterior = emailOf(usuario);

            usuarioMapper.applyUpdate(usuario, request);

            String emailNuevo = emailOf(usuario);
            if (hasChanged(emailAnterior, emailNuevo)) {
                usuario.setEmailVerificado(false);
            }

            Usuario saved = usuarioRepository.save(usuario);

            auditoriaSeguridadService.registerSuccess(
                    TipoAuditoriaSeguridad.USUARIO_ACTUALIZADO,
                    resolveActor(actor),
                    saved,
                    "Usuario actualizado: " + usernameOf(saved) + "."
            );

            return usuarioMapper.toResponse(saved);
        });
    }

    @Override
    @Transactional
    public UsuarioResponseDto changeEstado(
            AuthenticatedUserContext actor,
            Long idUsuario,
            UsuarioChangeEstadoRequestDto request
    ) {
        return executeWithTechnicalLog("changeEstado", actor, idUsuario, () -> {
            if (request == null) {
                throw new ValidationException(
                        "USUARIO_ESTADO_REQUEST_OBLIGATORIO",
                        "La solicitud de cambio de estado es obligatoria."
                );
            }

            Usuario usuario = usuarioValidator.requireWithRolById(idUsuario);
            EstadoRegistro nuevoEstado = request.estado();

            applyPolicyForEstado(actor, usuario, nuevoEstado);
            usuarioValidator.validateCambioEstado(usuario, nuevoEstado);

            if (nuevoEstado.equals(usuario.getEstado())) {
                return usuarioMapper.toResponse(usuario);
            }

            TipoAuditoriaSeguridad tipoAuditoria = applyEstado(usuario, nuevoEstado);
            Usuario saved = usuarioRepository.save(usuario);

            auditoriaSeguridadService.registerSuccess(
                    tipoAuditoria,
                    resolveActor(actor),
                    saved,
                    buildEstadoMessage(saved, nuevoEstado, request.motivo())
            );

            revokeSessionsIfEstadoInvalidatesAuthentication(actor, saved, nuevoEstado, request.motivo());

            return usuarioMapper.toResponse(saved);
        });
    }

    @Override
    @Transactional
    public UsuarioResponseDto changeRol(
            AuthenticatedUserContext actor,
            Long idUsuario,
            UsuarioChangeRolRequestDto request
    ) {
        return executeWithTechnicalLog("changeRol", actor, idUsuario, () -> {
            if (request == null) {
                throw new ValidationException(
                        "USUARIO_ROL_REQUEST_OBLIGATORIO",
                        "La solicitud de cambio de rol es obligatoria."
                );
            }

            Usuario usuario = usuarioValidator.requireWithRolById(idUsuario);
            usuarioPolicy.ensureCanChangeRol(actor, usuario);

            Rol nuevoRol = rolValidator.requireActivoById(request.idRol());
            usuarioValidator.validateCambioRol(usuario, nuevoRol);

            if (usuario.getRol() != null
                    && usuario.getRol().getId() != null
                    && usuario.getRol().getId().equals(nuevoRol.getId())) {
                return usuarioMapper.toResponse(usuario);
            }

            usuario.setRol(nuevoRol);
            Usuario saved = usuarioRepository.save(usuario);

            auditoriaSeguridadService.registerSuccess(
                    TipoAuditoriaSeguridad.ROL_CAMBIADO,
                    resolveActor(actor),
                    saved,
                    buildRolMessage(saved, nuevoRol, request.motivo())
            );

            revokeSessionsAfterRoleChange(actor, saved, nuevoRol, request.motivo());

            return usuarioMapper.toResponse(saved);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public UsuarioDetailResponseDto findById(
            AuthenticatedUserContext actor,
            Long idUsuario
    ) {
        return executeWithTechnicalLog("findById", actor, idUsuario, () -> {
            usuarioPolicy.ensureCanFindById(actor, idUsuario);

            Usuario usuario = usuarioValidator.requireWithRolById(idUsuario);

            return usuarioMapper.toDetail(usuario);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponseDto<UsuarioResponseDto> findAll(
            AuthenticatedUserContext actor,
            UsuarioFilterDto filter,
            PageRequestDto pageRequest
    ) {
        return executeWithTechnicalLog("findAll", actor, null, () -> {
            usuarioPolicy.ensureCanList(actor);
            validateDateRange(filter == null ? null : filter.fechaDesde(), filter == null ? null : filter.fechaHasta());

            Pageable pageable = paginationService.toPageable(pageRequest, "createdAt", SORT_FIELDS);

            Page<Usuario> page = usuarioRepository.findAll(
                    UsuarioSpecifications.from(filter),
                    pageable
            );

            return paginationMapper.toPageResponse(page, usuarioMapper::toResponse);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public List<UsuarioLookupDto> findLookup(AuthenticatedUserContext actor) {
        return executeWithTechnicalLog("findLookup", actor, null, () -> {
            usuarioPolicy.ensureCanList(actor);

            return usuarioRepository.findTop100ByEstadoOrderByNombresAscApellidosAsc(EstadoRegistro.ACTIVO)
                    .stream()
                    .map(usuarioMapper::toLookup)
                    .toList();
        });
    }

    private void validateCreateRequest(UsuarioCreateRequestDto request) {
        if (request == null) {
            throw new ValidationException(
                    "USUARIO_CREATE_REQUEST_OBLIGATORIO",
                    "La solicitud de creación de usuario es obligatoria."
            );
        }
    }

    private void applyPolicyForEstado(
            AuthenticatedUserContext actor,
            Usuario usuario,
            EstadoRegistro nuevoEstado
    ) {
        if (nuevoEstado == null) {
            usuarioPolicy.ensureCanChangeEstado(actor, usuario, null);
            return;
        }

        switch (nuevoEstado) {
            case ACTIVO -> usuarioPolicy.ensureCanUnlock(actor, usuario);
            case BLOQUEADO -> usuarioPolicy.ensureCanBlock(actor, usuario);
            case ELIMINADO -> usuarioPolicy.ensureCanDelete(actor, usuario);
            case INACTIVO -> usuarioPolicy.ensureCanChangeEstado(actor, usuario, nuevoEstado);
        }
    }

    private TipoAuditoriaSeguridad applyEstado(
            Usuario usuario,
            EstadoRegistro nuevoEstado
    ) {
        Instant now = Instant.now(clock);

        return switch (nuevoEstado) {
            case ACTIVO -> {
                usuario.activar();
                yield TipoAuditoriaSeguridad.USUARIO_ACTIVADO;
            }
            case INACTIVO -> {
                usuario.inactivar();
                yield TipoAuditoriaSeguridad.USUARIO_INACTIVADO;
            }
            case BLOQUEADO -> {
                usuario.bloquearTemporalmente(now.plusSeconds(appProperties.getSecurity().getLockMinutes() * 60));
                yield TipoAuditoriaSeguridad.USUARIO_BLOQUEADO;
            }
            case ELIMINADO -> {
                usuario.eliminarLogicamente(now);
                yield TipoAuditoriaSeguridad.USUARIO_ELIMINADO;
            }
        };
    }

    private void revokeSessionsIfEstadoInvalidatesAuthentication(
            AuthenticatedUserContext actor,
            Usuario usuario,
            EstadoRegistro nuevoEstado,
            String motivo
    ) {
        if (usuario == null || usuario.getId() == null || nuevoEstado == null) {
            return;
        }

        if (EstadoRegistro.ACTIVO.equals(nuevoEstado)) {
            return;
        }

        String detalle = "Sesiones revocadas por cambio de estado a " + nuevoEstado + ".";
        String motivoNormalizado = StringNormalizer.normalizeSpaces(motivo);

        if (motivoNormalizado != null) {
            detalle = detalle + " Motivo: " + truncate(motivoNormalizado, 180);
        }

        sesionService.revokeUserActiveSessions(actor, usuario.getId(), detalle);
    }

    private void revokeSessionsAfterRoleChange(
            AuthenticatedUserContext actor,
            Usuario usuario,
            Rol nuevoRol,
            String motivo
    ) {
        if (usuario == null || usuario.getId() == null) {
            return;
        }

        String codigoRol = nuevoRol == null ? null : nuevoRol.getCodigo();
        String detalle = "Sesiones revocadas por cambio de rol a " + codigoRol + ".";
        String motivoNormalizado = StringNormalizer.normalizeSpaces(motivo);

        if (motivoNormalizado != null) {
            detalle = detalle + " Motivo: " + truncate(motivoNormalizado, 180);
        }

        sesionService.revokeUserActiveSessions(actor, usuario.getId(), detalle);
    }

    private void sendWelcomeAfterCommit(Usuario usuario) {
        String email = emailOf(usuario);
        String nombreCompleto = nombreCompleto(usuario);
        String username = usernameOf(usuario);

        if (email == null) {
            return;
        }

        Runnable task = () -> {
            try {
                emailService.sendWelcomeUser(
                        email,
                        nombreCompleto,
                        username
                );
            } catch (RuntimeException exception) {
                log.error(
                        "No se pudo enviar correo de bienvenida - idUsuario={}, username={}, requestId={}",
                        usuario == null ? null : usuario.getId(),
                        username,
                        requestId(),
                        exception
                );
            }
        };

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    task.run();
                }
            });
            return;
        }

        task.run();
    }

    private void validateDateRange(
            Instant desde,
            Instant hasta
    ) {
        if (desde != null && hasta != null && hasta.isBefore(desde)) {
            throw new ValidationException(
                    "USUARIO_RANGO_FECHAS_INVALIDO",
                    "La fecha hasta no puede ser anterior a la fecha desde."
            );
        }
    }

    private Usuario resolveActor(AuthenticatedUserContext actor) {
        if (actor == null || actor.idUsuario() == null) {
            return null;
        }

        return usuarioRepository.findById(actor.idUsuario()).orElse(null);
    }

    private String buildEstadoMessage(
            Usuario usuario,
            EstadoRegistro nuevoEstado,
            String motivo
    ) {
        String message = "Usuario " + usernameOf(usuario) + " cambió a estado " + nuevoEstado + ".";

        String normalizedReason = StringNormalizer.normalizeSpaces(motivo);
        return normalizedReason == null ? message : message + " Motivo: " + truncate(normalizedReason, 250);
    }

    private String buildRolMessage(
            Usuario usuario,
            Rol nuevoRol,
            String motivo
    ) {
        String codigoRol = nuevoRol == null ? null : nuevoRol.getCodigo();
        String message = "Usuario " + usernameOf(usuario) + " cambió al rol " + codigoRol + ".";

        String normalizedReason = StringNormalizer.normalizeSpaces(motivo);
        return normalizedReason == null ? message : message + " Motivo: " + truncate(normalizedReason, 250);
    }

    private String usernameOf(Usuario usuario) {
        return usuario == null || usuario.getUsername() == null ? null : usuario.getUsername().getValue();
    }

    private String emailOf(Usuario usuario) {
        return usuario == null || usuario.getEmail() == null ? null : usuario.getEmail().getValue();
    }

    private String nombreCompleto(Usuario usuario) {
        String nombres = usuario == null ? null : StringNormalizer.normalizeSpaces(usuario.getNombres());
        String apellidos = usuario == null ? null : StringNormalizer.normalizeSpaces(usuario.getApellidos());

        if (nombres == null && apellidos == null) {
            return "usuario";
        }

        if (nombres == null) {
            return apellidos;
        }

        if (apellidos == null) {
            return nombres;
        }

        return nombres + " " + apellidos;
    }

    private boolean hasChanged(String previous, String current) {
        if (previous == null && current == null) {
            return false;
        }

        if (previous == null || current == null) {
            return true;
        }

        return !previous.equalsIgnoreCase(current);
    }

    private String truncate(
            String value,
            int maxLength
    ) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }

        return value.substring(0, maxLength);
    }

    private <T> T executeWithTechnicalLog(
            String method,
            AuthenticatedUserContext actor,
            Long idUsuario,
            Supplier<T> action
    ) {
        try {
            return action.get();
        } catch (BusinessException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            log.error(
                    "Error inesperado en UsuarioServiceImpl.{} - actor={}, idUsuario={}, requestId={}",
                    method,
                    actorUsername(actor),
                    idUsuario,
                    requestId(),
                    exception
            );
            throw exception;
        }
    }

    private String actorUsername(AuthenticatedUserContext actor) {
        if (actor == null || actor.username() == null) {
            return "ANONIMO";
        }

        return actor.username();
    }

    private String requestId() {
        return AuditContextHolder.getRequestIdOrNull();
    }
}