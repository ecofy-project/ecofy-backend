package br.com.ecofy.ms_budgeting.adapters.out.persistence.mapper;

import br.com.ecofy.ms_budgeting.adapters.out.persistence.entity.BudgetConsumptionEntity;
import br.com.ecofy.ms_budgeting.core.domain.BudgetConsumption;
import br.com.ecofy.ms_budgeting.core.domain.enums.ConsumptionSource;
import br.com.ecofy.ms_budgeting.core.domain.valueobject.Money;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Currency;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Correção Dia 6: reference_date (NOT NULL no schema) deve ser preenchido no toEntity
 * (antes ficava null e todo save quebrava por violação de NOT NULL).
 */
class BudgetConsumptionReferenceDateTest {

    @Test
    void toEntity_shouldSetReferenceDateFromPeriodEnd() {
        LocalDate start = LocalDate.of(2026, 1, 1);
        LocalDate end = LocalDate.of(2026, 1, 31);
        Instant now = Instant.parse("2026-01-15T10:00:00Z");

        BudgetConsumption domain = new BudgetConsumption(
                UUID.randomUUID(),
                UUID.randomUUID(),
                start,
                end,
                new Money(new BigDecimal("12.50"), Currency.getInstance("BRL")),
                ConsumptionSource.CATEGORIZED_TX,
                now,
                now
        );

        BudgetConsumptionEntity entity = BudgetConsumptionMapper.toEntity(domain);

        assertNotNull(entity.getReferenceDate(), "reference_date não pode ser null (NOT NULL no schema)");
        assertEquals(end, entity.getReferenceDate());
    }
}
