package com.upsjb.ms1.specification;

import com.upsjb.ms1.domain.entity.LoginAttempt;
import com.upsjb.ms1.dto.auditoria.filter.LoginAttemptFilterDto;
import com.upsjb.ms1.shared.specification.SpecificationBuilder;
import com.upsjb.ms1.util.StringNormalizer;
import org.springframework.data.jpa.domain.Specification;

public final class LoginAttemptSpecifications {

    private LoginAttemptSpecifications() {
    }

    public static Specification<LoginAttempt> from(LoginAttemptFilterDto filter) {
        Specification<LoginAttempt> specification = SpecificationBuilder.alwaysTrue();

        if (filter == null) {
            return specification;
        }

        return specification
                .and(SpecificationBuilder.likeIgnoreCaseIfPresent("usernameOrEmail", filter.usernameOrEmail()))
                .and(SpecificationBuilder.equalsIfPresent("tipoLogin", filter.tipoLogin()))
                .and(SpecificationBuilder.equalsIfPresent("exitoso", filter.exitoso()))
                .and(SpecificationBuilder.likeIgnoreCaseIfPresent("failureCode", filter.failureCode()))
                .and(ipAddress(filter.ipAddress()))
                .and(SpecificationBuilder.instantGreaterThanOrEqualIfPresent("attemptedAt", filter.fechaDesde()))
                .and(SpecificationBuilder.instantLessThanOrEqualIfPresent("attemptedAt", filter.fechaHasta()));
    }

    private static Specification<LoginAttempt> ipAddress(String value) {
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