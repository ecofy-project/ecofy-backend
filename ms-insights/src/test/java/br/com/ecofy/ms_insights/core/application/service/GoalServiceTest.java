package br.com.ecofy.ms_insights.core.application.service;

import br.com.ecofy.ms_insights.core.application.command.CreateGoalCommand;
import br.com.ecofy.ms_insights.core.application.command.UpdateGoalCommand;
import br.com.ecofy.ms_insights.core.application.result.GoalResult;
import br.com.ecofy.ms_insights.core.domain.Goal;
import br.com.ecofy.ms_insights.core.domain.enums.GoalStatus;
import br.com.ecofy.ms_insights.core.domain.exception.BusinessValidationException;
import br.com.ecofy.ms_insights.core.domain.exception.GoalNotFoundException;
import br.com.ecofy.ms_insights.core.domain.valueobject.Money;
import br.com.ecofy.ms_insights.core.domain.valueobject.UserId;
import br.com.ecofy.ms_insights.core.port.out.LoadGoalsPort;
import br.com.ecofy.ms_insights.core.port.out.SaveGoalPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class GoalServiceTest {

    private LoadGoalsPort loadGoalsPort;
    private SaveGoalPort saveGoalPort;
    private GoalService service;

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-01-15T10:00:00Z"), ZoneOffset.UTC);

    @BeforeEach
    void setUp() {
        loadGoalsPort = mock(LoadGoalsPort.class);
        saveGoalPort = mock(SaveGoalPort.class);
        service = new GoalService(loadGoalsPort, saveGoalPort, CLOCK);
        when(saveGoalPort.save(any(Goal.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void create_shouldPersistAndReturnResult() {
        UUID userId = UUID.randomUUID();
        GoalResult result = service.create(new CreateGoalCommand(userId, "Trip", 100_000L, "brl", GoalStatus.ACTIVE));

        assertThat(result.userId()).isEqualTo(userId);
        assertThat(result.targetCents()).isEqualTo(100_000L);
        assertThat(result.currency()).isEqualTo("BRL"); // normalizado uppercase
        assertThat(result.status()).isEqualTo(GoalStatus.ACTIVE);
        verify(saveGoalPort).save(any(Goal.class));
    }

    @Test
    void create_shouldDefaultStatusToActive_whenNull() {
        GoalResult result = service.create(new CreateGoalCommand(UUID.randomUUID(), "Trip", 100L, "BRL", null));
        assertThat(result.status()).isEqualTo(GoalStatus.ACTIVE);
    }

    @Test
    void create_shouldRejectInvalidCurrencyLength() {
        assertThatThrownBy(() -> service.create(new CreateGoalCommand(UUID.randomUUID(), "Trip", 100L, "REAIS", GoalStatus.ACTIVE)))
                .isInstanceOf(BusinessValidationException.class);
    }

    @Test
    void create_shouldRejectNegativeTarget() {
        assertThatThrownBy(() -> service.create(new CreateGoalCommand(UUID.randomUUID(), "Trip", -1L, "BRL", GoalStatus.ACTIVE)))
                .isInstanceOf(BusinessValidationException.class);
    }

    @Test
    void update_shouldThrowNotFound_whenMissing() {
        UUID goalId = UUID.randomUUID();
        when(loadGoalsPort.findById(goalId)).thenReturn(null);

        assertThatThrownBy(() -> service.update(new UpdateGoalCommand(goalId, "x", null, null, null)))
                .isInstanceOf(GoalNotFoundException.class);
    }

    @Test
    void update_shouldRejectTargetCentsWithoutCurrency() {
        UUID goalId = UUID.randomUUID();
        var existing = new Goal(goalId, new UserId(UUID.randomUUID()), "Trip",
                new Money(1000, "BRL"), GoalStatus.ACTIVE, Instant.now(CLOCK), Instant.now(CLOCK));
        when(loadGoalsPort.findById(goalId)).thenReturn(existing);

        assertThatThrownBy(() -> service.update(new UpdateGoalCommand(goalId, null, 5000L, null, null)))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("both targetCents and currency");
    }

    @Test
    void update_shouldApplyMergedFields() {
        UUID goalId = UUID.randomUUID();
        var existing = new Goal(goalId, new UserId(UUID.randomUUID()), "Trip",
                new Money(1000, "BRL"), GoalStatus.ACTIVE, Instant.now(CLOCK), Instant.now(CLOCK));
        when(loadGoalsPort.findById(goalId)).thenReturn(existing);

        GoalResult r = service.update(new UpdateGoalCommand(goalId, "Trip2", 5000L, "usd", GoalStatus.PAUSED));

        assertThat(r.name()).isEqualTo("Trip2");
        assertThat(r.targetCents()).isEqualTo(5000L);
        assertThat(r.currency()).isEqualTo("USD");
        assertThat(r.status()).isEqualTo(GoalStatus.PAUSED);
    }

    @Test
    void get_shouldThrowNotFound_whenMissing() {
        UUID goalId = UUID.randomUUID();
        when(loadGoalsPort.findById(goalId)).thenReturn(null);
        assertThatThrownBy(() -> service.get(goalId)).isInstanceOf(GoalNotFoundException.class);
    }
}
