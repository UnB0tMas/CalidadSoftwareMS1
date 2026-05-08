package com.upsjb.ms1.specification;

import com.upsjb.ms1.domain.entity.UsuarioIpHistorial;
import com.upsjb.ms1.dto.auditoria.filter.UsuarioIpHistorialFilterDto;
import com.upsjb.ms1.util.StringNormalizer;
import jakarta.persistence.criteria.JoinType;
import java.time.Instant;
import org.springframework.data.jpa.domain.Specification;

public final class UsuarioIpHistorialSpecifications {

    private UsuarioIpHistorialSpecifications() {
    }

    public static Specification<UsuarioIpHistorial> from(UsuarioIpHistorialFilterDto filter) {
        Specification<UsuarioIpHistorial> specification = alwaysTrue();

        if (filter == null) {
            return specification;
        }

        return specification
                .and(usuario(filter.idUsuario()))
                .and(username(filter.username()))
                .and(ipAddress(filter.ipAddress()))
                .and(sospechosa(filter.sospechosa()))
                .and(bloqueada(filter.bloqueada()))
                .and(fechaDesde(filter.fechaDesde()))
                .and(fechaHasta(filter.fechaHasta()));
    }

    private static Specification<UsuarioIpHistorial> usuario(Long idUsuario) {
        return (root, query, cb) -> idUsuario == null
                ? cb.conjunction()
                : cb.equal(root.get("usuario").get("id"), idUsuario);
    }

    private static Specification<UsuarioIpHistorial> username(String value) {
        return (root, query, cb) -> {
            String normalized = likeValue(value);

            if (normalized == null) {
                return cb.conjunction();
            }

            return cb.like(
                    cb.lower(root.join("usuario", JoinType.LEFT).get("username").get("value")),
                    normalized
            );
        };
    }

    private static Specification<UsuarioIpHistorial> ipAddress(String value) {
        return (root, query, cb) -> {
            String normalized = likeValue(value);

            if (normalized == null) {
                return cb.conjunction();
            }

            return cb.like(cb.lower(root.get("ipAddress").get("value")), normalized);
        };
    }

    private static Specification<UsuarioIpHistorial> sospechosa(Boolean value) {
        return (root, query, cb) -> value == null
                ? cb.conjunction()
                : cb.equal(root.get("sospechosa"), value);
    }

    private static Specification<UsuarioIpHistorial> bloqueada(Boolean value) {
        return (root, query, cb) -> value == null
                ? cb.conjunction()
                : cb.equal(root.get("bloqueada"), value);
    }

    private static Specification<UsuarioIpHistorial> fechaDesde(Instant value) {
        return (root, query, cb) -> value == null
                ? cb.conjunction()
                : cb.greaterThanOrEqualTo(root.get("ultimoUsoAt"), value);
    }

    private static Specification<UsuarioIpHistorial> fechaHasta(Instant value) {
        return (root, query, cb) -> value == null
                ? cb.conjunction()
                : cb.lessThanOrEqualTo(root.get("ultimoUsoAt"), value);
    }

    private static Specification<UsuarioIpHistorial> alwaysTrue() {
        return (root, query, cb) -> cb.conjunction();
    }

    private static String likeValue(String value) {
        String normalized = StringNormalizer.lower(value);
        return normalized == null ? null : "%" + normalized + "%";
    }
}