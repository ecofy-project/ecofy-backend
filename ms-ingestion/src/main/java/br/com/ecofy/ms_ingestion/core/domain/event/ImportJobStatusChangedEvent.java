package br.com.ecofy.ms_ingestion.core.domain.event;

import br.com.ecofy.ms_ingestion.core.domain.enums.ImportJobStatus;

import java.time.Instant;
import java.util.UUID;

public record ImportJobStatusChangedEvent(

        UUID importJobId,

        ImportJobStatus oldStatus,

        ImportJobStatus newStatus,

        Instant changedAt

) { }
