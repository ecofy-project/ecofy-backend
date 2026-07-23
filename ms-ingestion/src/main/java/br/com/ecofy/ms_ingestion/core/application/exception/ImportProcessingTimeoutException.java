package br.com.ecofy.ms_ingestion.core.application.exception;

import java.time.Duration;

// Sinaliza que o processamento excedeu a duração máxima e foi interrompido em ponto seguro.
public class ImportProcessingTimeoutException extends IngestionException {

    public ImportProcessingTimeoutException(Duration timeout) {
        super(
                IngestionErrorCode.IMPORT_PROCESSING_TIMEOUT,
                "Import processing exceeded the maximum allowed duration",
                "timeout=" + timeout
        );
    }
}
