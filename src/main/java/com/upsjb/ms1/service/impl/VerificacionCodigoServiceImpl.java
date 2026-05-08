// src/main/java/com/upsjb/ms1/service/impl/VerificacionCodigoServiceImpl.java
package com.upsjb.ms1.service.impl;

import com.upsjb.ms1.config.AppPropertiesConfig;
import com.upsjb.ms1.domain.entity.Usuario;
import com.upsjb.ms1.domain.entity.VerificacionCodigo;
import com.upsjb.ms1.domain.enums.EstadoVerificacionCodigo;
import com.upsjb.ms1.domain.enums.TipoAuditoriaSeguridad;
import com.upsjb.ms1.domain.enums.TipoCodigoVerificacion;
import com.upsjb.ms1.domain.value.EmailValue;
import com.upsjb.ms1.domain.value.IpAddressValue;
import com.upsjb.ms1.domain.value.UserAgentValue;
import com.upsjb.ms1.dto.verificacion.request.ResendVerificationCodeRequestDto;
import com.upsjb.ms1.dto.verificacion.request.SendVerificationCodeRequestDto;
import com.upsjb.ms1.dto.verificacion.request.VerifyCodeRequestDto;
import com.upsjb.ms1.dto.verificacion.response.VerificationCodeResponseDto;
import com.upsjb.ms1.dto.verificacion.response.VerificationStatusResponseDto;
import com.upsjb.ms1.mapper.VerificacionCodigoMapper;
import com.upsjb.ms1.repository.UsuarioRepository;
import com.upsjb.ms1.repository.VerificacionCodigoRepository;
import com.upsjb.ms1.service.contract.AuditoriaSeguridadService;
import com.upsjb.ms1.service.contract.EmailService;
import com.upsjb.ms1.service.contract.VerificacionCodigoService;
import com.upsjb.ms1.shared.audit.AuditContext;
import com.upsjb.ms1.shared.audit.AuditContextHolder;
import com.upsjb.ms1.shared.exception.BusinessException;
import com.upsjb.ms1.shared.exception.ValidationException;
import com.upsjb.ms1.util.EmailMaskingUtil;
import com.upsjb.ms1.util.RandomCodeUtil;
import com.upsjb.ms1.util.StringNormalizer;
import com.upsjb.ms1.util.TokenHashUtil;
import com.upsjb.ms1.validator.VerificacionCodigoValidator;
import java.time.Clock;
import java.time.Instant;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VerificacionCodigoServiceImpl implements VerificacionCodigoService {

    private static final Logger log = LoggerFactory.getLogger(VerificacionCodigoServiceImpl.class);

    private static final String SEND_MESSAGE = "Código de verificación enviado correctamente.";
    private static final String RESEND_MESSAGE = "Código de verificación reenviado correctamente.";
    private static final String VERIFY_MESSAGE = "Código validado correctamente.";
    private static final String STATUS_MESSAGE = "Estado del código de verificación consultado correctamente.";

    private final VerificacionCodigoRepository verificacionCodigoRepository;
    private final UsuarioRepository usuarioRepository;
    private final VerificacionCodigoMapper verificacionCodigoMapper;
    private final VerificacionCodigoValidator verificacionCodigoValidator;
    private final EmailService emailService;
    private final AuditoriaSeguridadService auditoriaSeguridadService;
    private final AppPropertiesConfig appProperties;
    private final Clock clock;

    public VerificacionCodigoServiceImpl(
            VerificacionCodigoRepository verificacionCodigoRepository,
            UsuarioRepository usuarioRepository,
            VerificacionCodigoMapper verificacionCodigoMapper,
            VerificacionCodigoValidator verificacionCodigoValidator,
            EmailService emailService,
            AuditoriaSeguridadService auditoriaSeguridadService,
            AppPropertiesConfig appProperties,
            Clock clock
    ) {
        this.verificacionCodigoRepository = verificacionCodigoRepository;
        this.usuarioRepository = usuarioRepository;
        this.verificacionCodigoMapper = verificacionCodigoMapper;
        this.verificacionCodigoValidator = verificacionCodigoValidator;
        this.emailService = emailService;
        this.auditoriaSeguridadService = auditoriaSeguridadService;
        this.appProperties = appProperties;
        this.clock = clock;
    }

    @Override
    @Transactional
    public VerificationCodeResponseDto send(SendVerificationCodeRequestDto request) {
        return executeWithTechnicalLog(
                "send",
                request == null ? null : request.email(),
                request == null ? null : request.tipoCodigo(),
                () -> {
                    if (request == null) {
                        throw new ValidationException(
                                "CODIGO_SEND_REQUEST_OBLIGATORIO",
                                "La solicitud de envío de código es obligatoria."
                        );
                    }

                    return createAndSend(
                            request.email(),
                            request.tipoCodigo(),
                            true,
                            SEND_MESSAGE
                    );
                }
        );
    }

    @Override
    @Transactional
    public VerificationCodeResponseDto resend(ResendVerificationCodeRequestDto request) {
        return executeWithTechnicalLog(
                "resend",
                request == null ? null : request.email(),
                request == null ? null : request.tipoCodigo(),
                () -> {
                    if (request == null) {
                        throw new ValidationException(
                                "CODIGO_RESEND_REQUEST_OBLIGATORIO",
                                "La solicitud de reenvío de código es obligatoria."
                        );
                    }

                    return createAndSend(
                            request.email(),
                            request.tipoCodigo(),
                            true,
                            RESEND_MESSAGE
                    );
                }
        );
    }

    @Override
    @Transactional
    public VerificationStatusResponseDto verify(VerifyCodeRequestDto request) {
        return executeWithTechnicalLog(
                "verify",
                request == null ? null : request.email(),
                request == null ? null : request.tipoCodigo(),
                () -> {
                    if (request == null) {
                        throw new ValidationException(
                                "CODIGO_VERIFY_REQUEST_OBLIGATORIO",
                                "La solicitud de validación de código es obligatoria."
                        );
                    }

                    String email = normalizeEmail(request.email());
                    TipoCodigoVerificacion tipoCodigo = requireTipoCodigo(request.tipoCodigo());

                    try {
                        VerificacionCodigo codigo = verificacionCodigoValidator.validateCodigoByEmail(
                                email,
                                tipoCodigo,
                                request.codigo()
                        );

                        Instant now = Instant.now(clock);
                        codigo.marcarValidado(now);

                        Usuario usuario = codigo.getUsuario();

                        if (usuario != null && shouldMarkEmailVerified(tipoCodigo)) {
                            usuario.marcarEmailVerificado();
                            usuarioRepository.save(usuario);
                        }

                        VerificacionCodigo saved = verificacionCodigoRepository.save(codigo);

                        auditoriaSeguridadService.registerSuccess(
                                TipoAuditoriaSeguridad.CODIGO_VALIDADO,
                                usuario,
                                usuario,
                                "Código de verificación validado correctamente. Tipo: " + tipoCodigo + "."
                        );

                        return verificacionCodigoMapper.toStatusResponse(saved, VERIFY_MESSAGE);
                    } catch (BusinessException exception) {
                        auditFailedVerification(email, tipoCodigo, exception);
                        throw exception;
                    }
                }
        );
    }

    @Override
    @Transactional(readOnly = true)
    public VerificationStatusResponseDto status(
            String email,
            TipoCodigoVerificacion tipoCodigo
    ) {
        return executeWithTechnicalLog(
                "status",
                email,
                tipoCodigo,
                () -> {
                    String normalizedEmail = normalizeEmail(email);
                    TipoCodigoVerificacion normalizedTipo = requireTipoCodigo(tipoCodigo);

                    VerificacionCodigo codigo = verificacionCodigoValidator.requireCodigoPendienteByEmail(
                            normalizedEmail,
                            normalizedTipo
                    );

                    return verificacionCodigoMapper.toStatusResponse(codigo, STATUS_MESSAGE);
                }
        );
    }

    @Override
    @Transactional
    public int expirePendingCodes() {
        return executeWithTechnicalLog(
                "expirePendingCodes",
                null,
                null,
                () -> {
                    int expired = verificacionCodigoRepository.expirePendingCodes(
                            EstadoVerificacionCodigo.PENDIENTE,
                            EstadoVerificacionCodigo.EXPIRADO,
                            Instant.now(clock)
                    );

                    if (expired > 0) {
                        auditoriaSeguridadService.registerWarning(
                                TipoAuditoriaSeguridad.CODIGO_EXPIRADO,
                                null,
                                null,
                                "Se expiraron " + expired + " códigos de verificación pendientes."
                        );
                    }

                    return expired;
                }
        );
    }

    @Override
    @Transactional
    public int revokePendingByEmailAndType(
            String email,
            TipoCodigoVerificacion tipoCodigo
    ) {
        return executeWithTechnicalLog(
                "revokePendingByEmailAndType",
                email,
                tipoCodigo,
                () -> {
                    String normalizedEmail = normalizeEmail(email);
                    TipoCodigoVerificacion normalizedTipo = requireTipoCodigo(tipoCodigo);

                    return verificacionCodigoRepository.revokePendingCodesByEmailAndType(
                            normalizedEmail,
                            normalizedTipo,
                            EstadoVerificacionCodigo.PENDIENTE,
                            EstadoVerificacionCodigo.REVOCADO,
                            Instant.now(clock)
                    );
                }
        );
    }

    private VerificationCodeResponseDto createAndSend(
            String rawEmail,
            TipoCodigoVerificacion rawTipoCodigo,
            boolean validateCooldown,
            String message
    ) {
        String email = normalizeEmail(rawEmail);
        TipoCodigoVerificacion tipoCodigo = requireTipoCodigo(rawTipoCodigo);
        Instant now = Instant.now(clock);

        if (validateCooldown) {
            validateCooldown(email, tipoCodigo, now);
        }

        Usuario usuario = usuarioRepository.findByEmail_ValueIgnoreCase(email).orElse(null);

        verificacionCodigoRepository.revokePendingCodesByEmailAndType(
                email,
                tipoCodigo,
                EstadoVerificacionCodigo.PENDIENTE,
                EstadoVerificacionCodigo.REVOCADO,
                now
        );

        String codigoPlano = RandomCodeUtil.defaultCode();

        VerificacionCodigo codigo = VerificacionCodigo.builder()
                .usuario(usuario)
                .email(EmailValue.of(email))
                .tipoCodigo(tipoCodigo)
                .codigoHash(TokenHashUtil.hash(codigoPlano))
                .estado(EstadoVerificacionCodigo.PENDIENTE)
                .intentosFallidos(0)
                .maxIntentos(appProperties.getSecurity().getVerificationMaxAttempts())
                .expiresAt(now.plusSeconds(resolveExpirationSeconds(tipoCodigo)))
                .ipAddress(resolveIpAddressValue())
                .userAgent(resolveUserAgentValue())
                .build();

        VerificacionCodigo saved = verificacionCodigoRepository.save(codigo);

        emailService.sendVerificationCode(
                email,
                nombreCompleto(usuario),
                codigoPlano,
                saved.getExpiresAt(),
                tipoCodigo
        );

        auditoriaSeguridadService.registerSuccess(
                TipoAuditoriaSeguridad.CODIGO_ENVIADO,
                usuario,
                usuario,
                "Código de verificación enviado a "
                        + EmailMaskingUtil.mask(email)
                        + ". Tipo: "
                        + tipoCodigo
                        + "."
        );

        return verificacionCodigoMapper.toResponse(saved, message);
    }

    private void validateCooldown(
            String email,
            TipoCodigoVerificacion tipoCodigo,
            Instant now
    ) {
        verificacionCodigoRepository
                .findFirstByEmail_ValueIgnoreCaseAndTipoCodigoAndEstadoOrderByExpiresAtDesc(
                        email,
                        tipoCodigo,
                        EstadoVerificacionCodigo.PENDIENTE
                )
                .ifPresent(existing -> {
                    Instant createdAt = existing.getCreatedAt();
                    long cooldownSeconds = appProperties.getSecurity().getVerificationCodeCooldownSeconds();

                    if (createdAt != null && createdAt.plusSeconds(cooldownSeconds).isAfter(now)) {
                        throw new ValidationException(
                                "CODIGO_COOLDOWN_ACTIVO",
                                "Debes esperar antes de solicitar un nuevo código."
                        );
                    }
                });
    }

    private void auditFailedVerification(
            String email,
            TipoCodigoVerificacion tipoCodigo,
            BusinessException exception
    ) {
        try {
            Usuario usuario = usuarioRepository.findByEmail_ValueIgnoreCase(email).orElse(null);

            auditoriaSeguridadService.registerFailure(
                    resolveAuditFailureType(exception),
                    usuario,
                    usuario,
                    "Validación de código fallida. Email: "
                            + EmailMaskingUtil.mask(email)
                            + ". Tipo: "
                            + tipoCodigo
                            + ". Motivo: "
                            + exception.getCode()
                            + "."
            );
        } catch (RuntimeException auditException) {
            log.error(
                    "No se pudo registrar auditoría de fallo de código - email={}, tipoCodigo={}, requestId={}",
                    EmailMaskingUtil.mask(email),
                    tipoCodigo,
                    requestId(),
                    auditException
            );
        }
    }

    private TipoAuditoriaSeguridad resolveAuditFailureType(BusinessException exception) {
        String code = exception.getCode();

        if ("CODIGO_VERIFICACION_EXPIRADO".equals(code)) {
            return TipoAuditoriaSeguridad.CODIGO_EXPIRADO;
        }

        if ("CODIGO_VERIFICACION_BLOQUEADO".equals(code)
                || "CODIGO_VERIFICACION_INTENTOS_EXCEDIDOS".equals(code)) {
            return TipoAuditoriaSeguridad.CODIGO_BLOQUEADO;
        }

        return TipoAuditoriaSeguridad.CODIGO_FALLIDO;
    }

    private boolean shouldMarkEmailVerified(TipoCodigoVerificacion tipoCodigo) {
        return TipoCodigoVerificacion.VERIFICACION_EMAIL.equals(tipoCodigo)
                || TipoCodigoVerificacion.REGISTRO_USUARIO.equals(tipoCodigo);
    }

    private long resolveExpirationSeconds(TipoCodigoVerificacion tipoCodigo) {
        if (TipoCodigoVerificacion.RECUPERACION_PASSWORD.equals(tipoCodigo)) {
            return appProperties.getSecurity().getPasswordResetTokenMinutes() * 60;
        }

        return appProperties.getSecurity().getPasswordChangeCodeMinutes() * 60;
    }

    private String normalizeEmail(String email) {
        try {
            return EmailValue.of(email).getValue();
        } catch (IllegalArgumentException exception) {
            throw new ValidationException(
                    "CODIGO_EMAIL_INVALIDO",
                    exception.getMessage()
            );
        }
    }

    private TipoCodigoVerificacion requireTipoCodigo(TipoCodigoVerificacion tipoCodigo) {
        if (tipoCodigo == null) {
            throw new ValidationException(
                    "CODIGO_TIPO_OBLIGATORIO",
                    "El tipo de código es obligatorio."
            );
        }

        return tipoCodigo;
    }

    private IpAddressValue resolveIpAddressValue() {
        AuditContext context = AuditContextHolder.get();
        String ipAddress = StringNormalizer.trimToNull(context.ipAddress());

        if (ipAddress == null) {
            return null;
        }

        try {
            return IpAddressValue.of(ipAddress);
        } catch (IllegalArgumentException exception) {
            log.warn(
                    "IP de auditoría inválida para código de verificación - ip={}, requestId={}",
                    ipAddress,
                    requestId(),
                    exception
            );
            return null;
        }
    }

    private UserAgentValue resolveUserAgentValue() {
        AuditContext context = AuditContextHolder.get();
        return UserAgentValue.of(context.userAgent());
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

    private <T> T executeWithTechnicalLog(
            String method,
            String email,
            TipoCodigoVerificacion tipoCodigo,
            Supplier<T> action
    ) {
        try {
            return action.get();
        } catch (BusinessException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            log.error(
                    "Error inesperado en VerificacionCodigoServiceImpl.{} - email={}, tipoCodigo={}, requestId={}",
                    method,
                    EmailMaskingUtil.mask(email),
                    tipoCodigo,
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