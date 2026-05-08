package com.upsjb.ms1.policy;

import com.upsjb.ms1.domain.entity.UsuarioSesion;
import com.upsjb.ms1.security.principal.AuthenticatedUserContext;
import com.upsjb.ms1.shared.exception.ForbiddenException;
import org.springframework.stereotype.Component;

@Component
public class SesionPolicy {

    public void ensureCanListOwnSessions(AuthenticatedUserContext actor) {
        ensureAuthenticated(actor);
    }

    public void ensureCanListUserSessions(
            AuthenticatedUserContext actor,
            Long idUsuarioConsultado
    ) {
        ensureAuthenticated(actor);

        if (actor.isAdmin() || actor.isSelf(idUsuarioConsultado)) {
            return;
        }

        throw new ForbiddenException(
                "SESION_LISTAR_AJENAS_DENEGADO",
                "No tienes permisos para consultar sesiones de otro usuario."
        );
    }

    public void ensureCanViewSession(
            AuthenticatedUserContext actor,
            UsuarioSesion sesion
    ) {
        ensureAuthenticated(actor);

        Long idUsuarioSesion = resolveSessionUserId(sesion);

        if (actor.isAdmin() || actor.isSelf(idUsuarioSesion)) {
            return;
        }

        throw new ForbiddenException(
                "SESION_CONSULTAR_AJENA_DENEGADO",
                "No tienes permisos para consultar esta sesión."
        );
    }

    public void ensureCanRevokeSession(
            AuthenticatedUserContext actor,
            UsuarioSesion sesion
    ) {
        ensureAuthenticated(actor);

        Long idUsuarioSesion = resolveSessionUserId(sesion);

        if (actor.isAdmin() || actor.isSelf(idUsuarioSesion)) {
            return;
        }

        throw new ForbiddenException(
                "SESION_REVOCAR_AJENA_DENEGADO",
                "No tienes permisos para revocar esta sesión."
        );
    }

    public void ensureCanLogoutAllOwnSessions(
            AuthenticatedUserContext actor,
            Long idUsuario
    ) {
        ensureAuthenticated(actor);

        if (!actor.isSelf(idUsuario)) {
            throw new ForbiddenException(
                    "SESION_LOGOUT_GLOBAL_AJENO_DENEGADO",
                    "No puedes cerrar todas las sesiones de otro usuario."
            );
        }
    }

    public void ensureCanAdminRevokeUserSessions(
            AuthenticatedUserContext actor,
            Long idUsuario
    ) {
        ensureAuthenticated(actor);

        if (!actor.isAdmin()) {
            throw new ForbiddenException(
                    "SESION_REVOCACION_ADMIN_REQUIERE_ADMIN",
                    "Solo un administrador puede revocar sesiones de otros usuarios."
            );
        }
    }

    private void ensureAuthenticated(AuthenticatedUserContext actor) {
        if (actor == null || !actor.authenticated()) {
            throw new ForbiddenException(
                    "SESION_USUARIO_NO_AUTENTICADO",
                    "Debes iniciar sesión para administrar sesiones."
            );
        }
    }

    private Long resolveSessionUserId(UsuarioSesion sesion) {
        if (sesion == null || sesion.getUsuario() == null) {
            return null;
        }

        return sesion.getUsuario().getId();
    }
}