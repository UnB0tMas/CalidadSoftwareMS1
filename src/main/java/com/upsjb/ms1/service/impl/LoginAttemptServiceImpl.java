package com.upsjb.ms1.service.impl;

import com.upsjb.ms1.domain.entity.LoginAttempt;
import com.upsjb.ms1.domain.entity.Usuario;
import com.upsjb.ms1.domain.enums.TipoLogin;
import com.upsjb.ms1.domain.value.IpAddressValue;
import com.upsjb.ms1.domain.value.UserAgentValue;
import com.upsjb.ms1.dto.auditoria.filter.LoginAttemptFilterDto;
import com.upsjb.ms1.dto.auditoria.response.LoginAttemptResponseDto;
import com.upsjb.ms1.dto.shared.PageRequestDto;
import com.upsjb.ms1.dto.shared.PageResponseDto;
import com.upsjb.ms1.mapper.LoginAttemptMapper;
import com.upsjb.ms1.policy.AuditoriaSeguridadPolicy;
import com.upsjb.ms1.repository.LoginAttemptRepository;
import com.upsjb.ms1.security.principal.AuthenticatedUserContext;
import com.upsjb.ms1.service.contract.LoginAttemptService;
import com.upsjb.ms1.shared.audit.AuditContext;
import com.upsjb.ms1.shared.audit.AuditContextHolder;
import com.upsjb.ms1.shared.exception.BusinessException;
import com.upsjb.ms1.shared.exception.NotFoundException;
import com.upsjb.ms1.shared.exception.ValidationException;
import com.upsjb.ms1.shared.pagination.PaginationMapper;
import com.upsjb.ms1.shared.pagination.PaginationService;
import com.upsjb.ms1.specification.LoginAttemptSpecifications;
import com.upsjb.ms1.util.EmailMaskingUtil;
import com.upsjb.ms1.util.StringNormalizer;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LoginAttemptServiceImpl implements LoginAttemptService {

    private static final Logger log = LoggerFactory.getLogger(LoginAttemptServiceImpl.class);

    private static final String DEFAULT_IP = "0.0.0.0";
    private static final String DEFAULT_USER_AGENT = "UNKNOWN";

    private static final Set<String> SORT_FIELDS = Set.of(
            "attemptedAt",
            "usernameOrEmail",
            "tipoLogin",
            "exitoso",
            "failureCode"
    );

    private final LoginAttemptRepository loginAttemptRepository;
    private final LoginAttemptMapper loginAttemptMapper;
    private final AuditoriaSeguridadPolicy auditoriaSeguridadPolicy;
    private final PaginationService paginationService;
    private final PaginationMapper paginationMapper;
    private final Clock clock;

    public LoginAttemptServiceImpl(
            LoginAttemptRepository loginAttemptRepository,
            LoginAttemptMapper loginAttemptMapper,
            AuditoriaSeguridadPolicy auditoriaSeguridadPolicy,
            PaginationService paginationService,
            PaginationMapper paginationMapper,
            Clock clock
    ) {
        this.loginAttemptRepository = loginAttemptRepository;
        this.loginAttemptMapper = loginAttemptMapper;
        this.auditoriaSeguridadPolicy = auditoriaSeguridadPolicy;
        this.paginationService = paginationService;
        this.paginationMapper = paginationMapper;
        this.clock = clock;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public LoginAttemptResponseDto recordSuccess(
            Usuario usuario,
            String usernameOrEmail,
            TipoLogin tipoLogin
    ) {
        return recordAttempt(
                usuario,
                usernameOrEmail,
                tipoLogin,
                true,
                null,
                null
        );
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public LoginAttemptResponseDto recordFailure(
            Usuario usuario,
            String usernameOrEmail,
            TipoLogin tipoLogin,
            String failureCode,
            String failureReason
    ) {
        return recordAttempt(
                usuario,
                usernameOrEmail,
                tipoLogin,
                false,
                failureCode,
                failureReason
        );
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public LoginAttemptResponseDto recordAttempt(
            Usuario usuario,
            String usernameOrEmail,
            TipoLogin tipoLogin,
            boolean exitoso,
            String failureCode,
            String failureReason
    ) {
        return executeWithTechnicalLog(
                "recordAttempt",
                usuarioRef(usuario),
                safeIdentifier(usernameOrEmail),
                () -> {
                    String normalizedUsernameOrEmail = requireUsernameOrEmail(usernameOrEmail);
                    TipoLogin normalizedTipoLogin = requireTipoLogin(tipoLogin);

                    LoginAttempt attempt = LoginAttempt.builder()
                            .usuario(usuario)
                            .usernameOrEmail(normalizedUsernameOrEmail)
                            .tipoLogin(normalizedTipoLogin)
                            .exitoso(exitoso)
                            .failureCode(truncate(failureCode, 80))
                            .failureReason(truncate(failureReason, 250))
                            .ipAddress(IpAddressValue.of(resolveIpAddress()))
                            .userAgent(UserAgentValue.of(resolveUserAgent()))
                            .attemptedAt(Instant.now(clock))
                            .requestId(AuditContextHolder.getRequestIdOrNull())
                            .build();

                    return loginAttemptMapper.toResponse(loginAttemptRepository.save(attempt));
                }
        );
    }

    @Override
    @Transactional(readOnly = true)
    public LoginAttemptResponseDto findById(
            AuthenticatedUserContext actor,
            Long idLoginAttempt
    ) {
        return executeWithTechnicalLog(
                "findById",
                actorRef(actor),
                idLoginAttempt,
                () -> {
                    auditoriaSeguridadPolicy.ensureCanListLoginAttempts(actor);

                    if (idLoginAttempt == null) {
                        throw new ValidationException(
                                "LOGIN_ATTEMPT_ID_OBLIGATORIO",
                                "El identificador del intento de login es obligatorio."
                        );
                    }

                    LoginAttempt attempt = loginAttemptRepository.findById(idLoginAttempt)
                            .orElseThrow(() -> new NotFoundException(
                                    "LOGIN_ATTEMPT_NO_ENCONTRADO",
                                    "No se encontró el intento de login solicitado."
                            ));

                    return loginAttemptMapper.toResponse(attempt);
                }
        );
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponseDto<LoginAttemptResponseDto> findAll(
            AuthenticatedUserContext actor,
            LoginAttemptFilterDto filter,
            PageRequestDto pageRequest
    ) {
        return executeWithTechnicalLog(
                "findAll",
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
                            SORT_FIELDS
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
    public List<LoginAttemptResponseDto> findRecentByUsernameOrEmail(
            AuthenticatedUserContext actor,
            String usernameOrEmail
    ) {
        return executeWithTechnicalLog(
                "findRecentByUsernameOrEmail",
                actorRef(actor),
                safeIdentifier(usernameOrEmail),
                () -> {
                    auditoriaSeguridadPolicy.ensureCanListLoginAttempts(actor);

                    String normalized = requireUsernameOrEmail(usernameOrEmail);

                    return loginAttemptRepository.findRecentByUsernameOrEmailIgnoreCase(
                                    normalized,
                                    PageRequest.of(0, 10, Sort.by(
                                            Sort.Order.desc("attemptedAt"),
                                            Sort.Order.desc("id")
                                    ))
                            )
                            .stream()
                            .map(loginAttemptMapper::toResponse)
                            .toList();
                }
        );
    }

    @Override
    @Transactional(readOnly = true)
    public long countRecentFailuresByUsernameOrEmail(
            String usernameOrEmail,
            Instant since
    ) {
        return executeWithTechnicalLog(
                "countRecentFailuresByUsernameOrEmail",
                "SYSTEM",
                safeIdentifier(usernameOrEmail),
                () -> {
                    String normalized = requireUsernameOrEmail(usernameOrEmail);
                    Instant normalizedSince = requireSince(since);

                    return loginAttemptRepository.countFailedByUsernameOrEmailIgnoreCaseAfter(
                            normalized,
                            normalizedSince
                    );
                }
        );
    }

    @Override
    @Transactional(readOnly = true)
    public long countRecentFailuresByIp(
            String ipAddress,
            Instant since
    ) {
        return executeWithTechnicalLog(
                "countRecentFailuresByIp",
                "SYSTEM",
                safeIp(ipAddress),
                () -> {
                    String normalizedIp = requireIpAddress(ipAddress);
                    Instant normalizedSince = requireSince(since);

                    return loginAttemptRepository.countByIpAddress_ValueAndExitosoFalseAndAttemptedAtAfter(
                            normalizedIp,
                            normalizedSince
                    );
                }
        );
    }

    private String requireUsernameOrEmail(String usernameOrEmail) {
        String normalized = StringNormalizer.lower(usernameOrEmail);

        if (normalized == null) {
            throw new ValidationException(
                    "LOGIN_ATTEMPT_USERNAME_OBLIGATORIO",
                    "El username o email es obligatorio."
            );
        }

        if (normalized.length() > 180) {
            throw new ValidationException(
                    "LOGIN_ATTEMPT_USERNAME_LONGITUD_INVALIDA",
                    "El username o email no puede superar 180 caracteres."
            );
        }

        return normalized;
    }

    private TipoLogin requireTipoLogin(TipoLogin tipoLogin) {
        if (tipoLogin == null) {
            throw new ValidationException(
                    "LOGIN_ATTEMPT_TIPO_OBLIGATORIO",
                    "El tipo de login es obligatorio."
            );
        }

        return tipoLogin;
    }

    private String requireIpAddress(String ipAddress) {
        String normalizedIp = StringNormalizer.trimToNull(ipAddress);

        if (normalizedIp == null) {
            throw new ValidationException(
                    "LOGIN_ATTEMPT_IP_OBLIGATORIA",
                    "La dirección IP es obligatoria para contar intentos fallidos."
            );
        }

        if (normalizedIp.length() > 45) {
            throw new ValidationException(
                    "LOGIN_ATTEMPT_IP_LONGITUD_INVALIDA",
                    "La dirección IP no puede superar 45 caracteres."
            );
        }

        return normalizedIp;
    }

    private Instant requireSince(Instant since) {
        if (since == null) {
            throw new ValidationException(
                    "LOGIN_ATTEMPT_FECHA_DESDE_OBLIGATORIA",
                    "La fecha desde es obligatoria para contar intentos fallidos."
            );
        }

        return since;
    }

    private void validateDateRange(
            Instant desde,
            Instant hasta
    ) {
        if (desde != null && hasta != null && hasta.isBefore(desde)) {
            throw new ValidationException(
                    "LOGIN_ATTEMPT_RANGO_FECHAS_INVALIDO",
                    "La fecha hasta no puede ser anterior a la fecha desde."
            );
        }
    }

    private String resolveIpAddress() {
        AuditContext context = AuditContextHolder.get();
        String ipAddress = StringNormalizer.trimToNull(context.ipAddress());
        return ipAddress == null ? DEFAULT_IP : truncate(ipAddress, 45);
    }

    private String resolveUserAgent() {
        AuditContext context = AuditContextHolder.get();
        String userAgent = StringNormalizer.trimToNull(context.userAgent());
        return userAgent == null ? DEFAULT_USER_AGENT : truncate(userAgent, 512);
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
                    "Error inesperado en LoginAttemptServiceImpl.{} - actor={}, recurso={}, requestId={}",
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

        if (usuario.getUsername() != null && StringNormalizer.trimToNull(usuario.getUsername().getValue()) != null) {
            return usuario.getUsername().getValue();
        }

        return usuario.getId() == null ? "USUARIO_SIN_ID" : "ID_" + usuario.getId();
    }

    private String safeIdentifier(String value) {
        String normalized = StringNormalizer.normalizeSpaces(value);

        if (normalized == null) {
            return "SIN_IDENTIFICADOR";
        }

        if (normalized.contains("@")) {
            return EmailMaskingUtil.mask(normalized);
        }

        return truncate(normalized, 80);
    }

    private String safeIp(String value) {
        String normalized = StringNormalizer.trimToNull(value);
        return normalized == null ? "SIN_IP" : truncate(normalized, 45);
    }

    private String truncate(String value, int maxLength) {
        String normalized = StringNormalizer.normalizeSpaces(value);

        if (normalized == null || normalized.length() <= maxLength) {
            return normalized;
        }

        return normalized.substring(0, maxLength);
    }
}