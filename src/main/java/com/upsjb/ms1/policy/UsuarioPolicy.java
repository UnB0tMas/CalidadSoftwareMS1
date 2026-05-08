package com.upsjb.ms1.policy;

import com.upsjb.ms1.domain.entity.Usuario;
import com.upsjb.ms1.domain.enums.EstadoRegistro;
import com.upsjb.ms1.security.principal.AuthenticatedUserContext;
import com.upsjb.ms1.shared.exception.ForbiddenException;
import org.springframework.stereotype.Component;

@Component
public class UsuarioPolicy {

    public void ensureCanCreate(AuthenticatedUserContext actor) {
        ensureAdmin(actor, "USUARIO_CREAR_REQUIERE_ADMIN");
    }

    public void ensureCanList(AuthenticatedUserContext actor) {
        ensureAdmin(actor, "USUARIO_LISTAR_REQUIERE_ADMIN");
    }

    public void ensureCanFindById(
            AuthenticatedUserContext actor,
            Long idUsuarioConsultado
    ) {
        ensureAuthenticated(actor);

        if (actor.isAdmin() || actor.isSelf(idUsuarioConsultado)) {
            return;
        }

        throw new ForbiddenException(
                "USUARIO_CONSULTAR_AJENO_DENEGADO",
                "No tienes permisos para consultar este usuario."
        );
    }

    public void ensureCanUpdate(
            AuthenticatedUserContext actor,
            Long idUsuarioActualizado
    ) {
        ensureAuthenticated(actor);

        if (actor.isAdmin() || actor.isSelf(idUsuarioActualizado)) {
            return;
        }

        throw new ForbiddenException(
                "USUARIO_ACTUALIZAR_AJENO_DENEGADO",
                "No tienes permisos para actualizar este usuario."
        );
    }

    public void ensureCanChangeEstado(
            AuthenticatedUserContext actor,
            Usuario usuario,
            EstadoRegistro nuevoEstado
    ) {
        ensureAdmin(actor, "USUARIO_ESTADO_REQUIERE_ADMIN");

        Long idUsuario = usuario == null ? null : usuario.getId();

        if (actor.isSelf(idUsuario)) {
            throw new ForbiddenException(
                    "USUARIO_NO_PUEDE_CAMBIAR_PROPIO_ESTADO",
                    "No puedes cambiar tu propio estado."
            );
        }
    }

    public void ensureCanBlock(
            AuthenticatedUserContext actor,
            Usuario usuario
    ) {
        ensureAdmin(actor, "USUARIO_BLOQUEAR_REQUIERE_ADMIN");

        Long idUsuario = usuario == null ? null : usuario.getId();

        if (actor.isSelf(idUsuario)) {
            throw new ForbiddenException(
                    "USUARIO_NO_PUEDE_BLOQUEARSE_A_SI_MISMO",
                    "No puedes bloquear tu propia cuenta."
            );
        }
    }

    public void ensureCanUnlock(
            AuthenticatedUserContext actor,
            Usuario usuario
    ) {
        ensureAdmin(actor, "USUARIO_DESBLOQUEAR_REQUIERE_ADMIN");
    }

    public void ensureCanChangeRol(
            AuthenticatedUserContext actor,
            Usuario usuario
    ) {
        ensureAdmin(actor, "USUARIO_CAMBIAR_ROL_REQUIERE_ADMIN");

        Long idUsuario = usuario == null ? null : usuario.getId();

        if (actor.isSelf(idUsuario)) {
            throw new ForbiddenException(
                    "USUARIO_NO_PUEDE_CAMBIAR_PROPIO_ROL",
                    "No puedes cambiar tu propio rol."
            );
        }
    }

    public void ensureCanDelete(
            AuthenticatedUserContext actor,
            Usuario usuario
    ) {
        ensureAdmin(actor, "USUARIO_ELIMINAR_REQUIERE_ADMIN");

        Long idUsuario = usuario == null ? null : usuario.getId();

        if (actor.isSelf(idUsuario)) {
            throw new ForbiddenException(
                    "USUARIO_NO_PUEDE_ELIMINARSE_A_SI_MISMO",
                    "No puedes eliminar tu propia cuenta."
            );
        }
    }

    private void ensureAuthenticated(AuthenticatedUserContext actor) {
        if (actor == null || !actor.authenticated()) {
            throw new ForbiddenException(
                    "USUARIO_NO_AUTENTICADO",
                    "Debes iniciar sesión para realizar esta acción."
            );
        }
    }

    private void ensureAdmin(AuthenticatedUserContext actor, String code) {
        ensureAuthenticated(actor);

        if (!actor.isAdmin()) {
            throw new ForbiddenException(
                    code,
                    "Solo un administrador puede administrar usuarios."
            );
        }
    }
}