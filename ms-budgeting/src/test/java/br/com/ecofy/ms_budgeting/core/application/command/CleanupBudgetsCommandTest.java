package br.com.ecofy.ms_budgeting.core.application.command;

import org.junit.jupiter.api.Test;

import java.lang.reflect.RecordComponent;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class CleanupBudgetsCommandTest {

    private static final UUID RUN_ID =
            UUID.fromString("11111111-1111-1111-1111-111111111111");

    private static final LocalDate REFERENCE_DATE =
            LocalDate.of(2026, 7, 5);

    private static final int RETENTION_DAYS = 90;

    @Test
    void shouldCreateCleanupBudgetsCommand() {
        CleanupBudgetsCommand command =
                new CleanupBudgetsCommand(RUN_ID, REFERENCE_DATE, RETENTION_DAYS);

        assertNotNull(command);
        assertEquals(RUN_ID, command.runId());
        assertEquals(REFERENCE_DATE, command.referenceDate());
        assertEquals(RETENTION_DAYS, command.retentionDays());
    }

    @Test
    void shouldCreateCleanupBudgetsCommandWhenRetentionDaysIsZero() {
        CleanupBudgetsCommand command =
                new CleanupBudgetsCommand(RUN_ID, REFERENCE_DATE, 0);

        assertNotNull(command);
        assertEquals(RUN_ID, command.runId());
        assertEquals(REFERENCE_DATE, command.referenceDate());
        assertEquals(0, command.retentionDays());
    }

    @Test
    void shouldThrowIllegalArgumentExceptionWhenRunIdIsNull() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new CleanupBudgetsCommand(null, REFERENCE_DATE, RETENTION_DAYS)
        );

        assertEquals("runId must not be null", exception.getMessage());
    }

    @Test
    void shouldThrowIllegalArgumentExceptionWhenReferenceDateIsNull() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new CleanupBudgetsCommand(RUN_ID, null, RETENTION_DAYS)
        );

        assertEquals("referenceDate must not be null", exception.getMessage());
    }

    @Test
    void shouldThrowIllegalArgumentExceptionWhenRetentionDaysIsNegative() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new CleanupBudgetsCommand(RUN_ID, REFERENCE_DATE, -1)
        );

        assertEquals("retentionDays must be >= 0", exception.getMessage());
    }

    @Test
    void shouldPrioritizeRunIdValidationWhenAllInvalidFieldsAreProvided() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new CleanupBudgetsCommand(null, null, -1)
        );

        assertEquals("runId must not be null", exception.getMessage());
    }

    @Test
    void shouldPrioritizeReferenceDateValidationBeforeRetentionDaysValidation() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new CleanupBudgetsCommand(RUN_ID, null, -1)
        );

        assertEquals("referenceDate must not be null", exception.getMessage());
    }

    @Test
    void shouldBeEqualWhenAllComponentsAreEqual() {
        CleanupBudgetsCommand command =
                new CleanupBudgetsCommand(RUN_ID, REFERENCE_DATE, RETENTION_DAYS);

        CleanupBudgetsCommand sameCommand =
                new CleanupBudgetsCommand(RUN_ID, REFERENCE_DATE, RETENTION_DAYS);

        assertEquals(command, sameCommand);
        assertEquals(command.hashCode(), sameCommand.hashCode());
    }

    @Test
    void shouldReturnTrueWhenComparingSameInstance() {
        CleanupBudgetsCommand command =
                new CleanupBudgetsCommand(RUN_ID, REFERENCE_DATE, RETENTION_DAYS);

        assertEquals(command, command);
    }

    @Test
    void shouldNotBeEqualWhenRunIdIsDifferent() {
        CleanupBudgetsCommand command =
                new CleanupBudgetsCommand(RUN_ID, REFERENCE_DATE, RETENTION_DAYS);

        CleanupBudgetsCommand different =
                new CleanupBudgetsCommand(
                        UUID.fromString("22222222-2222-2222-2222-222222222222"),
                        REFERENCE_DATE,
                        RETENTION_DAYS
                );

        assertNotEquals(command, different);
    }

    @Test
    void shouldNotBeEqualWhenReferenceDateIsDifferent() {
        CleanupBudgetsCommand command =
                new CleanupBudgetsCommand(RUN_ID, REFERENCE_DATE, RETENTION_DAYS);

        CleanupBudgetsCommand different =
                new CleanupBudgetsCommand(
                        RUN_ID,
                        REFERENCE_DATE.plusDays(1),
                        RETENTION_DAYS
                );

        assertNotEquals(command, different);
    }

    @Test
    void shouldNotBeEqualWhenRetentionDaysIsDifferent() {
        CleanupBudgetsCommand command =
                new CleanupBudgetsCommand(RUN_ID, REFERENCE_DATE, RETENTION_DAYS);

        CleanupBudgetsCommand different =
                new CleanupBudgetsCommand(RUN_ID, REFERENCE_DATE, 30);

        assertNotEquals(command, different);
    }

    @Test
    void shouldNotBeEqualToNull() {
        CleanupBudgetsCommand command =
                new CleanupBudgetsCommand(RUN_ID, REFERENCE_DATE, RETENTION_DAYS);

        assertNotEquals(null, command);
    }

    @Test
    void shouldNotBeEqualToDifferentType() {
        CleanupBudgetsCommand command =
                new CleanupBudgetsCommand(RUN_ID, REFERENCE_DATE, RETENTION_DAYS);

        assertNotEquals("not-a-command", command);
    }

    @Test
    void shouldGenerateToStringWithAllComponents() {
        CleanupBudgetsCommand command =
                new CleanupBudgetsCommand(RUN_ID, REFERENCE_DATE, RETENTION_DAYS);

        String result = command.toString();

        assertNotNull(result);
        assertTrue(result.contains("CleanupBudgetsCommand"));
        assertTrue(result.contains("runId=" + RUN_ID));
        assertTrue(result.contains("referenceDate=" + REFERENCE_DATE));
        assertTrue(result.contains("retentionDays=" + RETENTION_DAYS));
    }

    @Test
    void shouldBeRecord() {
        assertTrue(CleanupBudgetsCommand.class.isRecord());
    }

    @Test
    void shouldHaveExpectedRecordComponents() {
        RecordComponent[] components =
                CleanupBudgetsCommand.class.getRecordComponents();

        assertNotNull(components);
        assertEquals(3, components.length);

        assertArrayEquals(
                new String[]{
                        "runId",
                        "referenceDate",
                        "retentionDays"
                },
                Arrays.stream(components)
                        .map(RecordComponent::getName)
                        .toArray(String[]::new)
        );

        assertArrayEquals(
                new Class<?>[]{
                        UUID.class,
                        LocalDate.class,
                        int.class
                },
                Arrays.stream(components)
                        .map(RecordComponent::getType)
                        .toArray(Class<?>[]::new)
        );
    }
}