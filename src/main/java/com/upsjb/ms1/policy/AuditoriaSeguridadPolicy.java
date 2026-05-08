package com.upsjb.ms1.policy;

import com.upsjb.ms1.security.principal.AuthenticatedUserContext;
import com.upsjb.ms1.shared.exception.ForbiddenException;
import org.springframework.stereotype.Component;

@Component
public class AuditoriaSeguridadPolicy {

    public void ensureCanListSecurityAudit(AuthenticatedUserContext actor) {
        ensureAdmin(actor, "AUDITORIA_CONSULTA_REQUIERE_ADMIN");
    }

    public void ensureCanViewSecurityAuditDetail(AuthenticatedUserContext actor) {
        ensureAdmin(actor, "AUDITORIA_DETALLE_REQUIERE_ADMIN");
    }

    public void ensureCanListLoginAttempts(AuthenticatedUserContext actor) {
        ensureAdmin(actor, "LOGIN_ATTEMPTS_REQUIERE_ADMIN");
    }

    public void ensureCanListIpHistory(AuthenticatedUserContext actor) {
        ensureAdmin(actor, "IP_HISTORIAL_REQUIERE_ADMIN");
    }

    public void ensureCanViewOwnIpHistory(
            AuthenticatedUserContext actor,
            Long idUsuarioConsultado
    ) {
        ensureAuthenticated(actor);

        if (actor.isAdmin() || actor.isSelf(idUsuarioConsultado)) {
            return;
        }

        throw new ForbiddenException(
                "IP_HISTORIAL_AJENO_DENEGADO",
                "No tienes permisos para consultar el historial de IP de otro usuario."
        );
    }

    private void ensureAdmin(AuthenticatedUserContext actor, String code) {
        ensureAuthenticated(actor);

        if (!actor.isAdmin()) {
            throw new ForbiddenException(
                    code,
                    "Solo un administrador puede consultar información de auditoría."
            );
        }
    }

    private void ensureAuthenticated(AuthenticatedUserContext actor) {
        if (actor == null || !actor.authenticated()) {
            throw new ForbiddenException(
                    "AUDITORIA_USUARIO_NO_AUTENTICADO",
                    "Debes iniciar sesión para consultar auditoría."
            );
        }
    }
}