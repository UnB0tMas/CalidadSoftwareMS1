package com.upsjb.ms1.specification;

import com.upsjb.ms1.domain.entity.Rol;
import com.upsjb.ms1.domain.entity.Usuario;
import com.upsjb.ms1.dto.usuario.filter.UsuarioFilterDto;
import com.upsjb.ms1.util.StringNormalizer;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import java.time.Instant;
import org.springframework.data.jpa.domain.Specification;

public final class UsuarioSpecifications {

    private UsuarioSpecifications() {
    }

    public static Specification<Usuario> from(UsuarioFilterDto filter) {
        Specification<Usuario> specification = alwaysTrue();

        if (filter == null) {
            return specification;
        }

        return specification
                .and(search(filter.search()))
                .and(username(filter.username()))
                .and(email(filter.email()))
                .and(nombres(filter.nombres()))
                .and(apellidos(filter.apellidos()))
                .and(idRol(filter.idRol()))
                .and(codigoRol(filter.codigoRol()))
                .and(estado(filter))
                .and(emailVerificado(filter))
                .and(requiereCambioPassword(filter))
                .and(fechaDesde(filter.fechaDesde()))
                .and(fechaHasta(filter.fechaHasta()));
    }

    public static Specification<Usuario> active() {
        return (root, query, cb) -> cb.equal(
                root.get("estado"),
                com.upsjb.ms1.domain.enums.EstadoRegistro.ACTIVO
        );
    }

    private static Specification<Usuario> search(String value) {
        return (root, query, cb) -> {
            String normalized = likeValue(value);

            if (normalized == null) {
                return cb.conjunction();
            }

            Join<Usuario, Rol> rolJoin = root.join("rol", JoinType.LEFT);

            return cb.or(
                    cb.like(cb.lower(root.get("username").get("value")), normalized),
                    cb.like(cb.lower(root.get("email").get("value")), normalized),
                    cb.like(cb.lower(root.get("nombres")), normalized),
                    cb.like(cb.lower(root.get("apellidos")), normalized),
                    cb.like(cb.lower(rolJoin.get("codigo")), normalized),
                    cb.like(cb.lower(rolJoin.get("nombre")), normalized)
            );
        };
    }

    private static Specification<Usuario> username(String value) {
        return (root, query, cb) -> {
            String normalized = likeValue(value);

            if (normalized == null) {
                return cb.conjunction();
            }

            return cb.like(cb.lower(root.get("username").get("value")), normalized);
        };
    }

    private static Specification<Usuario> email(String value) {
        return (root, query, cb) -> {
            String normalized = likeValue(value);

            if (normalized == null) {
                return cb.conjunction();
            }

            return cb.like(cb.lower(root.get("email").get("value")), normalized);
        };
    }

    private static Specification<Usuario> nombres(String value) {
        return (root, query, cb) -> {
            String normalized = likeValue(value);

            if (normalized == null) {
                return cb.conjunction();
            }

            return cb.like(cb.lower(root.get("nombres")), normalized);
        };
    }

    private static Specification<Usuario> apellidos(String value) {
        return (root, query, cb) -> {
            String normalized = likeValue(value);

            if (normalized == null) {
                return cb.conjunction();
            }

            return cb.like(cb.lower(root.get("apellidos")), normalized);
        };
    }

    private static Specification<Usuario> idRol(Long idRol) {
        return (root, query, cb) -> idRol == null
                ? cb.conjunction()
                : cb.equal(root.get("rol").get("id"), idRol);
    }

    private static Specification<Usuario> codigoRol(String value) {
        return (root, query, cb) -> {
            String normalized = StringNormalizer.upper(value);

            if (normalized == null) {
                return cb.conjunction();
            }

            Join<Usuario, Rol> rolJoin = root.join("rol", JoinType.LEFT);
            return cb.equal(cb.upper(rolJoin.get("codigo")), normalized);
        };
    }

    private static Specification<Usuario> estado(UsuarioFilterDto filter) {
        return (root, query, cb) -> filter.estado() == null
                ? cb.conjunction()
                : cb.equal(root.get("estado"), filter.estado());
    }

    private static Specification<Usuario> emailVerificado(UsuarioFilterDto filter) {
        return (root, query, cb) -> filter.emailVerificado() == null
                ? cb.conjunction()
                : cb.equal(root.get("emailVerificado"), filter.emailVerificado());
    }

    private static Specification<Usuario> requiereCambioPassword(UsuarioFilterDto filter) {
        return (root, query, cb) -> filter.requiereCambioPassword() == null
                ? cb.conjunction()
                : cb.equal(root.get("requiereCambioPassword"), filter.requiereCambioPassword());
    }

    private static Specification<Usuario> fechaDesde(Instant value) {
        return (root, query, cb) -> value == null
                ? cb.conjunction()
                : cb.greaterThanOrEqualTo(root.get("createdAt"), value);
    }

    private static Specification<Usuario> fechaHasta(Instant value) {
        return (root, query, cb) -> value == null
                ? cb.conjunction()
                : cb.lessThanOrEqualTo(root.get("createdAt"), value);
    }

    private static Specification<Usuario> alwaysTrue() {
        return (root, query, cb) -> cb.conjunction();
    }

    private static String likeValue(String value) {
        String normalized = StringNormalizer.lower(value);
        return normalized == null ? null : "%" + normalized + "%";
    }
}