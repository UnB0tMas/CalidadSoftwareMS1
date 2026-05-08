package com.upsjb.ms1.service.impl;

import com.upsjb.ms1.domain.entity.Rol;
import com.upsjb.ms1.domain.entity.Usuario;
import com.upsjb.ms1.domain.entity.UsuarioSesion;
import com.upsjb.ms1.domain.enums.TipoAuditoriaSeguridad;
import com.upsjb.ms1.domain.enums.TipoLogin;
import com.upsjb.ms1.dto.auth.request.RegisterClienteRequestDto;
import com.upsjb.ms1.dto.auth.response.AuthTokenResponseDto;
import com.upsjb.ms1.dto.auth.response.SessionResponseDto;
import com.upsjb.ms1.dto.usuario.request.UsuarioCreateRequestDto;
import com.upsjb.ms1.mapper.UsuarioMapper;
import com.upsjb.ms1.repository.RolRepository;
import com.upsjb.ms1.repository.UsuarioRepository;
import com.upsjb.ms1.security.jwt.JwtTokenService;
import com.upsjb.ms1.security.roles.SecurityRoles;
import com.upsjb.ms1.service.contract.AuditoriaSeguridadService;
import com.upsjb.ms1.service.contract.ClienteRegistrationService;
import com.upsjb.ms1.service.contract.LoginAttemptService;
import com.upsjb.ms1.service.contract.SesionService;
import com.upsjb.ms1.service.contract.UsuarioIpHistorialService;
import com.upsjb.ms1.shared.audit.AuditContext;
import com.upsjb.ms1.shared.audit.AuditContextHolder;
import com.upsjb.ms1.shared.exception.BusinessException;
import com.upsjb.ms1.shared.exception.ValidationException;
import com.upsjb.ms1.util.StringNormalizer;
import com.upsjb.ms1.validator.RolValidator;
import com.upsjb.ms1.validator.UsuarioValidator;
import java.time.Clock;
import java.time.Instant;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ClienteRegistrationServiceImpl implements ClienteRegistrationService {

    private static final Logger log = LoggerFactory.getLogger(ClienteRegistrationServiceImpl.class);

    private static final String DEFAULT_IP = "0.0.0.0";
    private static final String DEFAULT_USER_AGENT = "UNKNOWN";

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final UsuarioMapper usuarioMapper;
    private final UsuarioValidator usuarioValidator;
    private final RolValidator rolValidator;
    private final PasswordEncoder passwordEncoder;
    private final SesionService sesionService;
    private final JwtTokenService jwtTokenService;
    private final LoginAttemptService loginAttemptService;
    private final UsuarioIpHistorialService usuarioIpHistorialService;
    private final AuditoriaSeguridadService auditoriaSeguridadService;
    private final Clock clock;

    public ClienteRegistrationServiceImpl(
            UsuarioRepository usuarioRepository,
            RolRepository rolRepository,
            UsuarioMapper usuarioMapper,
            UsuarioValidator usuarioValidator,
            RolValidator rolValidator,
            PasswordEncoder passwordEncoder,
            SesionService sesionService,
            JwtTokenService jwtTokenService,
            LoginAttemptService loginAttemptService,
            UsuarioIpHistorialService usuarioIpHistorialService,
            AuditoriaSeguridadService auditoriaSeguridadService,
            Clock clock
    ) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.usuarioMapper = usuarioMapper;
        this.usuarioValidator = usuarioValidator;
        this.rolValidator = rolValidator;
        this.passwordEncoder = passwordEncoder;
        this.sesionService = sesionService;
        this.jwtTokenService = jwtTokenService;
        this.loginAttemptService = loginAttemptService;
        this.usuarioIpHistorialService = usuarioIpHistorialService;
        this.auditoriaSeguridadService = auditoriaSeguridadService;
        this.clock = clock;
    }

    @Override
    @Transactional
    public AuthTokenResponseDto register(RegisterClienteRequestDto request) {
        return executeWithTechnicalLog(
                "register",
                "ANONIMO",
                request == null ? null : request.email(),
                () -> doRegister(request)
        );
    }

    private AuthTokenResponseDto doRegister(RegisterClienteRequestDto request) {
        validateRequestPresent(request);

        Rol clienteRole = requireClienteRoleActivo();

        usuarioValidator.validateCreate(
                request.username(),
                request.email(),
                clienteRole.getId(),
                request.password(),
                request.confirmarPassword(),
                request.nombres(),
                request.apellidos()
        );

        String passwordHash = passwordEncoder.encode(request.password());

        UsuarioCreateRequestDto createRequest = new UsuarioCreateRequestDto(
                request.username(),
                request.email(),
                request.password(),
                request.nombres(),
                request.apellidos(),
                clienteRole.getId(),
                false,
                false
        );

        Usuario usuario = usuarioMapper.toEntity(createRequest, clienteRole, passwordHash);
        usuario.setEmailVerificado(false);
        usuario.setRequiereCambioPassword(false);

        Usuario savedUsuario = usuarioRepository.save(usuario);

        SesionService.CreatedSession createdSession = sesionService.createSession(
                savedUsuario,
                TipoLogin.CREDENCIALES,
                request.deviceFingerprint(),
                resolveIpAddress(),
                resolveUserAgent()
        );

        Instant now = Instant.now(clock);
        savedUsuario.registrarLoginExitoso(now);
        savedUsuario = usuarioRepository.save(savedUsuario);

        loginAttemptService.recordSuccess(
                savedUsuario,
                savedUsuario.getEmail().getValue(),
                TipoLogin.CREDENCIALES
        );

        usuarioIpHistorialService.registerUsage(
                savedUsuario,
                resolveIpAddress(),
                resolveUserAgent()
        );

        auditoriaSeguridadService.registerSuccess(
                TipoAuditoriaSeguridad.USUARIO_CREADO,
                savedUsuario,
                savedUsuario,
                "Registro público de cliente."
        );

        auditoriaSeguridadService.registerSuccess(
                TipoAuditoriaSeguridad.LOGIN_EXITOSO,
                savedUsuario,
                savedUsuario,
                "Login automático posterior al registro de cliente."
        );

        return buildTokenResponse(
                savedUsuario,
                createdSession.session(),
                createdSession.refreshToken(),
                createdSession.response()
        );
    }

    private void validateRequestPresent(RegisterClienteRequestDto request) {
        if (request == null) {
            throw new ValidationException(
                    "REGISTRO_CLIENTE_REQUEST_OBLIGATORIO",
                    "La solicitud de registro de cliente es obligatoria."
            );
        }
    }

    private Rol requireClienteRoleActivo() {
        Rol clienteRole = rolRepository.findByCodigoIgnoreCase(SecurityRoles.CLIENTE)
                .orElseThrow(() -> new ValidationException(
                        "ROL_CLIENTE_NO_CONFIGURADO",
                        "El rol CLIENTE debe existir y estar activo para registrar clientes."
                ));

        rolValidator.validateActivo(clienteRole);

        return clienteRole;
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

    private String resolveIpAddress() {
        AuditContext context = AuditContextHolder.get();
        String ipAddress = context == null ? null : StringNormalizer.trimToNull(context.ipAddress());
        return ipAddress == null ? DEFAULT_IP : ipAddress;
    }

    private String resolveUserAgent() {
        AuditContext context = AuditContextHolder.get();
        String userAgent = context == null ? null : StringNormalizer.trimToNull(context.userAgent());
        return userAgent == null ? DEFAULT_USER_AGENT : userAgent;
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
                    "Error inesperado en ClienteRegistrationServiceImpl.{} - actor={}, recurso={}, requestId={}",
                    method,
                    actor,
                    resource,
                    AuditContextHolder.getRequestIdOrNull(),
                    exception
            );
            throw exception;
        }
    }
}