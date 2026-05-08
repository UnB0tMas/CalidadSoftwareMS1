package com.upsjb.ms1.specification;

import com.upsjb.ms1.domain.entity.Rol;
import com.upsjb.ms1.dto.rol.filter.RolFilterDto;
import com.upsjb.ms1.util.StringNormalizer;
import org.springframework.data.jpa.domain.Specification;

public final class RolSpecifications {

    private RolSpecifications() {
    }

    public static Specification<Rol> from(RolFilterDto filter) {
        Specification<Rol> specification = alwaysTrue();

        if (filter == null) {
            return specification;
        }

        specification = specification
                .and(search(filter.search()))
                .and(codigo(filter.codigo()))
                .and(nombre(filter.nombre()))
                .and((root, query, cb) -> filter.estado() == null
                        ? cb.conjunction()
                        : cb.equal(root.get("estado"), filter.estado()))
                .and((root, query, cb) -> filter.rolSistema() == null
                        ? cb.conjunction()
                        : cb.equal(root.get("rolSistema"), filter.rolSistema()));

        return specification;
    }

    private static Specification<Rol> search(String value) {
        return (root, query, cb) -> {
            String normalized = likeValue(value);

            if (normalized == null) {
                return cb.conjunction();
            }

            return cb.or(
                    cb.like(cb.lower(root.get("codigo")), normalized),
                    cb.like(cb.lower(root.get("nombre")), normalized),
                    cb.like(cb.lower(root.get("descripcion")), normalized)
            );
        };
    }

    private static Specification<Rol> codigo(String value) {
        return (root, query, cb) -> {
            String normalized = likeValue(value);

            if (normalized == null) {
                return cb.conjunction();
            }

            return cb.like(cb.lower(root.get("codigo")), normalized);
        };
    }

    private static Specification<Rol> nombre(String value) {
        return (root, query, cb) -> {
            String normalized = likeValue(value);

            if (normalized == null) {
                return cb.conjunction();
            }

            return cb.like(cb.lower(root.get("nombre")), normalized);
        };
    }

    private static Specification<Rol> alwaysTrue() {
        return (root, query, cb) -> cb.conjunction();
    }

    private static String likeValue(String value) {
        String normalized = StringNormalizer.lower(value);
        return normalized == null ? null : "%" + normalized + "%";
    }
}