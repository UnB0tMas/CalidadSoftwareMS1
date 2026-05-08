package com.upsjb.ms1.validator;

import com.upsjb.ms1.domain.entity.Usuario;
import com.upsjb.ms1.shared.exception.ValidationException;
import com.upsjb.ms1.util.PasswordPolicyUtil;
import java.util.List;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class PasswordValidator {

    private final PasswordEncoder passwordEncoder;

    public PasswordValidator(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    public void validateNuevaPassword(String nuevaPassword, String confirmarNuevaPassword) {
        validatePasswordNoVacia(nuevaPassword, "PASSWORD_NUEVA_OBLIGATORIA", "La nueva contraseña es obligatoria.");
        validatePasswordNoVacia(confirmarNuevaPassword, "PASSWORD_CONFIRMACION_OBLIGATORIA", "La confirmación de contraseña es obligatoria.");
        validateConfirmacion(nuevaPassword, confirmarNuevaPassword);
        validateFortaleza(nuevaPassword);
    }

    public void validateCambioPassword(
            Usuario usuario,
            String passwordActual,
            String nuevaPassword,
            String confirmarNuevaPassword
    ) {
        if (usuario == null) {
            throw new ValidationException(
                    "USUARIO_OBLIGATORIO",
                    "El usuario es obligatorio para validar el cambio de contraseña."
            );
        }

        validatePasswordNoVacia(passwordActual, "PASSWORD_ACTUAL_OBLIGATORIA", "La contraseña actual es obligatoria.");
        validateNuevaPassword(nuevaPassword, confirmarNuevaPassword);
        validatePasswordActual(usuario, passwordActual);
        validatePasswordDiferente(usuario, nuevaPassword);
    }

    public void validateResetPassword(
            Usuario usuario,
            String nuevaPassword,
            String confirmarNuevaPassword
    ) {
        if (usuario == null) {
            throw new ValidationException(
                    "USUARIO_OBLIGATORIO",
                    "El usuario es obligatorio para validar el reset de contraseña."
            );
        }

        validateNuevaPassword(nuevaPassword, confirmarNuevaPassword);
        validatePasswordDiferente(usuario, nuevaPassword);
    }

    public void validatePasswordActual(Usuario usuario, String passwordActual) {
        if (usuario == null || usuario.getPasswordHash() == null) {
            throw new ValidationException(
                    "PASSWORD_ACTUAL_INVALIDA",
                    "La contraseña actual no es correcta."
            );
        }

        if (!passwordEncoder.matches(passwordActual, usuario.getPasswordHash())) {
            throw new ValidationException(
                    "PASSWORD_ACTUAL_INVALIDA",
                    "La contraseña actual no es correcta."
            );
        }
    }

    public void validatePasswordDiferente(Usuario usuario, String nuevaPassword) {
        if (usuario == null || usuario.getPasswordHash() == null) {
            return;
        }

        if (passwordEncoder.matches(nuevaPassword, usuario.getPasswordHash())) {
            throw new ValidationException(
                    "PASSWORD_REPETIDA",
                    "La nueva contraseña no puede ser igual a la contraseña actual."
            );
        }
    }

    public void validateConfirmacion(String nuevaPassword, String confirmarNuevaPassword) {
        if (!nuevaPassword.equals(confirmarNuevaPassword)) {
            throw new ValidationException(
                    "PASSWORD_CONFIRMACION_NO_COINCIDE",
                    "La confirmación de contraseña no coincide."
            );
        }
    }

    public void validateFortaleza(String password) {
        List<String> errores = PasswordPolicyUtil.validate(password);

        if (!errores.isEmpty()) {
            throw new ValidationException(
                    "PASSWORD_DEBIL",
                    String.join(" ", errores)
            );
        }
    }

    private void validatePasswordNoVacia(String password, String code, String message) {
        if (password == null || password.isBlank()) {
            throw new ValidationException(code, message);
        }

        if (password.length() > 120) {
            throw new ValidationException(
                    "PASSWORD_LONGITUD_INVALIDA",
                    "La contraseña no puede superar 120 caracteres."
            );
        }
    }
}