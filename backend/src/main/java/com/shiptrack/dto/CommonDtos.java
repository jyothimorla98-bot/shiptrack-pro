package com.shiptrack.dto;

import org.springframework.data.domain.Page;

import java.util.List;

public final class CommonDtos {

    private CommonDtos() {}

    public record ApiMessage(String message) {}

    public record PageResponse<T>(
            List<T> content,
            int page,
            int size,
            long totalElements,
            int totalPages,
            boolean last
    ) {
        public static <S, T> PageResponse<T> of(Page<S> page, List<T> mapped) {
            return new PageResponse<>(mapped, page.getNumber(), page.getSize(),
                    page.getTotalElements(), page.getTotalPages(), page.isLast());
        }
    }
}
