package br.com.ecofy.ms_budgeting.adapters.in.sched;

import br.com.ecofy.ms_budgeting.core.application.command.RecalculateBudgetsCommand;
import br.com.ecofy.ms_budgeting.core.port.in.RecalculateBudgetsUseCase;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.lang.reflect.Method;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BudgetRecalculationSchedulerTest {

    private static final Instant FIXED_INSTANT =
            Instant.parse("2026-06-25T15:30:00Z");

    private static final Clock FIXED_CLOCK =
            Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC);

    private final RecalculateBudgetsUseCase useCase = mock(RecalculateBudgetsUseCase.class);

    private final BudgetRecalculationScheduler scheduler =
            new BudgetRecalculationScheduler(useCase, FIXED_CLOCK);

    @Test
    void shouldRecalculateBudgetsSuccessfully() {
        scheduler.recalc();

        ArgumentCaptor<RecalculateBudgetsCommand> commandCaptor =
                ArgumentCaptor.forClass(RecalculateBudgetsCommand.class);

        verify(useCase).recalculate(commandCaptor.capture());

        RecalculateBudgetsCommand command = commandCaptor.getValue();

        assertNotNull(command);

        Object runId = readAny(command, "runId", "getRunId");
        Object userId = readAny(command, "userId", "getUserId");
        Object referenceDate = readAny(command, "referenceDate", "getReferenceDate");

        assertNotNull(runId);
        assertInstanceOf(UUID.class, runId);
        assertNull(userId);
        assertEquals(LocalDate.of(2026, 6, 25), referenceDate);

        verifyNoMoreInteractions(useCase);
    }

    @Test
    void shouldCatchExceptionWhenUseCaseFails() {
        doThrow(new RuntimeException("recalculation failure"))
                .when(useCase)
                .recalculate(any(RecalculateBudgetsCommand.class));

        assertDoesNotThrow(scheduler::recalc);

        verify(useCase).recalculate(any(RecalculateBudgetsCommand.class));
        verifyNoMoreInteractions(useCase);
    }

    @Test
    void shouldGenerateDifferentRunIdsForEachExecution() {
        scheduler.recalc();
        scheduler.recalc();

        ArgumentCaptor<RecalculateBudgetsCommand> commandCaptor =
                ArgumentCaptor.forClass(RecalculateBudgetsCommand.class);

        verify(useCase, times(2)).recalculate(commandCaptor.capture());

        RecalculateBudgetsCommand firstCommand = commandCaptor.getAllValues().get(0);
        RecalculateBudgetsCommand secondCommand = commandCaptor.getAllValues().get(1);

        Object firstRunId = readAny(firstCommand, "runId", "getRunId");
        Object secondRunId = readAny(secondCommand, "runId", "getRunId");

        assertNotNull(firstRunId);
        assertNotNull(secondRunId);
        assertInstanceOf(UUID.class, firstRunId);
        assertInstanceOf(UUID.class, secondRunId);
        assertNotEquals(firstRunId, secondRunId);

        assertEquals(
                LocalDate.of(2026, 6, 25),
                readAny(firstCommand, "referenceDate", "getReferenceDate")
        );

        assertEquals(
                LocalDate.of(2026, 6, 25),
                readAny(secondCommand, "referenceDate", "getReferenceDate")
        );
    }

    private static Object readAny(Object target, String... methodNames) {
        for (String methodName : methodNames) {
            try {
                Method method = target.getClass().getMethod(methodName);
                return method.invoke(target);
            } catch (NoSuchMethodException ignored) {
                // tenta o próximo nome
            } catch (Exception exception) {
                throw new AssertionError("Failed to read method: " + methodName, exception);
            }
        }

        throw new AssertionError(
                "None of the methods exist on "
                        + target.getClass().getName()
                        + ": "
                        + String.join(", ", methodNames)
        );
    }
}