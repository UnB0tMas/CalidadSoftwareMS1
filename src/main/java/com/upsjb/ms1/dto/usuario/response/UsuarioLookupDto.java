package com.upsjb.ms1.dto.usuario.response;

import java.io.Serializable;

public record UsuarioLookupDto(
        Long idUsuario,
        String username,
        String email,
        String nombreCompleto
) implements Serializable {
}