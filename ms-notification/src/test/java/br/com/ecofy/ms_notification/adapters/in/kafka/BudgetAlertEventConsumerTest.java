package br.com.ecofy.ms_notification.adapters.in.kafka;

import br.com.ecofy.ms_notification.adapters.in.kafka.dto.BudgetAlertEventMessage;
import br.com.ecofy.ms_notification.adapters.in.kafka.mapper.InboundEventMapper;
import br.com.ecofy.ms_notification.core.application.command.HandleDomainEventCommand;
import br.com.ecofy.ms_notification.core.domain.enums.DomainEventType;
import br.com.ecofy.ms_notification.core.port.in.HandleDomainEventNotificationUseCase;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class BudgetAlertEventConsumerTest {

    private final InboundEventMapper mapper = mock(InboundEventMapper.class);
    private final HandleDomainEventNotificationUseCase useCase = mock(HandleDomainEventNotificationUseCase.class);
    private final BudgetAlertEventConsumer consumer = new BudgetAlertEventConsumer(mapper, useCase);

    private BudgetAlertEventMessage message() {
        return new BudgetAlertEventMessage(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                new BigDecimal("1000.00"), new BigDecimal("800.00"), 80, "WARNING", null);
    }

    @Test
    void consume_shouldMapAndDelegateToUseCase() {
        var msg = message();
        var cmd = new HandleDomainEventCommand(DomainEventType.BUDGET_ALERT, msg.userId(), Map.of(), "evt-1");
        when(mapper.fromBudgetAlert(msg)).thenReturn(cmd);

        consumer.consume(msg, "eco.budget.alert", 0, 1L);

        verify(mapper).fromBudgetAlert(msg);
        verify(useCase).handle(cmd);
    }

    @Test
    void consume_shouldThrowWhenMessageIsNull() {
        assertThatThrownBy(() -> consumer.consume(null, "eco.budget.alert", 0, 1L))
                .isInstanceOf(NullPointerException.class);
        verifyNoInteractions(mapper, useCase);
    }

    @Test
    void consume_shouldPropagateExceptionFromUseCase() {
        var msg = message();
        var cmd = new HandleDomainEventCommand(DomainEventType.BUDGET_ALERT, msg.userId(), Map.of(), "evt-1");
        when(mapper.fromBudgetAlert(msg)).thenReturn(cmd);
        doThrow(new RuntimeException("handler boom")).when(useCase).handle(any());

        assertThatThrownBy(() -> consumer.consume(msg, "eco.budget.alert", 0, 1L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("handler boom");
    }
}
