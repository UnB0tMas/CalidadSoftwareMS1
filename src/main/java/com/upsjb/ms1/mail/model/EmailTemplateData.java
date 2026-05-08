package com.upsjb.ms1.mail.model;

import com.upsjb.ms1.util.StringNormalizer;
import java.io.Serializable;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public record EmailTemplateData(
        String templateName,
        String subject,
        Map<String, Object> variables
) implements Serializable {

    public EmailTemplateData {
        templateName = StringNormalizer.trimToNull(templateName);
        subject = StringNormalizer.normalizeSpaces(subject);

        if (templateName == null) {
            throw new IllegalArgumentException("El nombre de la plantilla es obligatorio.");
        }

        variables = variables == null
                ? Map.of()
                : Collections.unmodifiableMap(new LinkedHashMap<>(variables));
    }

    public static EmailTemplateData of(
            String templateName,
            String subject,
            Map<String, Object> variables
    ) {
        return new EmailTemplateData(templateName, subject, variables);
    }

    public String getString(String key) {
        Object value = variables.get(key);
        return value == null ? null : String.valueOf(value);
    }

    public String getStringOrDefault(String key, String defaultValue) {
        String value = getString(key);
        return value == null || value.isBlank() ? defaultValue : value;
    }

    public Instant getInstant(String key) {
        Object value = variables.get(key);

        if (value instanceof Instant instant) {
            return instant;
        }

        if (value instanceof String stringValue && !stringValue.isBlank()) {
            return Instant.parse(stringValue);
        }

        return null;
    }

    public EmailTemplateData withDefaults(Map<String, Object> defaults) {
        Map<String, Object> merged = new LinkedHashMap<>();

        if (defaults != null) {
            merged.putAll(defaults);
        }

        merged.putAll(variables);

        return new EmailTemplateData(templateName, subject, merged);
    }
}