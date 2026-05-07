package com.upsjb.ms1.repository;

import com.upsjb.ms1.domain.entity.PasswordResetToken;
import com.upsjb.ms1.domain.enums.EstadoVerificacionCodigo;
import com.upsjb.ms1.domain.enums.TipoToken;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long>, JpaSpecificationExecutor<PasswordResetToken> {

    Optional<PasswordResetToken> findByTokenHash(String tokenHash);

    Optional<PasswordResetToken> findFirstByUsuario_IdAndTipoTokenAndEstadoOrderByExpiresAtDesc(
            Long idUsuario,
            TipoToken tipoToken,
            EstadoVerificacionCodigo estado
    );

    List<PasswordResetToken> findByUsuario_IdAndEstado(Long idUsuario, EstadoVerificacionCodigo estado);

    boolean existsByTokenHashAndEstado(String tokenHash, EstadoVerificacionCodigo estado);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update PasswordResetToken token
               set token.estado = :estadoExpirado
             where token.estado = :estadoPendiente
               and token.expiresAt <= :ahora
            """)
    int expirePendingTokens(
            @Param("estadoPendiente") EstadoVerificacionCodigo estadoPendiente,
            @Param("estadoExpirado") EstadoVerificacionCodigo estadoExpirado,
            @Param("ahora") Instant ahora
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update PasswordResetToken token
               set token.estado = :estadoRevocado,
                   token.revokedAt = :fechaRevocacion
             where token.usuario.id = :idUsuario
               and token.tipoToken = :tipoToken
               and token.estado = :estadoPendiente
            """)
    int revokePendingTokensByUsuarioAndType(
            @Param("idUsuario") Long idUsuario,
            @Param("tipoToken") TipoToken tipoToken,
            @Param("estadoPendiente") EstadoVerificacionCodigo estadoPendiente,
            @Param("estadoRevocado") EstadoVerificacionCodigo estadoRevocado,
            @Param("fechaRevocacion") Instant fechaRevocacion
    );
}