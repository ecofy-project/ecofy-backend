package br.com.ecofy.ms_ingestion.core.application.exception;

import java.util.List;

// Sinaliza cabeçalho incompatível com o formato esperado, carregando quais colunas reprovaram.
public class InvalidFileHeaderException extends IngestionException {

    private final List<ErrorDetail> details;

    public InvalidFileHeaderException(List<ErrorDetail> details) {
        super(IngestionErrorCode.INVALID_FILE_HEADER, "The file header does not match the expected format");
        this.details = details == null ? List.of() : List.copyOf(details);
    }

    public List<ErrorDetail> getDetails() {
        return details;
    }
}
