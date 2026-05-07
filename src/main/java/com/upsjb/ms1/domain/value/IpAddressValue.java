package com.upsjb.ms1.domain.value;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class IpAddressValue implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final int MAX_LENGTH = 45;

    @Column(name = "ip_address", length = MAX_LENGTH)
    private String value;

    private IpAddressValue(String value) {
        this.value = normalize(value);
    }

    public static IpAddressValue of(String rawValue) {
        return new IpAddressValue(rawValue);
    }

    private static String normalize(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            throw new IllegalArgumentException("La dirección IP es obligatoria.");
        }

        String normalized = rawValue.trim();

        if (normalized.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("La dirección IP supera la longitud máxima permitida.");
        }

        try {
            InetAddress.getByName(normalized);
            return normalized;
        } catch (UnknownHostException exception) {
            throw new IllegalArgumentException("La dirección IP no es válida.", exception);
        }
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

        if (!(object instanceof IpAddressValue that)) {
            return false;
        }

        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
}