package br.com.ecofy.ms_budgeting.adapters.out.messaging.mapper;

import br.com.ecofy.ms_budgeting.adapters.out.messaging.dto.BudgetAlertEvent;
import br.com.ecofy.ms_budgeting.core.domain.BudgetAlert;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

// Converte alertas de orçamento em eventos destinados ao serviço de notificações.
public final class EventMapper {

    private static final String SOURCE = "ms-budgeting";

    private EventMapper() {
    }

    // Converte o alerta utilizando o relógio UTC padrão.
    public static BudgetAlertEvent toEvent(BudgetAlert alert) {
        return toEvent(alert, Clock.systemUTC());
    }

    // Converte o alerta utilizando o relógio informado.
    public static BudgetAlertEvent toEvent(BudgetAlert alert, Clock clock) {
        Objects.requireNonNull(alert, "alert must not be null");
        Objects.requireNonNull(clock, "clock must not be null");

        if (alert.getBudgetId() == null) {
            throw new IllegalArgumentException("alert.budgetId must not be null");
        }
        if (alert.getSeverity() == null) {
            throw new IllegalArgumentException("alert.severity must not be null");
        }

        var metadata = new BudgetAlertEvent.EventMetadata(
                deterministicEventId(alert),
                null,
                Instant.now(clock),
                SOURCE
        );

        return new BudgetAlertEvent(
                alert.getUserId(),
                alert.getBudgetId(),
                alert.getCategoryId(),
                alert.getLimitAmount(),
                alert.getConsumedAmount(),
                alert.getConsumedPct(),
                alert.getSeverity().name(),
                metadata
        );
    }

    private static String deterministicEventId(BudgetAlert alert) {
        String base = String.valueOf(alert.getBudgetId())
                + "|" + String.valueOf(alert.getConsumptionId())
                + "|" + String.valueOf(alert.getSeverity())
                + "|" + String.valueOf(alert.getPeriodStart())
                + "|" + String.valueOf(alert.getPeriodEnd());

        return UUID.nameUUIDFromBytes(base.getBytes(StandardCharsets.UTF_8)).toString();
    }
}
