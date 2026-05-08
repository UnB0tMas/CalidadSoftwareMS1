package com.upsjb.ms1.specification;

import com.upsjb.ms1.domain.entity.UsuarioSesion;
import com.upsjb.ms1.domain.enums.EstadoSesion;
import com.upsjb.ms1.util.StringNormalizer;
import java.time.Instant;
import org.springframework.data.jpa.domain.Specification;

public final class UsuarioSesionSpecifications {

    private UsuarioSesionSpecifications() {
    }

    public static Specification<UsuarioSesion> from(
            Long idUsuario,
            EstadoSesion estado,
            String ipAddress,
            Instant fechaDesde,
            Instant fechaHasta
    ) {
        return alwaysTrue()
                .and(usuario(idUsuario))
                .and(estado(estado))
                .and(ipAddress(ipAddress))
                .and(fechaDesde(fechaDesde))
                .and(fechaHasta(fechaHasta));
    }

    private static Specification<UsuarioSesion> usuario(Long idUsuario) {
        return (root, query, cb) -> idUsuario == null
                ? cb.conjunction()
                : cb.equal(root.get("usuario").get("id"), idUsuario);
    }

    private static Specification<UsuarioSesion> estado(EstadoSesion estado) {
        return (root, query, cb) -> estado == null
                ? cb.conjunction()
                : cb.equal(root.get("estado"), estado);
    }

    private static Specification<UsuarioSesion> ipAddress(String value) {
        return (root, query, cb) -> {
            String normalized = likeValue(value);

            if (normalized == null) {
                return cb.conjunction();
            }

            return cb.like(cb.lower(root.get("ipAddress").get("value")), normalized);
        };
    }

    private static Specification<UsuarioSesion> fechaDesde(Instant value) {
        return (root, query, cb) -> value == null
                ? cb.conjunction()
                : cb.greaterThanOrEqualTo(root.get("createdAt"), value);
    }

    private static Specification<UsuarioSesion> fechaHasta(Instant value) {
        return (root, query, cb) -> value == null
                ? cb.conjunction()
                : cb.lessThanOrEqualTo(root.get("createdAt"), value);
    }

    private static Specification<UsuarioSesion> alwaysTrue() {
        return (root, query, cb) -> cb.conjunction();
    }

    private static String likeValue(String value) {
        String normalized = StringNormalizer.lower(value);
        return normalized == null ? null : "%" + normalized + "%";
    }
}