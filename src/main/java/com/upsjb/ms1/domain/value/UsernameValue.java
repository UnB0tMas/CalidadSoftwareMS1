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
public class UsernameValue implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final int MIN_LENGTH = 4;
    private static final int MAX_LENGTH = 60;

    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-z0-9._-]+$");

    @Column(name = "username", length = MAX_LENGTH)
    private String value;

    private UsernameValue(String value) {
        this.value = normalize(value);
    }

    public static UsernameValue of(String rawValue) {
        return new UsernameValue(rawValue);
    }

    private static String normalize(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            throw new IllegalArgumentException("El username es obligatorio.");
        }

        String normalized = rawValue.trim().toLowerCase(Locale.ROOT);

        if (normalized.length() < MIN_LENGTH) {
            throw new IllegalArgumentException("El username debe tener al menos 4 caracteres.");
        }

        if (normalized.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("El username supera la longitud máxima permitida.");
        }

        if (!USERNAME_PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException(
                    "El username solo puede contener letras minúsculas, números, punto, guion y guion bajo."
            );
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

        if (!(object instanceof UsernameValue that)) {
            return false;
        }

        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
}