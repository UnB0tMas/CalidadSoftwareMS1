// src/main/java/com/upsjb/ms1/service/contract/EmailService.java
package com.upsjb.ms1.service.contract;

import com.upsjb.ms1.domain.enums.TipoCodigoVerificacion;
import com.upsjb.ms1.mail.model.EmailMessage;
import com.upsjb.ms1.mail.model.EmailRecipient;
import com.upsjb.ms1.mail.model.EmailTemplateData;
import java.time.Instant;

public interface EmailService {

    void send(EmailMessage message);

    void sendTemplate(
            EmailRecipient recipient,
            EmailTemplateData templateData
    );

    void sendVerificationCode(
            String email,
            String nombreUsuario,
            String codigo,
            Instant expiresAt,
            TipoCodigoVerificacion tipoCodigo
    );

    void sendPasswordChangeVerification(
            String email,
            String nombreUsuario,
            String codigo,
            Instant expiresAt
    );

    void sendPasswordChanged(
            String email,
            String nombreUsuario,
            Instant changedAt
    );

    void sendPasswordReset(
            String email,
            String nombreUsuario,
            String tokenOrCode,
            String resetUrl,
            Instant expiresAt
    );

    void sendSuspiciousLogin(
            String email,
            String nombreUsuario,
            Instant loginAt,
            String ipAddress,
            String userAgent
    );

    void sendWelcomeUser(
            String email,
            String nombreUsuario,
            String username
    );
}