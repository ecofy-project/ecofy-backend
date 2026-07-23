package br.com.ecofy.ms_ingestion.core.port.in;

import br.com.ecofy.ms_ingestion.core.domain.ImportFile;
import br.com.ecofy.ms_ingestion.core.domain.enums.ImportFileType;

import java.io.InputStream;
import java.util.UUID;

public interface UploadFileUseCase {

    record UploadFileCommand(
            UUID ownerId,
            String originalFileName,
            ImportFileType type,
            String declaredContentType,
            long declaredSizeBytes,
            InputStream content,
            String idempotencyKey,
            String correlationId
    ) {
    }

    ImportFile upload(UploadFileCommand command);
}
