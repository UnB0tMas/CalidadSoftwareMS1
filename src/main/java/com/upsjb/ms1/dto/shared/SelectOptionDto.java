package com.upsjb.ms1.dto.shared;

import java.io.Serializable;

public record SelectOptionDto<T>(
        T value,
        String label,
        boolean disabled
) implements Serializable {
}