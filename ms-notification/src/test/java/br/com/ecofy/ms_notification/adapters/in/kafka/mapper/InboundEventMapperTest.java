package br.com.ecofy.ms_notification.adapters.in.kafka.mapper;

import br.com.ecofy.ms_notification.adapters.in.kafka.dto.BudgetAlertEventMessage;
import br.com.ecofy.ms_notification.adapters.in.kafka.dto.InsightCreatedEventMessage;
import br.com.ecofy.ms_notification.adapters.in.kafka.dto.MessageMetadata;
import br.com.ecofy.ms_notification.core.application.command.HandleDomainEventCommand;
import br.com.ecofy.ms_notification.core.domain.enums.DomainEventType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InboundEventMapperTest {

    private final InboundEventMapper mapper = new InboundEventMapper();

    @Test
    void fromBudgetAlert_shouldMapUserIdAndPayloadFieldsAndUseEventIdAsIdempotencyKey() {
        UUID userId = UUID.randomUUID();
        UUID budgetId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        var meta = new MessageMetadata("evt-100", "corr-1", Instant.now(), "ms-budgeting");

        var msg = new BudgetAlertEventMessage(userId, budgetId, categoryId,
                new BigDecimal("1000.00"), new BigDecimal("800.00"), 80, "WARNING", meta);

        HandleDomainEventCommand cmd = mapper.fromBudgetAlert(msg);

        assertThat(cmd.eventType()).isEqualTo(DomainEventType.BUDGET_ALERT);
        assertThat(cmd.userId()).isEqualTo(userId);
        assertThat(cmd.idempotencyKey()).isEqualTo("evt-100");
        assertThat(cmd.payload()).containsEntry("budgetId", budgetId)
                .containsEntry("categoryId", categoryId)
                .containsEntry("consumedPct", 80)
                .containsEntry("severity", "WARNING");
    }

    @Test
    void fromBudgetAlert_shouldRejectNullUserId() {
        var msg = new BudgetAlertEventMessage(null, UUID.randomUUID(), UUID.randomUUID(),
                BigDecimal.ONE, BigDecimal.ZERO, 10, "WARNING", null);

        assertThatThrownBy(() -> mapper.fromBudgetAlert(msg))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("userId");
    }

    @Test
    void fromInsightCreated_shouldMapFieldsAndFallbackIdempotencyKeyWhenNoEventId() {
        UUID userId = UUID.randomUUID();
        UUID insightId = UUID.randomUUID();

        var msg = new InsightCreatedEventMessage(userId, insightId, "SPENDING_SPIKE",
                "2026-01-01", "2026-01-31", null);

        HandleDomainEventCommand cmd = mapper.fromInsightCreated(msg);

        assertThat(cmd.eventType()).isEqualTo(DomainEventType.INSIGHT_CREATED);
        assertThat(cmd.userId()).isEqualTo(userId);
        assertThat(cmd.payload()).containsEntry("insightId", insightId)
                .containsEntry("insightType", "SPENDING_SPIKE");
        assertThat(cmd.idempotencyKey()).isNotBlank(); // fallback UUID
    }
}
