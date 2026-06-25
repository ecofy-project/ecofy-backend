package br.com.ecofy.ms_budgeting.adapters.in.sched;

import br.com.ecofy.ms_budgeting.core.application.command.CleanupBudgetsCommand;
import br.com.ecofy.ms_budgeting.core.port.in.CleanupBudgetsUseCase;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class BudgetCleanupSchedulerTest {

    private static final Instant FIXED_INSTANT =
            Instant.parse("2026-06-25T03:00:00Z");

    private static final Clock FIXED_CLOCK =
            Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC);

    private final CleanupBudgetsUseCase useCase = mock(CleanupBudgetsUseCase.class);
    private final BudgetingSchedulingProperties props = mock(BudgetingSchedulingProperties.class);

    private final BudgetCleanupScheduler scheduler =
            new BudgetCleanupScheduler(useCase, props, FIXED_CLOCK);

    @Test
    void shouldSkipCleanupWhenCleanupIsDisabled() {
        when(props.isCleanupEnabled()).thenReturn(false);

        scheduler.cleanup();

        verify(props).isCleanupEnabled();
        verify(props, never()).getCleanupRetentionDays();
        verifyNoInteractions(useCase);
    }

    @Test
    void shouldExecuteCleanupWhenEnabled() {
        when(props.isCleanupEnabled()).thenReturn(true);
        when(props.getCleanupRetentionDays()).thenReturn(90);

        stubSuccessfulCleanup(3L, 5L);

        scheduler.cleanup();

        ArgumentCaptor<CleanupBudgetsCommand> commandCaptor =
                ArgumentCaptor.forClass(CleanupBudgetsCommand.class);

        verify(useCase).cleanup(commandCaptor.capture());

        CleanupBudgetsCommand command = commandCaptor.getValue();

        assertNotNull(command);
        assertNotNull(readAny(command, "runId", "getRunId"));
        assertInstanceOf(UUID.class, readAny(command, "runId", "getRunId"));

        assertEquals(
                LocalDate.of(2026, 6, 25),
                readAny(command, "referenceDate", "getReferenceDate")
        );

        assertEquals(
                90,
                readAny(command, "retentionDays", "cleanupRetentionDays", "getRetentionDays", "getCleanupRetentionDays")
        );

        verify(props).isCleanupEnabled();
        verify(props).getCleanupRetentionDays();

        assertFalse(runningFlag().get());
    }

    @Test
    void shouldSkipCleanupWhenAlreadyRunning() throws Exception {
        when(props.isCleanupEnabled()).thenReturn(true);

        runningFlag().set(true);

        scheduler.cleanup();

        verify(props).isCleanupEnabled();
        verify(props, never()).getCleanupRetentionDays();
        verifyNoInteractions(useCase);

        assertTrue(runningFlag().get());

        runningFlag().set(false);
    }

    @Test
    void shouldCatchUseCaseExceptionAndReleaseRunningFlag() {
        when(props.isCleanupEnabled()).thenReturn(true);
        when(props.getCleanupRetentionDays()).thenReturn(30);

        when(useCase.cleanup(any(CleanupBudgetsCommand.class)))
                .thenThrow(new RuntimeException("database unavailable"));

        assertDoesNotThrow(scheduler::cleanup);

        verify(useCase).cleanup(any(CleanupBudgetsCommand.class));

        assertFalse(runningFlag().get());
    }

    @Test
    void shouldReleaseRunningFlagAfterSuccessfulExecution() {
        when(props.isCleanupEnabled()).thenReturn(true);
        when(props.getCleanupRetentionDays()).thenReturn(15);

        stubSuccessfulCleanup(1L, 2L);

        scheduler.cleanup();

        verify(useCase).cleanup(any(CleanupBudgetsCommand.class));

        assertFalse(runningFlag().get());
    }

    @Test
    void shouldAllowNewExecutionAfterPreviousSuccessfulExecution() {
        when(props.isCleanupEnabled()).thenReturn(true);
        when(props.getCleanupRetentionDays()).thenReturn(45);

        stubSuccessfulCleanup(2L, 4L);

        scheduler.cleanup();
        scheduler.cleanup();

        verify(useCase, times(2)).cleanup(any(CleanupBudgetsCommand.class));

        assertFalse(runningFlag().get());
    }

    @Test
    void shouldAllowNewExecutionAfterPreviousFailedExecution() {
        when(props.isCleanupEnabled()).thenReturn(true);
        when(props.getCleanupRetentionDays()).thenReturn(60);

        when(useCase.cleanup(any(CleanupBudgetsCommand.class)))
                .thenThrow(new RuntimeException("first failure"))
                .thenAnswer(invocation -> cleanupResult(invocation.getMethod().getReturnType(), 10L, 20L));

        assertDoesNotThrow(scheduler::cleanup);
        assertDoesNotThrow(scheduler::cleanup);

        verify(useCase, times(2)).cleanup(any(CleanupBudgetsCommand.class));

        assertFalse(runningFlag().get());
    }

    private void stubSuccessfulCleanup(long budgetsDeleted, long consumptionsDeleted) {
        when(useCase.cleanup(any(CleanupBudgetsCommand.class)))
                .thenAnswer(invocation ->
                        cleanupResult(
                                invocation.getMethod().getReturnType(),
                                budgetsDeleted,
                                consumptionsDeleted
                        )
                );
    }

    private static Object cleanupResult(
            Class<?> returnType,
            long budgetsDeleted,
            long consumptionsDeleted
    ) throws Exception {
        for (Constructor<?> constructor : returnType.getDeclaredConstructors()) {
            Class<?>[] parameterTypes = constructor.getParameterTypes();

            if (parameterTypes.length == 2
                    && isNumericType(parameterTypes[0])
                    && isNumericType(parameterTypes[1])) {

                constructor.setAccessible(true);

                return constructor.newInstance(
                        numericValue(parameterTypes[0], budgetsDeleted),
                        numericValue(parameterTypes[1], consumptionsDeleted)
                );
            }
        }

        return mock(returnType, invocation -> {
            String methodName = invocation.getMethod().getName();
            Class<?> methodReturnType = invocation.getMethod().getReturnType();

            if ("budgetsDeleted".equals(methodName)) {
                return numericValue(methodReturnType, budgetsDeleted);
            }

            if ("consumptionsDeleted".equals(methodName)) {
                return numericValue(methodReturnType, consumptionsDeleted);
            }

            return RETURNS_DEFAULTS.answer(invocation);
        });
    }

    private static boolean isNumericType(Class<?> type) {
        return type == long.class
                || type == Long.class
                || type == int.class
                || type == Integer.class;
    }

    private static Object numericValue(Class<?> type, long value) {
        if (type == int.class || type == Integer.class) {
            return (int) value;
        }

        return value;
    }

    private AtomicBoolean runningFlag() {
        try {
            Field field = BudgetCleanupScheduler.class.getDeclaredField("running");
            field.setAccessible(true);
            return (AtomicBoolean) field.get(scheduler);
        } catch (Exception exception) {
            throw new AssertionError("Failed to access running flag", exception);
        }
    }

    private static Object readAny(Object target, String... methodNames) {
        for (String methodName : methodNames) {
            try {
                Method method = target.getClass().getMethod(methodName);
                return method.invoke(target);
            } catch (NoSuchMethodException ignored) {
                // tenta próximo nome
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