// src/main/java/com/upsjb/ms1/service/impl/EmailServiceImpl.java
package com.upsjb.ms1.service.impl;

import com.upsjb.ms1.config.AppPropertiesConfig;
import com.upsjb.ms1.config.MailConfig;
import com.upsjb.ms1.domain.enums.TipoCodigoVerificacion;
import com.upsjb.ms1.mail.model.EmailMessage;
import com.upsjb.ms1.mail.model.EmailRecipient;
import com.upsjb.ms1.mail.model.EmailTemplateData;
import com.upsjb.ms1.mail.template.EmailTemplateRenderer;
import com.upsjb.ms1.mail.template.PasswordChangedTemplate;
import com.upsjb.ms1.mail.template.PasswordChangeVerificationTemplate;
import com.upsjb.ms1.mail.template.PasswordResetTemplate;
import com.upsjb.ms1.mail.template.SuspiciousLoginTemplate;
import com.upsjb.ms1.mail.template.VerificationCodeTemplate;
import com.upsjb.ms1.mail.template.WelcomeUserTemplate;
import com.upsjb.ms1.service.contract.EmailService;
import com.upsjb.ms1.shared.audit.AuditContext;
import com.upsjb.ms1.shared.audit.AuditContextHolder;
import com.upsjb.ms1.shared.exception.ValidationException;
import com.upsjb.ms1.util.EmailMaskingUtil;
import com.upsjb.ms1.util.StringNormalizer;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.io.UnsupportedEncodingException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.mail.MailPreparationException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailServiceImpl implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailServiceImpl.class);

    private static final boolean MULTIPART = true;

    private final JavaMailSender mailSender;
    private final EmailTemplateRenderer templateRenderer;
    private final AppPropertiesConfig appProperties;

    public EmailServiceImpl(
            JavaMailSender mailSender,
            EmailTemplateRenderer templateRenderer,
            AppPropertiesConfig appProperties
    ) {
        this.mailSender = mailSender;
        this.templateRenderer = templateRenderer;
        this.appProperties = appProperties;
    }

    @Override
    public void send(EmailMessage message) {
        validateMessage(message);

        if (!appProperties.getMail().isEnabled()) {
            log.debug(
                    "Correo no enviado porque el módulo mail está deshabilitado - to={}, requestId={}",
                    safeRecipients(message.to()),
                    requestId()
            );
            return;
        }

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    mimeMessage,
                    MULTIPART,
                    MailConfig.DEFAULT_ENCODING
            );

            helper.setFrom(toInternetAddress(message.from()));
            helper.setTo(toInternetAddresses(message.to()));

            if (!message.cc().isEmpty()) {
                helper.setCc(toInternetAddresses(message.cc()));
            }

            if (!message.bcc().isEmpty()) {
                helper.setBcc(toInternetAddresses(message.bcc()));
            }

            if (message.replyTo() != null) {
                helper.setReplyTo(toInternetAddress(message.replyTo()));
            }

            helper.setSubject(message.subject());

            if (StringNormalizer.trimToNull(message.htmlBody()) != null
                    && StringNormalizer.trimToNull(message.textBody()) != null) {
                helper.setText(message.textBody(), message.htmlBody());
            } else if (StringNormalizer.trimToNull(message.htmlBody()) != null) {
                helper.setText(message.htmlBody(), true);
            } else {
                helper.setText(message.textBody(), false);
            }

            mailSender.send(mimeMessage);

            log.debug(
                    "Correo enviado correctamente - to={}, subject={}, requestId={}",
                    safeRecipients(message.to()),
                    safeSubject(message.subject()),
                    requestId()
            );
        } catch (MessagingException | UnsupportedEncodingException exception) {
            log.error(
                    "No se pudo preparar el correo electrónico - to={}, subject={}, requestId={}",
                    safeRecipients(message.to()),
                    safeSubject(message.subject()),
                    requestId(),
                    exception
            );

            throw new MailPreparationException(
                    "No se pudo preparar el correo electrónico.",
                    exception
            );
        } catch (MailException exception) {
            log.error(
                    "No se pudo enviar el correo electrónico - to={}, subject={}, requestId={}",
                    safeRecipients(message.to()),
                    safeSubject(message.subject()),
                    requestId(),
                    exception
            );

            throw exception;
        }
    }

    @Override
    public void sendTemplate(
            EmailRecipient recipient,
            EmailTemplateData templateData
    ) {
        validateTemplateRequest(recipient, templateData);

        if (!appProperties.getMail().isEnabled()) {
            log.debug(
                    "Plantilla de correo no enviada porque el módulo mail está deshabilitado - template={}, to={}, requestId={}",
                    templateData.templateName(),
                    EmailMaskingUtil.mask(recipient.email()),
                    requestId()
            );
            return;
        }

        EmailTemplateData enrichedData = enrich(templateData);
        String subject = templateRenderer.resolveSubject(enrichedData);
        String html = templateRenderer.renderHtml(enrichedData);
        String text = templateRenderer.renderText(enrichedData);

        send(EmailMessage.html(
                defaultSender(),
                recipient,
                subject,
                html,
                text
        ));
    }

    @Override
    public void sendVerificationCode(
            String email,
            String nombreUsuario,
            String codigo,
            Instant expiresAt,
            TipoCodigoVerificacion tipoCodigo
    ) {
        Map<String, Object> variables = new LinkedHashMap<>();
        variables.put("nombreUsuario", nombreUsuario);
        variables.put("codigo", codigo);
        variables.put("expiresAt", expiresAt);
        variables.put("tipoCodigo", tipoCodigo == null ? null : tipoCodigo.name());
        variables.put("operationTitle", resolveOperationTitle(tipoCodigo));
        variables.put("instruction", resolveVerificationInstruction(tipoCodigo));
        variables.put("securityNotice", resolveSecurityNotice(tipoCodigo));

        sendTemplate(
                EmailRecipient.of(email, nombreUsuario),
                EmailTemplateData.of(
                        VerificationCodeTemplate.TEMPLATE_NAME,
                        null,
                        variables
                )
        );
    }

    @Override
    public void sendPasswordChangeVerification(
            String email,
            String nombreUsuario,
            String codigo,
            Instant expiresAt
    ) {
        Map<String, Object> variables = new LinkedHashMap<>();
        variables.put("nombreUsuario", nombreUsuario);
        variables.put("codigo", codigo);
        variables.put("expiresAt", expiresAt);

        sendTemplate(
                EmailRecipient.of(email, nombreUsuario),
                EmailTemplateData.of(
                        PasswordChangeVerificationTemplate.TEMPLATE_NAME,
                        null,
                        variables
                )
        );
    }

    @Override
    public void sendPasswordChanged(
            String email,
            String nombreUsuario,
            Instant changedAt
    ) {
        Map<String, Object> variables = new LinkedHashMap<>();
        variables.put("nombreUsuario", nombreUsuario);
        variables.put("changedAt", changedAt);

        sendTemplate(
                EmailRecipient.of(email, nombreUsuario),
                EmailTemplateData.of(
                        PasswordChangedTemplate.TEMPLATE_NAME,
                        null,
                        variables
                )
        );
    }

    @Override
    public void sendPasswordReset(
            String email,
            String nombreUsuario,
            String tokenOrCode,
            String resetUrl,
            Instant expiresAt
    ) {
        Map<String, Object> variables = new LinkedHashMap<>();
        variables.put("nombreUsuario", nombreUsuario);
        variables.put("tokenOrCode", tokenOrCode);
        variables.put("resetUrl", resetUrl);
        variables.put("expiresAt", expiresAt);

        sendTemplate(
                EmailRecipient.of(email, nombreUsuario),
                EmailTemplateData.of(
                        PasswordResetTemplate.TEMPLATE_NAME,
                        null,
                        variables
                )
        );
    }

    @Override
    public void sendSuspiciousLogin(
            String email,
            String nombreUsuario,
            Instant loginAt,
            String ipAddress,
            String userAgent
    ) {
        Map<String, Object> variables = new LinkedHashMap<>();
        variables.put("nombreUsuario", nombreUsuario);
        variables.put("loginAt", loginAt);
        variables.put("ipAddress", ipAddress);
        variables.put("userAgent", userAgent);

        sendTemplate(
                EmailRecipient.of(email, nombreUsuario),
                EmailTemplateData.of(
                        SuspiciousLoginTemplate.TEMPLATE_NAME,
                        null,
                        variables
                )
        );
    }

    @Override
    public void sendWelcomeUser(
            String email,
            String nombreUsuario,
            String username
    ) {
        Map<String, Object> variables = new LinkedHashMap<>();
        variables.put("nombreUsuario", nombreUsuario);
        variables.put("username", username);

        sendTemplate(
                EmailRecipient.of(email, nombreUsuario),
                EmailTemplateData.of(
                        WelcomeUserTemplate.TEMPLATE_NAME,
                        null,
                        variables
                )
        );
    }

    private EmailTemplateData enrich(EmailTemplateData data) {
        return data.withDefaults(baseVariables());
    }

    private Map<String, Object> baseVariables() {
        Map<String, Object> variables = new LinkedHashMap<>();
        AuditContext auditContext = AuditContextHolder.get();

        variables.put("appName", appProperties.getName());
        variables.put("frontendUrl", appProperties.getFrontendUrl());
        variables.put("gatewayUrl", appProperties.getGatewayUrl());
        variables.put("supportEmail", appProperties.getMail().getSupportEmail());
        variables.put("ipAddress", auditContext == null ? null : auditContext.ipAddress());
        variables.put("userAgent", auditContext == null ? null : auditContext.userAgent());
        variables.put("requestId", auditContext == null ? null : auditContext.requestId());

        return variables;
    }

    private EmailRecipient defaultSender() {
        return EmailRecipient.of(
                appProperties.getMail().getFrom(),
                appProperties.getMail().getSenderName()
        );
    }

    private void validateMessage(EmailMessage message) {
        if (message == null) {
            throw new ValidationException(
                    "EMAIL_MESSAGE_OBLIGATORIO",
                    "El mensaje de correo es obligatorio."
            );
        }

        validateRecipient(message.from(), "EMAIL_REMITENTE_OBLIGATORIO", "El remitente del correo es obligatorio.");

        if (message.to() == null || message.to().isEmpty()) {
            throw new ValidationException(
                    "EMAIL_DESTINATARIO_OBLIGATORIO",
                    "El correo debe tener al menos un destinatario."
            );
        }

        message.to().forEach(recipient -> validateRecipient(
                recipient,
                "EMAIL_DESTINATARIO_INVALIDO",
                "El destinatario del correo no es válido."
        ));

        if (message.cc() != null) {
            message.cc().forEach(recipient -> validateRecipient(
                    recipient,
                    "EMAIL_CC_INVALIDO",
                    "El destinatario en copia no es válido."
            ));
        }

        if (message.bcc() != null) {
            message.bcc().forEach(recipient -> validateRecipient(
                    recipient,
                    "EMAIL_BCC_INVALIDO",
                    "El destinatario en copia oculta no es válido."
            ));
        }

        if (message.replyTo() != null) {
            validateRecipient(
                    message.replyTo(),
                    "EMAIL_REPLY_TO_INVALIDO",
                    "El correo de respuesta no es válido."
            );
        }

        if (StringNormalizer.trimToNull(message.subject()) == null) {
            throw new ValidationException(
                    "EMAIL_SUBJECT_OBLIGATORIO",
                    "El asunto del correo es obligatorio."
            );
        }

        if (StringNormalizer.trimToNull(message.htmlBody()) == null
                && StringNormalizer.trimToNull(message.textBody()) == null) {
            throw new ValidationException(
                    "EMAIL_BODY_OBLIGATORIO",
                    "El cuerpo del correo es obligatorio."
            );
        }
    }

    private void validateTemplateRequest(
            EmailRecipient recipient,
            EmailTemplateData templateData
    ) {
        validateRecipient(
                recipient,
                "EMAIL_TEMPLATE_DESTINATARIO_OBLIGATORIO",
                "El destinatario del correo es obligatorio."
        );

        if (templateData == null) {
            throw new ValidationException(
                    "EMAIL_TEMPLATE_DATA_OBLIGATORIO",
                    "Los datos de la plantilla de correo son obligatorios."
            );
        }

        if (StringNormalizer.trimToNull(templateData.templateName()) == null) {
            throw new ValidationException(
                    "EMAIL_TEMPLATE_NAME_OBLIGATORIO",
                    "El nombre de la plantilla de correo es obligatorio."
            );
        }
    }

    private void validateRecipient(
            EmailRecipient recipient,
            String code,
            String message
    ) {
        if (recipient == null || StringNormalizer.trimToNull(recipient.email()) == null) {
            throw new ValidationException(code, message);
        }
    }

    private InternetAddress toInternetAddress(EmailRecipient recipient)
            throws UnsupportedEncodingException, AddressException {
        if (StringNormalizer.trimToNull(recipient.name()) == null) {
            return new InternetAddress(recipient.email());
        }

        return new InternetAddress(
                recipient.email(),
                recipient.name(),
                MailConfig.DEFAULT_ENCODING
        );
    }

    private InternetAddress[] toInternetAddresses(List<EmailRecipient> recipients)
            throws UnsupportedEncodingException, AddressException {
        InternetAddress[] addresses = new InternetAddress[recipients.size()];

        for (int i = 0; i < recipients.size(); i++) {
            addresses[i] = toInternetAddress(recipients.get(i));
        }

        return addresses;
    }

    private String resolveOperationTitle(TipoCodigoVerificacion tipoCodigo) {
        if (tipoCodigo == null) {
            return "verificar tu cuenta";
        }

        return switch (tipoCodigo) {
            case VERIFICACION_EMAIL -> "verificar tu correo electrónico";
            case REGISTRO_USUARIO -> "completar tu registro";
            case CAMBIO_EMAIL -> "confirmar el cambio de correo electrónico";
            case CAMBIO_PASSWORD -> "confirmar el cambio de contraseña";
            case RECUPERACION_PASSWORD -> "recuperar tu contraseña";
            case LOGIN_SOSPECHOSO -> "validar un inicio de sesión sospechoso";
        };
    }

    private String resolveVerificationInstruction(TipoCodigoVerificacion tipoCodigo) {
        if (tipoCodigo == null) {
            return "Usa el siguiente código para continuar con la operación de seguridad.";
        }

        return switch (tipoCodigo) {
            case VERIFICACION_EMAIL -> "Usa el siguiente código para verificar tu correo electrónico.";
            case REGISTRO_USUARIO -> "Usa el siguiente código para completar el registro de tu cuenta.";
            case CAMBIO_EMAIL -> "Usa el siguiente código para confirmar el cambio de correo electrónico.";
            case CAMBIO_PASSWORD -> "Usa el siguiente código para confirmar el cambio de contraseña.";
            case RECUPERACION_PASSWORD -> "Usa el siguiente código para continuar con la recuperación de contraseña.";
            case LOGIN_SOSPECHOSO -> "Detectamos un inicio de sesión que requiere verificación adicional. Usa este código para continuar.";
        };
    }

    private String resolveSecurityNotice(TipoCodigoVerificacion tipoCodigo) {
        if (TipoCodigoVerificacion.LOGIN_SOSPECHOSO.equals(tipoCodigo)) {
            return "Si no intentaste iniciar sesión, cambia tu contraseña y revisa la seguridad de tu cuenta.";
        }

        if (TipoCodigoVerificacion.CAMBIO_EMAIL.equals(tipoCodigo)
                || TipoCodigoVerificacion.CAMBIO_PASSWORD.equals(tipoCodigo)
                || TipoCodigoVerificacion.RECUPERACION_PASSWORD.equals(tipoCodigo)) {
            return "Si no solicitaste esta operación, ignora este correo y revisa la seguridad de tu cuenta.";
        }

        return "Si no solicitaste este código, puedes ignorar este correo.";
    }

    private String requestId() {
        AuditContext auditContext = AuditContextHolder.get();
        return auditContext == null ? null : auditContext.requestId();
    }

    private String safeRecipients(List<EmailRecipient> recipients) {
        if (recipients == null || recipients.isEmpty()) {
            return "SIN_DESTINATARIOS";
        }

        return String.join(
                ", ",
                recipients.stream()
                        .filter(recipient -> recipient != null && recipient.email() != null)
                        .map(recipient -> EmailMaskingUtil.mask(recipient.email()))
                        .distinct()
                        .toList()
        );
    }

    private String safeSubject(String subject) {
        String normalized = StringNormalizer.normalizeSpaces(subject);

        if (normalized == null) {
            return "SIN_ASUNTO";
        }

        return normalized.length() <= 120
                ? normalized
                : normalized.substring(0, 120);
    }
}