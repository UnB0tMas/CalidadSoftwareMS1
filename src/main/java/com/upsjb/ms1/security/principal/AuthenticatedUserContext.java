package com.upsjb.ms1.security.principal;

import com.upsjb.ms1.security.roles.SecurityRoles;
import java.io.Serializable;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public record AuthenticatedUserContext(
        Long idUsuario,
        String username,
        String email,
        Long idRol,
        String codigoRol,
        String nombreRol,
        Set<String> authorities,
        Long sessionId,
        String ipAddress,
        String userAgent
) implements Serializable {

    public AuthenticatedUserContext {
        authorities = authorities == null
                ? Set.of()
                : Collections.unmodifiableSet(authorities.stream()
                .filter(Objects::nonNull)
                .map(SecurityRoles::normalizeAuthority)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet()));
    }

    public static AuthenticatedUserContext anonymous() {
        return new AuthenticatedUserContext(
                null,
                null,
                null,
                null,
                null,
                null,
                Set.of(),
                null,
                null,
                null
        );
    }

    public boolean authenticated() {
        return idUsuario != null;
    }

    public boolean isAdmin() {
        return hasRole(SecurityRoles.ADMIN);
    }

    public boolean isEmpleado() {
        return hasRole(SecurityRoles.EMPLEADO);
    }

    public boolean isCliente() {
        return hasRole(SecurityRoles.CLIENTE);
    }

    public boolean isSelf(Long targetUserId) {
        return idUsuario != null && Objects.equals(idUsuario, targetUserId);
    }

    public boolean hasRole(String role) {
        String normalizedRole = SecurityRoles.normalizeRoleCode(role);

        if (normalizedRole == null) {
            return false;
        }

        String normalizedCodigoRol = SecurityRoles.normalizeRoleCode(codigoRol);
        String normalizedNombreRol = SecurityRoles.normalizeRoleCode(nombreRol);
        String expectedAuthority = SecurityRoles.toAuthority(normalizedRole);

        return normalizedRole.equals(normalizedCodigoRol)
                || normalizedRole.equals(normalizedNombreRol)
                || authorities.contains(expectedAuthority);
    }

    public boolean hasAuthority(String authority) {
        String normalized = SecurityRoles.normalizeAuthority(authority);
        return normalized != null && authorities.contains(normalized);
    }
}
