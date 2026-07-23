package br.com.ecofy.ms_ingestion.core.application.exception;

public class ImportFileTypeRequiredException extends IngestionException {

    public ImportFileTypeRequiredException() {
        super(IngestionErrorCode.UNSUPPORTED_FILE_TYPE, "ImportFileType (type) must not be null for uploaded file");
    }

}
