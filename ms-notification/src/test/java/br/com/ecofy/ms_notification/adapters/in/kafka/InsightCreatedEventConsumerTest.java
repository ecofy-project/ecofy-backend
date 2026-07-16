package br.com.ecofy.ms_notification.adapters.in.kafka;

import br.com.ecofy.ms_notification.adapters.in.kafka.dto.InsightCreatedEventMessage;
import br.com.ecofy.ms_notification.adapters.in.kafka.mapper.InboundEventMapper;
import br.com.ecofy.ms_notification.core.application.command.HandleDomainEventCommand;
import br.com.ecofy.ms_notification.core.domain.enums.DomainEventType;
import br.com.ecofy.ms_notification.core.port.in.HandleDomainEventNotificationUseCase;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class InsightCreatedEventConsumerTest {

    private final InboundEventMapper mapper = mock(InboundEventMapper.class);
    private final HandleDomainEventNotificationUseCase useCase = mock(HandleDomainEventNotificationUseCase.class);
    private final InsightCreatedEventConsumer consumer = new InsightCreatedEventConsumer(mapper, useCase);

    private InsightCreatedEventMessage message() {
        return new InsightCreatedEventMessage(UUID.randomUUID(), UUID.randomUUID(),
                "SPENDING_SPIKE", "2026-01-01", "2026-01-31", null);
    }

    @Test
    void consume_shouldMapAndDelegateToUseCase() {
        var msg = message();
        var cmd = new HandleDomainEventCommand(DomainEventType.INSIGHT_CREATED, msg.userId(), Map.of(), "evt-2");
        when(mapper.fromInsightCreated(msg)).thenReturn(cmd);

        consumer.consume(msg, "eco.insight.created", 0, 5L);

        verify(mapper).fromInsightCreated(msg);
        verify(useCase).handle(cmd);
    }

    @Test
    void consume_shouldThrowWhenMessageIsNull() {
        assertThatThrownBy(() -> consumer.consume(null, "eco.insight.created", 0, 5L))
                .isInstanceOf(NullPointerException.class);
        verifyNoInteractions(mapper, useCase);
    }

    @Test
    void consume_shouldPropagateExceptionFromUseCase() {
        var msg = message();
        var cmd = new HandleDomainEventCommand(DomainEventType.INSIGHT_CREATED, msg.userId(), Map.of(), "evt-2");
        when(mapper.fromInsightCreated(msg)).thenReturn(cmd);
        doThrow(new RuntimeException("insight boom")).when(useCase).handle(any());

        assertThatThrownBy(() -> consumer.consume(msg, "eco.insight.created", 0, 5L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("insight boom");
    }
}
