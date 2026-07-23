package br.com.ecofy.ms_ingestion.core.application.exception;

// Sinaliza tipo de arquivo não permitido, registrando qual verificação reprovou.
public class UnsupportedFileTypeException extends IngestionException {

    public UnsupportedFileTypeException(String reason, String detail) {
        super(IngestionErrorCode.UNSUPPORTED_FILE_TYPE, reason, detail);
    }
}
