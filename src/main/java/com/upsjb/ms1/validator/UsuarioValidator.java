// src/main/java/com/upsjb/ms1/validator/UsuarioValidator.java
package com.upsjb.ms1.validator;

import com.upsjb.ms1.domain.entity.Rol;
import com.upsjb.ms1.domain.entity.Usuario;
import com.upsjb.ms1.domain.enums.EstadoRegistro;
import com.upsjb.ms1.domain.value.EmailValue;
import com.upsjb.ms1.domain.value.UsernameValue;
import com.upsjb.ms1.repository.UsuarioRepository;
import com.upsjb.ms1.security.roles.SecurityRoles;
import com.upsjb.ms1.shared.exception.ConflictException;
import com.upsjb.ms1.shared.exception.NotFoundException;
import com.upsjb.ms1.shared.exception.ValidationException;
import com.upsjb.ms1.util.StringNormalizer;
import java.time.Clock;
import java.time.Instant;
import org.springframework.stereotype.Component;

@Component
public class UsuarioValidator {

    private static final int NOMBRES_MAX_LENGTH = 120;
    private static final int APELLIDOS_MAX_LENGTH = 120;

    private final UsuarioRepository usuarioRepository;
    private final RolValidator rolValidator;
    private final PasswordValidator passwordValidator;
    private final Clock clock;

    public UsuarioValidator(
            UsuarioRepository usuarioRepository,
            RolValidator rolValidator,
            PasswordValidator passwordValidator,
            Clock clock
    ) {
        this.usuarioRepository = usuarioRepository;
        this.rolValidator = rolValidator;
        this.passwordValidator = passwordValidator;
        this.clock = clock;
    }

