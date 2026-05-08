package com.upsjb.ms1.service.impl;

import com.upsjb.ms1.config.AppPropertiesConfig;
import com.upsjb.ms1.domain.entity.Rol;
import com.upsjb.ms1.domain.entity.Usuario;
import com.upsjb.ms1.domain.entity.UsuarioOAuth2Cuenta;
import com.upsjb.ms1.domain.entity.UsuarioSesion;
import com.upsjb.ms1.domain.enums.EstadoRegistro;
import com.upsjb.ms1.domain.enums.ProveedorOAuth2;
import com.upsjb.ms1.domain.enums.TipoAuditoriaSeguridad;
import com.upsjb.ms1.domain.enums.TipoLogin;
import com.upsjb.ms1.domain.value.EmailValue;
import com.upsjb.ms1.domain.value.UsernameValue;
import com.upsjb.ms1.dto.auth.request.LoginRequestDto;
import com.upsjb.ms1.dto.auth.request.LogoutRequestDto;
import com.upsjb.ms1.dto.auth.request.RefreshTokenRequestDto;
import com.upsjb.ms1.dto.auth.response.AuthTokenResponseDto;
import com.upsjb.ms1.dto.auth.response.AuthUserResponseDto;
import com.upsjb.ms1.dto.auth.response.SessionResponseDto;
import com.upsjb.ms1.mapper.UsuarioMapper;
import com.upsjb.ms1.policy.AuthPolicy;
import com.upsjb.ms1.repository.RolRepository;
import com.upsjb.ms1.repository.UsuarioOAuth2CuentaRepository;
import com.upsjb.ms1.repository.UsuarioRepository;
import com.upsjb.ms1.security.jwt.JwtTokenService;
import com.upsjb.ms1.security.oauth2.OAuth2UserInfo;
import com.upsjb.ms1.security.principal.AuthenticatedUserContext;
import com.upsjb.ms1.security.roles.SecurityRoles;
import com.upsjb.ms1.service.contract.AuditoriaSeguridadService;
import com.upsjb.ms1.service.contract.AuthService;
import com.upsjb.ms1.service.contract.LoginAttemptService;
import com.upsjb.ms1.service.contract.SesionService;
import com.upsjb.ms1.service.contract.UsuarioIpHistorialService;
import com.upsjb.ms1.shared.audit.AuditContext;
import com.upsjb.ms1.shared.audit.AuditContextHolder;
import com.upsjb.ms1.shared.exception.BusinessException;
import com.upsjb.ms1.shared.exception.ConflictException;
import com.upsjb.ms1.shared.exception.NotFoundException;
import com.upsjb.ms1.shared.exception.UnauthorizedException;
import com.upsjb.ms1.shared.exception.ValidationException;
import com.upsjb.ms1.util.RandomTokenUtil;
import com.upsjb.ms1.util.StringNormalizer;
import com.upsjb.ms1.validator.AuthValidator;
import com.upsjb.ms1.validator.SesionValidator;
import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthServiceImpl implements AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);

    private static final String DEFAULT_IP = "0.0.0.0";
    private static final String DEFAULT_USER_AGENT = "UNKNOWN";
    private static final int USERNAME_MAX_LENGTH = 60;
    private static final int NAME_MAX_LENGTH = 120;
    private static final int DISPLAY_NAME_MAX_LENGTH = 160;
    private static final int AVATAR_URL_MAX_LENGTH = 500;
    private static final int OAUTH2_INTERNAL_PASSWORD_TOKEN_BYTES = 32;

    private final UsuarioRepository usuarioRepository;
    private final UsuarioOAuth2CuentaRepository usuarioOAuth2CuentaRepository;
    private final RolRepository rolRepository;
    private final UsuarioMapper usuarioMapper;
    private final AuthValidator authValidator;
    private final SesionValidator sesionValidator;
    private final AuthPolicy authPolicy;
    private final JwtTokenService jwtTokenService;
    private final PasswordEncoder passwordEncoder;
    private final AuditoriaSeguridadService auditoriaSeguridadService;
    private final LoginAttemptService loginAttemptService;
    private final UsuarioIpHistorialService usuarioIpHistorialService;
    private final SesionService sesionService;
    private final AppPropertiesConfig appProperties;
    private final Clock clock;

    public AuthServiceImpl(
            UsuarioRepository usuarioRepository,
            UsuarioOAuth2CuentaRepository usuarioOAuth2CuentaRepository,
            RolRepository rolRepository,
            UsuarioMapper usuarioMapper,
            AuthValidator authValidator,
            SesionValidator sesionValidator,
            AuthPolicy authPolicy,
            JwtTokenService jwtTokenService,
            PasswordEncoder passwordEncoder,
            AuditoriaSeguridadService auditoriaSeguridadService,
            LoginAttemptService loginAttemptService,
            UsuarioIpHistorialService usuarioIpHistorialService,
            SesionService sesionService,
            AppPropertiesConfig appProperties,
            Clock clock
    ) {
        this.usuarioRepository = usuarioRepository;
        this.usuarioOAuth2CuentaRepository = usuarioOAuth2CuentaRepository;
        this.rolRepository = rolRepository;
        this.usuarioMapper = usuarioMapper;
        this.authValidator = authValidator;
        this.sesionValidator = sesionValidator;
        this.authPolicy = authPolicy;
        this.jwtTokenService = jwtTokenService;
        this.passwordEncoder = passwordEncoder;
        this.auditoriaSeguridadService = auditoriaSeguridadService;
        this.loginAttemptService = loginAttemptService;
        this.usuarioIpHistorialService = usuarioIpHistorialService;
        this.sesionService = sesionService;
        this.appProperties = appProperties;
        this.clock = clock;
    }

    @Override
    @Transactional
    public AuthTokenResponseDto login(LoginRequestDto request) {
        return executeWithTechnicalLog(
                "login",
                "ANONIMO",
                request == null ? null : request.usernameOrEmail(),
                () -> {
                    String usernameOrEmail = authValidator.requireCredentialIdentifier(
                            request == null ? null : request.usernameOrEmail()
                    );

                    Usuario usuario = requireUsuarioForLogin(usernameOrEmail);
                    String password = authValidator.requirePassword(request == null ? null : request.password());

                    if (!passwordEncoder.matches(password, usuario.getPasswordHash())) {
                        loginAttemptService.recordFailure(
                                usuario,
                                usernameOrEmail,
                                TipoLogin.CREDENCIALES,
                                "CREDENCIALES_INVALIDAS",
                                "Contraseña incorrecta."
                        );

                        applyLockIfFailedAttemptsExceeded(usuario, usernameOrEmail);

                        auditoriaSeguridadService.registerFailure(
                                TipoAuditoriaSeguridad.LOGIN_FALLIDO,
                                usuario,
                                usuario,
                                "Intento de login fallido por contraseña inválida."
                        );

                        throw new UnauthorizedException(
                                "CREDENCIALES_INVALIDAS",
                                "Las credenciales ingresadas no son válidas."
                        );
                    }

                    Instant now = Instant.now(clock);

                    SesionService.CreatedSession createdSession = sesionService.createSession(
                            usuario,
                            TipoLogin.CREDENCIALES,
                            request == null ? null : request.deviceFingerprint(),
                            resolveIpAddress(),
                            resolveUserAgent()
                    );

                    usuario.registrarLoginExitoso(now);
                    usuarioRepository.save(usuario);

                    loginAttemptService.recordSuccess(
                            usuario,
                            usernameOrEmail,
                            TipoLogin.CREDENCIALES
                    );

                    usuarioIpHistorialService.registerUsage(
                            usuario,
                            resolveIpAddress(),
                            resolveUserAgent()
                    );

                    auditoriaSeguridadService.registerSuccess(
                            TipoAuditoriaSeguridad.LOGIN_EXITOSO,
                            usuario,
                            usuario,
                            "Login exitoso con credenciales."
                    );

                    return buildTokenResponse(
                            usuario,
                            createdSession.session(),
                            createdSession.refreshToken(),
                            createdSession.response()
                    );
                }
        );
    }

    @Override
    @Transactional
    public AuthTokenResponseDto loginOAuth2(
            OAuth2UserInfo userInfo,
            String deviceFingerprint,
            String ipAddress,
            String userAgent
    ) {
        String normalizedIpAddress = normalizeIpAddress(ipAddress);
        String normalizedUserAgent = normalizeUserAgent(userAgent);

        return executeWithTechnicalLog(
                "loginOAuth2",
                "OAUTH2",
                userInfo == null ? null : userInfo.proveedor(),
                () -> {
                    try {
                        return doLoginOAuth2(
                                userInfo,
                                deviceFingerprint,
                                normalizedIpAddress,
                                normalizedUserAgent
                        );
                    } catch (BusinessException exception) {
                        registerOAuth2Failure(
                                userInfo,
                                exception,
                                normalizedIpAddress,
                                normalizedUserAgent
                        );
                        throw exception;
                    }
                }
        );
    }

    @Override
    @Transactional
    public AuthTokenResponseDto refresh(RefreshTokenRequestDto request) {
        return executeWithTechnicalLog(
                "refresh",
                "REFRESH_TOKEN",
                "ROTACION_REFRESH_TOKEN",
                () -> {
                    SesionService.RotatedSession rotatedSession = sesionService.rotateRefreshToken(
                            request == null ? null : request.refreshToken()
                    );

                    Usuario usuario = requireUsuarioFromSession(rotatedSession.session());

                    usuarioIpHistorialService.registerUsage(
                            usuario,
                            resolveIpAddress(),
                            resolveUserAgent()
                    );

                    return buildTokenResponse(
                            usuario,
                            rotatedSession.session(),
                            rotatedSession.refreshToken(),
                            rotatedSession.response()
                    );
                }
        );
    }

    @Override
    @Transactional
    public SessionResponseDto logout(
            AuthenticatedUserContext actor,
            LogoutRequestDto request
    ) {
        return executeWithTechnicalLog(
                "logout",
                actorRef(actor),
                request == null ? null : request.idSesion(),
                () -> {
                    authPolicy.ensureCanLogoutCurrentSession(actor);

                    UsuarioSesion sesion = resolveLogoutSession(actor, request);
                    sesionValidator.validatePerteneceAUsuario(sesion, actor.idUsuario());

                    return sesionService.revokeSession(
                            actor,
                            sesion.getId(),
                            "Logout solicitado por el usuario."
                    );
                }
        );
    }

    @Override
    @Transactional
    public int logoutAll(AuthenticatedUserContext actor) {
        return executeWithTechnicalLog(
                "logoutAll",
                actorRef(actor),
                actor == null ? null : actor.idUsuario(),
                () -> {
                    authPolicy.ensureCanLogoutAllOwnSessions(actor);

                    return sesionService.revokeOwnActiveSessions(
                            actor,
                            "Logout global solicitado por el usuario."
                    );
                }
        );
    }

    @Override
    @Transactional(readOnly = true)
    public AuthUserResponseDto me(AuthenticatedUserContext actor) {
        return executeWithTechnicalLog(
                "me",
                actorRef(actor),
                actor == null ? null : actor.idUsuario(),
                () -> {
                    authPolicy.ensureCanViewCurrentUser(actor);

                    Usuario usuario = usuarioRepository.findWithRolById(actor.idUsuario())
                            .orElseThrow(() -> new NotFoundException(
                                    "USUARIO_NO_ENCONTRADO",
                                    "No se encontró el usuario autenticado."
                            ));

                    return usuarioMapper.toAuthUserResponse(usuario);
                }
        );
    }

    private AuthTokenResponseDto doLoginOAuth2(
            OAuth2UserInfo userInfo,
            String deviceFingerprint,
            String ipAddress,
            String userAgent
    ) {
        validateOAuth2UserInfo(userInfo);

        String email = requireVerifiedOAuth2Email(userInfo);
        Instant now = Instant.now(clock);

        UsuarioOAuth2Cuenta cuenta = usuarioOAuth2CuentaRepository
                .findByProveedorAndProviderUserId(userInfo.proveedor(), userInfo.providerUserId())
                .orElse(null);

        Usuario usuario;
        boolean cuentaCreada = false;
        boolean usuarioCreadoPorOAuth2 = false;

        if (cuenta != null) {
            validateOAuth2CuentaActiva(cuenta);

            usuario = requireUsuarioFromOAuth2Cuenta(cuenta);
            authValidator.validateUsuarioPuedeAutenticarse(usuario);

            updateOAuth2Cuenta(cuenta, userInfo, now);
        } else {
            OAuth2UserResolution resolution = resolveOrCreateOAuth2Usuario(userInfo, email);

            usuario = resolution.usuario();
            usuarioCreadoPorOAuth2 = resolution.usuarioCreado();

            authValidator.validateUsuarioPuedeAutenticarse(usuario);

            cuenta = createOAuth2Cuenta(usuario, userInfo, now);
            cuentaCreada = true;
        }

        usuario.registrarLoginExitoso(now);
        Usuario savedUsuario = usuarioRepository.save(usuario);
        UsuarioOAuth2Cuenta savedCuenta = usuarioOAuth2CuentaRepository.save(cuenta);

        String normalizedIpAddress = normalizeIpAddress(ipAddress);
        String normalizedUserAgent = normalizeUserAgent(userAgent);

        SesionService.CreatedSession createdSession = sesionService.createSession(
                savedUsuario,
                TipoLogin.OAUTH2,
                deviceFingerprint,
                normalizedIpAddress,
                normalizedUserAgent
        );

        loginAttemptService.recordSuccess(
                savedUsuario,
                email,
                TipoLogin.OAUTH2,
                normalizedIpAddress,
                normalizedUserAgent
        );

        usuarioIpHistorialService.registerUsage(
                savedUsuario,
                normalizedIpAddress,
                normalizedUserAgent
        );

        if (usuarioCreadoPorOAuth2) {
            auditoriaSeguridadService.registerSuccess(
                    TipoAuditoriaSeguridad.USUARIO_CREADO,
                    savedUsuario,
                    savedUsuario,
                    "Usuario CLIENTE creado automáticamente mediante OAuth2."
            );
        }

        if (cuentaCreada) {
            auditoriaSeguridadService.registerSuccess(
                    TipoAuditoriaSeguridad.OAUTH2_CUENTA_VINCULADA,
                    savedUsuario,
                    savedUsuario,
                    "Cuenta OAuth2 vinculada con proveedor "
                            + savedCuenta.getProveedor()
                            + " para usuario con rol "
                            + resolveUsuarioRolCodigo(savedUsuario)
                            + "."
            );
        }

        auditoriaSeguridadService.registerSuccess(
                TipoAuditoriaSeguridad.OAUTH2_LOGIN_EXITOSO,
                savedUsuario,
                savedUsuario,
                "Login OAuth2 exitoso con proveedor " + savedCuenta.getProveedor() + "."
        );

        return buildTokenResponse(
                savedUsuario,
                createdSession.session(),
                createdSession.refreshToken(),
                createdSession.response()
        );
    }

    private OAuth2UserResolution resolveOrCreateOAuth2Usuario(
            OAuth2UserInfo userInfo,
            String email
    ) {
        Usuario existing = usuarioRepository.findByEmail_ValueIgnoreCase(email).orElse(null);

        if (existing != null) {
            validateProviderNotAlreadyLinkedToDifferentAccount(userInfo.proveedor(), existing.getId());

            if (userInfo.emailVerified()) {
                existing.marcarEmailVerificado();
            }

            return new OAuth2UserResolution(existing, false);
        }

        Rol clienteRole = rolRepository.findByCodigoIgnoreCase(SecurityRoles.CLIENTE)
                .orElseThrow(() -> new ValidationException(
                        "ROL_CLIENTE_NO_CONFIGURADO",
                        "El rol CLIENTE debe existir y estar activo para crear usuarios OAuth2."
                ));

        if (!clienteRole.estaActivo()) {
            throw new ValidationException(
                    "ROL_CLIENTE_INACTIVO",
                    "El rol CLIENTE no está activo para crear usuarios OAuth2."
            );
        }

        String username = generateAvailableUsername(userInfo);
        String[] nombrePartes = resolveNames(userInfo, username);

        Usuario usuario = Usuario.builder()
                .rol(clienteRole)
                .username(UsernameValue.of(username))
                .email(EmailValue.of(email))
                .passwordHash(generateOAuth2InternalPasswordHash())
                .nombres(nombrePartes[0])
                .apellidos(nombrePartes[1])
                .estado(EstadoRegistro.ACTIVO)
                .emailVerificado(true)
                .requiereCambioPassword(false)
                .build();

        return new OAuth2UserResolution(usuarioRepository.save(usuario), true);
    }

    private String generateOAuth2InternalPasswordHash() {
        /*
         * BCrypt acepta como entrada máxima 72 bytes. RandomTokenUtil.secureToken()
         * genera 64 bytes aleatorios y luego Base64Url, lo que produce una cadena
         * mayor a ese límite. Para usuarios OAuth2 no se usa contraseña local, pero
         * la entidad exige password_hash no nulo; por eso se guarda un secreto
         * interno aleatorio de 32 bytes, codificado antes de persistir.
         */
        return passwordEncoder.encode(RandomTokenUtil.secureToken(OAUTH2_INTERNAL_PASSWORD_TOKEN_BYTES));
    }

    private UsuarioOAuth2Cuenta createOAuth2Cuenta(
            Usuario usuario,
            OAuth2UserInfo userInfo,
            Instant now
    ) {
        validateProviderSubjectDisponible(userInfo.proveedor(), userInfo.providerUserId());

        return UsuarioOAuth2Cuenta.builder()
                .usuario(usuario)
                .proveedor(userInfo.proveedor())
                .providerUserId(StringNormalizer.normalizeSpaces(userInfo.providerUserId()))
                .email(EmailValue.of(requireVerifiedOAuth2Email(userInfo)))
                .emailVerificado(userInfo.emailVerified())
                .displayName(truncate(StringNormalizer.normalizeSpaces(userInfo.name()), DISPLAY_NAME_MAX_LENGTH))
                .avatarUrl(truncate(StringNormalizer.trimToNull(userInfo.avatarUrl()), AVATAR_URL_MAX_LENGTH))
                .estado(EstadoRegistro.ACTIVO)
                .ultimoLoginAt(now)
                .build();
    }

    private void updateOAuth2Cuenta(
            UsuarioOAuth2Cuenta cuenta,
            OAuth2UserInfo userInfo,
            Instant now
    ) {
        cuenta.setEmail(EmailValue.of(requireVerifiedOAuth2Email(userInfo)));
        cuenta.setEmailVerificado(userInfo.emailVerified());
        cuenta.setDisplayName(truncate(StringNormalizer.normalizeSpaces(userInfo.name()), DISPLAY_NAME_MAX_LENGTH));
        cuenta.setAvatarUrl(truncate(StringNormalizer.trimToNull(userInfo.avatarUrl()), AVATAR_URL_MAX_LENGTH));
        cuenta.registrarLogin(now);
    }

    private String resolveUsuarioRolCodigo(Usuario usuario) {
        if (usuario == null || usuario.getRol() == null) {
            return "SIN_ROL";
        }

        String codigo = SecurityRoles.normalizeRoleCode(usuario.getRol().getCodigo());
        return codigo == null ? "SIN_ROL" : codigo;
    }

    private void validateOAuth2UserInfo(OAuth2UserInfo userInfo) {
        if (userInfo == null) {
            throw new ValidationException(
                    "OAUTH2_USER_INFO_OBLIGATORIO",
                    "La información OAuth2 del proveedor es obligatoria."
            );
        }

        if (userInfo.proveedor() == null) {
            throw new ValidationException(
                    "OAUTH2_PROVEEDOR_OBLIGATORIO",
                    "El proveedor OAuth2 es obligatorio."
            );
        }

        if (StringNormalizer.normalizeSpaces(userInfo.providerUserId()) == null) {
            throw new ValidationException(
                    "OAUTH2_PROVIDER_USER_ID_OBLIGATORIO",
                    "El identificador del usuario OAuth2 es obligatorio."
            );
        }
    }

    private String requireVerifiedOAuth2Email(OAuth2UserInfo userInfo) {
        String email;

        try {
            email = EmailValue.of(userInfo == null ? null : userInfo.email()).getValue();
        } catch (IllegalArgumentException exception) {
            throw new UnauthorizedException(
                    "OAUTH2_EMAIL_INVALIDO",
                    "El proveedor OAuth2 no entregó un correo válido."
            );
        }

        if (!userInfo.emailVerified()) {
            throw new UnauthorizedException(
                    "OAUTH2_EMAIL_NO_VERIFICADO",
                    "El correo del proveedor OAuth2 debe estar verificado."
            );
        }

        return email;
    }

    private void validateOAuth2CuentaActiva(UsuarioOAuth2Cuenta cuenta) {
        if (cuenta == null || !cuenta.estaActiva()) {
            throw new UnauthorizedException(
                    "OAUTH2_CUENTA_INACTIVA",
                    "La cuenta OAuth2 vinculada no está activa."
            );
        }
    }

    private Usuario requireUsuarioFromOAuth2Cuenta(UsuarioOAuth2Cuenta cuenta) {
        Usuario usuario = cuenta == null ? null : cuenta.getUsuario();

        if (usuario == null || usuario.getId() == null) {
            throw new UnauthorizedException(
                    "OAUTH2_USUARIO_INTERNO_INVALIDO",
                    "La cuenta OAuth2 no tiene un usuario interno válido asociado."
            );
        }

        return usuario;
    }

    private void validateProviderNotAlreadyLinkedToDifferentAccount(
            ProveedorOAuth2 proveedor,
            Long idUsuario
    ) {
        usuarioOAuth2CuentaRepository.findByProveedorAndUsuario_Id(proveedor, idUsuario)
                .ifPresent(existing -> {
                    throw new ConflictException(
                            "OAUTH2_PROVEEDOR_YA_VINCULADO",
                            "El usuario ya tiene una cuenta vinculada para este proveedor OAuth2."
                    );
                });
    }

    private void validateProviderSubjectDisponible(
            ProveedorOAuth2 proveedor,
            String providerUserId
    ) {
        if (usuarioOAuth2CuentaRepository.existsByProveedorAndProviderUserId(proveedor, providerUserId)) {
            throw new ConflictException(
                    "OAUTH2_CUENTA_DUPLICADA",
                    "La cuenta OAuth2 ya se encuentra vinculada a otro usuario."
            );
        }
    }

    private void registerOAuth2Failure(
            OAuth2UserInfo userInfo,
            BusinessException exception,
            String ipAddress,
            String userAgent
    ) {
        String identifier = resolveOAuth2AttemptIdentifier(userInfo);

        loginAttemptService.recordFailure(
                null,
                identifier,
                TipoLogin.OAUTH2,
                exception.getCode(),
                exception.getMessage(),
                normalizeIpAddress(ipAddress),
                normalizeUserAgent(userAgent)
        );

        auditoriaSeguridadService.registerFailure(
                TipoAuditoriaSeguridad.OAUTH2_LOGIN_FALLIDO,
                null,
                null,
                "Login OAuth2 fallido: " + exception.getCode() + "."
        );
    }

    private String resolveOAuth2AttemptIdentifier(OAuth2UserInfo userInfo) {
        String email = StringNormalizer.lower(userInfo == null ? null : userInfo.email());

        if (email != null) {
            return email;
        }

        if (userInfo == null) {
            return "OAUTH2_DESCONOCIDO";
        }

        String providerUserId = StringNormalizer.normalizeSpaces(userInfo.providerUserId());
        String provider = userInfo.proveedor() == null ? "OAUTH2" : userInfo.proveedor().name();

        return providerUserId == null ? provider : provider + ":" + providerUserId;
    }

    private Usuario requireUsuarioForLogin(String usernameOrEmail) {
        try {
            return authValidator.requireUsuarioForLogin(usernameOrEmail);
        } catch (UnauthorizedException exception) {
            loginAttemptService.recordFailure(
                    null,
                    usernameOrEmail,
                    TipoLogin.CREDENCIALES,
                    exception.getCode(),
                    exception.getMessage()
            );

            auditoriaSeguridadService.registerFailure(
                    TipoAuditoriaSeguridad.LOGIN_FALLIDO,
                    null,
                    null,
                    "Intento de login fallido."
            );

            throw exception;
        }
    }

    private void applyLockIfFailedAttemptsExceeded(
            Usuario usuario,
            String usernameOrEmail
    ) {
        if (usuario == null || usuario.getId() == null) {
            return;
        }

        Instant now = Instant.now(clock);
        Instant since = now.minusSeconds(appProperties.getSecurity().getLockMinutes() * 60L);

        long failedAttempts = loginAttemptService.countRecentFailuresByUsernameOrEmail(
                usernameOrEmail,
                since
        );

        if (failedAttempts < appProperties.getSecurity().getMaxLoginAttempts()) {
            return;
        }

        usuario.bloquearTemporalmente(
                now.plusSeconds(appProperties.getSecurity().getLockMinutes() * 60L)
        );

        usuarioRepository.save(usuario);

        auditoriaSeguridadService.registerWarning(
                TipoAuditoriaSeguridad.USUARIO_BLOQUEADO,
                usuario,
                usuario,
                "Usuario bloqueado temporalmente por intentos fallidos de login."
        );
    }

    private AuthTokenResponseDto buildTokenResponse(
            Usuario usuario,
            UsuarioSesion sesion,
            String refreshToken,
            SessionResponseDto sessionResponse
    ) {
        String accessToken = jwtTokenService.generateAccessToken(usuario, sesion);
        Instant accessTokenExpiresAt = jwtTokenService.getExpiration(accessToken);

        return new AuthTokenResponseDto(
                AuthTokenResponseDto.BEARER,
                accessToken,
                refreshToken,
                accessTokenExpiresAt,
                sesion.getExpiresAt(),
                usuarioMapper.toAuthUserResponse(usuario),
                sessionResponse
        );
    }

    private UsuarioSesion resolveLogoutSession(
            AuthenticatedUserContext actor,
            LogoutRequestDto request
    ) {
        if (request != null && request.refreshToken() != null && !request.refreshToken().isBlank()) {
            return sesionValidator.requireByRefreshToken(request.refreshToken());
        }

        if (request != null && request.idSesion() != null) {
            return sesionValidator.requireById(request.idSesion());
        }

        if (actor != null && actor.sessionId() != null) {
            return sesionValidator.requireById(actor.sessionId());
        }

        throw new ValidationException(
                "SESION_LOGOUT_IDENTIFICADOR_OBLIGATORIO",
                "Debes enviar el identificador de sesión o el refresh token."
        );
    }

    private Usuario requireUsuarioFromSession(UsuarioSesion sesion) {
        Usuario usuario = sesion == null ? null : sesion.getUsuario();

        if (usuario == null || usuario.getId() == null) {
            throw new UnauthorizedException(
                    "SESION_USUARIO_INVALIDO",
                    "La sesión no tiene un usuario válido asociado."
            );
        }

        return usuario;
    }

    private String generateAvailableUsername(OAuth2UserInfo userInfo) {
        String base = normalizeUsernameCandidate(firstNonBlank(
                userInfo.username(),
                userInfo.email() == null ? null : userInfo.email().split("@", 2)[0],
                userInfo.name(),
                "cliente"
        ));

        String candidate = truncate(base, USERNAME_MAX_LENGTH);

        if (candidate.length() < 4) {
            candidate = (candidate + "user").substring(0, 4);
        }

        if (!usuarioRepository.existsByUsername_ValueIgnoreCase(candidate)) {
            return UsernameValue.of(candidate).getValue();
        }

        for (int counter = 2; counter <= 9999; counter++) {
            String suffix = "-" + counter;
            int maxBaseLength = USERNAME_MAX_LENGTH - suffix.length();
            String next = truncate(candidate, maxBaseLength) + suffix;

            if (!usuarioRepository.existsByUsername_ValueIgnoreCase(next)) {
                return UsernameValue.of(next).getValue();
            }
        }

        throw new ConflictException(
                "OAUTH2_USERNAME_NO_DISPONIBLE",
                "No fue posible generar un username disponible para el usuario OAuth2."
        );
    }

    private String normalizeUsernameCandidate(String value) {
        String normalized = StringNormalizer.removeAccents(value);

        if (normalized == null) {
            normalized = "cliente";
        }

        normalized = normalized.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9._-]", ".")
                .replaceAll("[.]{2,}", ".")
                .replaceAll("^[._-]+", "")
                .replaceAll("[._-]+$", "");

        if (normalized.length() < 4) {
            normalized = normalized + "user";
        }

        return normalized;
    }

    private String[] resolveNames(
            OAuth2UserInfo userInfo,
            String username
    ) {
        String displayName = StringNormalizer.normalizeSpaces(userInfo.name());

        if (displayName == null) {
            displayName = username;
        }

        String[] parts = displayName.split("\\s+");

        if (parts.length == 1) {
            return new String[]{truncate(parts[0], NAME_MAX_LENGTH), "OAuth2"};
        }

        String apellido = parts[parts.length - 1];
        String nombres = displayName.substring(0, displayName.length() - apellido.length()).trim();

        return new String[]{
                truncate(nombres, NAME_MAX_LENGTH),
                truncate(apellido, NAME_MAX_LENGTH)
        };
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }

        for (String value : values) {
            String normalized = StringNormalizer.normalizeSpaces(value);
            if (normalized != null) {
                return normalized;
            }
        }

        return null;
    }

    private String normalizeIpAddress(String value) {
        String normalized = StringNormalizer.trimToNull(value);
        return normalized == null ? DEFAULT_IP : normalized;
    }

    private String normalizeUserAgent(String value) {
        String normalized = StringNormalizer.trimToNull(value);
        return normalized == null ? DEFAULT_USER_AGENT : normalized;
    }

    private String resolveIpAddress() {
        AuditContext context = AuditContextHolder.get();

        if (context == null) {
            return DEFAULT_IP;
        }

        String ipAddress = StringNormalizer.trimToNull(context.ipAddress());
        return ipAddress == null ? DEFAULT_IP : ipAddress;
    }

    private String resolveUserAgent() {
        AuditContext context = AuditContextHolder.get();

        if (context == null) {
            return DEFAULT_USER_AGENT;
        }

        String userAgent = StringNormalizer.trimToNull(context.userAgent());
        return userAgent == null ? DEFAULT_USER_AGENT : userAgent;
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
                    "Error inesperado en AuthServiceImpl.{} - actor={}, recurso={}, requestId={}",
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

    private record OAuth2UserResolution(
            Usuario usuario,
            boolean usuarioCreado
    ) {
    }
}
