package com.upsjb.ms1.repository;

import com.upsjb.ms1.domain.entity.UsuarioSesion;
import com.upsjb.ms1.domain.enums.EstadoSesion;
import com.upsjb.ms1.domain.enums.MotivoRevocacionSesion;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UsuarioSesionRepository extends JpaRepository<UsuarioSesion, Long>, JpaSpecificationExecutor<UsuarioSesion> {

    Optional<UsuarioSesion> findByRefreshTokenHash(String refreshTokenHash);

    Optional<UsuarioSesion> findByIdAndUsuario_Id(Long idSesion, Long idUsuario);

    List<UsuarioSesion> findByUsuario_IdAndEstado(Long idUsuario, EstadoSesion estado);

    Page<UsuarioSesion> findByUsuario_Id(Long idUsuario, Pageable pageable);

    boolean existsByRefreshTokenHashAndEstado(String refreshTokenHash, EstadoSesion estado);

    long countByUsuario_IdAndEstado(Long idUsuario, EstadoSesion estado);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update UsuarioSesion sesion
               set sesion.estado = :estadoExpirada,
                   sesion.revokedAt = :ahora,
                   sesion.motivoRevocacion = :motivo
             where sesion.estado = :estadoActiva
               and sesion.expiresAt <= :ahora
            """)
    int expireSessions(
            @Param("estadoActiva") EstadoSesion estadoActiva,
            @Param("estadoExpirada") EstadoSesion estadoExpirada,
            @Param("motivo") MotivoRevocacionSesion motivo,
            @Param("ahora") Instant ahora
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update UsuarioSesion sesion
               set sesion.estado = :estadoRevocada,
                   sesion.revokedAt = :fechaRevocacion,
                   sesion.motivoRevocacion = :motivo,
                   sesion.revocationReasonDetail = :detalle
             where sesion.usuario.id = :idUsuario
               and sesion.estado = :estadoActiva
            """)
    int revokeActiveSessionsByUsuario(
            @Param("idUsuario") Long idUsuario,
            @Param("estadoActiva") EstadoSesion estadoActiva,
            @Param("estadoRevocada") EstadoSesion estadoRevocada,
            @Param("motivo") MotivoRevocacionSesion motivo,
            @Param("detalle") String detalle,
            @Param("fechaRevocacion") Instant fechaRevocacion
    );
}