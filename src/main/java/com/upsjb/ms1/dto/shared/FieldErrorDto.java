package com.upsjb.ms1.dto.shared;

import java.io.Serializable;

public record FieldErrorDto(
        String field,
        String message
) implements Serializable {
}