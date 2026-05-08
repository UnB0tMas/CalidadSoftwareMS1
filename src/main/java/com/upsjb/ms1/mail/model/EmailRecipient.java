package com.upsjb.ms1.mail.model;

import com.upsjb.ms1.util.StringNormalizer;
import java.io.Serializable;

public record EmailRecipient(
        String email,
        String name
) implements Serializable {

    public EmailRecipient {
        email = StringNormalizer.lower(email);
        name = StringNormalizer.normalizeSpaces(name);

        if (email == null) {
            throw new IllegalArgumentException("El correo del destinatario es obligatorio.");
        }
    }

    public static EmailRecipient of(String email) {
        return new EmailRecipient(email, null);
    }

    public static EmailRecipient of(String email, String name) {
        return new EmailRecipient(email, name);
    }

    public String displayNameOrEmail() {
        return name == null ? email : name;
    }
}