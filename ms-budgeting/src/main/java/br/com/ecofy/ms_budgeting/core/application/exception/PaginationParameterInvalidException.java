package br.com.ecofy.ms_budgeting.core.application.exception;

// Sinaliza parâmetro de paginação ou ordenação fora do permitido.
public class PaginationParameterInvalidException extends RuntimeException {

    public PaginationParameterInvalidException(String message) {
        super(message);
    }
}
