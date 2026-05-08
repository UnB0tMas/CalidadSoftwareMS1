package com.upsjb.ms1.shared.audit;

import java.io.Serializable;
import java.time.Instant;

public record AuditContext(
        String requestId,
        String correlationId,
        String ipAddress,
        String userAgent,
        String httpMethod,
        String path,
        Long idUsuario,
        String username,
        Instant requestAt
) implements Serializable {

    public static AuditContext empty() {
        return new AuditContext(null, null, null, null, null, null, null, null, null);
    }

    public AuditContext withUser(Long idUsuario, String username) {
        return new AuditContext(
                requestId,
                correlationId,
                ipAddress,
                userAgent,
                httpMethod,
                path,
                idUsuario,
                username,
                requestAt
        );
    }
}