package br.com.ecofy.ms_ingestion.core.port.in;

import br.com.ecofy.ms_ingestion.core.domain.RawTransaction;

import java.util.List;
import java.util.UUID;

public interface IngestTransactionEventUseCase {

    record IngestEventCommand(
            UUID ownerId,
            String sourceSystem,
            String eventId,
            String correlationId,
            List<RawTransaction> transactions
    ) {
    }

    void ingest(IngestEventCommand command);
}
