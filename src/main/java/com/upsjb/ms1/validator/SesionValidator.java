package com.upsjb.ms1.validator;

import com.upsjb.ms1.domain.entity.UsuarioSesion;
import com.upsjb.ms1.domain.enums.EstadoSesion;
import com.upsjb.ms1.repository.UsuarioSesionRepository;
import com.upsjb.ms1.shared.exception.NotFoundException;
import com.upsjb.ms1.shared.exception.UnauthorizedException;
import com.upsjb.ms1.shared.exception.ValidationException;
import com.upsjb.ms1.util.StringNormalizer;
import com.upsjb.ms1.util.TokenHashUtil;
import java.time.Clock;
import java.time.Instant;
import org.springframework.stereotype.Component;

@Component
public class SesionValidator {

    private final UsuarioSesionRepository usuarioSesionRepository;
    private final Clock clock;

    public SesionValidator(
            UsuarioSesionRepository usuarioSesionRepository,
            Clock clock
    ) {
        this.usuarioSesionRepository = usuarioSesionRepository;
        this.clock = clock;
    }

    public UsuarioSesion requireById(Long idSesion) {
        if (idSesion == null) {
            throw new ValidationException(
                    "SESION_ID_OBLIGATORIO",
                    "La sesión es obligatoria."
            );
        }

        return usuarioSesionRepository.findById(idSesion)
                .orElseThrow(() -> new NotFoundException(
                        "SESION_NO_ENCONTRADA",
                        "No se encontró la sesión solicitada."
                ));
    }

    public UsuarioSesion requireByIdAndUsuario(Long idSesion, Long idUsuario) {
        if (idSesion == null) {
            throw new ValidationException(
                    "SESION_ID_OBLIGATORIO",
                    "La sesión es obligatoria."
            );
        }

        if (idUsuario == null) {
            throw new ValidationException(
                    "USUARIO_ID_OBLIGATORIO",
                    "El usuario es obligatorio."
            );
        }

        return usuarioSesionRepository.findByIdAndUsuario_Id(idSesion, idUsuario)
                .orElseThrow(() -> new NotFoundException(
                        "SESION_NO_ENCONTRADA",
                        "No se encontró la sesión solicitada para el usuario."
                ));
    }

    public UsuarioSesion requireByRefreshToken(String refreshToken) {
        String tokenHash = hashRefreshToken(refreshToken);

        UsuarioSesion sesion = usuarioSesionRepository.findByRefreshTokenHash(tokenHash)
                .orElseThrow(() -> new UnauthorizedException(
                        "REFRESH_TOKEN_INVALIDO",
                        "El refresh token no es válido."
                ));

        validateRefreshTokenUsable(sesion);

        return sesion;
    }

    public void validateActiva(UsuarioSesion sesion) {
        if (sesion == null) {
            throw new NotFoundException(
                    "SESION_NO_ENCONTRADA",
                    "No se encontró la sesión solicitada."
            );
        }

        if (sesion.estaRevocada()) {
            throw new UnauthorizedException(
                    "SESION_REVOCADA",
                    "La sesión fue revocada."
            );
        }

        if (sesion.estaCerrada()) {
            throw new UnauthorizedException(
                    "SESION_CERRADA",
                    "La sesión fue cerrada."
            );
        }

        if (sesion.estaExpirada()) {
            throw new UnauthorizedException(
                    "REFRESH_TOKEN_EXPIRADO",
                    "La sesión se encuentra expirada."
            );
        }

        if (!EstadoSesion.ACTIVA.equals(sesion.getEstado())) {
            throw new UnauthorizedException(
                    "SESION_NO_ACTIVA",
                    "La sesión no se encuentra activa."
            );
        }
    }

    public void validateVigente(UsuarioSesion sesion) {
        validateActiva(sesion);

        Instant ahora = Instant.now(clock);

        if (!sesion.estaVigente(ahora)) {
            throw new UnauthorizedException(
                    "REFRESH_TOKEN_EXPIRADO",
                    "El refresh token se encuentra expirado."
            );
        }
    }

    public void validatePerteneceAUsuario(UsuarioSesion sesion, Long idUsuario) {
        if (sesion == null || sesion.getUsuario() == null || sesion.getUsuario().getId() == null) {
            throw new ValidationException(
                    "SESION_USUARIO_INVALIDO",
                    "La sesión no tiene un usuario válido asociado."
            );
        }

        if (idUsuario == null || !sesion.getUsuario().getId().equals(idUsuario)) {
            throw new UnauthorizedException(
                    "SESION_NO_PERTENECE_USUARIO",
                    "La sesión no pertenece al usuario indicado."
            );
        }
    }

    public void validateRefreshTokenUsable(UsuarioSesion sesion) {
        validateVigente(sesion);
    }

    public String hashRefreshToken(String refreshToken) {
        String normalized = StringNormalizer.trimToNull(refreshToken);

        if (normalized == null) {
            throw new ValidationException(
                    "REFRESH_TOKEN_OBLIGATORIO",
                    "El refresh token es obligatorio."
            );
        }

        if (normalized.length() > 4096) {
            throw new ValidationException(
                    "REFRESH_TOKEN_LONGITUD_INVALIDA",
                    "El refresh token no puede superar 4096 caracteres."
            );
        }

        return TokenHashUtil.hash(normalized);
    }
}