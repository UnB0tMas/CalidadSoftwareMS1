package com.upsjb.ms1.repository;

import com.upsjb.ms1.domain.entity.UsuarioOAuth2Cuenta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface UsuarioOAuth2CuentaRepository extends JpaRepository<UsuarioOAuth2Cuenta, Long>, JpaSpecificationExecutor<UsuarioOAuth2Cuenta> {
}