package br.com.ecofy.ms_users.core.application.pagination;

import java.util.List;

public record PagedResult<T>(
        List<T> content,
        int page,
        int size,
        long totalElements
) {

    public PagedResult {
        content = (content == null) ? List.of() : List.copyOf(content);
    }

    public int totalPages() {
        if (size <= 0) {
            return 0;
        }
        return (int) Math.ceil((double) totalElements / (double) size);
    }

    public boolean first() {
        return page == 0;
    }

    public boolean last() {
        return page >= totalPages() - 1;
    }

    public <R> PagedResult<R> map(java.util.function.Function<T, R> mapper) {
        return new PagedResult<>(content.stream().map(mapper).toList(), page, size, totalElements);
    }
}
