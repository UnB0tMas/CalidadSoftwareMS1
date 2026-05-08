// src/main/java/com/upsjb/ms1/mail/template/EmailTemplateRenderer.java
package com.upsjb.ms1.mail.template;

import com.upsjb.ms1.mail.model.EmailTemplateData;
import com.upsjb.ms1.shared.exception.ValidationException;
import org.springframework.stereotype.Component;

@Component
public class EmailTemplateRenderer {

    private final PasswordChangedTemplate passwordChangedTemplate;
    private final PasswordChangeVerificationTemplate passwordChangeVerificationTemplate;
    private final PasswordResetTemplate passwordResetTemplate;
    private final SuspiciousLoginTemplate suspiciousLoginTemplate;
    private final VerificationCodeTemplate verificationCodeTemplate;
    private final WelcomeUserTemplate welcomeUserTemplate;

    public EmailTemplateRenderer(
            PasswordChangedTemplate passwordChangedTemplate,
            PasswordChangeVerificationTemplate passwordChangeVerificationTemplate,
            PasswordResetTemplate passwordResetTemplate,
            SuspiciousLoginTemplate suspiciousLoginTemplate,
            VerificationCodeTemplate verificationCodeTemplate,
            WelcomeUserTemplate welcomeUserTemplate
    ) {
        this.passwordChangedTemplate = passwordChangedTemplate;
        this.passwordChangeVerificationTemplate = passwordChangeVerificationTemplate;
        this.passwordResetTemplate = passwordResetTemplate;
        this.suspiciousLoginTemplate = suspiciousLoginTemplate;
        this.verificationCodeTemplate = verificationCodeTemplate;
        this.welcomeUserTemplate = welcomeUserTemplate;
    }

    public String renderHtml(EmailTemplateData data) {
        return switch (data.templateName()) {
            case PasswordChangedTemplate.TEMPLATE_NAME -> passwordChangedTemplate.renderHtml(data);
            case PasswordChangeVerificationTemplate.TEMPLATE_NAME -> passwordChangeVerificationTemplate.renderHtml(data);
            case PasswordResetTemplate.TEMPLATE_NAME -> passwordResetTemplate.renderHtml(data);
            case SuspiciousLoginTemplate.TEMPLATE_NAME -> suspiciousLoginTemplate.renderHtml(data);
            case VerificationCodeTemplate.TEMPLATE_NAME -> verificationCodeTemplate.renderHtml(data);
            case WelcomeUserTemplate.TEMPLATE_NAME -> welcomeUserTemplate.renderHtml(data);
            default -> throw new ValidationException(
                    "EMAIL_TEMPLATE_NO_SOPORTADO",
                    "La plantilla de correo solicitada no está soportada."
            );
        };
    }

    public String renderText(EmailTemplateData data) {
        return switch (data.templateName()) {
            case PasswordChangedTemplate.TEMPLATE_NAME -> passwordChangedTemplate.renderText(data);
            case PasswordChangeVerificationTemplate.TEMPLATE_NAME -> passwordChangeVerificationTemplate.renderText(data);
            case PasswordResetTemplate.TEMPLATE_NAME -> passwordResetTemplate.renderText(data);
            case SuspiciousLoginTemplate.TEMPLATE_NAME -> suspiciousLoginTemplate.renderText(data);
            case VerificationCodeTemplate.TEMPLATE_NAME -> verificationCodeTemplate.renderText(data);
            case WelcomeUserTemplate.TEMPLATE_NAME -> welcomeUserTemplate.renderText(data);
            default -> throw new ValidationException(
                    "EMAIL_TEMPLATE_NO_SOPORTADO",
                    "La plantilla de correo solicitada no está soportada."
            );
        };
    }

    public String resolveSubject(EmailTemplateData data) {
        if (data.subject() != null && !data.subject().isBlank()) {
            return data.subject();
        }

        return switch (data.templateName()) {
            case PasswordChangedTemplate.TEMPLATE_NAME -> passwordChangedTemplate.subject(data);
            case PasswordChangeVerificationTemplate.TEMPLATE_NAME -> passwordChangeVerificationTemplate.subject(data);
            case PasswordResetTemplate.TEMPLATE_NAME -> passwordResetTemplate.subject(data);
            case SuspiciousLoginTemplate.TEMPLATE_NAME -> suspiciousLoginTemplate.subject(data);
            case VerificationCodeTemplate.TEMPLATE_NAME -> verificationCodeTemplate.subject(data);
            case WelcomeUserTemplate.TEMPLATE_NAME -> welcomeUserTemplate.subject(data);
            default -> throw new ValidationException(
                    "EMAIL_TEMPLATE_NO_SOPORTADO",
                    "La plantilla de correo solicitada no está soportada."
            );
        };
    }

    public static String escapeHtml(String value) {
        if (value == null) {
            return "";
        }

        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}