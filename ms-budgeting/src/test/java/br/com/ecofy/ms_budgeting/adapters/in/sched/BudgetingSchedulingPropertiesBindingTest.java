package br.com.ecofy.ms_budgeting.adapters.in.sched;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.jupiter.api.Assertions.*;

// Garante que as propriedades de agendamento são resolvidas a partir do prefixo unificado.
class BudgetingSchedulingPropertiesBindingTest {

    private BudgetingSchedulingProperties bind(MockEnvironment env) {
        return new Binder(ConfigurationPropertySources.get(env))
                .bind("ecofy.budgeting.scheduling", BudgetingSchedulingProperties.class)
                .orElseGet(BudgetingSchedulingProperties::new);
    }

    @Test
    void shouldBindAllSchedulingPropertiesFromUnifiedPrefix() {
        MockEnvironment env = new MockEnvironment()
                .withProperty("ecofy.budgeting.scheduling.enabled", "true")
                .withProperty("ecofy.budgeting.scheduling.recalculation-enabled", "false")
                .withProperty("ecofy.budgeting.scheduling.cleanup-enabled", "true")
                .withProperty("ecofy.budgeting.scheduling.cleanup-retention-days", "45")
                .withProperty("ecofy.budgeting.scheduling.cleanup-cron", "0 0 4 * * *")
                .withProperty("ecofy.budgeting.scheduling.recalculate-cron", "0 0/30 * * * *");

        BudgetingSchedulingProperties props = bind(env);

        assertTrue(props.isEnabled());
        assertFalse(props.isRecalculationEnabled());
        assertTrue(props.isCleanupEnabled());
        assertEquals(45, props.getCleanupRetentionDays());
        assertEquals("0 0 4 * * *", props.getCleanupCron());
        assertEquals("0 0/30 * * * *", props.getRecalculateCron());
    }

    @Test
    void shouldUseSafeDefaultsWhenNothingConfigured() {
        BudgetingSchedulingProperties props = bind(new MockEnvironment());

        assertTrue(props.isEnabled());
        assertTrue(props.isRecalculationEnabled());
        assertFalse(props.isCleanupEnabled()); // default seguro: não apaga sozinho
        assertEquals(90, props.getCleanupRetentionDays());
        assertEquals("0 0 3 * * *", props.getCleanupCron());
        assertEquals("0 0/15 * * * *", props.getRecalculateCron());
    }
}
