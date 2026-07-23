package br.com.ecofy.ms_ingestion.core.application.exception;

public class InvalidFileSizeException extends IngestionException {

    public InvalidFileSizeException(long sizeBytes) {
        super(IngestionErrorCode.FILE_EMPTY, "File size must be greater than zero", "sizeBytes=" + sizeBytes);
    }

}
