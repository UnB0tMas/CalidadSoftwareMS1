package com.upsjb.ms1.policy;

import com.upsjb.ms1.domain.entity.Rol;
import com.upsjb.ms1.security.principal.AuthenticatedUserContext;
import com.upsjb.ms1.shared.exception.ForbiddenException;
import org.springframework.stereotype.Component;

@Component
public class RolPolicy {

    public void ensureCanCreate(AuthenticatedUserContext actor) {
        ensureAdmin(actor, "ROL_CREAR_REQUIERE_ADMIN");
    }

    public void ensureCanUpdate(AuthenticatedUserContext actor, Rol rol) {
        ensureAdmin(actor, "ROL_ACTUALIZAR_REQUIERE_ADMIN");
    }

    public void ensureCanChangeEstado(AuthenticatedUserContext actor, Rol rol) {
        ensureAdmin(actor, "ROL_CAMBIAR_ESTADO_REQUIERE_ADMIN");
    }

    public void ensureCanDelete(AuthenticatedUserContext actor, Rol rol) {
        ensureAdmin(actor, "ROL_ELIMINAR_REQUIERE_ADMIN");
    }

    public void ensureCanFindById(AuthenticatedUserContext actor) {
        ensureAdmin(actor, "ROL_CONSULTAR_REQUIERE_ADMIN");
    }

    public void ensureCanList(AuthenticatedUserContext actor) {
        ensureAdmin(actor, "ROL_LISTAR_REQUIERE_ADMIN");
    }

    public void ensureCanLookup(AuthenticatedUserContext actor) {
        ensureAdmin(actor, "ROL_LOOKUP_REQUIERE_ADMIN");
    }

    private void ensureAdmin(AuthenticatedUserContext actor, String code) {
        if (actor == null || !actor.authenticated()) {
            throw new ForbiddenException(
                    "ROL_USUARIO_NO_AUTENTICADO",
                    "Debes iniciar sesión para administrar roles."
            );
        }

        if (!actor.isAdmin()) {
            throw new ForbiddenException(
                    code,
                    "Solo un administrador puede administrar roles."
            );
        }
    }
}