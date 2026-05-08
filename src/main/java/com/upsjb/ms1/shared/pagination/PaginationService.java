package com.upsjb.ms1.shared.pagination;

import com.upsjb.ms1.dto.shared.PageRequestDto;
import com.upsjb.ms1.shared.exception.ValidationException;
import java.util.Locale;
import java.util.Set;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
public class PaginationService {

    private static final int MAX_SIZE = 100;
    private static final Set<String> DIRECTIONS = Set.of("ASC", "DESC");

    public Pageable toPageable(PageRequestDto request, String defaultSortBy, Set<String> allowedSortFields) {
        int page = request == null ? PageRequestDto.DEFAULT_PAGE : request.normalizedPage();
        int size = request == null ? PageRequestDto.DEFAULT_SIZE : request.normalizedSize();

        if (page < 0) {
            throw new ValidationException("PAGINATION_PAGE_INVALID", "El número de página no puede ser negativo.");
        }

        if (size < 1 || size > MAX_SIZE) {
            throw new ValidationException("PAGINATION_SIZE_INVALID", "El tamaño de página debe estar entre 1 y 100.");
        }

        String sortBy = normalizeSortBy(request == null ? null : request.sortBy(), defaultSortBy, allowedSortFields);
        Sort.Direction direction = normalizeDirection(request == null ? null : request.sortDirection());

        return PageRequest.of(page, size, Sort.by(direction, sortBy));
    }

    public Pageable toPageable(PageRequestDto request, String defaultSortBy) {
        return toPageable(request, defaultSortBy, Set.of(defaultSortBy));
    }

    private String normalizeSortBy(String requestedSortBy, String defaultSortBy, Set<String> allowedSortFields) {
        String sortBy = requestedSortBy == null || requestedSortBy.isBlank()
                ? defaultSortBy
                : requestedSortBy.trim();

        if (allowedSortFields != null && !allowedSortFields.isEmpty() && !allowedSortFields.contains(sortBy)) {
            throw new ValidationException(
                    "PAGINATION_SORT_FIELD_INVALID",
                    "El campo de ordenamiento no está permitido."
            );
        }

        return sortBy;
    }

    private Sort.Direction normalizeDirection(String rawDirection) {
        String direction = rawDirection == null || rawDirection.isBlank()
                ? PageRequestDto.DEFAULT_SORT_DIRECTION
                : rawDirection.trim().toUpperCase(Locale.ROOT);

        if (!DIRECTIONS.contains(direction)) {
            throw new ValidationException(
                    "PAGINATION_SORT_DIRECTION_INVALID",
                    "La dirección de ordenamiento debe ser ASC o DESC."
            );
        }

        return Sort.Direction.fromString(direction);
    }
}