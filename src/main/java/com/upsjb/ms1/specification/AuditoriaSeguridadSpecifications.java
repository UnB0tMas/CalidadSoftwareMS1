package com.upsjb.ms1.specification;

import com.upsjb.ms1.domain.entity.AuditoriaSeguridad;
import com.upsjb.ms1.dto.auditoria.filter.AuditoriaSeguridadFilterDto;
import com.upsjb.ms1.shared.specification.SpecificationBuilder;
import com.upsjb.ms1.util.StringNormalizer;
import org.springframework.data.jpa.domain.Specification;

public final class AuditoriaSeguridadSpecifications {

    private AuditoriaSeguridadSpecifications() {
    }

    public static Specification<AuditoriaSeguridad> from(AuditoriaSeguridadFilterDto filter) {
        Specification<AuditoriaSeguridad> specification = SpecificationBuilder.alwaysTrue();

        if (filter == null) {
            return specification;
        }

        return specification
                .and(usuarioActor(filter.idUsuarioActor()))
                .and(usuarioAfectado(filter.idUsuarioAfectado()))
                .and(SpecificationBuilder.equalsIfPresent("tipo", filter.tipoEvento()))
                .and(username(filter.username()))
                .and(SpecificationBuilder.likeIgnoreCaseIfPresent("resultado", filter.resultado()))
                .and(ipAddress(filter.ipAddress()))
                .and(SpecificationBuilder.likeIgnoreCaseIfPresent("requestId", filter.requestId()))
                .and(SpecificationBuilder.likeIgnoreCaseIfPresent("path", filter.path()))
                .and(SpecificationBuilder.instantGreaterThanOrEqualIfPresent("eventAt", filter.fechaDesde()))
                .and(SpecificationBuilder.instantLessThanOrEqualIfPresent("eventAt", filter.fechaHasta()));
    }

    private static Specification<AuditoriaSeguridad> usuarioActor(Long idUsuarioActor) {
        return (root, query, cb) -> idUsuarioActor == null
                ? cb.conjunction()
                : cb.equal(root.get("usuarioActor").get("id"), idUsuarioActor);
    }

    private static Specification<AuditoriaSeguridad> usuarioAfectado(Long idUsuarioAfectado) {
        return (root, query, cb) -> idUsuarioAfectado == null
                ? cb.conjunction()
                : cb.equal(root.get("usuarioAfectado").get("id"), idUsuarioAfectado);
    }

    private static Specification<AuditoriaSeguridad> username(String value) {
        return (root, query, cb) -> {
            String normalized = likeValue(value);

            if (normalized == null) {
                return cb.conjunction();
            }

            return cb.or(
                    cb.like(cb.lower(root.get("usernameActor")), normalized),
                    cb.like(cb.lower(root.get("usernameAfectado")), normalized)
            );
        };
    }

    private static Specification<AuditoriaSeguridad> ipAddress(String value) {
        return (root, query, cb) -> {
            String normalized = likeValue(value);

            if (normalized == null) {
                return cb.conjunction();
            }

            return cb.like(cb.lower(root.get("ipAddress").get("value")), normalized);
        };
    }

    private static String likeValue(String value) {
        String normalized = StringNormalizer.lower(value);
        return normalized == null ? null : "%" + normalized + "%";
    }
}