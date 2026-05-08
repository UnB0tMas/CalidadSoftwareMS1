package com.upsjb.ms1.bootstrap;

import com.upsjb.ms1.domain.entity.Rol;
import com.upsjb.ms1.domain.enums.EstadoRegistro;
import com.upsjb.ms1.repository.RolRepository;
import com.upsjb.ms1.security.roles.SecurityRoles;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class SystemRolesBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SystemRolesBootstrap.class);

    private final RolRepository rolRepository;

    public SystemRolesBootstrap(RolRepository rolRepository) {
        this.rolRepository = rolRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        for (SystemRoleDefinition role : systemRoles()) {
            ensureRole(role);
        }
    }

    private void ensureRole(SystemRoleDefinition definition) {
        Rol rol = rolRepository.findByCodigoIgnoreCase(definition.codigo()).orElse(null);

        if (rol == null) {
            Rol created = Rol.builder()
                    .codigo(definition.codigo())
                    .nombre(definition.nombre())
                    .descripcion(definition.descripcion())
                    .estado(EstadoRegistro.ACTIVO)
                    .rolSistema(true)
                    .build();

            rolRepository.save(created);
            log.info("Rol de sistema creado: {}", definition.codigo());
            return;
        }

        boolean changed = false;

        if (!definition.codigo().equals(rol.getCodigo())) {
            rol.setCodigo(definition.codigo());
            changed = true;
        }

        if (!definition.nombre().equals(rol.getNombre())) {
            rol.setNombre(definition.nombre());
            changed = true;
        }

        if (!definition.descripcion().equals(rol.getDescripcion())) {
            rol.setDescripcion(definition.descripcion());
            changed = true;
        }

        if (!rol.isRolSistema()) {
            rol.setRolSistema(true);
            changed = true;
        }

        if (!EstadoRegistro.ACTIVO.equals(rol.getEstado())) {
            rol.activar();
            changed = true;
        }

        if (changed) {
            rolRepository.save(rol);
            log.info("Rol de sistema verificado y actualizado: {}", definition.codigo());
        } else {
            log.debug("Rol de sistema verificado sin cambios: {}", definition.codigo());
        }
    }

    private List<SystemRoleDefinition> systemRoles() {
        return List.of(
                new SystemRoleDefinition(
                        SecurityRoles.ADMIN,
                        "Administrador",
                        "Rol de sistema con acceso administrativo."
                ),
                new SystemRoleDefinition(
                        SecurityRoles.EMPLEADO,
                        "Empleado",
                        "Rol de sistema para usuarios internos creados por un administrador."
                ),
                new SystemRoleDefinition(
                        SecurityRoles.CLIENTE,
                        "Cliente",
                        "Rol de sistema para clientes registrados en la aplicación."
                )
        );
    }

    private record SystemRoleDefinition(
            String codigo,
            String nombre,
            String descripcion
    ) {
    }
}