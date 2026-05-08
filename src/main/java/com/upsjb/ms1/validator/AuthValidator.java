package com.upsjb.ms1.validator;

import com.upsjb.ms1.config.AppPropertiesConfig;
import com.upsjb.ms1.domain.entity.Rol;
import com.upsjb.ms1.domain.entity.Usuario;
import com.upsjb.ms1.repository.UsuarioRepository;
import com.upsjb.ms1.service.contract.LoginAttemptService;
import com.upsjb.ms1.shared.exception.UnauthorizedException;
import com.upsjb.ms1.shared.exception.ValidationException;
import com.upsjb.ms1.util.StringNormalizer;
import java.time.Clock;
import java.time.Instant;
import org.springframework.stereotype.Component;

@Component
public class AuthValidator {

    private final UsuarioRepository usuarioRepository;
    private final LoginAttemptService loginAttemptService;
    private final AppPropertiesConfig appProperties;
    private final Clock clock;

    public AuthValidator(
            UsuarioRepository usuarioRepository,
            LoginAttemptService loginAttemptService,
            AppPropertiesConfig appProperties,
            Clock clock
    ) {
        this.usuarioRepository = usuarioRepository;
        this.loginAttemptService = loginAttemptService;
        this.appProperties = appProperties;
        this.clock = clock;
    }

    public Usuario requireUsuarioForLogin(String usernameOrEmail) {
        String normalized = requireCredentialIdentifier(usernameOrEmail);

        Usuario usuario = usuarioRepository.findByUsernameOrEmailWithRol(normalized)
                .orElseThrow(() -> new UnauthorizedException(
                        "CREDENCIALES_INVALIDAS",
                        "Las credenciales ingresadas no son válidas."
                ));

        validateUsuarioPuedeAutenticarse(usuario);
        validateMaxFailedAttempts(normalized);

        return usuario;
    }

    public void validateUsuarioPuedeAutenticarse(Usuario usuario) {
        if (usuario == null) {
            throw new UnauthorizedException(
                    "CREDENCIALES_INVALIDAS",
                    "Las credenciales ingresadas no son válidas."
            );
        }

        if (usuario.estaEliminado()) {
            throw new UnauthorizedException(
                    "USUARIO_NO_DISPONIBLE",
                    "La cuenta no se encuentra disponible."
            );
        }

        if (usuario.estaInactivo()) {
            throw new UnauthorizedException(
                    "USUARIO_INACTIVO",
                    "La cuenta se encuentra inactiva."
            );
        }

        Instant ahora = Instant.now(clock);

        if (usuario.estaBloqueado() || usuario.tieneBloqueoTemporalActivo(ahora)) {
            throw new UnauthorizedException(
                    "USUARIO_BLOQUEADO",
                    "La cuenta se encuentra bloqueada temporalmente."
            );
        }

        Rol rol = usuario.getRol();

        if (rol == null || !rol.estaActivo()) {
            throw new UnauthorizedException(
                    "ROL_INACTIVO",
                    "El rol asociado a la cuenta no está activo."
            );
        }
    }

    public void validatePasswordMatches(boolean matches) {
        if (!matches) {
            throw new UnauthorizedException(
                    "CREDENCIALES_INVALIDAS",
                    "Las credenciales ingresadas no son válidas."
            );
        }
    }

    public String requireCredentialIdentifier(String usernameOrEmail) {
        String normalized = StringNormalizer.lower(usernameOrEmail);

        if (normalized == null) {
            throw new ValidationException(
                    "AUTH_CREDENCIAL_OBLIGATORIA",
                    "El username o email es obligatorio."
            );
        }

        if (normalized.length() > 180) {
            throw new ValidationException(
                    "AUTH_CREDENCIAL_LONGITUD_INVALIDA",
                    "El username o email no puede superar 180 caracteres."
            );
        }

        return normalized;
    }

    public String requirePassword(String password) {
        String normalized = StringNormalizer.trimToNull(password);

        if (normalized == null) {
            throw new ValidationException(
                    "AUTH_PASSWORD_OBLIGATORIA",
                    "La contraseña es obligatoria."
            );
        }

        if (normalized.length() > 120) {
            throw new ValidationException(
                    "AUTH_PASSWORD_LONGITUD_INVALIDA",
                    "La contraseña no puede superar 120 caracteres."
            );
        }

        return normalized;
    }

    private void validateMaxFailedAttempts(String usernameOrEmail) {
        Instant desde = Instant.now(clock)
                .minusSeconds(appProperties.getSecurity().getLockMinutes() * 60L);

        long failedAttempts = loginAttemptService.countRecentFailuresByUsernameOrEmail(
                usernameOrEmail,
                desde
        );

        if (failedAttempts >= appProperties.getSecurity().getMaxLoginAttempts()) {
            throw new UnauthorizedException(
                    "MAX_LOGIN_ATTEMPTS_EXCEEDED",
                    "Se superó el número máximo de intentos fallidos. Intenta nuevamente más tarde."
            );
        }
    }
}