package com.upsjb.ms1.security.roles;

import java.util.Locale;
import java.util.Set;

public final class SecurityRoles {

    public static final String ROLE_PREFIX = "ROLE_";

    public static final String ADMIN = "ADMIN";
    public static final String EMPLEADO = "EMPLEADO";
    public static final String CLIENTE = "CLIENTE";

    public static final String AUTHORITY_ADMIN = ROLE_PREFIX + ADMIN;
    public static final String AUTHORITY_EMPLEADO = ROLE_PREFIX + EMPLEADO;
    public static final String AUTHORITY_CLIENTE = ROLE_PREFIX + CLIENTE;

    public static final Set<String> SYSTEM_ROLE_CODES = Set.of(ADMIN, EMPLEADO, CLIENTE);
    public static final Set<String> SYSTEM_AUTHORITIES = Set.of(AUTHORITY_ADMIN, AUTHORITY_EMPLEADO, AUTHORITY_CLIENTE);

    private SecurityRoles() {
    }

    public static boolean isSystemRole(String value) {
        String normalized = normalizeRoleCode(value);
        return normalized != null && SYSTEM_ROLE_CODES.contains(normalized);
    }

    public static String toAuthority(String roleCode) {
        String normalized = normalizeRoleCode(roleCode);
        return normalized == null ? null : ROLE_PREFIX + normalized;
    }

    public static String normalizeRoleCode(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        String normalized = value.trim().toUpperCase(Locale.ROOT);

        if (normalized.startsWith(ROLE_PREFIX)) {
            return normalized.substring(ROLE_PREFIX.length());
        }

        return normalized;
    }

    public static String normalizeAuthority(String value) {
        String roleCode = normalizeRoleCode(value);
        return roleCode == null ? null : ROLE_PREFIX + roleCode;
    }
}
