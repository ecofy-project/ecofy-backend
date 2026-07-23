package br.com.ecofy.ms_ingestion.core.application.exception;

import java.util.List;

// Sinaliza parâmetro de paginação ou ordenação fora do permitido.
public class PaginationParameterInvalidException extends IngestionException {

    private final List<ErrorDetail> details;

    public PaginationParameterInvalidException(List<ErrorDetail> details) {
        super(IngestionErrorCode.PAGINATION_PARAMETER_INVALID, "Invalid pagination parameter");
        this.details = details == null ? List.of() : List.copyOf(details);
    }

    public List<ErrorDetail> getDetails() {
        return details;
    }
}
