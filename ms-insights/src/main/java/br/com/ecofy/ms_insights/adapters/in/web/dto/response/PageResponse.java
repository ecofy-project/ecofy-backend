package br.com.ecofy.ms_insights.adapters.in.web.dto.response;

import br.com.ecofy.ms_insights.core.port.out.PageResult;

import java.util.List;
import java.util.function.Function;

// Define o contrato de página da API, sem expor os tipos de paginação da infraestrutura.
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {
    public static <D, T> PageResponse<T> from(PageResult<D> result, Function<D, T> mapper) {
        return new PageResponse<>(
                result.content().stream().map(mapper).toList(),
                result.page(),
                result.size(),
                result.totalElements(),
                result.totalPages(),
                result.first(),
                result.last());
    }
}
