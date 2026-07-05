package br.com.ecofy.ms_budgeting.core.application.command;

import org.junit.jupiter.api.Test;

import java.lang.reflect.RecordComponent;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class RecalculateBudgetsCommandTest {

    private static final UUID RUN_ID =
            UUID.fromString("11111111-1111-1111-1111-111111111111");

    private static final UUID USER_ID =
            UUID.fromString("22222222-2222-2222-2222-222222222222");

    private static final LocalDate REFERENCE_DATE =
            LocalDate.of(2026, 7, 5);

    @Test
    void shouldCreateRecalculateBudgetsCommandForSpecificUserUsingConstructor() {
        RecalculateBudgetsCommand command =
                new RecalculateBudgetsCommand(RUN_ID, USER_ID, REFERENCE_DATE);

        assertNotNull(command);
        assertEquals(RUN_ID, command.runId());
        assertEquals(USER_ID, command.userId());
        assertEquals(REFERENCE_DATE, command.referenceDate());
    }

    @Test
    void shouldCreateRecalculateBudgetsCommandForAllUsersUsingConstructor() {
        RecalculateBudgetsCommand command =
                new RecalculateBudgetsCommand(RUN_ID, null, REFERENCE_DATE);

        assertNotNull(command);
        assertEquals(RUN_ID, command.runId());
        assertNull(command.userId());
        assertEquals(REFERENCE_DATE, command.referenceDate());
    }

    @Test
    void shouldThrowIllegalArgumentExceptionWhenRunIdIsNull() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new RecalculateBudgetsCommand(null, USER_ID, REFERENCE_DATE)
        );

        assertEquals("runId must not be null", exception.getMessage());
    }

    @Test
    void shouldThrowIllegalArgumentExceptionWhenReferenceDateIsNull() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new RecalculateBudgetsCommand(RUN_ID, USER_ID, null)
        );

        assertEquals("referenceDate must not be null", exception.getMessage());
    }

    @Test
    void shouldPrioritizeRunIdValidationBeforeReferenceDateValidation() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new RecalculateBudgetsCommand(null, USER_ID, null)
        );

        assertEquals("runId must not be null", exception.getMessage());
    }

    @Test
    void shouldCreateCommandForAllUsersUsingFactoryMethod() {
        RecalculateBudgetsCommand command =
                RecalculateBudgetsCommand.forAllUsers(RUN_ID, REFERENCE_DATE);

        assertNotNull(command);
        assertEquals(RUN_ID, command.runId());
        assertNull(command.userId());
        assertEquals(REFERENCE_DATE, command.referenceDate());
    }

    @Test
    void shouldThrowIllegalArgumentExceptionWhenForAllUsersRunIdIsNull() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> RecalculateBudgetsCommand.forAllUsers(null, REFERENCE_DATE)
        );

        assertEquals("runId must not be null", exception.getMessage());
    }

    @Test
    void shouldThrowIllegalArgumentExceptionWhenForAllUsersReferenceDateIsNull() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> RecalculateBudgetsCommand.forAllUsers(RUN_ID, null)
        );

        assertEquals("referenceDate must not be null", exception.getMessage());
    }

    @Test
    void shouldCreateCommandForUserUsingFactoryMethod() {
        RecalculateBudgetsCommand command =
                RecalculateBudgetsCommand.forUser(RUN_ID, USER_ID, REFERENCE_DATE);

        assertNotNull(command);
        assertEquals(RUN_ID, command.runId());
        assertEquals(USER_ID, command.userId());
        assertEquals(REFERENCE_DATE, command.referenceDate());
    }

    @Test
    void shouldThrowIllegalArgumentExceptionWhenForUserUserIdIsNull() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> RecalculateBudgetsCommand.forUser(RUN_ID, null, REFERENCE_DATE)
        );

        assertEquals("userId must not be null", exception.getMessage());
    }

    @Test
    void shouldThrowIllegalArgumentExceptionWhenForUserRunIdIsNull() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> RecalculateBudgetsCommand.forUser(null, USER_ID, REFERENCE_DATE)
        );

        assertEquals("runId must not be null", exception.getMessage());
    }

    @Test
    void shouldThrowIllegalArgumentExceptionWhenForUserReferenceDateIsNull() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> RecalculateBudgetsCommand.forUser(RUN_ID, USER_ID, null)
        );

        assertEquals("referenceDate must not be null", exception.getMessage());
    }

    @Test
    void shouldPrioritizeUserIdValidationInsideForUser() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> RecalculateBudgetsCommand.forUser(null, null, null)
        );

        assertEquals("userId must not be null", exception.getMessage());
    }

    @Test
    void shouldBeEqualWhenAllComponentsAreEqual() {
        RecalculateBudgetsCommand command =
                new RecalculateBudgetsCommand(RUN_ID, USER_ID, REFERENCE_DATE);

        RecalculateBudgetsCommand sameCommand =
                new RecalculateBudgetsCommand(RUN_ID, USER_ID, REFERENCE_DATE);

        assertEquals(command, sameCommand);
        assertEquals(command.hashCode(), sameCommand.hashCode());
    }

    @Test
    void shouldBeEqualWhenUserIdIsNullInBothCommands() {
        RecalculateBudgetsCommand command =
                new RecalculateBudgetsCommand(RUN_ID, null, REFERENCE_DATE);

        RecalculateBudgetsCommand sameCommand =
                new RecalculateBudgetsCommand(RUN_ID, null, REFERENCE_DATE);

        assertEquals(command, sameCommand);
        assertEquals(command.hashCode(), sameCommand.hashCode());
    }

    @Test
    void shouldReturnTrueWhenComparingSameInstance() {
        RecalculateBudgetsCommand command =
                new RecalculateBudgetsCommand(RUN_ID, USER_ID, REFERENCE_DATE);

        assertEquals(command, command);
    }

    @Test
    void shouldNotBeEqualWhenRunIdIsDifferent() {
        RecalculateBudgetsCommand command =
                new RecalculateBudgetsCommand(RUN_ID, USER_ID, REFERENCE_DATE);

        RecalculateBudgetsCommand different =
                new RecalculateBudgetsCommand(
                        UUID.fromString("33333333-3333-3333-3333-333333333333"),
                        USER_ID,
                        REFERENCE_DATE
                );

        assertNotEquals(command, different);
    }

    @Test
    void shouldNotBeEqualWhenUserIdIsDifferent() {
        RecalculateBudgetsCommand command =
                new RecalculateBudgetsCommand(RUN_ID, USER_ID, REFERENCE_DATE);

        RecalculateBudgetsCommand different =
                new RecalculateBudgetsCommand(
                        RUN_ID,
                        UUID.fromString("44444444-4444-4444-4444-444444444444"),
                        REFERENCE_DATE
                );

        assertNotEquals(command, different);
    }

    @Test
    void shouldNotBeEqualWhenOneUserIdIsNull() {
        RecalculateBudgetsCommand command =
                new RecalculateBudgetsCommand(RUN_ID, USER_ID, REFERENCE_DATE);

        RecalculateBudgetsCommand different =
                new RecalculateBudgetsCommand(RUN_ID, null, REFERENCE_DATE);

        assertNotEquals(command, different);
    }

    @Test
    void shouldNotBeEqualWhenReferenceDateIsDifferent() {
        RecalculateBudgetsCommand command =
                new RecalculateBudgetsCommand(RUN_ID, USER_ID, REFERENCE_DATE);

        RecalculateBudgetsCommand different =
                new RecalculateBudgetsCommand(
                        RUN_ID,
                        USER_ID,
                        REFERENCE_DATE.plusDays(1)
                );

        assertNotEquals(command, different);
    }

    @Test
    void shouldNotBeEqualToNull() {
        RecalculateBudgetsCommand command =
                new RecalculateBudgetsCommand(RUN_ID, USER_ID, REFERENCE_DATE);

        assertNotEquals(null, command);
    }

    @Test
    void shouldNotBeEqualToDifferentType() {
        RecalculateBudgetsCommand command =
                new RecalculateBudgetsCommand(RUN_ID, USER_ID, REFERENCE_DATE);

        assertNotEquals("not-a-command", command);
    }

    @Test
    void shouldGenerateToStringWithAllComponents() {
        RecalculateBudgetsCommand command =
                new RecalculateBudgetsCommand(RUN_ID, USER_ID, REFERENCE_DATE);

        String result = command.toString();

        assertNotNull(result);
        assertTrue(result.contains("RecalculateBudgetsCommand"));
        assertTrue(result.contains("runId=" + RUN_ID));
        assertTrue(result.contains("userId=" + USER_ID));
        assertTrue(result.contains("referenceDate=" + REFERENCE_DATE));
    }

    @Test
    void shouldGenerateToStringWhenUserIdIsNull() {
        RecalculateBudgetsCommand command =
                new RecalculateBudgetsCommand(RUN_ID, null, REFERENCE_DATE);

        String result = command.toString();

        assertNotNull(result);
        assertTrue(result.contains("RecalculateBudgetsCommand"));
        assertTrue(result.contains("runId=" + RUN_ID));
        assertTrue(result.contains("userId=null"));
        assertTrue(result.contains("referenceDate=" + REFERENCE_DATE));
    }

    @Test
    void shouldBeRecord() {
        assertTrue(RecalculateBudgetsCommand.class.isRecord());
    }

    @Test
    void shouldHaveExpectedRecordComponents() {
        RecordComponent[] components =
                RecalculateBudgetsCommand.class.getRecordComponents();

        assertNotNull(components);
        assertEquals(3, components.length);

        assertArrayEquals(
                new String[]{
                        "runId",
                        "userId",
                        "referenceDate"
                },
                Arrays.stream(components)
                        .map(RecordComponent::getName)
                        .toArray(String[]::new)
        );

        assertArrayEquals(
                new Class<?>[]{
                        UUID.class,
                        UUID.class,
                        LocalDate.class
                },
                Arrays.stream(components)
                        .map(RecordComponent::getType)
                        .toArray(Class<?>[]::new)
        );
    }
}