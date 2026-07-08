package br.com.ecofy.ms_budgeting.core.application.exception;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

import static org.junit.jupiter.api.Assertions.*;

class BudgetingValidationExceptionTest {

    private static final String CODE = "BUDGETING_VALIDATION_ERROR";
    private static final String MESSAGE = "Validation error";

    @Test
    void shouldCreateBudgetingValidationExceptionWithMessage() {
        BudgetingValidationException exception =
                new BudgetingValidationException(MESSAGE);

        assertNotNull(exception);
        assertInstanceOf(BudgetingApplicationException.class, exception);
        assertInstanceOf(RuntimeException.class, exception);
        assertEquals(CODE, exception.getCode());
        assertEquals(MESSAGE, exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    void shouldCreateBudgetingValidationExceptionWithMessageAndCause() {
        RuntimeException cause = new RuntimeException("root cause");

        BudgetingValidationException exception =
                new BudgetingValidationException(MESSAGE, cause);

        assertNotNull(exception);
        assertInstanceOf(BudgetingApplicationException.class, exception);
        assertInstanceOf(RuntimeException.class, exception);
        assertEquals(CODE, exception.getCode());
        assertEquals(MESSAGE, exception.getMessage());
        assertSame(cause, exception.getCause());
        assertEquals("root cause", exception.getCause().getMessage());
    }

    @Test
    void shouldAllowNullMessageBecauseConstructorHasNoValidation() {
        BudgetingValidationException exception =
                new BudgetingValidationException(null);

        assertNotNull(exception);
        assertEquals(CODE, exception.getCode());
        assertNull(exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    void shouldAllowNullMessageWithCauseBecauseConstructorHasNoValidation() {
        RuntimeException cause = new RuntimeException("root cause");

        BudgetingValidationException exception =
                new BudgetingValidationException(null, cause);

        assertNotNull(exception);
        assertEquals(CODE, exception.getCode());
        assertNull(exception.getMessage());
        assertSame(cause, exception.getCause());
    }

    @Test
    void shouldAllowNullCauseBecauseConstructorHasNoValidation() {
        BudgetingValidationException exception =
                new BudgetingValidationException(MESSAGE, null);

        assertNotNull(exception);
        assertEquals(CODE, exception.getCode());
        assertEquals(MESSAGE, exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    void shouldAllowNullMessageAndNullCauseBecauseConstructorHasNoValidation() {
        BudgetingValidationException exception =
                new BudgetingValidationException(null, null);

        assertNotNull(exception);
        assertEquals(CODE, exception.getCode());
        assertNull(exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    void shouldExtendBudgetingApplicationException() {
        assertEquals(
                BudgetingApplicationException.class,
                BudgetingValidationException.class.getSuperclass()
        );
    }

    @Test
    void shouldNotBeAbstract() {
        assertFalse(Modifier.isAbstract(BudgetingValidationException.class.getModifiers()));
    }

    @Test
    void shouldHavePublicConstructorWithMessage() throws Exception {
        Constructor<BudgetingValidationException> constructor =
                BudgetingValidationException.class.getDeclaredConstructor(String.class);

        assertTrue(Modifier.isPublic(constructor.getModifiers()));
    }

    @Test
    void shouldHavePublicConstructorWithMessageAndCause() throws Exception {
        Constructor<BudgetingValidationException> constructor =
                BudgetingValidationException.class.getDeclaredConstructor(
                        String.class,
                        Throwable.class
                );

        assertTrue(Modifier.isPublic(constructor.getModifiers()));
    }

    @Test
    void shouldHaveOnlyExpectedConstructors() {
        Constructor<?>[] constructors =
                BudgetingValidationException.class.getDeclaredConstructors();

        assertEquals(2, constructors.length);
    }
}