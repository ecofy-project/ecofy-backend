package br.com.ecofy.ms_ingestion.core.domain.event;

import java.util.List;
import java.util.UUID;

public record TransactionsImportedEvent(

        UUID importJobId,

        List<UUID> rawTransactionIds

) { }
