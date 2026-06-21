package io.github.roberaf.routely.api.dto;

import java.util.List;
import java.util.function.Function;

import org.springframework.data.domain.Page;

/**
 * Plain, stable shape for paged results. We never hand a {@code PageImpl} back
 * to a client directly - its JSON shape is a Spring Data implementation detail
 * that has changed across versions, so this record is the one contract callers
 * can rely on.
 */
public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {

	public static <S, T> PageResponse<T> of(Page<S> page, Function<S, T> mapper) {
		return new PageResponse<>(
				page.getContent().stream().map(mapper).toList(),
				page.getNumber(),
				page.getSize(),
				page.getTotalElements(),
				page.getTotalPages());
	}
}
