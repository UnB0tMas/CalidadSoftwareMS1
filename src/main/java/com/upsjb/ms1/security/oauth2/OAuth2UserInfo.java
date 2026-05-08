package com.upsjb.ms1.security.oauth2;

import com.upsjb.ms1.domain.enums.ProveedorOAuth2;
import java.io.Serializable;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public record OAuth2UserInfo(
        ProveedorOAuth2 proveedor,
        String providerUserId,
        String email,
        String name,
        String username,
        String avatarUrl,
        boolean emailVerified,
        Map<String, Object> rawAttributes
) implements Serializable {

    public OAuth2UserInfo {
        rawAttributes = rawAttributes == null
                ? Map.of()
                : Collections.unmodifiableMap(new LinkedHashMap<>(rawAttributes));

        if (proveedor == null) {
            throw new IllegalArgumentException("El proveedor OAuth2 es obligatorio.");
        }

        if (providerUserId == null || providerUserId.isBlank()) {
            throw new IllegalArgumentException("El identificador del usuario OAuth2 es obligatorio.");
        }
    }
}