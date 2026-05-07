package com.upsjb.ms1.domain.value;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EmailValue implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final int MAX_LENGTH = 180;

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$",
            Pattern.CASE_INSENSITIVE
    );

    @Column(name = "email", length = MAX_LENGTH)
    private String value;

    private EmailValue(String value) {
        this.value = normalize(value);
    }

    public static EmailValue of(String rawValue) {
        return new EmailValue(rawValue);
    }

    private static String normalize(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            throw new IllegalArgumentException("El email es obligatorio.");
        }

        String normalized = rawValue.trim().toLowerCase(Locale.ROOT);

        if (normalized.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("El email supera la longitud máxima permitida.");
        }

        if (!EMAIL_PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException("El formato del email no es válido.");
        }

        return normalized;
    }

    @Override
    public String toString() {
        return value;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (!(object instanceof EmailValue that)) {
            return false;
        }

        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
}