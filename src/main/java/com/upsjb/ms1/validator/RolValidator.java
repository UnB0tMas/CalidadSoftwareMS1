package com.upsjb.ms1.validator;

import com.upsjb.ms1.domain.entity.Rol;
import com.upsjb.ms1.domain.enums.EstadoRegistro;
import com.upsjb.ms1.repository.RolRepository;
import com.upsjb.ms1.repository.UsuarioRepository;
import com.upsjb.ms1.security.roles.SecurityRoles;
import com.upsjb.ms1.shared.exception.ConflictException;
import com.upsjb.ms1.shared.exception.NotFoundException;
import com.upsjb.ms1.shared.exception.ValidationException;
import com.upsjb.ms1.util.StringNormalizer;
import org.springframework.stereotype.Component;

@Component
public class RolValidator {

    private final RolRepository rolRepository;
    private final UsuarioRepository usuarioRepository;

    public RolValidator(
            RolRepository rolRepository,
            UsuarioRepository usuarioRepository
    ) {
        this.rolRepository = rolRepository;
        this.usuarioRepository = usuarioRepository;
    }

    public Rol requireById(Long idRol) {
        if (idRol == null) {
            throw new ValidationException(
                    "ROL_ID_OBLIGATORIO",
                    "El rol es obligatorio."
            );
        }

        return rolRepository.findById(idRol)
                .orElseThrow(() -> new NotFoundException(
                        "ROL_NO_ENCONTRADO",
                        "No se encontró el rol solicitado."
                ));
    }

    public Rol requireActivoById(Long idRol) {
        Rol rol = requireById(idRol);
        validateActivo(rol);
        return rol;
    }

    public void validateCreate(String codigo, String nombre) {
        String codigoNormalizado = normalizeCodigo(codigo);
        validateSystemRoleCode(codigoNormalizado);

        String nombreNormalizado = normalizeNombre(nombre);

        if (rolRepository.existsByCodigoIgnoreCase(codigoNormalizado)) {
            throw new ConflictException(
                    "ROL_CODIGO_DUPLICADO",
                    "El código del rol ya se encuentra registrado."
            );
        }

        if (rolRepository.existsByNombreIgnoreCase(nombreNormalizado)) {
            throw new ConflictException(
                    "ROL_NOMBRE_DUPLICADO",
                    "El nombre del rol ya se encuentra registrado."
            );
        }
    }

    public void validateUpdate(Long idRol, String nombre) {
        Rol rol = requireById(idRol);
        validateEditable(rol);

        String nombreNormalizado = StringNormalizer.normalizeSpaces(nombre);

        if (nombreNormalizado != null
                && rolRepository.existsByNombreIgnoreCaseAndIdNot(nombreNormalizado, idRol)) {
            throw new ConflictException(
                    "ROL_NOMBRE_DUPLICADO",
                    "El nombre del rol ya se encuentra registrado."
            );
        }
    }

    public void validateActivo(Rol rol) {
        if (rol == null) {
            throw new NotFoundException(
                    "ROL_NO_ENCONTRADO",
                    "No se encontró el rol solicitado."
            );
        }

        if (!rol.estaActivo()) {
            throw new ValidationException(
                    "ROL_INACTIVO",
                    "El rol no está activo."
            );
        }
    }

    public void validateEditable(Rol rol) {
        if (rol == null) {
            throw new NotFoundException(
                    "ROL_NO_ENCONTRADO",
                    "No se encontró el rol solicitado."
            );
        }

        if (rol.isRolSistema()) {
            throw new ValidationException(
                    "ROL_NO_EDITABLE",
                    "El rol del sistema no puede modificarse."
            );
        }

        if (rol.estaEliminado()) {
            throw new ValidationException(
                    "ROL_ELIMINADO",
                    "El rol eliminado no puede modificarse."
            );
        }
    }

    public void validatePuedeInactivar(Rol rol) {
        validateEditable(rol);

        long usuariosActivos = usuarioRepository.countByRol_IdAndEstado(rol.getId(), EstadoRegistro.ACTIVO);

        if (usuariosActivos > 0) {
            throw new ConflictException(
                    "ROL_CON_USUARIOS_ACTIVOS",
                    "No se puede inactivar el rol porque tiene usuarios activos asociados."
            );
        }
    }

    private void validateSystemRoleCode(String codigoNormalizado) {
        String normalized = SecurityRoles.normalizeRoleCode(codigoNormalizado);

        if (!SecurityRoles.SYSTEM_ROLE_CODES.contains(normalized)) {
            throw new ValidationException(
                    "ROL_CODIGO_NO_PERMITIDO",
                    "Solo se permiten los roles ADMIN, EMPLEADO y CLIENTE."
            );
        }
    }

    private String normalizeCodigo(String codigo) {
        String normalized = StringNormalizer.upper(codigo);

        if (normalized == null) {
            throw new ValidationException(
                    "ROL_CODIGO_OBLIGATORIO",
                    "El código del rol es obligatorio."
            );
        }

        if (normalized.length() < 2 || normalized.length() > 40) {
            throw new ValidationException(
                    "ROL_CODIGO_LONGITUD_INVALIDA",
                    "El código del rol debe tener entre 2 y 40 caracteres."
            );
        }

        return normalized;
    }

    private String normalizeNombre(String nombre) {
        String normalized = StringNormalizer.normalizeSpaces(nombre);

        if (normalized == null) {
            throw new ValidationException(
                    "ROL_NOMBRE_OBLIGATORIO",
                    "El nombre del rol es obligatorio."
            );
        }

        if (normalized.length() < 2 || normalized.length() > 80) {
            throw new ValidationException(
                    "ROL_NOMBRE_LONGITUD_INVALIDA",
                    "El nombre del rol debe tener entre 2 y 80 caracteres."
            );
        }

        return normalized;
    }
}