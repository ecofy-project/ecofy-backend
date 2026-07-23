package br.com.ecofy.ms_notification.adapters.in.kafka.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

// Representa o evento de alerta de orçamento no envelope uniforme, ignorando campos novos do produtor.
@JsonIgnoreProperties(ignoreUnknown = true)
public record BudgetAlertEventMessage(
        UUID eventId,
        String eventType,
        Integer eventVersion,
        Instant occurredAt,
        String producer,
        String correlationId,
        UUID causationId,
        Data data
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Data(
            UUID userId,
            UUID budgetId,
            UUID categoryId,
            LocalDate periodStart,
            LocalDate periodEnd,
            BigDecimal limitAmount,
            BigDecimal consumedAmount,
            BigDecimal percentageConsumed,
            String currency,
            String alertLevel
    ) {}
}
