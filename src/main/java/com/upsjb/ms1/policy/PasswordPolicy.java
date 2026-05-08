package com.upsjb.ms1.policy;

import com.upsjb.ms1.security.principal.AuthenticatedUserContext;
import com.upsjb.ms1.shared.exception.ForbiddenException;
import org.springframework.stereotype.Component;

@Component
public class PasswordPolicy {

    public void ensureCanRequestOwnPasswordChange(AuthenticatedUserContext actor) {
        ensureAuthenticated(actor);
    }

    public void ensureCanConfirmOwnPasswordChange(
            AuthenticatedUserContext actor,
            Long idUsuarioPassword
    ) {
        ensureAuthenticated(actor);

        if (!actor.isSelf(idUsuarioPassword)) {
            throw new ForbiddenException(
                    "PASSWORD_CAMBIO_USUARIO_AJENO_DENEGADO",
                    "No puedes confirmar el cambio de contraseña de otro usuario."
            );
        }
    }

    public void ensureCanForcePasswordReset(
            AuthenticatedUserContext actor,
            Long idUsuarioPassword
    ) {
        ensureAuthenticated(actor);

        if (actor.isAdmin() || actor.isSelf(idUsuarioPassword)) {
            return;
        }

        throw new ForbiddenException(
                "PASSWORD_RESET_FORZADO_DENEGADO",
                "No tienes permisos para forzar el restablecimiento de contraseña de este usuario."
        );
    }

    public void ensureCanCompletePublicPasswordReset() {
        /*
         * La recuperación por olvido de contraseña es pública por diseño,
         * pero la autorización real depende de validar token/código, expiración e intentos
         * en PasswordResetService y VerificacionCodigoValidator.
         */
    }

    private void ensureAuthenticated(AuthenticatedUserContext actor) {
        if (actor == null || !actor.authenticated()) {
            throw new ForbiddenException(
                    "PASSWORD_USUARIO_NO_AUTENTICADO",
                    "Debes iniciar sesión para realizar esta operación de contraseña."
            );
        }
    }
}