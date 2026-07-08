package br.com.ecofy.ms_budgeting.core.application.exception;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import static org.junit.jupiter.api.Assertions.*;

class BudgetingApplicationExceptionTest {

    private static final String CODE = "BUDGETING_ERROR";
    private static final String MESSAGE = "Budgeting application error";

    @Test
    void shouldCreateExceptionWithCodeAndMessage() {
        TestBudgetingApplicationException exception =
                new TestBudgetingApplicationException(CODE, MESSAGE);

        assertNotNull(exception);
        assertInstanceOf(RuntimeException.class, exception);
        assertEquals(CODE, exception.getCode());
        assertEquals(MESSAGE, exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    void shouldCreateExceptionWithCodeMessageAndCause() {
        RuntimeException cause = new RuntimeException("root cause");

        TestBudgetingApplicationException exception =
                new TestBudgetingApplicationException(CODE, MESSAGE, cause);

        assertNotNull(exception);
        assertInstanceOf(RuntimeException.class, exception);
        assertEquals(CODE, exception.getCode());
        assertEquals(MESSAGE, exception.getMessage());
        assertSame(cause, exception.getCause());
        assertEquals("root cause", exception.getCause().getMessage());
    }

    @Test
    void shouldAllowNullCodeBecauseConstructorHasNoValidation() {
        TestBudgetingApplicationException exception =
                new TestBudgetingApplicationException(null, MESSAGE);

        assertNull(exception.getCode());
        assertEquals(MESSAGE, exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    void shouldAllowNullMessageBecauseConstructorHasNoValidation() {
        TestBudgetingApplicationException exception =
                new TestBudgetingApplicationException(CODE, null);

        assertEquals(CODE, exception.getCode());
        assertNull(exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    void shouldAllowNullCodeAndNullMessageBecauseConstructorHasNoValidation() {
        TestBudgetingApplicationException exception =
                new TestBudgetingApplicationException(null, null);

        assertNull(exception.getCode());
        assertNull(exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    void shouldAllowNullCauseBecauseConstructorHasNoValidation() {
        TestBudgetingApplicationException exception =
                new TestBudgetingApplicationException(CODE, MESSAGE, null);

        assertEquals(CODE, exception.getCode());
        assertEquals(MESSAGE, exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    void shouldAllowAllArgumentsNullBecauseConstructorHasNoValidation() {
        TestBudgetingApplicationException exception =
                new TestBudgetingApplicationException(null, null, null);

        assertNull(exception.getCode());
        assertNull(exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    void shouldHaveExpectedPrivateFinalCodeField() throws Exception {
        Field field =
                BudgetingApplicationException.class.getDeclaredField("code");

        assertEquals(String.class, field.getType());
        assertTrue(Modifier.isPrivate(field.getModifiers()));
        assertTrue(Modifier.isFinal(field.getModifiers()));
    }

    @Test
    void shouldBeAbstractClass() {
        assertTrue(Modifier.isAbstract(BudgetingApplicationException.class.getModifiers()));
    }

    @Test
    void shouldExtendRuntimeException() {
        assertEquals(
                RuntimeException.class,
                BudgetingApplicationException.class.getSuperclass()
        );
    }

    @Test
    void shouldHaveProtectedConstructorWithCodeAndMessage() throws Exception {
        Constructor<BudgetingApplicationException> constructor =
                BudgetingApplicationException.class.getDeclaredConstructor(
                        String.class,
                        String.class
                );

        assertTrue(Modifier.isProtected(constructor.getModifiers()));
    }

    @Test
    void shouldHaveProtectedConstructorWithCodeMessageAndCause() throws Exception {
        Constructor<BudgetingApplicationException> constructor =
                BudgetingApplicationException.class.getDeclaredConstructor(
                        String.class,
                        String.class,
                        Throwable.class
                );

        assertTrue(Modifier.isProtected(constructor.getModifiers()));
    }

    static class TestBudgetingApplicationException
            extends BudgetingApplicationException {

        TestBudgetingApplicationException(String code, String message) {
            super(code, message);
        }

        TestBudgetingApplicationException(
                String code,
                String message,
                Throwable cause
        ) {
            super(code, message, cause);
        }
    }
}