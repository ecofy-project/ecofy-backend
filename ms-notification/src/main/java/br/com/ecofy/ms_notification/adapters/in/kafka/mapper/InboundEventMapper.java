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

@Component
public class InboundEventMapper {

    // Converte a mensagem de budget alert (Kafka) em um comando de aplicação normalizado para o handler de eventos de domínio.
    public HandleDomainEventCommand fromBudgetAlert(BudgetAlertEventMessage msg) {
        Objects.requireNonNull(msg, "msg must not be null");

        UUID userId = requireUserId(msg.userId());

        Map<String, Object> payload = new HashMap<>(8);
        putIfNotNull(payload, "budgetId", msg.budgetId());
        putIfNotNull(payload, "categoryId", msg.categoryId());
        putIfNotNull(payload, "limitAmount", msg.limitAmount());
        putIfNotNull(payload, "consumedAmount", msg.consumedAmount());
        putIfNotNull(payload, "consumedPct", msg.consumedPct());
        putIfNotNull(payload, "severity", msg.severity());

        String eventId = (msg.metadata() != null) ? msg.metadata().eventId() : null;

        return toCommand(DomainEventType.BUDGET_ALERT, userId, payload, eventId);
    }

    // Converte a mensagem de insight.created (Kafka) em um comando de aplicação normalizado para o handler de eventos de domínio.
    public HandleDomainEventCommand fromInsightCreated(InsightCreatedEventMessage msg) {
        Objects.requireNonNull(msg, "msg must not be null");

        UUID userId = requireUserId(msg.userId());

        Map<String, Object> payload = new HashMap<>(6);
        putIfNotNull(payload, "insightId", msg.insightId());
        putIfNotNull(payload, "insightType", msg.insightType());
        putIfNotNull(payload, "periodStart", msg.periodStart());
        putIfNotNull(payload, "periodEnd", msg.periodEnd());

        String eventId = (msg.metadata() != null) ? msg.metadata().eventId() : null;

        return toCommand(DomainEventType.INSIGHT_CREATED, userId, payload, eventId);
    }

    // Centraliza a criação do HandleDomainEventCommand incluindo tipo, userId, payload imutável e chave de idempotência derivada do eventId.
    private static HandleDomainEventCommand toCommand(DomainEventType type,
                                                      UUID userId,
                                                      Map<String, Object> payload,
                                                      String eventId) {

        String idempotencyKey = resolveIdempotencyKey(eventId);
        return new HandleDomainEventCommand(type, userId, Map.copyOf(payload), idempotencyKey);
    }

    // Valida e retorna o userId obrigatório para processamento do evento.
    private static UUID requireUserId(UUID userId) {
        return Objects.requireNonNull(userId, "userId must not be null");
    }

    // Resolve a chave de idempotência priorizando eventId estável (quando presente) e gerando UUID apenas como fallback.
    private static String resolveIdempotencyKey(String eventId) {
        return (eventId != null && !eventId.isBlank())
                ? eventId
                : UUID.randomUUID().toString();
    }

    // Adiciona um par chave/valor ao payload somente quando o valor não é nulo, evitando ruído e mantendo compatibilidade.
    private static void putIfNotNull(Map<String, Object> target, String key, Object value) {
        if (value != null) target.put(key, value);
    }

}
