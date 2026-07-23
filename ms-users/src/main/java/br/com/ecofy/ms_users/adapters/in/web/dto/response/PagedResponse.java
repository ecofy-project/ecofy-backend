package br.com.ecofy.ms_users.adapters.in.web.dto.response;

import br.com.ecofy.ms_users.core.application.pagination.PagedResult;
import java.util.List;

// Define o contrato estável de página usado por todas as listagens paginadas do serviço.
public record PagedResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {

    public static <T> PagedResponse<T> from(PagedResult<T> result) {
        return new PagedResponse<>(
                result.content(),
                result.page(),
                result.size(),
                result.totalElements(),
                result.totalPages(),
                result.first(),
                result.last());
    }
}
