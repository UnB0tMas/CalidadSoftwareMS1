package com.upsjb.ms1.repository;

import com.upsjb.ms1.domain.entity.UsuarioIpHistorial;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface UsuarioIpHistorialRepository extends JpaRepository<UsuarioIpHistorial, Long>, JpaSpecificationExecutor<UsuarioIpHistorial> {

    Optional<UsuarioIpHistorial> findByUsuario_IdAndIpAddress_Value(Long idUsuario, String ipAddress);

    boolean existsByUsuario_IdAndIpAddress_Value(Long idUsuario, String ipAddress);

    List<UsuarioIpHistorial> findTop10ByUsuario_IdOrderByUltimoUsoAtDesc(Long idUsuario);

    Page<UsuarioIpHistorial> findByUsuario_Id(Long idUsuario, Pageable pageable);

    long countByUsuario_IdAndSospechosaTrue(Long idUsuario);

    long countByBloqueadaTrue();
}