package br.com.ecofy.ms_budgeting.core.application.command;

import java.time.Instant;
import java.util.UUID;

public record ProcessTransactionForBudgetCommand(

        UUID runId,

        UUID transactionId,

        UUID userId,

        UUID categoryId,

        EventMetadata metadata

) {

    // Agrupa os identificadores necessários para processar uma transação no contexto de budgets.
    public ProcessTransactionForBudgetCommand { }

    public record EventMetadata(
            String eventId,
            String correlationId,
            String topic,
            int partition,
            long offset,
            String key,
            Instant receivedAt
    ) {
        // Carrega metadados do evento (Kafka) para rastreio, auditoria e troubleshooting.
        public EventMetadata { }
    }

}
