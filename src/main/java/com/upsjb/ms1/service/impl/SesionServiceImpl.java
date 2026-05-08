package com.upsjb.ms1.service.impl;

import com.upsjb.ms1.config.AppPropertiesConfig;
import com.upsjb.ms1.domain.entity.Usuario;
import com.upsjb.ms1.domain.entity.UsuarioSesion;
import com.upsjb.ms1.domain.enums.EstadoSesion;
import com.upsjb.ms1.domain.enums.MotivoRevocacionSesion;
import com.upsjb.ms1.domain.enums.TipoAuditoriaSeguridad;
import com.upsjb.ms1.domain.enums.TipoLogin;
import com.upsjb.ms1.domain.value.IpAddressValue;
import com.upsjb.ms1.domain.value.UserAgentValue;
import com.upsjb.ms1.dto.auth.response.SessionResponseDto;
import com.upsjb.ms1.dto.shared.PageRequestDto;
import com.upsjb.ms1.dto.shared.PageResponseDto;
import com.upsjb.ms1.mapper.UsuarioSesionMapper;
import com.upsjb.ms1.policy.SesionPolicy;
import com.upsjb.ms1.repository.UsuarioRepository;
import com.upsjb.ms1.repository.UsuarioSesionRepository;
import com.upsjb.ms1.security.principal.AuthenticatedUserContext;
import com.upsjb.ms1.service.contract.AuditoriaSeguridadService;
import com.upsjb.ms1.service.contract.SesionService;
import com.upsjb.ms1.shared.exception.NotFoundException;
import com.upsjb.ms1.shared.exception.UnauthorizedException;
import com.upsjb.ms1.shared.exception.ValidationException;
import com.upsjb.ms1.shared.pagination.PaginationMapper;
import com.upsjb.ms1.shared.pagination.PaginationService;
import com.upsjb.ms1.specification.UsuarioSesionSpecifications;
import com.upsjb.ms1.util.RandomTokenUtil;
import com.upsjb.ms1.util.StringNormalizer;
import com.upsjb.ms1.util.TokenHashUtil;
import com.upsjb.ms1.validator.AuthValidator;
import com.upsjb.ms1.validator.SesionValidator;
import java.time.Clock;
import java.time.Instant;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SesionServiceImpl implements SesionService {

    private static final String DEFAULT_IP = "0.0.0.0";
    private static final String DEFAULT_USER_AGENT = "UNKNOWN";

    private static final Set<String> SORT_FIELDS = Set.of(
            "id",
            "estado",
            "tipoLogin",
            "expiresAt",
            "lastUsedAt",
            "revokedAt",
            "createdAt"
    );

    private final UsuarioSesionRepository usuarioSesionRepository;
    private final UsuarioRepository usuarioRepository;
    private final UsuarioSesionMapper usuarioSesionMapper;
    private final SesionValidator sesionValidator;
    private final AuthValidator authValidator;
    private final SesionPolicy sesionPolicy;
    private final AuditoriaSeguridadService auditoriaSeguridadService;
    private final PaginationService paginationService;
    private final PaginationMapper paginationMapper;
    private final AppPropertiesConfig appProperties;
    private final Clock clock;

    public SesionServiceImpl(
            UsuarioSesionRepository usuarioSesionRepository,
            UsuarioRepository usuarioRepository,
            UsuarioSesionMapper usuarioSesionMapper,
            SesionValidator sesionValidator,
            AuthValidator authValidator,
            SesionPolicy sesionPolicy,
            AuditoriaSeguridadService auditoriaSeguridadService,
            PaginationService paginationService,
            PaginationMapper paginationMapper,
            AppPropertiesConfig appProperties,
            Clock clock
    ) {
        this.usuarioSesionRepository = usuarioSesionRepository;
        this.usuarioRepository = usuarioRepository;
        this.usuarioSesionMapper = usuarioSesionMapper;
        this.sesionValidator = sesionValidator;
        this.authValidator = authValidator;
        this.sesionPolicy = sesionPolicy;
        this.auditoriaSeguridadService = auditoriaSeguridadService;
        this.paginationService = paginationService;
        this.paginationMapper = paginationMapper;
        this.appProperties = appProperties;
        this.clock = clock;
    }

    @Override
    @Transactional
    public CreatedSession createSession(
            Usuario usuario,
            TipoLogin tipoLogin,
            String deviceFingerprint,
            String ipAddress,
            String userAgent
    ) {
        validateCreateSession(usuario, tipoLogin);

        Instant now = Instant.now(clock);
        String refreshToken = RandomTokenUtil.secureToken();

        UsuarioSesion session = UsuarioSesion.builder()
                .usuario(usuario)
                .refreshTokenHash(TokenHashUtil.hash(refreshToken))
                .tipoLogin(tipoLogin)
                .estado(EstadoSesion.ACTIVA)
                .ipAddress(IpAddressValue.of(resolveIpAddress(ipAddress)))
                .userAgent(UserAgentValue.of(resolveUserAgent(userAgent)))
                .deviceFingerprint(truncate(StringNormalizer.normalizeSpaces(deviceFingerprint), 160))
                .expiresAt(now.plusSeconds(appProperties.getSecurity().getRefreshTokenDays() * 24 * 60 * 60))
                .lastUsedAt(now)
                .build();

        UsuarioSesion saved = usuarioSesionRepository.save(session);

        return new CreatedSession(
                saved,
                refreshToken,
                usuarioSesionMapper.toResponse(saved, saved.getId())
        );
    }

    @Override
    @Transactional
    public RotatedSession rotateRefreshToken(String refreshToken) {
        String refreshTokenHash = sesionValidator.hashRefreshToken(refreshToken);

        UsuarioSesion session = usuarioSesionRepository.findByRefreshTokenHash(refreshTokenHash)
                .orElseThrow(() -> {
                    auditoriaSeguridadService.registerFailure(
                            TipoAuditoriaSeguridad.TOKEN_INVALIDO,
                            null,
                            null,
                            "Refresh token inexistente o inválido."
                    );

                    return new UnauthorizedException(
                            "REFRESH_TOKEN_INVALIDO",
                            "El refresh token no es válido."
                    );
                });

        Usuario usuario = requireUsuarioFromSession(session);
        Instant now = Instant.now(clock);

        if (!session.estaActiva()) {
            handleRefreshTokenReuseOrInvalidState(session, usuario, now);

            throw new UnauthorizedException(
                    "REFRESH_TOKEN_NO_ACTIVO",
                    "El refresh token no se encuentra activo."
            );
        }

        if (!session.estaVigente(now)) {
            session.marcarExpirada(now);
            usuarioSesionRepository.save(session);

            auditoriaSeguridadService.registerFailure(
                    TipoAuditoriaSeguridad.TOKEN_INVALIDO,
                    usuario,
                    usuario,
                    "Refresh token expirado."
            );

            throw new UnauthorizedException(
                    "REFRESH_TOKEN_EXPIRADO",
                    "El refresh token se encuentra expirado."
            );
        }

        authValidator.validateUsuarioPuedeAutenticarse(usuario);

        String newRefreshToken = RandomTokenUtil.secureToken();
        session.rotarRefreshToken(TokenHashUtil.hash(newRefreshToken), now);

        UsuarioSesion saved = usuarioSesionRepository.save(session);

        auditoriaSeguridadService.registerSuccess(
                TipoAuditoriaSeguridad.REFRESH_TOKEN,
                usuario,
                usuario,
                "Refresh token rotado correctamente."
        );

        return new RotatedSession(
                saved,
                newRefreshToken,
                usuarioSesionMapper.toResponse(saved, saved.getId())
        );
    }

    @Override
    @Transactional(readOnly = true)
    public SessionResponseDto findById(
            AuthenticatedUserContext actor,
            Long idSesion
    ) {
        UsuarioSesion session = sesionValidator.requireById(idSesion);

        sesionPolicy.ensureCanViewSession(actor, session);

        return usuarioSesionMapper.toResponse(session, actor == null ? null : actor.sessionId());
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponseDto<SessionResponseDto> findOwnSessions(
            AuthenticatedUserContext actor,
            EstadoSesion estado,
            PageRequestDto pageRequest
    ) {
        sesionPolicy.ensureCanListOwnSessions(actor);

        Pageable pageable = paginationService.toPageable(pageRequest, "createdAt", SORT_FIELDS);

        Page<UsuarioSesion> page = usuarioSesionRepository.findAll(
                UsuarioSesionSpecifications.from(
                        actor.idUsuario(),
                        estado,
                        null,
                        null,
                        null
                ),
                pageable
        );

        return paginationMapper.toPageResponse(
                page,
                session -> usuarioSesionMapper.toResponse(session, actor.sessionId())
        );
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponseDto<SessionResponseDto> findUserSessions(
            AuthenticatedUserContext actor,
            Long idUsuario,
            EstadoSesion estado,
            PageRequestDto pageRequest
    ) {
        sesionPolicy.ensureCanListUserSessions(actor, idUsuario);

        if (idUsuario == null) {
            throw new ValidationException(
                    "SESION_USUARIO_ID_OBLIGATORIO",
                    "El usuario es obligatorio para consultar sesiones."
            );
        }

        Pageable pageable = paginationService.toPageable(pageRequest, "createdAt", SORT_FIELDS);

        Page<UsuarioSesion> page = usuarioSesionRepository.findAll(
                UsuarioSesionSpecifications.from(
                        idUsuario,
                        estado,
                        null,
                        null,
                        null
                ),
                pageable
        );

        return paginationMapper.toPageResponse(
                page,
                session -> usuarioSesionMapper.toResponse(session, actor == null ? null : actor.sessionId())
        );
    }

    @Override
    @Transactional
    public SessionResponseDto revokeSession(
            AuthenticatedUserContext actor,
            Long idSesion,
            String motivo
    ) {
        UsuarioSesion session = sesionValidator.requireById(idSesion);

        sesionPolicy.ensureCanRevokeSession(actor, session);

        if (!session.estaActiva()) {
            return usuarioSesionMapper.toResponse(session, actor == null ? null : actor.sessionId());
        }

        Instant now = Instant.now(clock);
        MotivoRevocacionSesion motivoRevocacion = resolveRevocationReason(actor, session);
        String detalle = normalizeReason(motivo, "Sesión revocada desde el backend.");

        session.revocar(motivoRevocacion, detalle, now);

        UsuarioSesion saved = usuarioSesionRepository.save(session);

        auditoriaSeguridadService.registerSuccess(
                TipoAuditoriaSeguridad.SESION_REVOCADA,
                saved.getUsuario(),
                saved.getUsuario(),
                "Sesión revocada. Motivo: " + motivoRevocacion + "."
        );

        return usuarioSesionMapper.toResponse(saved, actor == null ? null : actor.sessionId());
    }

    @Override
    @Transactional
    public int revokeOwnActiveSessions(
            AuthenticatedUserContext actor,
            String motivo
    ) {
        sesionPolicy.ensureCanLogoutAllOwnSessions(actor, actor == null ? null : actor.idUsuario());

        Instant now = Instant.now(clock);
        String detalle = normalizeReason(motivo, "Revocación de todas las sesiones propias.");

        int revoked = usuarioSesionRepository.revokeActiveSessionsByUsuario(
                actor.idUsuario(),
                EstadoSesion.ACTIVA,
                EstadoSesion.REVOCADA,
                MotivoRevocacionSesion.LOGOUT_GLOBAL,
                detalle,
                now
        );

        Usuario usuario = requireUsuario(actor.idUsuario());

        auditoriaSeguridadService.registerSuccess(
                TipoAuditoriaSeguridad.LOGOUT_GLOBAL,
                usuario,
                usuario,
                "Sesiones activas revocadas por logout global: " + revoked + "."
        );

        return revoked;
    }

    @Override
    @Transactional
    public int revokeUserActiveSessions(
            AuthenticatedUserContext actor,
            Long idUsuario,
            String motivo
    ) {
        sesionPolicy.ensureCanAdminRevokeUserSessions(actor, idUsuario);

        if (idUsuario == null) {
            throw new ValidationException(
                    "SESION_USUARIO_ID_OBLIGATORIO",
                    "El usuario es obligatorio para revocar sesiones."
            );
        }

        Instant now = Instant.now(clock);
        String detalle = normalizeReason(motivo, "Revocación administrativa de sesiones.");

        int revoked = usuarioSesionRepository.revokeActiveSessionsByUsuario(
                idUsuario,
                EstadoSesion.ACTIVA,
                EstadoSesion.REVOCADA,
                MotivoRevocacionSesion.ADMIN_REVOKE,
                detalle,
                now
        );

        Usuario usuario = requireUsuario(idUsuario);

        auditoriaSeguridadService.registerSuccess(
                TipoAuditoriaSeguridad.SESION_REVOCADA,
                null,
                usuario,
                "Sesiones activas revocadas por administrador: " + revoked + "."
        );

        return revoked;
    }

    @Override
    @Transactional
    public int expireExpiredSessions() {
        return usuarioSesionRepository.expireSessions(
                EstadoSesion.ACTIVA,
                EstadoSesion.EXPIRADA,
                MotivoRevocacionSesion.EXPIRED,
                Instant.now(clock)
        );
    }

    private void validateCreateSession(
            Usuario usuario,
            TipoLogin tipoLogin
    ) {
        if (usuario == null || usuario.getId() == null) {
            throw new ValidationException(
                    "SESION_USUARIO_OBLIGATORIO",
                    "El usuario es obligatorio para crear sesión."
            );
        }

        if (tipoLogin == null) {
            throw new ValidationException(
                    "SESION_TIPO_LOGIN_OBLIGATORIO",
                    "El tipo de login es obligatorio para crear sesión."
            );
        }
    }

    private Usuario requireUsuarioFromSession(UsuarioSesion session) {
        Usuario usuario = session == null ? null : session.getUsuario();

        if (usuario == null || usuario.getId() == null) {
            throw new UnauthorizedException(
                    "SESION_USUARIO_INVALIDO",
                    "La sesión no tiene un usuario válido asociado."
            );
        }

        return usuario;
    }

    private void handleRefreshTokenReuseOrInvalidState(
            UsuarioSesion session,
            Usuario usuario,
            Instant now
    ) {
        if (session.estaRevocada() || session.estaCerrada()) {
            usuarioSesionRepository.revokeActiveSessionsByUsuario(
                    usuario.getId(),
                    EstadoSesion.ACTIVA,
                    EstadoSesion.REVOCADA,
                    MotivoRevocacionSesion.REFRESH_REUSE_DETECTED,
                    "Reutilización de refresh token no activo.",
                    now
            );

            auditoriaSeguridadService.registerWarning(
                    TipoAuditoriaSeguridad.REFRESH_TOKEN_REUTILIZADO,
                    usuario,
                    usuario,
                    "Se detectó reutilización de refresh token."
            );

            return;
        }

        auditoriaSeguridadService.registerFailure(
                TipoAuditoriaSeguridad.TOKEN_INVALIDO,
                usuario,
                usuario,
                "Refresh token en estado no permitido."
        );
    }

    private MotivoRevocacionSesion resolveRevocationReason(
            AuthenticatedUserContext actor,
            UsuarioSesion session
    ) {
        Long idUsuarioSesion = session.getUsuario() == null ? null : session.getUsuario().getId();

        if (actor != null && actor.isAdmin() && !actor.isSelf(idUsuarioSesion)) {
            return MotivoRevocacionSesion.ADMIN_REVOKE;
        }

        return MotivoRevocacionSesion.LOGOUT;
    }

    private Usuario requireUsuario(Long idUsuario) {
        return usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new NotFoundException(
                        "USUARIO_NO_ENCONTRADO",
                        "No se encontró el usuario solicitado."
                ));
    }

    private String resolveIpAddress(String ipAddress) {
        String normalized = StringNormalizer.trimToNull(ipAddress);
        return normalized == null ? DEFAULT_IP : normalized;
    }

    private String resolveUserAgent(String userAgent) {
        String normalized = StringNormalizer.trimToNull(userAgent);
        return normalized == null ? DEFAULT_USER_AGENT : normalized;
    }

    private String normalizeReason(
            String motivo,
            String defaultValue
    ) {
        String normalized = StringNormalizer.normalizeSpaces(motivo);
        return normalized == null ? defaultValue : truncate(normalized, 250);
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
}