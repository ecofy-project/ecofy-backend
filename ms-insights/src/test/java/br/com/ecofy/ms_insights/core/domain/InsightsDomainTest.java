package br.com.ecofy.ms_insights.core.domain;

import br.com.ecofy.ms_insights.core.domain.enums.GoalStatus;
import br.com.ecofy.ms_insights.core.domain.enums.PeriodGranularity;
import br.com.ecofy.ms_insights.core.domain.valueobject.Money;
import br.com.ecofy.ms_insights.core.domain.valueobject.Period;
import br.com.ecofy.ms_insights.core.domain.valueobject.UserId;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InsightsDomainTest {

    @Test
    void money_shouldRejectBlankCurrencyAndSupportArithmetic() {
        assertThatThrownBy(() -> new Money(100, " ")).isInstanceOf(IllegalArgumentException.class);

        Money a = new Money(150, "BRL");
        Money b = new Money(50, "BRL");
        assertThat(a.plus(b).cents()).isEqualTo(200);
        assertThat(a.minus(b).cents()).isEqualTo(100);
    }

    @Test
    void money_shouldRejectCurrencyMismatch() {
        assertThatThrownBy(() -> new Money(100, "BRL").plus(new Money(100, "USD")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void period_shouldRejectEndBeforeStart() {
        assertThatThrownBy(() -> new Period(LocalDate.of(2026, 1, 31), LocalDate.of(2026, 1, 1), PeriodGranularity.MONTH))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("end must be >= start");
    }

    @Test
    void period_shouldAcceptValidRange() {
        var p = new Period(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31), PeriodGranularity.MONTH);
        assertThat(p.granularity()).isEqualTo(PeriodGranularity.MONTH);
    }

    @Test
    void goal_withUpdate_shouldPreserveIdAndUserAndCreatedAt() {
        UUID id = UUID.randomUUID();
        var user = new UserId(UUID.randomUUID());
        Instant created = Instant.parse("2026-01-01T00:00:00Z");
        var goal = new Goal(id, user, "Trip", new Money(1000, "BRL"), GoalStatus.ACTIVE, created, created);

        Instant updated = Instant.parse("2026-02-01T00:00:00Z");
        var changed = goal.withUpdate("Trip 2", new Money(2000, "BRL"), GoalStatus.PAUSED, updated);

        assertThat(changed.getId()).isEqualTo(id);
        assertThat(changed.getUserId()).isEqualTo(user);
        assertThat(changed.getCreatedAt()).isEqualTo(created);
        assertThat(changed.getName()).isEqualTo("Trip 2");
        assertThat(changed.getTarget().cents()).isEqualTo(2000);
        assertThat(changed.getStatus()).isEqualTo(GoalStatus.PAUSED);
        assertThat(changed.getUpdatedAt()).isEqualTo(updated);
    }
}
