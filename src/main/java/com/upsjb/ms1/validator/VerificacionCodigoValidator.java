// src/main/java/com/upsjb/ms1/validator/VerificacionCodigoValidator.java
package com.upsjb.ms1.validator;

import com.upsjb.ms1.domain.entity.VerificacionCodigo;
import com.upsjb.ms1.domain.enums.EstadoVerificacionCodigo;
import com.upsjb.ms1.domain.enums.TipoCodigoVerificacion;
import com.upsjb.ms1.domain.value.EmailValue;
import com.upsjb.ms1.repository.VerificacionCodigoRepository;
import com.upsjb.ms1.shared.exception.NotFoundException;
import com.upsjb.ms1.shared.exception.ValidationException;
import com.upsjb.ms1.util.StringNormalizer;
import com.upsjb.ms1.util.TokenHashUtil;
import java.time.Clock;
import java.time.Instant;
import org.springframework.stereotype.Component;

@Component
public class VerificacionCodigoValidator {

    private final VerificacionCodigoRepository verificacionCodigoRepository;
    private final Clock clock;

    public VerificacionCodigoValidator(
            VerificacionCodigoRepository verificacionCodigoRepository,
            Clock clock
    ) {
        this.verificacionCodigoRepository = verificacionCodigoRepository;
        this.clock = clock;
    }

    public VerificacionCodigo requireCodigoPendienteByEmail(
            String email,
            TipoCodigoVerificacion tipoCodigo
    ) {
        String emailNormalizado = normalizeEmail(email);
        TipoCodigoVerificacion tipoNormalizado = requireTipoCodigo(tipoCodigo);

        return verificacionCodigoRepository
                .findFirstByEmail_ValueIgnoreCaseAndTipoCodigoAndEstadoOrderByExpiresAtDesc(
                        emailNormalizado,
                        tipoNormalizado,
                        EstadoVerificacionCodigo.PENDIENTE
                )
                .orElseThrow(() -> new NotFoundException(
                        "CODIGO_VERIFICACION_NO_ENCONTRADO",
                        "No se encontró un código de verificación pendiente para el correo indicado."
                ));
    }

    public VerificacionCodigo requireCodigoPendienteByUsuario(
            Long idUsuario,
            TipoCodigoVerificacion tipoCodigo
    ) {
        if (idUsuario == null) {
            throw new ValidationException(
                    "CODIGO_USUARIO_OBLIGATORIO",
                    "El usuario es obligatorio."
            );
        }

        TipoCodigoVerificacion tipoNormalizado = requireTipoCodigo(tipoCodigo);

        return verificacionCodigoRepository
                .findFirstByUsuario_IdAndTipoCodigoAndEstadoOrderByExpiresAtDesc(
                        idUsuario,
                        tipoNormalizado,
                        EstadoVerificacionCodigo.PENDIENTE
                )
                .orElseThrow(() -> new NotFoundException(
                        "CODIGO_VERIFICACION_NO_ENCONTRADO",
                        "No se encontró un código de verificación pendiente para el usuario indicado."
                ));
    }

    public VerificacionCodigo validateCodigoByEmail(
            String email,
            TipoCodigoVerificacion tipoCodigo,
            String codigoPlano
    ) {
        VerificacionCodigo codigo = requireCodigoPendienteByEmail(email, tipoCodigo);
        validateCodigoUsable(codigo);
        validateCodigoMatches(codigo, codigoPlano);
        return codigo;
    }

    public VerificacionCodigo validateCodigoByUsuario(
            Long idUsuario,
            TipoCodigoVerificacion tipoCodigo,
            String codigoPlano
    ) {
        VerificacionCodigo codigo = requireCodigoPendienteByUsuario(idUsuario, tipoCodigo);
        validateCodigoUsable(codigo);
        validateCodigoMatches(codigo, codigoPlano);
        return codigo;
    }

    public void validateCodigoUsable(VerificacionCodigo codigo) {
        if (codigo == null) {
            throw new NotFoundException(
                    "CODIGO_VERIFICACION_NO_ENCONTRADO",
                    "No se encontró el código de verificación."
            );
        }

        Instant ahora = Instant.now(clock);

        if (codigo.estaValidado()) {
            throw new ValidationException(
                    "CODIGO_VERIFICACION_USADO",
                    "El código de verificación ya fue utilizado."
            );
        }

        if (codigo.estaRevocado()) {
            throw new ValidationException(
                    "CODIGO_VERIFICACION_REVOCADO",
                    "El código de verificación fue revocado."
            );
        }

        if (codigo.estaBloqueado()) {
            throw new ValidationException(
                    "CODIGO_VERIFICACION_BLOQUEADO",
                    "El código de verificación fue bloqueado por intentos fallidos."
            );
        }

        if (codigo.estaExpirado(ahora)) {
            if (codigo.estaPendiente()) {
                codigo.marcarExpirado();
                verificacionCodigoRepository.save(codigo);
            }

            throw new ValidationException(
                    "CODIGO_VERIFICACION_EXPIRADO",
                    "El código de verificación expiró."
            );
        }

        if (!codigo.estaPendiente()) {
            throw new ValidationException(
                    "CODIGO_VERIFICACION_INVALIDO",
                    "El código de verificación no se encuentra pendiente."
            );
        }

        if (!codigo.tieneIntentosDisponibles()) {
            codigo.setEstado(EstadoVerificacionCodigo.BLOQUEADO);
            verificacionCodigoRepository.save(codigo);

            throw new ValidationException(
                    "CODIGO_VERIFICACION_INTENTOS_EXCEDIDOS",
                    "Se superó el número máximo de intentos para este código."
            );
        }
    }

    public void validateCodigoMatches(VerificacionCodigo codigo, String codigoPlano) {
        String normalized = StringNormalizer.trimToNull(codigoPlano);

        if (normalized == null) {
            throw new ValidationException(
                    "CODIGO_VERIFICACION_OBLIGATORIO",
                    "El código de verificación es obligatorio."
            );
        }

        if (normalized.length() < 4 || normalized.length() > 12) {
            throw new ValidationException(
                    "CODIGO_VERIFICACION_LONGITUD_INVALIDA",
                    "El código de verificación debe tener entre 4 y 12 caracteres."
            );
        }

        if (!TokenHashUtil.matches(normalized, codigo.getCodigoHash())) {
            codigo.registrarIntentoFallido();
            verificacionCodigoRepository.save(codigo);

            if (codigo.estaBloqueado()) {
                throw new ValidationException(
                        "CODIGO_VERIFICACION_INTENTOS_EXCEDIDOS",
                        "Se superó el número máximo de intentos para este código."
                );
            }

            throw new ValidationException(
                    "CODIGO_VERIFICACION_INVALIDO",
                    "El código de verificación no es válido."
            );
        }
    }

    private String normalizeEmail(String email) {
        try {
            return EmailValue.of(email).getValue();
        } catch (IllegalArgumentException exception) {
            throw new ValidationException(
                    "CODIGO_EMAIL_INVALIDO",
                    exception.getMessage()
            );
        }
    }

    private TipoCodigoVerificacion requireTipoCodigo(TipoCodigoVerificacion tipoCodigo) {
        if (tipoCodigo == null) {
            throw new ValidationException(
                    "CODIGO_TIPO_OBLIGATORIO",
                    "El tipo de código es obligatorio."
            );
        }

        return tipoCodigo;
    }
}