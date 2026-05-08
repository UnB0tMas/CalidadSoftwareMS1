package com.upsjb.ms1.policy;

import com.upsjb.ms1.security.principal.AuthenticatedUserContext;
import com.upsjb.ms1.shared.exception.ForbiddenException;
import org.springframework.stereotype.Component;

@Component
public class AuthPolicy {

    public void ensureAuthenticated(AuthenticatedUserContext actor) {
        if (actor == null || !actor.authenticated()) {
            throw new ForbiddenException(
                    "AUTH_USUARIO_NO_AUTENTICADO",
                    "Debes iniciar sesión para realizar esta acción."
            );
        }
    }

    public void ensureCanViewCurrentUser(AuthenticatedUserContext actor) {
        ensureAuthenticated(actor);
    }

    public void ensureCanLogoutCurrentSession(AuthenticatedUserContext actor) {
        ensureAuthenticated(actor);
    }

    public void ensureCanLogoutAllOwnSessions(AuthenticatedUserContext actor) {
        ensureAuthenticated(actor);
    }

    public void ensureCanAccessAdminAuthOperation(AuthenticatedUserContext actor) {
        ensureAdmin(actor);
    }

    private void ensureAdmin(AuthenticatedUserContext actor) {
        ensureAuthenticated(actor);

        if (!actor.isAdmin()) {
            throw new ForbiddenException(
                    "AUTH_REQUIERE_ADMIN",
                    "Solo un administrador puede realizar esta acción."
            );
        }
    }
}