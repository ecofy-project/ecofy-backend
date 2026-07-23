package br.com.ecofy.ms_ingestion.core.port.out;

import java.util.List;

public record PageResult<T>(
        List<T> content,
        int page,
        int size,
        long totalElements
) {

    public PageResult {
        content = content == null ? List.of() : List.copyOf(content);
    }

    public int totalPages() {
        if (size <= 0) {
            return 0;
        }
        return (int) Math.ceil((double) totalElements / size);
    }

    public boolean first() {
        return page == 0;
    }

    public boolean last() {
        return page >= totalPages() - 1;
    }
}