    public Usuario requireById(Long idUsuario) {
        if (idUsuario == null) {
            throw new ValidationException(
                    "USUARIO_ID_OBLIGATORIO",
                    "El usuario es obligatorio."
            );
        }

        return usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new NotFoundException(
                        "USUARIO_NO_ENCONTRADO",
                        "No se encontró el usuario solicitado."
                ));
    }

    public Usuario requireWithRolById(Long idUsuario) {
        if (idUsuario == null) {
            throw new ValidationException(
                    "USUARIO_ID_OBLIGATORIO",
                    "El usuario es obligatorio."
            );
        }

        return usuarioRepository.findWithRolById(idUsuario)
                .orElseThrow(() -> new NotFoundException(
                        "USUARIO_NO_ENCONTRADO",
                        "No se encontró el usuario solicitado."
                ));
    }

    public Usuario requireActivoById(Long idUsuario) {
        Usuario usuario = requireWithRolById(idUsuario);
        validateActivo(usuario);
        validateNoBloqueado(usuario);
        validateRolActivo(usuario);
        return usuario;
    }

    public void validateCreate(
            String username,
            String email,
            Long idRol,
            String password,
            String confirmarPassword
    ) {
        validateCreateBase(username, email, idRol, password, confirmarPassword);
    }

    public void validateCreate(
            String username,
            String email,
            Long idRol,
            String password,
            String confirmarPassword,
            String nombres,
            String apellidos
    ) {
        validateCreateBase(username, email, idRol, password, confirmarPassword);
        validateRequiredText(
                nombres,
                "USUARIO_NOMBRES_OBLIGATORIOS",
                "Los nombres son obligatorios.",
                "USUARIO_NOMBRES_LONGITUD_INVALIDA",
                "Los nombres no pueden superar 120 caracteres.",
                NOMBRES_MAX_LENGTH
        );
        validateRequiredText(
                apellidos,
                "USUARIO_APELLIDOS_OBLIGATORIOS",
                "Los apellidos son obligatorios.",
                "USUARIO_APELLIDOS_LONGITUD_INVALIDA",
                "Los apellidos no pueden superar 120 caracteres.",
                APELLIDOS_MAX_LENGTH
        );
    }

    public void validateUpdate(Long idUsuario, String email) {
        Usuario usuario = requireById(idUsuario);
        validateUpdate(usuario, email);
    }

    public void validateUpdate(Usuario usuario, String email) {
        validateUsuarioEditable(usuario);
        validateEmailDisponibleParaUpdate(usuario.getId(), email);
    }

    public void validateUpdate(
            Usuario usuario,
            String email,
            String nombres,
            String apellidos
    ) {
        validateUpdate(usuario, email);
        validateOptionalText(
                nombres,
                "USUARIO_NOMBRES_INVALIDOS",
                "Los nombres no pueden estar vacíos.",
                "USUARIO_NOMBRES_LONGITUD_INVALIDA",
                "Los nombres no pueden superar 120 caracteres.",
                NOMBRES_MAX_LENGTH
        );
        validateOptionalText(
                apellidos,
                "USUARIO_APELLIDOS_INVALIDOS",
                "Los apellidos no pueden estar vacíos.",
                "USUARIO_APELLIDOS_LONGITUD_INVALIDA",
                "Los apellidos no pueden superar 120 caracteres.",
                APELLIDOS_MAX_LENGTH
        );
    }

    public void validateCambioRol(Long idUsuario, Long idRol) {
        Usuario usuario = requireWithRolById(idUsuario);
        Rol nuevoRol = rolValidator.requireActivoById(idRol);
        validateCambioRol(usuario, nuevoRol);
    }

    public void validateCambioRol(Usuario usuario, Rol nuevoRol) {
        validateUsuarioEditable(usuario);

        if (nuevoRol == null) {
            throw new ValidationException(
                    "USUARIO_ROL_OBLIGATORIO",
                    "El nuevo rol es obligatorio."
            );
        }

        rolValidator.validateActivo(nuevoRol);

        if (isSameRol(usuario.getRol(), nuevoRol)) {
            return;
        }

        if (isActiveAdmin(usuario) && !isAdminRole(nuevoRol)) {
            validateNoEsUltimoAdminActivo();
        }
    }

    public void validateCambioEstado(Usuario usuario, EstadoRegistro nuevoEstado) {
        if (usuario == null) {
            throw new NotFoundException(
                    "USUARIO_NO_ENCONTRADO",
                    "No se encontró el usuario solicitado."
            );
        }

        if (nuevoEstado == null) {
            throw new ValidationException(
                    "USUARIO_ESTADO_OBLIGATORIO",
                    "El nuevo estado del usuario es obligatorio."
            );
        }

        if (usuario.estaEliminado() && !EstadoRegistro.ACTIVO.equals(nuevoEstado)) {
            throw new ValidationException(
                    "USUARIO_ELIMINADO",
                    "No se puede cambiar el estado de un usuario eliminado."
            );
        }

        if (isActiveAdmin(usuario) && !EstadoRegistro.ACTIVO.equals(nuevoEstado)) {
            validateNoEsUltimoAdminActivo();
        }
    }

    public void validateActivo(Usuario usuario) {
        if (usuario == null) {
            throw new NotFoundException(
                    "USUARIO_NO_ENCONTRADO",
                    "No se encontró el usuario solicitado."
            );
        }

        if (usuario.estaEliminado()) {
            throw new ValidationException(
                    "USUARIO_ELIMINADO",
                    "El usuario fue eliminado."
            );
        }

        if (usuario.estaInactivo()) {
            throw new ValidationException(
                    "USUARIO_INACTIVO",
                    "El usuario se encuentra inactivo."
            );
        }
    }

    public void validateNoBloqueado(Usuario usuario) {
        if (usuario == null) {
            throw new NotFoundException(
                    "USUARIO_NO_ENCONTRADO",
                    "No se encontró el usuario solicitado."
            );
        }

        Instant ahora = Instant.now(clock);

        if (usuario.estaBloqueado() || usuario.tieneBloqueoTemporalActivo(ahora)) {
            throw new ValidationException(
                    "USUARIO_BLOQUEADO",
                    "El usuario se encuentra bloqueado."
            );
        }
    }

    public void validateRolActivo(Usuario usuario) {
        if (usuario == null || usuario.getRol() == null) {
            throw new ValidationException(
                    "USUARIO_ROL_INVALIDO",
                    "El usuario no tiene un rol válido asociado."
            );
        }

        rolValidator.validateActivo(usuario.getRol());
    }

    public String normalizeUsername(String username) {
        try {
            return UsernameValue.of(username).getValue();
        } catch (IllegalArgumentException exception) {
            throw new ValidationException(
                    "USUARIO_USERNAME_INVALIDO",
                    exception.getMessage()
            );
        }
    }

    public String normalizeEmail(String email) {
        try {
            return EmailValue.of(email).getValue();
        } catch (IllegalArgumentException exception) {
            throw new ValidationException(
                    "USUARIO_EMAIL_INVALIDO",
                    exception.getMessage()
            );
        }
    }

    private void validateCreateBase(
            String username,
            String email,
            Long idRol,
            String password,
            String confirmarPassword
    ) {
        String usernameNormalizado = normalizeUsername(username);
        String emailNormalizado = normalizeEmail(email);

        if (usuarioRepository.existsByUsername_ValueIgnoreCase(usernameNormalizado)) {
            throw new ConflictException(
                    "USUARIO_USERNAME_DUPLICADO",
                    "El username ya se encuentra registrado."
            );
        }

        if (usuarioRepository.existsByEmail_ValueIgnoreCase(emailNormalizado)) {
            throw new ConflictException(
                    "USUARIO_EMAIL_DUPLICADO",
                    "El correo electrónico ya se encuentra registrado."
            );
        }

        if (idRol == null) {
            throw new ValidationException(
                    "USUARIO_ROL_OBLIGATORIO",
                    "El rol del usuario es obligatorio."
            );
        }

        passwordValidator.validateNuevaPassword(password, confirmarPassword);
    }

    private void validateEmailDisponibleParaUpdate(Long idUsuario, String email) {
        String emailNormalizado = normalizeOptionalEmail(email);

        if (emailNormalizado != null
                && usuarioRepository.existsByEmail_ValueIgnoreCaseAndIdNot(emailNormalizado, idUsuario)) {
            throw new ConflictException(
                    "USUARIO_EMAIL_DUPLICADO",
                    "El correo electrónico ya se encuentra registrado."
            );
        }
    }

    private void validateUsuarioEditable(Usuario usuario) {
        if (usuario == null) {
            throw new NotFoundException(
                    "USUARIO_NO_ENCONTRADO",
                    "No se encontró el usuario solicitado."
            );
        }

        if (usuario.estaEliminado()) {
            throw new ValidationException(
                    "USUARIO_ELIMINADO",
                    "No se puede modificar un usuario eliminado."
            );
        }
    }

    private String normalizeOptionalEmail(String email) {
        if (email == null) {
            return null;
        }

        return normalizeEmail(email);
    }

    private void validateRequiredText(
            String value,
            String requiredCode,
            String requiredMessage,
            String lengthCode,
            String lengthMessage,
            int maxLength
    ) {
        String normalized = StringNormalizer.normalizeSpaces(value);

        if (normalized == null) {
            throw new ValidationException(requiredCode, requiredMessage);
        }

        if (normalized.length() > maxLength) {
            throw new ValidationException(lengthCode, lengthMessage);
        }
    }

    private void validateOptionalText(
            String value,
            String blankCode,
            String blankMessage,
            String lengthCode,
            String lengthMessage,
            int maxLength
    ) {
        if (value == null) {
            return;
        }

        String normalized = StringNormalizer.normalizeSpaces(value);

        if (normalized == null) {
            throw new ValidationException(blankCode, blankMessage);
        }

        if (normalized.length() > maxLength) {
            throw new ValidationException(lengthCode, lengthMessage);
        }
    }

    private boolean isSameRol(Rol actual, Rol nuevo) {
        return actual != null
                && actual.getId() != null
                && nuevo != null
                && actual.getId().equals(nuevo.getId());
    }

    private boolean isActiveAdmin(Usuario usuario) {
        return usuario != null
                && usuario.estaActivo()
                && isAdminRole(usuario.getRol());
    }

    private boolean isAdminRole(Rol rol) {
        return rol != null
                && rol.getCodigo() != null
                && SecurityRoles.ADMIN.equalsIgnoreCase(SecurityRoles.normalizeRoleCode(rol.getCodigo()));
    }

    private void validateNoEsUltimoAdminActivo() {
        long administradoresActivos = usuarioRepository.countByRol_CodigoIgnoreCaseAndEstado(
                SecurityRoles.ADMIN,
                EstadoRegistro.ACTIVO
        );

        if (administradoresActivos <= 1) {
            throw new ConflictException(
                    "USUARIO_ULTIMO_ADMIN_ACTIVO",
                    "No se puede completar la operación porque el sistema debe conservar al menos un administrador activo."
            );
        }
    }
}