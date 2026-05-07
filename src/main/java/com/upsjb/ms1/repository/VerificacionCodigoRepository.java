package com.upsjb.ms1.repository;

import com.upsjb.ms1.domain.entity.VerificacionCodigo;
import com.upsjb.ms1.domain.enums.EstadoVerificacionCodigo;
import com.upsjb.ms1.domain.enums.TipoCodigoVerificacion;
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
public interface VerificacionCodigoRepository extends JpaRepository<VerificacionCodigo, Long>, JpaSpecificationExecutor<VerificacionCodigo> {

    Optional<VerificacionCodigo> findByCodigoHash(String codigoHash);

    Optional<VerificacionCodigo> findFirstByEmail_ValueIgnoreCaseAndTipoCodigoAndEstadoOrderByExpiresAtDesc(
            String email,
            TipoCodigoVerificacion tipoCodigo,
            EstadoVerificacionCodigo estado
    );

    Optional<VerificacionCodigo> findFirstByUsuario_IdAndTipoCodigoAndEstadoOrderByExpiresAtDesc(
            Long idUsuario,
            TipoCodigoVerificacion tipoCodigo,
            EstadoVerificacionCodigo estado
    );

    List<VerificacionCodigo> findByUsuario_IdAndEstado(Long idUsuario, EstadoVerificacionCodigo estado);

    boolean existsByEmail_ValueIgnoreCaseAndTipoCodigoAndEstado(
            String email,
            TipoCodigoVerificacion tipoCodigo,
            EstadoVerificacionCodigo estado
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update VerificacionCodigo codigo
               set codigo.estado = :estadoExpirado
             where codigo.estado = :estadoPendiente
               and codigo.expiresAt <= :ahora
            """)
    int expirePendingCodes(
            @Param("estadoPendiente") EstadoVerificacionCodigo estadoPendiente,
            @Param("estadoExpirado") EstadoVerificacionCodigo estadoExpirado,
            @Param("ahora") Instant ahora
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update VerificacionCodigo codigo
               set codigo.estado = :estadoRevocado,
                   codigo.revokedAt = :fechaRevocacion
             where codigo.email.value = :email
               and codigo.tipoCodigo = :tipoCodigo
               and codigo.estado = :estadoPendiente
            """)
    int revokePendingCodesByEmailAndType(
            @Param("email") String email,
            @Param("tipoCodigo") TipoCodigoVerificacion tipoCodigo,
            @Param("estadoPendiente") EstadoVerificacionCodigo estadoPendiente,
            @Param("estadoRevocado") EstadoVerificacionCodigo estadoRevocado,
            @Param("fechaRevocacion") Instant fechaRevocacion
    );
}