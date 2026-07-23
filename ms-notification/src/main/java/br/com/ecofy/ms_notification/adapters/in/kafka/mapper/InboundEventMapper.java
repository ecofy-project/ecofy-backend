package br.com.ecofy.ms_notification.adapters.in.kafka.mapper;

import br.com.ecofy.ms_notification.adapters.in.kafka.dto.BudgetAlertEventMessage;
import br.com.ecofy.ms_notification.adapters.in.kafka.dto.InsightCreatedEventMessage;
import br.com.ecofy.ms_notification.core.application.command.HandleDomainEventCommand;
import br.com.ecofy.ms_notification.core.domain.enums.DomainEventType;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

// Converte eventos Kafka em comandos normalizados para processamento de notificações.
@Component
public class InboundEventMapper {

    // Converte alertas de orçamento em comandos de eventos de domínio.
    public HandleDomainEventCommand fromBudgetAlert(BudgetAlertEventMessage msg) {
        Objects.requireNonNull(msg, "msg must not be null");

        BudgetAlertEventMessage.Data data = Objects.requireNonNull(
                msg.data(),
                "budget alert data must not be null"
        );
        UUID userId = requireUserId(data.userId());

        Map<String, Object> payload = new HashMap<>(8);
        putIfNotNull(payload, "budgetId", data.budgetId());
        putIfNotNull(payload, "categoryId", data.categoryId());
        putIfNotNull(payload, "limitAmount", data.limitAmount());
        putIfNotNull(payload, "consumedAmount", data.consumedAmount());
        putIfNotNull(payload, "consumedPct", data.percentageConsumed());
        putIfNotNull(payload, "severity", data.alertLevel());

        return toCommand(
                DomainEventType.BUDGET_ALERT,
                userId,
                payload,
                stringOrNull(msg.eventId())
        );
    }

    // Converte insights criados em comandos de eventos de domínio.
    public HandleDomainEventCommand fromInsightCreated(InsightCreatedEventMessage msg) {
        Objects.requireNonNull(msg, "msg must not be null");

        UUID userId = requireUserId(msg.userId());

        Map<String, Object> payload = new HashMap<>(6);
        putIfNotNull(payload, "insightId", msg.insightId());
        putIfNotNull(payload, "insightType", msg.insightType());
        putIfNotNull(payload, "periodStart", msg.periodStart());
        putIfNotNull(payload, "periodEnd", msg.periodEnd());

        String eventId = (msg.metadata() != null)
                ? msg.metadata().eventId()
                : null;

        return toCommand(
                DomainEventType.INSIGHT_CREATED,
                userId,
                payload,
                eventId
        );
    }

    // Centraliza a criação de comandos com payload imutável e chave de idempotência.
    private static HandleDomainEventCommand toCommand(
            DomainEventType type,
            UUID userId,
            Map<String, Object> payload,
            String eventId
    ) {
        String idempotencyKey = resolveIdempotencyKey(eventId);
        return new HandleDomainEventCommand(
                type,
                userId,
                Map.copyOf(payload),
                idempotencyKey
        );
    }

    private static UUID requireUserId(UUID userId) {
        return Objects.requireNonNull(userId, "userId must not be null");
    }

    // Resolve a chave de idempotência com fallback para um identificador aleatório.
    private static String resolveIdempotencyKey(String eventId) {
        return (eventId != null && !eventId.isBlank())
                ? eventId
                : UUID.randomUUID().toString();
    }

    private static void putIfNotNull(
            Map<String, Object> target,
            String key,
            Object value
    ) {
        if (value != null) {
            target.put(key, value);
        }
    }

    private static String stringOrNull(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
