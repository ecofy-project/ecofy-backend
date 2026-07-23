package br.com.ecofy.ms_ingestion.core.application.exception;

// Sinaliza ausência do arquivo na requisição de upload.
public class FileRequiredException extends IngestionException {

    public FileRequiredException() {
        super(IngestionErrorCode.FILE_REQUIRED, "A file is required");
    }
}
