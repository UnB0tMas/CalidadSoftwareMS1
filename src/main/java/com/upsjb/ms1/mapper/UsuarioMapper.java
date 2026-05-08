package com.upsjb.ms1.mapper;

import com.upsjb.ms1.domain.entity.Rol;
import com.upsjb.ms1.domain.entity.Usuario;
import com.upsjb.ms1.domain.enums.EstadoRegistro;
import com.upsjb.ms1.domain.value.EmailValue;
import com.upsjb.ms1.domain.value.UsernameValue;
import com.upsjb.ms1.dto.auth.response.AuthUserResponseDto;
import com.upsjb.ms1.dto.usuario.request.UsuarioCreateRequestDto;
import com.upsjb.ms1.dto.usuario.request.UsuarioUpdateRequestDto;
import com.upsjb.ms1.dto.usuario.response.UsuarioDetailResponseDto;
import com.upsjb.ms1.dto.usuario.response.UsuarioLookupDto;
import com.upsjb.ms1.dto.usuario.response.UsuarioResponseDto;
import com.upsjb.ms1.util.StringNormalizer;
import org.springframework.stereotype.Component;

@Component
public class UsuarioMapper {

    public UsuarioResponseDto toResponse(Usuario usuario) {
        if (usuario == null) {
            return null;
        }

        Rol rol = usuario.getRol();

        return new UsuarioResponseDto(
                usuario.getId(),
                valueOfUsername(usuario),
                valueOfEmail(usuario),
                usuario.getNombres(),
                usuario.getApellidos(),
                rol == null ? null : rol.getId(),
                rol == null ? null : rol.getCodigo(),
                rol == null ? null : rol.getNombre(),
                usuario.getEstado(),
                usuario.isEmailVerificado(),
                usuario.isRequiereCambioPassword(),
                usuario.getCreatedAt(),
                usuario.getUpdatedAt()
        );
    }

    public UsuarioDetailResponseDto toDetail(Usuario usuario) {
        if (usuario == null) {
            return null;
        }

        Rol rol = usuario.getRol();

        return new UsuarioDetailResponseDto(
                usuario.getId(),
                valueOfUsername(usuario),
                valueOfEmail(usuario),
                usuario.getNombres(),
                usuario.getApellidos(),
                rol == null ? null : rol.getId(),
                rol == null ? null : rol.getCodigo(),
                rol == null ? null : rol.getNombre(),
                usuario.getEstado(),
                usuario.isEmailVerificado(),
                usuario.isRequiereCambioPassword(),
                usuario.getBloqueadoHasta(),
                usuario.getUltimoLoginAt(),
                usuario.getUltimoCambioPasswordAt(),
                usuario.getCreatedAt(),
                usuario.getUpdatedAt()
        );
    }

    public UsuarioLookupDto toLookup(Usuario usuario) {
        if (usuario == null) {
            return null;
        }

        return new UsuarioLookupDto(
                usuario.getId(),
                valueOfUsername(usuario),
                valueOfEmail(usuario),
                nombreCompleto(usuario)
        );
    }

    public AuthUserResponseDto toAuthUserResponse(Usuario usuario) {
        if (usuario == null) {
            return null;
        }

        Rol rol = usuario.getRol();

        return new AuthUserResponseDto(
                usuario.getId(),
                valueOfUsername(usuario),
                valueOfEmail(usuario),
                usuario.getNombres(),
                usuario.getApellidos(),
                rol == null ? null : rol.getId(),
                rol == null ? null : rol.getCodigo(),
                rol == null ? null : rol.getNombre(),
                usuario.getEstado(),
                usuario.isEmailVerificado(),
                usuario.isRequiereCambioPassword()
        );
    }

    public Usuario toEntity(UsuarioCreateRequestDto request, Rol rol, String passwordHash) {
        if (request == null) {
            return null;
        }

        return Usuario.builder()
                .rol(rol)
                .username(UsernameValue.of(request.username()))
                .email(EmailValue.of(request.email()))
                .passwordHash(passwordHash)
                .nombres(StringNormalizer.normalizeSpaces(request.nombres()))
                .apellidos(StringNormalizer.normalizeSpaces(request.apellidos()))
                .estado(EstadoRegistro.ACTIVO)
                .emailVerificado(Boolean.TRUE.equals(request.emailVerificado()))
                .requiereCambioPassword(Boolean.TRUE.equals(request.requiereCambioPassword()))
                .build();
    }

    public void applyUpdate(Usuario usuario, UsuarioUpdateRequestDto request) {
        if (usuario == null || request == null) {
            return;
        }

        if (request.email() != null) {
            usuario.setEmail(EmailValue.of(request.email()));
        }

        if (request.nombres() != null) {
            usuario.setNombres(StringNormalizer.normalizeSpaces(request.nombres()));
        }

        if (request.apellidos() != null) {
            usuario.setApellidos(StringNormalizer.normalizeSpaces(request.apellidos()));
        }
    }

    private String valueOfUsername(Usuario usuario) {
        return usuario.getUsername() == null ? null : usuario.getUsername().getValue();
    }

    private String valueOfEmail(Usuario usuario) {
        return usuario.getEmail() == null ? null : usuario.getEmail().getValue();
    }

    private String nombreCompleto(Usuario usuario) {
        String nombres = StringNormalizer.normalizeSpaces(usuario.getNombres());
        String apellidos = StringNormalizer.normalizeSpaces(usuario.getApellidos());

        if (nombres == null && apellidos == null) {
            return null;
        }

        if (nombres == null) {
            return apellidos;
        }

        if (apellidos == null) {
            return nombres;
        }

        return nombres + " " + apellidos;
    }
}