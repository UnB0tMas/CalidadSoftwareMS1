// src/main/java/com/upsjb/ms1/service/impl/PasswordResetServiceImpl.java
package com.upsjb.ms1.service.impl;

import com.upsjb.ms1.config.AppPropertiesConfig;
import com.upsjb.ms1.domain.entity.PasswordResetToken;
import com.upsjb.ms1.domain.entity.Usuario;
import com.upsjb.ms1.domain.entity.VerificacionCodigo;
import com.upsjb.ms1.domain.enums.EstadoSesion;
import com.upsjb.ms1.domain.enums.EstadoVerificacionCodigo;
import com.upsjb.ms1.domain.enums.MotivoRevocacionSesion;
import com.upsjb.ms1.domain.enums.TipoAuditoriaSeguridad;
import com.upsjb.ms1.domain.enums.TipoCodigoVerificacion;
import com.upsjb.ms1.domain.enums.TipoToken;
import com.upsjb.ms1.domain.value.EmailValue;
import com.upsjb.ms1.domain.value.IpAddressValue;
import com.upsjb.ms1.domain.value.UserAgentValue;
import com.upsjb.ms1.dto.password.request.ChangePasswordRequestDto;
import com.upsjb.ms1.dto.password.request.ConfirmChangePasswordRequestDto;
import com.upsjb.ms1.dto.password.request.ForgotPasswordRequestDto;
import com.upsjb.ms1.dto.password.request.ResetPasswordRequestDto;
import com.upsjb.ms1.dto.password.response.ChangePasswordVerificationResponseDto;
import com.upsjb.ms1.dto.password.response.PasswordOperationResponseDto;
import com.upsjb.ms1.mapper.PasswordResetTokenMapper;
import com.upsjb.ms1.policy.PasswordPolicy;
import com.upsjb.ms1.repository.PasswordResetTokenRepository;
import com.upsjb.ms1.repository.UsuarioRepository;
import com.upsjb.ms1.repository.UsuarioSesionRepository;
import com.upsjb.ms1.repository.VerificacionCodigoRepository;
import com.upsjb.ms1.security.principal.AuthenticatedUserContext;
import com.upsjb.ms1.service.contract.AuditoriaSeguridadService;
import com.upsjb.ms1.service.contract.EmailService;
import com.upsjb.ms1.service.contract.PasswordResetService;
import com.upsjb.ms1.shared.audit.AuditContext;
import com.upsjb.ms1.shared.audit.AuditContextHolder;
import com.upsjb.ms1.shared.exception.BusinessException;
import com.upsjb.ms1.shared.exception.ValidationException;
import com.upsjb.ms1.util.EmailMaskingUtil;
import com.upsjb.ms1.util.RandomCodeUtil;
import com.upsjb.ms1.util.RandomTokenUtil;
import com.upsjb.ms1.util.StringNormalizer;
import com.upsjb.ms1.util.TokenHashUtil;
import com.upsjb.ms1.validator.PasswordValidator;
import com.upsjb.ms1.validator.UsuarioValidator;
import com.upsjb.ms1.validator.VerificacionCodigoValidator;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PasswordResetServiceImpl implements PasswordResetService {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetServiceImpl.class);

    private static final String GENERIC_FORGOT_MESSAGE =
            "Si el correo está registrado, se enviaron instrucciones de recuperación.";

    private static final String PASSWORD_CHANGE_CODE_SENT_MESSAGE =
            "Código de verificación enviado correctamente al correo registrado.";

    private static final String PASSWORD_CHANGED_MESSAGE =
            "La contraseña fue cambiada correctamente.";

    private static final String PASSWORD_SESSION_REVOKE_DETAIL =
            "Sesiones revocadas por operación de contraseña.";

    private final UsuarioRepository usuarioRepository;
    private final UsuarioSesionRepository usuarioSesionRepository;
    private final VerificacionCodigoRepository verificacionCodigoRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordResetTokenMapper passwordResetTokenMapper;
    private final UsuarioValidator usuarioValidator;
    private final PasswordValidator passwordValidator;
    private final VerificacionCodigoValidator verificacionCodigoValidator;
    private final PasswordPolicy passwordPolicy;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final AuditoriaSeguridadService auditoriaSeguridadService;
    private final AppPropertiesConfig appProperties;
    private final Clock clock;

    public PasswordResetServiceImpl(
            UsuarioRepository usuarioRepository,
            UsuarioSesionRepository usuarioSesionRepository,
            VerificacionCodigoRepository verificacionCodigoRepository,
            PasswordResetTokenRepository passwordResetTokenRepository,
            PasswordResetTokenMapper passwordResetTokenMapper,
            UsuarioValidator usuarioValidator,
            PasswordValidator passwordValidator,
            VerificacionCodigoValidator verificacionCodigoValidator,
            PasswordPolicy passwordPolicy,
            PasswordEncoder passwordEncoder,
            EmailService emailService,
            AuditoriaSeguridadService auditoriaSeguridadService,
            AppPropertiesConfig appProperties,
            Clock clock
    ) {
        this.usuarioRepository = usuarioRepository;
        this.usuarioSesionRepository = usuarioSesionRepository;
        this.verificacionCodigoRepository = verificacionCodigoRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.passwordResetTokenMapper = passwordResetTokenMapper;
        this.usuarioValidator = usuarioValidator;
        this.passwordValidator = passwordValidator;
        this.verificacionCodigoValidator = verificacionCodigoValidator;
        this.passwordPolicy = passwordPolicy;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.auditoriaSeguridadService = auditoriaSeguridadService;
        this.appProperties = appProperties;
        this.clock = clock;
    }

    @Override
    @Transactional
    public ChangePasswordVerificationResponseDto requestPasswordChange(
            AuthenticatedUserContext actor,
            ChangePasswordRequestDto request
    ) {
        try {
            passwordPolicy.ensureCanRequestOwnPasswordChange(actor);
            requireChangePasswordRequest(request);

            Usuario usuario = usuarioValidator.requireActivoById(actor.idUsuario());

            passwordValidator.validateCambioPassword(
                    usuario,
                    request.passwordActual(),
                    request.nuevaPassword(),
                    request.confirmarNuevaPassword()
            );

            Instant now = Instant.now(clock);
            String codigoPlano = RandomCodeUtil.defaultCode();
            Instant expiresAt = now.plusSeconds(appProperties.getSecurity().getPasswordChangeCodeMinutes() * 60);

            verificacionCodigoRepository.revokePendingCodesByEmailAndType(
                    usuario.getEmail().getValue(),
                    TipoCodigoVerificacion.CAMBIO_PASSWORD,
                    EstadoVerificacionCodigo.PENDIENTE,
                    EstadoVerificacionCodigo.REVOCADO,
                    now
            );

            VerificacionCodigo codigo = VerificacionCodigo.builder()
                    .usuario(usuario)
                    .email(usuario.getEmail())
                    .tipoCodigo(TipoCodigoVerificacion.CAMBIO_PASSWORD)
                    .codigoHash(TokenHashUtil.hash(codigoPlano))
                    .estado(EstadoVerificacionCodigo.PENDIENTE)
                    .intentosFallidos(0)
                    .maxIntentos(appProperties.getSecurity().getVerificationMaxAttempts())
                    .expiresAt(expiresAt)
                    .ipAddress(resolveIpAddressValue())
                    .userAgent(resolveUserAgentValue())
                    .build();

            verificacionCodigoRepository.save(codigo);

            emailService.sendPasswordChangeVerification(
                    usuario.getEmail().getValue(),
                    nombreCompleto(usuario),
                    codigoPlano,
                    expiresAt
            );

            auditoriaSeguridadService.registerSuccess(
                    TipoAuditoriaSeguridad.CODIGO_ENVIADO,
                    usuario,
                    usuario,
                    "Código de verificación para cambio de contraseña enviado."
            );

            return new ChangePasswordVerificationResponseDto(
                    EmailMaskingUtil.mask(usuario.getEmail().getValue()),
                    expiresAt,
                    codigo.getMaxIntentos(),
                    PASSWORD_CHANGE_CODE_SENT_MESSAGE
            );
        } catch (BusinessException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            logUnexpected("requestPasswordChange", actor, null, exception);
            throw exception;
        }
    }

    @Override
    @Transactional
    public PasswordOperationResponseDto confirmPasswordChange(
            AuthenticatedUserContext actor,
            ConfirmChangePasswordRequestDto request
    ) {
        try {
            passwordPolicy.ensureCanConfirmOwnPasswordChange(actor, actor == null ? null : actor.idUsuario());
            requireConfirmChangePasswordRequest(request);

            Usuario usuario = usuarioValidator.requireActivoById(actor.idUsuario());

            VerificacionCodigo codigo = verificacionCodigoValidator.validateCodigoByUsuario(
                    usuario.getId(),
                    TipoCodigoVerificacion.CAMBIO_PASSWORD,
                    request.codigo()
            );

            passwordValidator.validateCambioPassword(
                    usuario,
                    request.passwordActual(),
                    request.nuevaPassword(),
                    request.confirmarNuevaPassword()
            );

            Instant now = Instant.now(clock);

            usuario.cambiarPasswordHash(passwordEncoder.encode(request.nuevaPassword()), now);
            codigo.marcarValidado(now);

            usuarioRepository.save(usuario);
            verificacionCodigoRepository.save(codigo);

            revokeActiveSessions(usuario.getId(), MotivoRevocacionSesion.PASSWORD_CHANGED, now);

            emailService.sendPasswordChanged(
                    usuario.getEmail().getValue(),
                    nombreCompleto(usuario),
                    now
            );

            auditoriaSeguridadService.registerSuccess(
                    TipoAuditoriaSeguridad.CODIGO_VALIDADO,
                    usuario,
                    usuario,
                    "Código de cambio de contraseña validado correctamente."
            );

            auditoriaSeguridadService.registerSuccess(
                    TipoAuditoriaSeguridad.PASSWORD_CAMBIADO,
                    usuario,
                    usuario,
                    PASSWORD_CHANGED_MESSAGE
            );

            return new PasswordOperationResponseDto(
                    EstadoVerificacionCodigo.VALIDADO,
                    PASSWORD_CHANGED_MESSAGE,
                    null
            );
        } catch (BusinessException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            logUnexpected("confirmPasswordChange", actor, null, exception);
            throw exception;
        }
    }

    @Override
    @Transactional
    public PasswordOperationResponseDto forgotPassword(ForgotPasswordRequestDto request) {
        try {
            passwordPolicy.ensureCanCompletePublicPasswordReset();
            requireForgotPasswordRequest(request);

            String email = normalizeEmail(request.email());

            usuarioRepository.findByEmail_ValueIgnoreCase(email)
                    .ifPresentOrElse(usuario -> {
                        if (isEligibleForPublicPasswordReset(usuario)) {
                            createPasswordResetTokenAndNotify(usuario);
                        }
                    }, () -> auditIgnoredForgotPassword(null));

            return genericForgotPublicResponse();
        } catch (BusinessException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            logUnexpected("forgotPassword", null, request == null ? null : request.email(), exception);
            throw exception;
        }
    }

    @Override
    @Transactional
    public PasswordOperationResponseDto resetPassword(ResetPasswordRequestDto request) {
        try {
            passwordPolicy.ensureCanCompletePublicPasswordReset();
            requireResetPasswordRequest(request);

            String email = normalizeEmail(request.email());
            String tokenPlano = requireToken(request.token());
            PasswordResetToken token = requireResetTokenByPlainToken(tokenPlano);

            validateResetTokenUsable(token);
            validateTokenMatchesEmail(token, email);

            Usuario usuario = token.getUsuario();

            if (usuario == null || usuario.getId() == null) {
                throw new ValidationException(
                        "PASSWORD_RESET_USUARIO_INVALIDO",
                        "El token de recuperación no tiene un usuario válido asociado."
                );
            }

            usuarioValidator.validateActivo(usuario);
            usuarioValidator.validateNoBloqueado(usuario);
            usuarioValidator.validateRolActivo(usuario);

            passwordValidator.validateResetPassword(
                    usuario,
                    request.nuevaPassword(),
                    request.confirmarNuevaPassword()
            );

            Instant now = Instant.now(clock);

            usuario.cambiarPasswordHash(passwordEncoder.encode(request.nuevaPassword()), now);
            token.marcarUsado(now);

            usuarioRepository.save(usuario);
            passwordResetTokenRepository.save(token);

            revokeActiveSessions(usuario.getId(), MotivoRevocacionSesion.PASSWORD_CHANGED, now);

            emailService.sendPasswordChanged(
                    usuario.getEmail().getValue(),
                    nombreCompleto(usuario),
                    now
            );

            auditoriaSeguridadService.registerSuccess(
                    TipoAuditoriaSeguridad.PASSWORD_RESET_COMPLETADO,
                    usuario,
                    usuario,
                    "Recuperación de contraseña completada correctamente."
            );

            return passwordResetTokenMapper.toCompletedResponse(token);
        } catch (BusinessException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            logUnexpected("resetPassword", null, request == null ? null : request.email(), exception);
            throw exception;
        }
    }

    private void requireChangePasswordRequest(ChangePasswordRequestDto request) {
        if (request == null) {
            throw new ValidationException(
                    "PASSWORD_CHANGE_REQUEST_OBLIGATORIO",
                    "La solicitud de cambio de contraseña es obligatoria."
            );
        }
    }

    private void requireConfirmChangePasswordRequest(ConfirmChangePasswordRequestDto request) {
        if (request == null) {
            throw new ValidationException(
                    "PASSWORD_CHANGE_CONFIRM_REQUEST_OBLIGATORIO",
                    "La confirmación de cambio de contraseña es obligatoria."
            );
        }
    }

    private void requireForgotPasswordRequest(ForgotPasswordRequestDto request) {
        if (request == null) {
            throw new ValidationException(
                    "PASSWORD_FORGOT_REQUEST_OBLIGATORIO",
                    "La solicitud de recuperación de contraseña es obligatoria."
            );
        }
    }

    private void requireResetPasswordRequest(ResetPasswordRequestDto request) {
        if (request == null) {
            throw new ValidationException(
                    "PASSWORD_RESET_REQUEST_OBLIGATORIO",
                    "La solicitud de restablecimiento de contraseña es obligatoria."
            );
        }
    }

    private boolean isEligibleForPublicPasswordReset(Usuario usuario) {
        try {
            usuarioValidator.validateActivo(usuario);
            usuarioValidator.validateNoBloqueado(usuario);
            usuarioValidator.validateRolActivo(usuario);
            return true;
        } catch (BusinessException exception) {
            auditIgnoredForgotPassword(usuario);
            return false;
        }
    }

    private void createPasswordResetTokenAndNotify(Usuario usuario) {
        Instant now = Instant.now(clock);
        String tokenPlano = RandomTokenUtil.secureToken();
        Instant expiresAt = now.plusSeconds(appProperties.getSecurity().getPasswordResetTokenMinutes() * 60);

        passwordResetTokenRepository.revokePendingTokensByUsuarioAndType(
                usuario.getId(),
                TipoToken.PASSWORD_RESET_TOKEN,
                EstadoVerificacionCodigo.PENDIENTE,
                EstadoVerificacionCodigo.REVOCADO,
                now
        );

        PasswordResetToken token = PasswordResetToken.builder()
                .usuario(usuario)
                .tokenHash(TokenHashUtil.hash(tokenPlano))
                .tipoToken(TipoToken.PASSWORD_RESET_TOKEN)
                .estado(EstadoVerificacionCodigo.PENDIENTE)
                .intentosFallidos(0)
                .maxIntentos(appProperties.getSecurity().getVerificationMaxAttempts())
                .expiresAt(expiresAt)
                .ipAddress(resolveIpAddressValue())
                .userAgent(resolveUserAgentValue())
                .build();

        passwordResetTokenRepository.save(token);

        emailService.sendPasswordReset(
                usuario.getEmail().getValue(),
                nombreCompleto(usuario),
                tokenPlano,
                buildResetUrl(tokenPlano),
                expiresAt
        );

        auditoriaSeguridadService.registerSuccess(
                TipoAuditoriaSeguridad.PASSWORD_RESET_SOLICITADO,
                usuario,
                usuario,
                "Token de recuperación de contraseña enviado."
        );
    }

    private void auditIgnoredForgotPassword(Usuario usuario) {
        auditoriaSeguridadService.registerWarning(
                TipoAuditoriaSeguridad.PASSWORD_RESET_SOLICITADO,
                usuario,
                usuario,
                "Solicitud de recuperación ignorada por correo no registrado o usuario no disponible."
        );
    }

    private PasswordOperationResponseDto genericForgotPublicResponse() {
        return new PasswordOperationResponseDto(
                EstadoVerificacionCodigo.PENDIENTE,
                GENERIC_FORGOT_MESSAGE,
                null
        );
    }

    private PasswordResetToken requireResetTokenByPlainToken(String tokenPlano) {
        String tokenHash = TokenHashUtil.hash(tokenPlano);

        return passwordResetTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> {
                    auditoriaSeguridadService.registerFailure(
                            TipoAuditoriaSeguridad.TOKEN_INVALIDO,
                            null,
                            null,
                            "Intento de recuperación con token inexistente."
                    );

                    return new ValidationException(
                            "PASSWORD_RESET_TOKEN_INVALIDO",
                            "El token de recuperación no es válido."
                    );
                });
    }

    private void validateTokenMatchesEmail(
            PasswordResetToken token,
            String email
    ) {
        Usuario usuario = token.getUsuario();

        String tokenEmail = usuario == null || usuario.getEmail() == null
                ? null
                : usuario.getEmail().getValue();

        if (!email.equalsIgnoreCase(tokenEmail)) {
            registerResetTokenFailedAttempt(
                    token,
                    usuario,
                    "Intento de recuperación con correo que no corresponde al token."
            );

            throw new ValidationException(
                    "PASSWORD_RESET_TOKEN_INVALIDO",
                    "El token de recuperación no es válido."
            );
        }
    }

    private void validateResetTokenUsable(PasswordResetToken token) {
        Instant now = Instant.now(clock);

        if (token == null) {
            throw new ValidationException(
                    "PASSWORD_RESET_TOKEN_INVALIDO",
                    "El token de recuperación no es válido."
            );
        }

        if (token.estaUsado()) {
            throw new ValidationException(
                    "PASSWORD_RESET_TOKEN_USADO",
                    "El token de recuperación ya fue utilizado."
            );
        }

        if (EstadoVerificacionCodigo.REVOCADO.equals(token.getEstado())) {
            throw new ValidationException(
                    "PASSWORD_RESET_TOKEN_REVOCADO",
                    "El token de recuperación fue revocado."
            );
        }

        if (EstadoVerificacionCodigo.BLOQUEADO.equals(token.getEstado())) {
            throw new ValidationException(
                    "PASSWORD_RESET_TOKEN_BLOQUEADO",
                    "El token de recuperación fue bloqueado por intentos fallidos."
            );
        }

        if (!token.estaVigente(now)) {
            token.marcarExpirado();
            passwordResetTokenRepository.save(token);

            auditoriaSeguridadService.registerWarning(
                    TipoAuditoriaSeguridad.CODIGO_EXPIRADO,
                    token.getUsuario(),
                    token.getUsuario(),
                    "Token de recuperación expirado."
            );

            throw new ValidationException(
                    "PASSWORD_RESET_TOKEN_EXPIRADO",
                    "El token de recuperación expiró."
            );
        }

        if (!token.tieneIntentosDisponibles()) {
            throw new ValidationException(
                    "PASSWORD_RESET_TOKEN_INTENTOS_EXCEDIDOS",
                    "El token de recuperación superó el máximo de intentos permitidos."
            );
        }
    }

    private void registerResetTokenFailedAttempt(
            PasswordResetToken token,
            Usuario usuario,
            String auditMessage
    ) {
        token.registrarIntentoFallido();
        passwordResetTokenRepository.save(token);

        auditoriaSeguridadService.registerFailure(
                TipoAuditoriaSeguridad.CODIGO_FALLIDO,
                usuario,
                usuario,
                auditMessage
        );

        if (EstadoVerificacionCodigo.BLOQUEADO.equals(token.getEstado())) {
            auditoriaSeguridadService.registerWarning(
                    TipoAuditoriaSeguridad.CODIGO_BLOQUEADO,
                    usuario,
                    usuario,
                    "Token de recuperación bloqueado por intentos fallidos."
            );
        }
    }

    private void revokeActiveSessions(
            Long idUsuario,
            MotivoRevocacionSesion motivo,
            Instant now
    ) {
        usuarioSesionRepository.revokeActiveSessionsByUsuario(
                idUsuario,
                EstadoSesion.ACTIVA,
                EstadoSesion.REVOCADA,
                motivo,
                PASSWORD_SESSION_REVOKE_DETAIL,
                now
        );
    }

    private String normalizeEmail(String email) {
        try {
            return EmailValue.of(email).getValue();
        } catch (IllegalArgumentException exception) {
            throw new ValidationException(
                    "PASSWORD_EMAIL_INVALIDO",
                    exception.getMessage()
            );
        }
    }

    private String requireToken(String token) {
        String normalized = StringNormalizer.trimToNull(token);

        if (normalized == null) {
            throw new ValidationException(
                    "PASSWORD_RESET_TOKEN_OBLIGATORIO",
                    "El token de recuperación es obligatorio."
            );
        }

        if (normalized.length() > 4096) {
            throw new ValidationException(
                    "PASSWORD_RESET_TOKEN_LONGITUD_INVALIDA",
                    "El token de recuperación no puede superar 4096 caracteres."
            );
        }

        return normalized;
    }

    private String buildResetUrl(String tokenPlano) {
        String frontendUrl = StringNormalizer.trimToNull(appProperties.getFrontendUrl());
        String baseUrl = frontendUrl == null ? "" : frontendUrl;

        return baseUrl
                + "/password/reset?token="
                + URLEncoder.encode(tokenPlano, StandardCharsets.UTF_8);
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

    private IpAddressValue resolveIpAddressValue() {
        AuditContext context = AuditContextHolder.get();
        String ipAddress = StringNormalizer.trimToNull(context.ipAddress());

        if (ipAddress == null) {
            return null;
        }

        return IpAddressValue.of(ipAddress);
    }

    private UserAgentValue resolveUserAgentValue() {
        AuditContext context = AuditContextHolder.get();
        return UserAgentValue.of(context.userAgent());
    }

    private void logUnexpected(
            String method,
            AuthenticatedUserContext actor,
            String email,
            RuntimeException exception
    ) {
        AuditContext context = AuditContextHolder.get();

        log.error(
                "Error inesperado en PasswordResetServiceImpl.{} - actor={}, email={}, requestId={}, path={}",
                method,
                actor == null ? "ANONIMO" : actor.username(),
                EmailMaskingUtil.mask(email),
                context.requestId(),
                context.path(),
                exception
        );
    }
}