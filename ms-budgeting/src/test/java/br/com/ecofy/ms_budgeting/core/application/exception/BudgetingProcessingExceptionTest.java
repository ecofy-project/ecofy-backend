package br.com.ecofy.ms_budgeting.core.application.exception;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

import static org.junit.jupiter.api.Assertions.*;

class BudgetingProcessingExceptionTest {

    private static final String CODE = "BUDGETING_PROCESSING_ERROR";
    private static final String MESSAGE = "Processing error";

    @Test
    void shouldCreateBudgetingProcessingExceptionWithMessage() {
        BudgetingProcessingException exception =
                new BudgetingProcessingException(MESSAGE);

        assertNotNull(exception);
        assertInstanceOf(BudgetingApplicationException.class, exception);
        assertInstanceOf(RuntimeException.class, exception);
        assertEquals(CODE, exception.getCode());
        assertEquals(MESSAGE, exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    void shouldCreateBudgetingProcessingExceptionWithMessageAndCause() {
        RuntimeException cause = new RuntimeException("root cause");

        BudgetingProcessingException exception =
                new BudgetingProcessingException(MESSAGE, cause);

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
        BudgetingProcessingException exception =
                new BudgetingProcessingException(null);

        assertNotNull(exception);
        assertEquals(CODE, exception.getCode());
        assertNull(exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    void shouldAllowNullMessageWithCauseBecauseConstructorHasNoValidation() {
        RuntimeException cause = new RuntimeException("root cause");

        BudgetingProcessingException exception =
                new BudgetingProcessingException(null, cause);

        assertNotNull(exception);
        assertEquals(CODE, exception.getCode());
        assertNull(exception.getMessage());
        assertSame(cause, exception.getCause());
    }

    @Test
    void shouldAllowNullCauseBecauseConstructorHasNoValidation() {
        BudgetingProcessingException exception =
                new BudgetingProcessingException(MESSAGE, null);

        assertNotNull(exception);
        assertEquals(CODE, exception.getCode());
        assertEquals(MESSAGE, exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    void shouldAllowNullMessageAndNullCauseBecauseConstructorHasNoValidation() {
        BudgetingProcessingException exception =
                new BudgetingProcessingException(null, null);

        assertNotNull(exception);
        assertEquals(CODE, exception.getCode());
        assertNull(exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    void shouldExtendBudgetingApplicationException() {
        assertEquals(
                BudgetingApplicationException.class,
                BudgetingProcessingException.class.getSuperclass()
        );
    }

    @Test
    void shouldNotBeAbstract() {
        assertFalse(Modifier.isAbstract(BudgetingProcessingException.class.getModifiers()));
    }

    @Test
    void shouldHavePublicConstructorWithMessage() throws Exception {
        Constructor<BudgetingProcessingException> constructor =
                BudgetingProcessingException.class.getDeclaredConstructor(String.class);

        assertTrue(Modifier.isPublic(constructor.getModifiers()));
    }

    @Test
    void shouldHavePublicConstructorWithMessageAndCause() throws Exception {
        Constructor<BudgetingProcessingException> constructor =
                BudgetingProcessingException.class.getDeclaredConstructor(
                        String.class,
                        Throwable.class
                );

        assertTrue(Modifier.isPublic(constructor.getModifiers()));
    }

    @Test
    void shouldHaveOnlyExpectedConstructors() {
        Constructor<?>[] constructors =
                BudgetingProcessingException.class.getDeclaredConstructors();

        assertEquals(2, constructors.length);
    }
}