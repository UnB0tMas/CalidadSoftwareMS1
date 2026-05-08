package com.upsjb.ms1.repository;

import com.upsjb.ms1.domain.entity.UsuarioOAuth2Cuenta;
import com.upsjb.ms1.domain.enums.ProveedorOAuth2;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface UsuarioOAuth2CuentaRepository extends JpaRepository<UsuarioOAuth2Cuenta, Long>, JpaSpecificationExecutor<UsuarioOAuth2Cuenta> {

    @EntityGraph(attributePaths = {"usuario", "usuario.rol"})
    Optional<UsuarioOAuth2Cuenta> findByProveedorAndProviderUserId(
            ProveedorOAuth2 proveedor,
            String providerUserId
    );

    @EntityGraph(attributePaths = {"usuario", "usuario.rol"})
    Optional<UsuarioOAuth2Cuenta> findByProveedorAndUsuario_Id(
            ProveedorOAuth2 proveedor,
            Long idUsuario
    );

    boolean existsByProveedorAndProviderUserId(
            ProveedorOAuth2 proveedor,
            String providerUserId
    );
}