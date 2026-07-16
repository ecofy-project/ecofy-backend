package br.com.ecofy.ms_budgeting.core.application.service;

import br.com.ecofy.ms_budgeting.core.application.result.BudgetOverviewResult;
import br.com.ecofy.ms_budgeting.core.domain.Budget;
import br.com.ecofy.ms_budgeting.core.domain.BudgetConsumption;
import br.com.ecofy.ms_budgeting.core.domain.enums.BudgetPeriodType;
import br.com.ecofy.ms_budgeting.core.domain.enums.BudgetStatus;
import br.com.ecofy.ms_budgeting.core.domain.enums.ConsumptionSource;
import br.com.ecofy.ms_budgeting.core.domain.valueobject.BudgetKey;
import br.com.ecofy.ms_budgeting.core.domain.valueobject.CategoryId;
import br.com.ecofy.ms_budgeting.core.domain.valueobject.Money;
import br.com.ecofy.ms_budgeting.core.domain.valueobject.Period;
import br.com.ecofy.ms_budgeting.core.domain.valueobject.UserId;
import br.com.ecofy.ms_budgeting.core.port.out.LoadBudgetConsumptionPort;
import br.com.ecofy.ms_budgeting.core.port.out.LoadBudgetsPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Currency;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BudgetQueryServiceOverviewTest {

    @Mock private LoadBudgetsPort loadBudgetsPort;
    @Mock private LoadBudgetConsumptionPort loadBudgetConsumptionPort;

    private static final Currency BRL = Currency.getInstance("BRL");
    private static final LocalDate START = LocalDate.of(2026, 1, 1);
    private static final LocalDate END = LocalDate.of(2026, 1, 31);

    private Budget budget(UUID id, UUID userId, BigDecimal limit) {
        return new Budget(
                id,
                new BudgetKey(new UserId(userId), new CategoryId(UUID.randomUUID()), new Period(START, END)),
                BudgetPeriodType.MONTHLY,
                new Money(limit, BRL),
                BudgetStatus.ACTIVE,
                Instant.now(), Instant.now());
    }

    @Test
    void overview_shouldReturnRealConsumption_notZeroStub() {
        var service = new BudgetQueryService(loadBudgetsPort, loadBudgetConsumptionPort);

        UUID userId = UUID.randomUUID();
        UUID budgetId = UUID.randomUUID();
        Budget b = budget(budgetId, userId, new BigDecimal("1000.00"));
        when(loadBudgetsPort.findByUserId(userId)).thenReturn(List.of(b));

        BudgetConsumption consumption = new BudgetConsumption(
                UUID.randomUUID(), budgetId, START, END,
                new Money(new BigDecimal("250.00"), BRL), ConsumptionSource.CATEGORIZED_TX,
                Instant.now(), Instant.now());
        when(loadBudgetConsumptionPort.findByBudgetAndPeriod(budgetId, START, END))
                .thenReturn(Optional.of(consumption));

        BudgetOverviewResult overview = service.overview(userId);

        assertEquals(1, overview.consumptions().size());
        var c = overview.consumptions().get(0);
        assertEquals(new BigDecimal("250.00"), c.consumedAmount());   // real, não zero
        assertEquals(new BigDecimal("1000.00"), c.limitAmount());
        assertEquals(0, c.consumedPct().compareTo(new BigDecimal("25.00"))); // 250/1000 = 25%
    }

    @Test
    void overview_shouldReturnZero_whenNoConsumptionYet() {
        var service = new BudgetQueryService(loadBudgetsPort, loadBudgetConsumptionPort);

        UUID userId = UUID.randomUUID();
        UUID budgetId = UUID.randomUUID();
        when(loadBudgetsPort.findByUserId(userId)).thenReturn(List.of(budget(budgetId, userId, new BigDecimal("500.00"))));
        when(loadBudgetConsumptionPort.findByBudgetAndPeriod(budgetId, START, END)).thenReturn(Optional.empty());

        BudgetOverviewResult overview = service.overview(userId);

        assertEquals(0, overview.consumptions().get(0).consumedAmount().compareTo(BigDecimal.ZERO));
    }
}
