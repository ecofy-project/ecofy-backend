package br.com.ecofy.ms_users.core.application.pagination;

import java.util.List;
import java.util.Locale;

public record PageQuery(int page, int size, String sortBy, Direction direction) {

    public enum Direction { ASC, DESC }

    public PageQuery {
        if (page < 0) {
            throw new InvalidPaginationException("page must be >= 0");
        }
        if (size <= 0) {
            throw new InvalidPaginationException("size must be > 0");
        }
    }

    // Cria uma query saneada, rejeitando campos de ordenação fora da allowlist.
    public static PageQuery of(
            Integer page,
            Integer size,
            String sort,
            int defaultSize,
            int maxSize,
            String defaultSortBy,
            List<String> sortAllowlist
    ) {
        int safePage = (page == null) ? 0 : page;
        if (safePage < 0) {
            throw new InvalidPaginationException("page must be >= 0");
        }

        int safeSize = (size == null) ? defaultSize : size;
        if (safeSize <= 0) {
            throw new InvalidPaginationException("size must be > 0");
        }
        // Teto obrigatório: um size gigante viraria um scan/payload absurdo.
        if (safeSize > maxSize) {
            throw new InvalidPaginationException("size must be <= " + maxSize);
        }

        String sortBy = defaultSortBy;
        Direction direction = Direction.DESC;

        if (sort != null && !sort.isBlank()) {
            String[] parts = sort.split(",");
            sortBy = parts[0].trim();
            if (parts.length > 1) {
                String rawDirection = parts[1].trim().toUpperCase(Locale.ROOT);
                if (!"ASC".equals(rawDirection) && !"DESC".equals(rawDirection)) {
                    throw new InvalidPaginationException("sort direction must be ASC or DESC");
                }
                direction = Direction.valueOf(rawDirection);
            }
            if (!sortAllowlist.contains(sortBy)) {
                // Não ecoa o valor recebido de volta ao cliente sem controle.
                throw new InvalidPaginationException(
                        "sort field is not allowed; allowed: " + String.join(", ", sortAllowlist));
            }
        }

        return new PageQuery(safePage, safeSize, sortBy, direction);
    }
}
