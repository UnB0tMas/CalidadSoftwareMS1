package com.upsjb.ms1.repository;

import com.upsjb.ms1.domain.entity.AuditoriaSeguridad;
import com.upsjb.ms1.domain.enums.TipoAuditoriaSeguridad;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface AuditoriaSeguridadRepository extends JpaRepository<AuditoriaSeguridad, Long>, JpaSpecificationExecutor<AuditoriaSeguridad> {

    Page<AuditoriaSeguridad> findByUsuarioActor_Id(Long idUsuarioActor, Pageable pageable);

    Page<AuditoriaSeguridad> findByUsuarioAfectado_Id(Long idUsuarioAfectado, Pageable pageable);

    Page<AuditoriaSeguridad> findByTipo(TipoAuditoriaSeguridad tipo, Pageable pageable);

    List<AuditoriaSeguridad> findTop20ByUsuarioActor_IdOrderByEventAtDesc(Long idUsuarioActor);

    List<AuditoriaSeguridad> findTop20ByRequestIdOrderByEventAtDesc(String requestId);

    long countByTipoAndEventAtBetween(
            TipoAuditoriaSeguridad tipo,
            Instant fechaDesde,
            Instant fechaHasta
    );
}