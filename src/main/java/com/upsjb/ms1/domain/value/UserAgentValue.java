package com.upsjb.ms1.domain.value;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserAgentValue implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final int MAX_LENGTH = 512;

    @Column(name = "user_agent", length = MAX_LENGTH)
    private String value;

    private UserAgentValue(String value) {
        this.value = normalize(value);
    }

    public static UserAgentValue of(String rawValue) {
        return new UserAgentValue(rawValue);
    }

    private static String normalize(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return "UNKNOWN";
        }

        String normalized = rawValue.trim().replaceAll("\\s+", " ");

        if (normalized.length() > MAX_LENGTH) {
            return normalized.substring(0, MAX_LENGTH);
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

        if (!(object instanceof UserAgentValue that)) {
            return false;
        }

        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
}