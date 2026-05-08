package com.upsjb.ms1.security.principal;

import com.upsjb.ms1.domain.entity.Usuario;
import com.upsjb.ms1.repository.UsuarioRepository;
import com.upsjb.ms1.util.StringNormalizer;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    public CustomUserDetailsService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String usernameOrEmail) throws UsernameNotFoundException {
        String normalized = StringNormalizer.lower(usernameOrEmail);

        if (normalized == null) {
            throw new UsernameNotFoundException("Usuario no encontrado.");
        }

        Usuario usuario = usuarioRepository.findByUsernameOrEmailWithRol(normalized)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado."));

        return CustomUserDetails.fromUsuario(usuario);
    }
}