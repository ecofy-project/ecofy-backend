package br.com.ecofy.ms_budgeting.core.application.command;

import br.com.ecofy.ms_budgeting.core.domain.enums.BudgetStatus;
import org.junit.jupiter.api.Test;

import java.lang.reflect.RecordComponent;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class UpdateBudgetCommandTest {

    private static final UUID BUDGET_ID =
            UUID.fromString("11111111-1111-1111-1111-111111111111");

    private static final BigDecimal NEW_LIMIT_AMOUNT =
            new BigDecimal("2500.75");

    private static final String CURRENCY =
            "BRL";

    private static final BudgetStatus STATUS =
            BudgetStatus.values()[0];

    @Test
    void shouldCreateUpdateBudgetCommand() {
        UpdateBudgetCommand command = command();

        assertNotNull(command);
        assertEquals(BUDGET_ID, command.budgetId());
        assertEquals(NEW_LIMIT_AMOUNT, command.newLimitAmount());
        assertEquals(CURRENCY, command.currency());
        assertEquals(STATUS, command.status());
    }

    @Test
    void shouldAllowAllFieldsNullBecauseRecordHasNoValidation() {
        UpdateBudgetCommand command =
                new UpdateBudgetCommand(
                        null,
                        null,
                        null,
                        null
                );

        assertNotNull(command);
        assertNull(command.budgetId());
        assertNull(command.newLimitAmount());
        assertNull(command.currency());
        assertNull(command.status());
    }

    @Test
    void shouldBeEqualWhenAllComponentsAreEqual() {
        UpdateBudgetCommand command = command();
        UpdateBudgetCommand sameCommand = command();

        assertEquals(command, sameCommand);
        assertEquals(command.hashCode(), sameCommand.hashCode());
    }

    @Test
    void shouldReturnTrueWhenComparingSameInstance() {
        UpdateBudgetCommand command = command();

        assertEquals(command, command);
    }

    @Test
    void shouldNotBeEqualWhenBudgetIdIsDifferent() {
        UpdateBudgetCommand command = command();

        UpdateBudgetCommand different =
                new UpdateBudgetCommand(
                        UUID.fromString("22222222-2222-2222-2222-222222222222"),
                        NEW_LIMIT_AMOUNT,
                        CURRENCY,
                        STATUS
                );

        assertNotEquals(command, different);
    }

    @Test
    void shouldNotBeEqualWhenNewLimitAmountIsDifferent() {
        UpdateBudgetCommand command = command();

        UpdateBudgetCommand different =
                new UpdateBudgetCommand(
                        BUDGET_ID,
                        new BigDecimal("999.99"),
                        CURRENCY,
                        STATUS
                );

        assertNotEquals(command, different);
    }

    @Test
    void shouldNotBeEqualWhenCurrencyIsDifferent() {
        UpdateBudgetCommand command = command();

        UpdateBudgetCommand different =
                new UpdateBudgetCommand(
                        BUDGET_ID,
                        NEW_LIMIT_AMOUNT,
                        "USD",
                        STATUS
                );

        assertNotEquals(command, different);
    }

    @Test
    void shouldNotBeEqualWhenStatusIsDifferent() {
        UpdateBudgetCommand command = command();

        UpdateBudgetCommand different =
                new UpdateBudgetCommand(
                        BUDGET_ID,
                        NEW_LIMIT_AMOUNT,
                        CURRENCY,
                        null
                );

        assertNotEquals(command, different);
    }

    @Test
    void shouldBeEqualWhenAllComponentsAreNull() {
        UpdateBudgetCommand command =
                new UpdateBudgetCommand(
                        null,
                        null,
                        null,
                        null
                );

        UpdateBudgetCommand sameCommand =
                new UpdateBudgetCommand(
                        null,
                        null,
                        null,
                        null
                );

        assertEquals(command, sameCommand);
        assertEquals(command.hashCode(), sameCommand.hashCode());
    }

    @Test
    void shouldNotBeEqualWhenOneBudgetIdIsNull() {
        UpdateBudgetCommand command = command();

        UpdateBudgetCommand different =
                new UpdateBudgetCommand(
                        null,
                        NEW_LIMIT_AMOUNT,
                        CURRENCY,
                        STATUS
                );

        assertNotEquals(command, different);
    }

    @Test
    void shouldNotBeEqualWhenOneNewLimitAmountIsNull() {
        UpdateBudgetCommand command = command();

        UpdateBudgetCommand different =
                new UpdateBudgetCommand(
                        BUDGET_ID,
                        null,
                        CURRENCY,
                        STATUS
                );

        assertNotEquals(command, different);
    }

    @Test
    void shouldNotBeEqualWhenOneCurrencyIsNull() {
        UpdateBudgetCommand command = command();

        UpdateBudgetCommand different =
                new UpdateBudgetCommand(
                        BUDGET_ID,
                        NEW_LIMIT_AMOUNT,
                        null,
                        STATUS
                );

        assertNotEquals(command, different);
    }

    @Test
    void shouldNotBeEqualToNull() {
        UpdateBudgetCommand command = command();

        assertNotEquals(null, command);
    }

    @Test
    void shouldNotBeEqualToDifferentType() {
        UpdateBudgetCommand command = command();

        assertNotEquals("not-a-command", command);
    }

    @Test
    void shouldGenerateToStringWithAllComponents() {
        UpdateBudgetCommand command = command();

        String result = command.toString();

        assertNotNull(result);
        assertTrue(result.contains("UpdateBudgetCommand"));
        assertTrue(result.contains("budgetId=" + BUDGET_ID));
        assertTrue(result.contains("newLimitAmount=" + NEW_LIMIT_AMOUNT));
        assertTrue(result.contains("currency=" + CURRENCY));
        assertTrue(result.contains("status=" + STATUS));
    }

    @Test
    void shouldGenerateToStringWithNullComponents() {
        UpdateBudgetCommand command =
                new UpdateBudgetCommand(
                        null,
                        null,
                        null,
                        null
                );

        String result = command.toString();

        assertNotNull(result);
        assertTrue(result.contains("UpdateBudgetCommand"));
        assertTrue(result.contains("budgetId=null"));
        assertTrue(result.contains("newLimitAmount=null"));
        assertTrue(result.contains("currency=null"));
        assertTrue(result.contains("status=null"));
    }

    @Test
    void shouldBeRecord() {
        assertTrue(UpdateBudgetCommand.class.isRecord());
    }

    @Test
    void shouldHaveExpectedRecordComponents() {
        RecordComponent[] components =
                UpdateBudgetCommand.class.getRecordComponents();

        assertNotNull(components);
        assertEquals(4, components.length);

        assertArrayEquals(
                new String[]{
                        "budgetId",
                        "newLimitAmount",
                        "currency",
                        "status"
                },
                Arrays.stream(components)
                        .map(RecordComponent::getName)
                        .toArray(String[]::new)
        );

        assertArrayEquals(
                new Class<?>[]{
                        UUID.class,
                        BigDecimal.class,
                        String.class,
                        BudgetStatus.class
                },
                Arrays.stream(components)
                        .map(RecordComponent::getType)
                        .toArray(Class<?>[]::new)
        );
    }

    private static UpdateBudgetCommand command() {
        return new UpdateBudgetCommand(
                BUDGET_ID,
                NEW_LIMIT_AMOUNT,
                CURRENCY,
                STATUS
        );
    }
}