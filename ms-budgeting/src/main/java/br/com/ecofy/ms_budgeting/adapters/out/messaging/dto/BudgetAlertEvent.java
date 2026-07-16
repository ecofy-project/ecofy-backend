package br.com.ecofy.ms_budgeting.adapters.out.messaging.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Evento BUDGET_ALERT publicado em {@code eco.budget.alert} e consumido pelo ms-notification.
 *
 * <p>Correção Dia 6 (item #10 / compatibilidade): a estrutura anterior
 * (eventId, occurredAt, budgetId, consumptionId, severity, message, periodStart, periodEnd)
 * era INCOMPATÍVEL com o consumidor {@code BudgetAlertEventMessage} do ms-notification, que
 * exige {@code userId} (não-nulo) e lê {@code categoryId/limitAmount/consumedAmount/consumedPct}.
 * Os nomes dos campos abaixo espelham exatamente o contrato do ms-notification (tópico inalterado).</p>
 */
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

    /** Metadados espelhando {@code MessageMetadata} do ms-notification (eventId/correlationId/occurredAt/source). */
    public record EventMetadata(

            String eventId,

            String correlationId,

            Instant occurredAt,

            String source

    ) { }
}
