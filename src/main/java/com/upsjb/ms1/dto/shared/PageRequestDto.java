package com.upsjb.ms1.dto.shared;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.io.Serializable;

public record PageRequestDto(
        @Min(value = 0, message = "El número de página no puede ser negativo.")
        Integer page,

        @Min(value = 1, message = "El tamaño de página debe ser al menos 1.")
        @Max(value = 100, message = "El tamaño de página no puede superar 100 registros.")
        Integer size,

        String sortBy,

        String sortDirection
) implements Serializable {

    public static final int DEFAULT_PAGE = 0;
    public static final int DEFAULT_SIZE = 20;
    public static final String DEFAULT_SORT_DIRECTION = "ASC";

    public int normalizedPage() {
        return page == null ? DEFAULT_PAGE : page;
    }

    public int normalizedSize() {
        return size == null ? DEFAULT_SIZE : size;
    }
}