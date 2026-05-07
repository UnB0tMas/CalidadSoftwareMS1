package com.upsjb.ms1.dto.shared;

import java.io.Serializable;
import java.util.List;

public record PageResponseDto<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last,
        boolean empty
) implements Serializable {
}