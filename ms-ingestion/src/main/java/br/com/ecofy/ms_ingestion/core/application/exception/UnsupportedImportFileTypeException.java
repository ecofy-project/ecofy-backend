package br.com.ecofy.ms_ingestion.core.application.exception;

import java.util.UUID;

public class UnsupportedImportFileTypeException extends IngestionException {

    public UnsupportedImportFileTypeException(String type, UUID importFileId) {
        super(
                IngestionErrorCode.UNSUPPORTED_FILE_TYPE,
                "Unsupported ImportFileType",
                "type=" + type + ", importFileId=" + importFileId
        );
    }

}
