package com.payanam.rail.dto;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/** One page of a server-side paginated list. */
public record PageView<T>(List<T> content, int page, int size, long totalElements, int totalPages) {

    public static <E, T> PageView<T> of(Page<E> page, Function<E, T> mapper) {
        return new PageView<>(page.getContent().stream().map(mapper).toList(), page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }
}
