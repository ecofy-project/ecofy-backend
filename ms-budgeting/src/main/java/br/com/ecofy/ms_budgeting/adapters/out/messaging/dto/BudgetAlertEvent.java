package br.com.ecofy.ms_budgeting.adapters.out.messaging.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

// Representa o evento de alerta de orçamento no formato esperado pelo ms-notification.
public record BudgetAlertEvent(

        UUID userId,

        UUID budgetId,

        UUID categoryId,

        BigDecimal limitAmount,

        BigDecimal consumedAmount,

        Integer consumedPct,

        String severity,

        EventMetadata metadata

) {

    // Transporta os metadados de rastreamento no formato esperado pelo consumidor.
    public record EventMetadata(

            String eventId,

            String correlationId,

            Instant occurredAt,

            String source

    ) { }
}
