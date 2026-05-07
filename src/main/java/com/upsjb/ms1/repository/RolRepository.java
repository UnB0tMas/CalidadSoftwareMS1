package com.upsjb.ms1.repository;

import com.upsjb.ms1.domain.entity.Rol;
import com.upsjb.ms1.domain.enums.EstadoRegistro;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface RolRepository extends JpaRepository<Rol, Long>, JpaSpecificationExecutor<Rol> {

    Optional<Rol> findByCodigoIgnoreCase(String codigo);

    Optional<Rol> findByNombreIgnoreCase(String nombre);

    boolean existsByCodigoIgnoreCase(String codigo);

    boolean existsByCodigoIgnoreCaseAndIdNot(String codigo, Long id);

    boolean existsByNombreIgnoreCase(String nombre);

    boolean existsByNombreIgnoreCaseAndIdNot(String nombre, Long id);

    List<Rol> findByEstadoOrderByNombreAsc(EstadoRegistro estado);

    long countByEstado(EstadoRegistro estado);
}