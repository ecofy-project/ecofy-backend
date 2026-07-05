package br.com.ecofy.ms_budgeting.core.application.command;

import br.com.ecofy.ms_budgeting.core.domain.enums.BudgetPeriodType;
import br.com.ecofy.ms_budgeting.core.domain.enums.BudgetStatus;
import org.junit.jupiter.api.Test;

import java.lang.reflect.RecordComponent;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class CreateBudgetCommandTest {

    private static final UUID USER_ID =
            UUID.fromString("11111111-1111-1111-1111-111111111111");

    private static final UUID CATEGORY_ID =
            UUID.fromString("22222222-2222-2222-2222-222222222222");

    private static final BudgetPeriodType PERIOD_TYPE =
            BudgetPeriodType.values()[0];

    private static final LocalDate PERIOD_START =
            LocalDate.of(2026, 7, 1);

    private static final LocalDate PERIOD_END =
            LocalDate.of(2026, 7, 31);

    private static final BigDecimal LIMIT_AMOUNT =
            new BigDecimal("1500.75");

    private static final String CURRENCY =
            "BRL";

    private static final BudgetStatus STATUS =
            BudgetStatus.values()[0];

    @Test
    void shouldCreateCreateBudgetCommand() {
        CreateBudgetCommand command = command();

        assertNotNull(command);
        assertEquals(USER_ID, command.userId());
        assertEquals(CATEGORY_ID, command.categoryId());
        assertEquals(PERIOD_TYPE, command.periodType());
        assertEquals(PERIOD_START, command.periodStart());
        assertEquals(PERIOD_END, command.periodEnd());
        assertEquals(LIMIT_AMOUNT, command.limitAmount());
        assertEquals(CURRENCY, command.currency());
        assertEquals(STATUS, command.status());
    }

    @Test
    void shouldAllowAllFieldsNullBecauseRecordHasNoValidation() {
        CreateBudgetCommand command = new CreateBudgetCommand(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );

        assertNotNull(command);
        assertNull(command.userId());
        assertNull(command.categoryId());
        assertNull(command.periodType());
        assertNull(command.periodStart());
        assertNull(command.periodEnd());
        assertNull(command.limitAmount());
        assertNull(command.currency());
        assertNull(command.status());
    }

    @Test
    void shouldBeEqualWhenAllComponentsAreEqual() {
        CreateBudgetCommand command = command();
        CreateBudgetCommand sameCommand = command();

        assertEquals(command, sameCommand);
        assertEquals(command.hashCode(), sameCommand.hashCode());
    }

    @Test
    void shouldReturnTrueWhenComparingSameInstance() {
        CreateBudgetCommand command = command();

        assertEquals(command, command);
    }

    @Test
    void shouldNotBeEqualWhenUserIdIsDifferent() {
        CreateBudgetCommand command = command();

        CreateBudgetCommand different = new CreateBudgetCommand(
                UUID.fromString("33333333-3333-3333-3333-333333333333"),
                CATEGORY_ID,
                PERIOD_TYPE,
                PERIOD_START,
                PERIOD_END,
                LIMIT_AMOUNT,
                CURRENCY,
                STATUS
        );

        assertNotEquals(command, different);
    }

    @Test
    void shouldNotBeEqualWhenCategoryIdIsDifferent() {
        CreateBudgetCommand command = command();

        CreateBudgetCommand different = new CreateBudgetCommand(
                USER_ID,
                UUID.fromString("44444444-4444-4444-4444-444444444444"),
                PERIOD_TYPE,
                PERIOD_START,
                PERIOD_END,
                LIMIT_AMOUNT,
                CURRENCY,
                STATUS
        );

        assertNotEquals(command, different);
    }

    @Test
    void shouldNotBeEqualWhenPeriodTypeIsDifferent() {
        CreateBudgetCommand command = command();

        CreateBudgetCommand different = new CreateBudgetCommand(
                USER_ID,
                CATEGORY_ID,
                null,
                PERIOD_START,
                PERIOD_END,
                LIMIT_AMOUNT,
                CURRENCY,
                STATUS
        );

        assertNotEquals(command, different);
    }

    @Test
    void shouldNotBeEqualWhenPeriodStartIsDifferent() {
        CreateBudgetCommand command = command();

        CreateBudgetCommand different = new CreateBudgetCommand(
                USER_ID,
                CATEGORY_ID,
                PERIOD_TYPE,
                PERIOD_START.plusDays(1),
                PERIOD_END,
                LIMIT_AMOUNT,
                CURRENCY,
                STATUS
        );

        assertNotEquals(command, different);
    }

    @Test
    void shouldNotBeEqualWhenPeriodEndIsDifferent() {
        CreateBudgetCommand command = command();

        CreateBudgetCommand different = new CreateBudgetCommand(
                USER_ID,
                CATEGORY_ID,
                PERIOD_TYPE,
                PERIOD_START,
                PERIOD_END.plusDays(1),
                LIMIT_AMOUNT,
                CURRENCY,
                STATUS
        );

        assertNotEquals(command, different);
    }

    @Test
    void shouldNotBeEqualWhenLimitAmountIsDifferent() {
        CreateBudgetCommand command = command();

        CreateBudgetCommand different = new CreateBudgetCommand(
                USER_ID,
                CATEGORY_ID,
                PERIOD_TYPE,
                PERIOD_START,
                PERIOD_END,
                new BigDecimal("999.99"),
                CURRENCY,
                STATUS
        );

        assertNotEquals(command, different);
    }

    @Test
    void shouldNotBeEqualWhenCurrencyIsDifferent() {
        CreateBudgetCommand command = command();

        CreateBudgetCommand different = new CreateBudgetCommand(
                USER_ID,
                CATEGORY_ID,
                PERIOD_TYPE,
                PERIOD_START,
                PERIOD_END,
                LIMIT_AMOUNT,
                "USD",
                STATUS
        );

        assertNotEquals(command, different);
    }

    @Test
    void shouldNotBeEqualWhenStatusIsDifferent() {
        CreateBudgetCommand command = command();

        CreateBudgetCommand different = new CreateBudgetCommand(
                USER_ID,
                CATEGORY_ID,
                PERIOD_TYPE,
                PERIOD_START,
                PERIOD_END,
                LIMIT_AMOUNT,
                CURRENCY,
                null
        );

        assertNotEquals(command, different);
    }

    @Test
    void shouldNotBeEqualToNull() {
        CreateBudgetCommand command = command();

        assertNotEquals(null, command);
    }

    @Test
    void shouldNotBeEqualToDifferentType() {
        CreateBudgetCommand command = command();

        assertNotEquals("not-a-command", command);
    }

    @Test
    void shouldGenerateToStringWithAllComponents() {
        CreateBudgetCommand command = command();

        String result = command.toString();

        assertNotNull(result);
        assertTrue(result.contains("CreateBudgetCommand"));
        assertTrue(result.contains("userId=" + USER_ID));
        assertTrue(result.contains("categoryId=" + CATEGORY_ID));
        assertTrue(result.contains("periodType=" + PERIOD_TYPE));
        assertTrue(result.contains("periodStart=" + PERIOD_START));
        assertTrue(result.contains("periodEnd=" + PERIOD_END));
        assertTrue(result.contains("limitAmount=" + LIMIT_AMOUNT));
        assertTrue(result.contains("currency=" + CURRENCY));
        assertTrue(result.contains("status=" + STATUS));
    }

    @Test
    void shouldGenerateToStringWithNullComponents() {
        CreateBudgetCommand command = new CreateBudgetCommand(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );

        String result = command.toString();

        assertNotNull(result);
        assertTrue(result.contains("CreateBudgetCommand"));
        assertTrue(result.contains("userId=null"));
        assertTrue(result.contains("categoryId=null"));
        assertTrue(result.contains("periodType=null"));
        assertTrue(result.contains("periodStart=null"));
        assertTrue(result.contains("periodEnd=null"));
        assertTrue(result.contains("limitAmount=null"));
        assertTrue(result.contains("currency=null"));
        assertTrue(result.contains("status=null"));
    }

    @Test
    void shouldBeRecord() {
        assertTrue(CreateBudgetCommand.class.isRecord());
    }

    @Test
    void shouldHaveExpectedRecordComponents() {
        RecordComponent[] components =
                CreateBudgetCommand.class.getRecordComponents();

        assertNotNull(components);
        assertEquals(8, components.length);

        assertArrayEquals(
                new String[]{
                        "userId",
                        "categoryId",
                        "periodType",
                        "periodStart",
                        "periodEnd",
                        "limitAmount",
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
                        UUID.class,
                        BudgetPeriodType.class,
                        LocalDate.class,
                        LocalDate.class,
                        BigDecimal.class,
                        String.class,
                        BudgetStatus.class
                },
                Arrays.stream(components)
                        .map(RecordComponent::getType)
                        .toArray(Class<?>[]::new)
        );
    }

    private static CreateBudgetCommand command() {
        return new CreateBudgetCommand(
                USER_ID,
                CATEGORY_ID,
                PERIOD_TYPE,
                PERIOD_START,
                PERIOD_END,
                LIMIT_AMOUNT,
                CURRENCY,
                STATUS
        );
    }
}