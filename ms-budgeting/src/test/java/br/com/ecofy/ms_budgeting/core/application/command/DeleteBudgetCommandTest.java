package br.com.ecofy.ms_budgeting.core.application.command;

import org.junit.jupiter.api.Test;

import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class DeleteBudgetCommandTest {

    private static final UUID BUDGET_ID =
            UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Test
    void shouldCreateDeleteBudgetCommand() {
        DeleteBudgetCommand command =
                new DeleteBudgetCommand(BUDGET_ID);

        assertNotNull(command);
        assertEquals(BUDGET_ID, command.budgetId());
    }

    @Test
    void shouldAllowNullBudgetIdBecauseRecordHasNoValidation() {
        DeleteBudgetCommand command =
                new DeleteBudgetCommand(null);

        assertNotNull(command);
        assertNull(command.budgetId());
    }

    @Test
    void shouldBeEqualWhenAllComponentsAreEqual() {
        DeleteBudgetCommand command =
                new DeleteBudgetCommand(BUDGET_ID);

        DeleteBudgetCommand sameCommand =
                new DeleteBudgetCommand(BUDGET_ID);

        assertEquals(command, sameCommand);
        assertEquals(command.hashCode(), sameCommand.hashCode());
    }

    @Test
    void shouldReturnTrueWhenComparingSameInstance() {
        DeleteBudgetCommand command =
                new DeleteBudgetCommand(BUDGET_ID);

        assertEquals(command, command);
    }

    @Test
    void shouldNotBeEqualWhenBudgetIdIsDifferent() {
        DeleteBudgetCommand command =
                new DeleteBudgetCommand(BUDGET_ID);

        DeleteBudgetCommand different =
                new DeleteBudgetCommand(
                        UUID.fromString("22222222-2222-2222-2222-222222222222")
                );

        assertNotEquals(command, different);
    }

    @Test
    void shouldNotBeEqualWhenOneBudgetIdIsNull() {
        DeleteBudgetCommand command =
                new DeleteBudgetCommand(BUDGET_ID);

        DeleteBudgetCommand different =
                new DeleteBudgetCommand(null);

        assertNotEquals(command, different);
    }

    @Test
    void shouldBeEqualWhenBothBudgetIdsAreNull() {
        DeleteBudgetCommand command =
                new DeleteBudgetCommand(null);

        DeleteBudgetCommand sameCommand =
                new DeleteBudgetCommand(null);

        assertEquals(command, sameCommand);
        assertEquals(command.hashCode(), sameCommand.hashCode());
    }

    @Test
    void shouldNotBeEqualToNull() {
        DeleteBudgetCommand command =
                new DeleteBudgetCommand(BUDGET_ID);

        assertNotEquals(null, command);
    }

    @Test
    void shouldNotBeEqualToDifferentType() {
        DeleteBudgetCommand command =
                new DeleteBudgetCommand(BUDGET_ID);

        assertNotEquals("not-a-command", command);
    }

    @Test
    void shouldGenerateToStringWithBudgetId() {
        DeleteBudgetCommand command =
                new DeleteBudgetCommand(BUDGET_ID);

        String result = command.toString();

        assertNotNull(result);
        assertTrue(result.contains("DeleteBudgetCommand"));
        assertTrue(result.contains("budgetId=" + BUDGET_ID));
    }

    @Test
    void shouldGenerateToStringWithNullBudgetId() {
        DeleteBudgetCommand command =
                new DeleteBudgetCommand(null);

        String result = command.toString();

        assertNotNull(result);
        assertTrue(result.contains("DeleteBudgetCommand"));
        assertTrue(result.contains("budgetId=null"));
    }

    @Test
    void shouldBeRecord() {
        assertTrue(DeleteBudgetCommand.class.isRecord());
    }

    @Test
    void shouldHaveExpectedRecordComponents() {
        RecordComponent[] components =
                DeleteBudgetCommand.class.getRecordComponents();

        assertNotNull(components);
        assertEquals(1, components.length);

        assertArrayEquals(
                new String[]{
                        "budgetId"
                },
                Arrays.stream(components)
                        .map(RecordComponent::getName)
                        .toArray(String[]::new)
        );

        assertArrayEquals(
                new Class<?>[]{
                        UUID.class
                },
                Arrays.stream(components)
                        .map(RecordComponent::getType)
                        .toArray(Class<?>[]::new)
        );
    }
}