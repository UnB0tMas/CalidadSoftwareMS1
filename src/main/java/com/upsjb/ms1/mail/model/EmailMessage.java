package com.upsjb.ms1.mail.model;

import com.upsjb.ms1.util.StringNormalizer;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;

public record EmailMessage(
        EmailRecipient from,
        List<EmailRecipient> to,
        List<EmailRecipient> cc,
        List<EmailRecipient> bcc,
        EmailRecipient replyTo,
        String subject,
        String htmlBody,
        String textBody
) implements Serializable {

    public EmailMessage {
        to = immutableRecipients(to);
        cc = immutableRecipients(cc);
        bcc = immutableRecipients(bcc);
        subject = StringNormalizer.normalizeSpaces(subject);
        htmlBody = StringNormalizer.trimToNull(htmlBody);
        textBody = StringNormalizer.trimToNull(textBody);

        if (from == null) {
            throw new IllegalArgumentException("El remitente es obligatorio.");
        }

        if (to.isEmpty()) {
            throw new IllegalArgumentException("Debe existir al menos un destinatario.");
        }

        if (subject == null) {
            throw new IllegalArgumentException("El asunto del correo es obligatorio.");
        }

        if (htmlBody == null && textBody == null) {
            throw new IllegalArgumentException("El contenido del correo es obligatorio.");
        }
    }

    public static EmailMessage html(
            EmailRecipient from,
            EmailRecipient to,
            String subject,
            String htmlBody,
            String textBody
    ) {
        return new EmailMessage(
                from,
                List.of(to),
                List.of(),
                List.of(),
                null,
                subject,
                htmlBody,
                textBody
        );
    }

    private static List<EmailRecipient> immutableRecipients(List<EmailRecipient> recipients) {
        if (recipients == null || recipients.isEmpty()) {
            return List.of();
        }

        return Collections.unmodifiableList(recipients);
    }
}