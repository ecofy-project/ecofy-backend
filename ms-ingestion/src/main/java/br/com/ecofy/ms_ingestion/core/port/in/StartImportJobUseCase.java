package br.com.ecofy.ms_ingestion.core.port.in;

import br.com.ecofy.ms_ingestion.core.domain.ImportJob;

import java.util.UUID;

public interface StartImportJobUseCase {

    record StartImportJobCommand(UUID importFileId, UUID ownerId, String correlationId) {
    }

    ImportJob start(StartImportJobCommand command);
}
