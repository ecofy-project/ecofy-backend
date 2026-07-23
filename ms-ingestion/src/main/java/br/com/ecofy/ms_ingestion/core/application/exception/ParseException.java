package br.com.ecofy.ms_ingestion.core.application.exception;

public class ParseException extends IngestionException {

    public ParseException(String message, Throwable cause) {
        super(IngestionErrorCode.INVALID_FILE_CONTENT, message, cause);
    }

}
