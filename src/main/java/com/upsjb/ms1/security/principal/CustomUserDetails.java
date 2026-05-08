package com.upsjb.ms1.security.principal;

import com.upsjb.ms1.domain.entity.Rol;
import com.upsjb.ms1.domain.entity.Usuario;
import com.upsjb.ms1.domain.enums.EstadoRegistro;
import com.upsjb.ms1.security.roles.SecurityRoles;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public class CustomUserDetails implements UserDetails {

    private final Long idUsuario;
    private final String username;
    private final String email;
    private final String passwordHash;
    private final Long idRol;
    private final String codigoRol;
    private final String nombreRol;
    private final EstadoRegistro estado;
    private final boolean rolActivo;
    private final Collection<? extends GrantedAuthority> authorities;

    private CustomUserDetails(
            Long idUsuario,
            String username,
            String email,
            String passwordHash,
            Long idRol,
            String codigoRol,
            String nombreRol,
            EstadoRegistro estado,
            boolean rolActivo
    ) {
        this.idUsuario = idUsuario;
        this.username = username;
        this.email = email;
        this.passwordHash = passwordHash;
        this.idRol = idRol;
        this.codigoRol = SecurityRoles.normalizeRoleCode(codigoRol);
        this.nombreRol = nombreRol;
        this.estado = estado;
        this.rolActivo = rolActivo;

        String authority = SecurityRoles.toAuthority(codigoRol);
        this.authorities = authority == null
                ? List.of()
                : List.of(new SimpleGrantedAuthority(authority));
    }

    public static CustomUserDetails fromUsuario(Usuario usuario) {
        Rol rol = usuario.getRol();

        String username = usuario.getUsername() == null ? null : usuario.getUsername().getValue();
        String email = usuario.getEmail() == null ? null : usuario.getEmail().getValue();
        Long idRol = rol == null ? null : rol.getId();
        String codigoRol = rol == null ? null : rol.getCodigo();
        String nombreRol = rol == null ? null : rol.getNombre();
        boolean rolActivo = rol != null && rol.estaActivo();

        return new CustomUserDetails(
                usuario.getId(),
                username,
                email,
                usuario.getPasswordHash(),
                idRol,
                codigoRol,
                nombreRol,
                usuario.getEstado(),
                rolActivo
        );
    }

    public Long idUsuario() {
        return idUsuario;
    }

    public String email() {
        return email;
    }

    public Long idRol() {
        return idRol;
    }

    public String codigoRol() {
        return codigoRol;
    }

    public String nombreRol() {
        return nombreRol;
    }

    public EstadoRegistro estado() {
        return estado;
    }

    public List<String> authorityNames() {
        return authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .toList();
    }

    public AuthenticatedUserContext toAuthenticatedUserContext(
            Long sessionId,
            String ipAddress,
            String userAgent
    ) {
        return new AuthenticatedUserContext(
                idUsuario,
                username,
                email,
                idRol,
                codigoRol,
                nombreRol,
                Set.copyOf(authorityNames()),
                sessionId,
                ipAddress,
                userAgent
        );
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return !EstadoRegistro.ELIMINADO.equals(estado);
    }

    @Override
    public boolean isAccountNonLocked() {
        return !EstadoRegistro.BLOQUEADO.equals(estado);
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return EstadoRegistro.ACTIVO.equals(estado) && rolActivo;
    }
}
