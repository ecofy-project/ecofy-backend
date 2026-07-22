package br.com.ecofy.ms_budgeting.adapters.out.outbox;

import br.com.ecofy.ms_budgeting.adapters.correlation.CorrelationContext;
import br.com.ecofy.ms_budgeting.adapters.out.messaging.EventTypes;
import br.com.ecofy.ms_budgeting.adapters.out.messaging.dto.BudgetAlertDataV1;
import br.com.ecofy.ms_budgeting.adapters.out.messaging.dto.EventEnvelope;
import br.com.ecofy.ms_budgeting.config.BudgetingProperties;
import br.com.ecofy.ms_budgeting.core.domain.BudgetAlert;
import br.com.ecofy.ms_budgeting.core.domain.outbox.OutboxEvent;
import br.com.ecofy.ms_budgeting.core.domain.valueobject.Money;
import br.com.ecofy.ms_budgeting.core.port.out.OutboxEventPort;
import br.com.ecofy.ms_budgeting.core.port.out.PublishBudgetAlertEventPort;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Currency;
import java.util.Objects;
import java.util.UUID;

// Persiste o alerta na Outbox dentro da transação do caso de uso, com eventId determinístico para idempotência.
@Slf4j
@Component
@Primary
public class BudgetAlertOutboxWriter implements PublishBudgetAlertEventPort {

    private final OutboxEventPort outboxPort;
    private final ObjectMapper objectMapper;
    private final BudgetingProperties props;
    private final MeterRegistry meterRegistry;
    private final Clock clock;

    public BudgetAlertOutboxWriter(OutboxEventPort outboxPort,
                                   ObjectMapper objectMapper,
                                   BudgetingProperties props,
                                   MeterRegistry meterRegistry,
                                   Clock clock) {
        this.outboxPort = Objects.requireNonNull(outboxPort, "outboxPort must not be null");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
        this.props = Objects.requireNonNull(props, "props must not be null");
        this.meterRegistry = Objects.requireNonNull(meterRegistry, "meterRegistry must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Override
    public void publish(BudgetAlert alert) {
        Objects.requireNonNull(alert, "alert must not be null");

        UUID userId = Objects.requireNonNull(alert.getUserId(),
                "alert.userId must not be null when publishing (envelope requires it)");

        UUID eventId = deterministicEventId(alert);
        Instant occurredAt = alert.getCreatedAt() != null ? alert.getCreatedAt() : Instant.now(clock);

        String correlationId = CorrelationContext.currentCorrelationIdOrGenerate();
        UUID causationId = CorrelationContext.currentCausationId();

        BudgetAlertDataV1 data = new BudgetAlertDataV1(
                userId,
                alert.getBudgetId(),
                alert.getCategoryId(),
                alert.getPeriodStart(),
                alert.getPeriodEnd(),
                alert.getLimitAmount(),
                alert.getConsumedAmount(),
                percentage(alert),
                alert.getCurrency(),
                alert.getSeverity().name()
        );

        EventEnvelope<BudgetAlertDataV1> envelope = new EventEnvelope<>(
                eventId,
                EventTypes.BUDGET_ALERT,
                EventTypes.BUDGET_ALERT_VERSION,
                occurredAt,
                EventTypes.PRODUCER,
                correlationId,
                causationId,
                data
        );

        // Chave de partição = userId (§13.4): alertas de um mesmo usuário na mesma partição,
        // preservando ordem por usuário. Estável entre republicações.
        String partitionKey = userId.toString();

        OutboxEvent outboxEvent = OutboxEvent.createPending(
                eventId,
                EventTypes.AGGREGATE_TYPE_BUDGET,
                alert.getBudgetId(),
                EventTypes.BUDGET_ALERT,
                EventTypes.BUDGET_ALERT_VERSION,
                props.topics().budgetAlert(),
                partitionKey,
                serialize(envelope),
                correlationId,
                causationId,
                occurredAt
        );

        // Participa da transação corrente do BudgetProjectionService (MANDATORY):
        // rollback do alerta = rollback da outbox, e vice-versa.
        outboxPort.save(outboxEvent);

        meterRegistry.counter("ecofy.budgeting.outbox.created.total",
                "event_type", EventTypes.BUDGET_ALERT).increment();

        log.info("[BudgetAlertOutboxWriter] - [publish] -> Evento gravado na Outbox (PENDING) eventId={} budgetId={} severity={} correlationId={}",
                eventId, alert.getBudgetId(), alert.getSeverity(), correlationId);
    }

    // Recalcula o percentual consumido com escala fixa a partir dos valores do alerta.
    private static BigDecimal percentage(BudgetAlert alert) {
        if (alert.getLimitAmount() == null || alert.getConsumedAmount() == null || alert.getCurrency() == null) {
            return alert.getConsumedPct() != null
                    ? new BigDecimal(alert.getConsumedPct()).setScale(2, Money.ROUNDING)
                    : BigDecimal.ZERO.setScale(2);
        }
        Currency currency = Currency.getInstance(alert.getCurrency());
        Money consumed = new Money(alert.getConsumedAmount(), currency);
        Money limit = new Money(alert.getLimitAmount(), currency);
        return consumed.percentageOf(limit);
    }

    // Deriva um eventId determinístico da condição do alerta, garantindo identidade estável na republicação.
    private static UUID deterministicEventId(BudgetAlert alert) {
        String base = alert.getBudgetId()
                + "|" + alert.getConsumptionId()
                + "|" + alert.getSeverity()
                + "|" + alert.getPeriodStart()
                + "|" + alert.getPeriodEnd();
        return UUID.nameUUIDFromBytes(base.getBytes(StandardCharsets.UTF_8));
    }

    private String serialize(EventEnvelope<BudgetAlertDataV1> envelope) {
        try {
            return objectMapper.writeValueAsString(envelope);
        } catch (JsonProcessingException e) {
            // Falha de serialização deve ABORTAR a transação: commitar o alerta sem o evento
            // correspondente é exatamente o que a outbox proíbe.
            throw new IllegalStateException("Failed to serialize budget alert envelope", e);
        }
    }
}
