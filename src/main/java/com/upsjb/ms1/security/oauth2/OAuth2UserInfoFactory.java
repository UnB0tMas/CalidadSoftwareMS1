package com.upsjb.ms1.security.oauth2;

import com.upsjb.ms1.domain.enums.ProveedorOAuth2;
import com.upsjb.ms1.shared.exception.ValidationException;
import com.upsjb.ms1.util.StringNormalizer;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class OAuth2UserInfoFactory {

    public OAuth2UserInfo from(
            String registrationId,
            Map<String, Object> attributes
    ) {
        ProveedorOAuth2 proveedor = resolveProvider(registrationId);

        return switch (proveedor) {
            case GOOGLE -> fromGoogle(attributes);
            case MICROSOFT -> fromMicrosoft(attributes);
            case GITHUB -> fromGithub(attributes);
        };
    }

    private OAuth2UserInfo fromGoogle(Map<String, Object> attributes) {
        return new OAuth2UserInfo(
                ProveedorOAuth2.GOOGLE,
                string(attributes, "sub"),
                StringNormalizer.lower(string(attributes, "email")),
                StringNormalizer.normalizeSpaces(string(attributes, "name")),
                usernameFromEmail(string(attributes, "email")),
                string(attributes, "picture"),
                Boolean.TRUE.equals(attributes.get("email_verified")),
                attributes
        );
    }

    private OAuth2UserInfo fromMicrosoft(Map<String, Object> attributes) {
        String email = firstNonBlank(
                string(attributes, "mail"),
                string(attributes, "userPrincipalName"),
                string(attributes, "email")
        );

        return new OAuth2UserInfo(
                ProveedorOAuth2.MICROSOFT,
                firstNonBlank(string(attributes, "id"), string(attributes, "sub")),
                StringNormalizer.lower(email),
                StringNormalizer.normalizeSpaces(string(attributes, "displayName")),
                usernameFromEmail(email),
                null,
                email != null,
                attributes
        );
    }

    private OAuth2UserInfo fromGithub(Map<String, Object> attributes) {
        String email = string(attributes, "email");
        String login = string(attributes, "login");

        return new OAuth2UserInfo(
                ProveedorOAuth2.GITHUB,
                String.valueOf(attributes.get("id")),
                StringNormalizer.lower(email),
                StringNormalizer.normalizeSpaces(firstNonBlank(string(attributes, "name"), login)),
                StringNormalizer.lower(login),
                string(attributes, "avatar_url"),
                email != null && !email.isBlank(),
                attributes
        );
    }

    private ProveedorOAuth2 resolveProvider(String registrationId) {
        String normalized = StringNormalizer.upper(registrationId);

        if (normalized == null) {
            throw new ValidationException(
                    "OAUTH2_PROVIDER_REQUIRED",
                    "El proveedor OAuth2 es obligatorio."
            );
        }

        try {
            return ProveedorOAuth2.valueOf(normalized);
        } catch (IllegalArgumentException exception) {
            throw new ValidationException(
                    "OAUTH2_PROVIDER_UNSUPPORTED",
                    "El proveedor OAuth2 no está soportado."
            );
        }
    }

    private String string(Map<String, Object> attributes, String key) {
        Object value = attributes.get(key);
        return value == null ? null : String.valueOf(value);
    }

    private String usernameFromEmail(String email) {
        String normalized = StringNormalizer.lower(email);

        if (normalized == null || !normalized.contains("@")) {
            return null;
        }

        return normalized.substring(0, normalized.indexOf('@'))
                .replaceAll("[^a-z0-9._-]", "");
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            String normalized = StringNormalizer.normalizeSpaces(value);

            if (normalized != null) {
                return normalized;
            }
        }

        return null;
    }
}