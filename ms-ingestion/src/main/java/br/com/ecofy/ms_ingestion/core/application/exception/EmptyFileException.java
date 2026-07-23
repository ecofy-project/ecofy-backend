package br.com.ecofy.ms_ingestion.core.application.exception;

// Sinaliza arquivo sem registros, contendo apenas cabeçalho ou nada.
public class EmptyFileException extends IngestionException {

    public EmptyFileException(String reason) {
        super(IngestionErrorCode.EMPTY_FILE, "The file contains no records", reason);
    }
}
