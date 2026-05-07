package com.upsjb.ms1.repository;

import com.upsjb.ms1.domain.entity.Usuario;
import com.upsjb.ms1.domain.enums.EstadoRegistro;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long>, JpaSpecificationExecutor<Usuario> {

    @EntityGraph(attributePaths = "rol")
    Optional<Usuario> findWithRolById(Long id);

    Optional<Usuario> findByUsername_ValueIgnoreCase(String username);

    Optional<Usuario> findByEmail_ValueIgnoreCase(String email);

    @EntityGraph(attributePaths = "rol")
    @Query("""
            select usuario
              from Usuario usuario
              join fetch usuario.rol rol
             where lower(usuario.username.value) = lower(:usernameOrEmail)
                or lower(usuario.email.value) = lower(:usernameOrEmail)
            """)
    Optional<Usuario> findByUsernameOrEmailWithRol(@Param("usernameOrEmail") String usernameOrEmail);

    boolean existsByUsername_ValueIgnoreCase(String username);

    boolean existsByUsername_ValueIgnoreCaseAndIdNot(String username, Long id);

    boolean existsByEmail_ValueIgnoreCase(String email);

    boolean existsByEmail_ValueIgnoreCaseAndIdNot(String email, Long id);

    List<Usuario> findByRol_IdAndEstado(Long idRol, EstadoRegistro estado);

    long countByRol_IdAndEstado(Long idRol, EstadoRegistro estado);

    long countByEstado(EstadoRegistro estado);
}