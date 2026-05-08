package com.upsjb.ms1.shared.specification;

import jakarta.persistence.criteria.Expression;
import java.time.Instant;
import org.springframework.data.jpa.domain.Specification;

public final class SpecificationBuilder {

    private SpecificationBuilder() {
    }

    public static <T> Specification<T> alwaysTrue() {
        return (root, query, cb) -> cb.conjunction();
    }

    public static <T> Specification<T> equalsIfPresent(String field, Object value) {
        return (root, query, cb) -> value == null ? cb.conjunction() : cb.equal(root.get(field), value);
    }

    public static <T> Specification<T> likeIgnoreCaseIfPresent(String field, String value) {
        return (root, query, cb) -> {
            if (value == null || value.isBlank()) {
                return cb.conjunction();
            }

            Expression<String> expression = cb.lower(root.get(field));
            return cb.like(expression, "%" + value.trim().toLowerCase() + "%");
        };
    }

    public static <T> Specification<T> instantGreaterThanOrEqualIfPresent(String field, Instant value) {
        return (root, query, cb) -> value == null
                ? cb.conjunction()
                : cb.greaterThanOrEqualTo(root.get(field), value);
    }

    public static <T> Specification<T> instantLessThanOrEqualIfPresent(String field, Instant value) {
        return (root, query, cb) -> value == null
                ? cb.conjunction()
                : cb.lessThanOrEqualTo(root.get(field), value);
    }
}