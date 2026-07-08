package br.com.ecofy.ms_budgeting.core.application.exception;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

import static org.junit.jupiter.api.Assertions.*;

class BudgetProjectionProcessingExceptionTest {

    private static final String CODE = "BUDGETING_PROCESSING_ERROR";
    private static final String MESSAGE = "Projection processing error";

    @Test
    void shouldCreateBudgetProjectionProcessingExceptionWithMessageAndCause() {
        RuntimeException cause = new RuntimeException("root cause");

        BudgetProjectionProcessingException exception =
                new BudgetProjectionProcessingException(MESSAGE, cause);

        assertNotNull(exception);
        assertInstanceOf(BudgetingApplicationException.class, exception);
        assertInstanceOf(BudgetingProcessingException.class, exception);
        assertInstanceOf(RuntimeException.class, exception);
        assertEquals(CODE, exception.getCode());
        assertEquals(MESSAGE, exception.getMessage());
        assertSame(cause, exception.getCause());
        assertEquals("root cause", exception.getCause().getMessage());
    }

    @Test
    void shouldAllowNullMessageBecauseConstructorHasNoValidation() {
        RuntimeException cause = new RuntimeException("root cause");

        BudgetProjectionProcessingException exception =
                new BudgetProjectionProcessingException(null, cause);

        assertNotNull(exception);
        assertEquals(CODE, exception.getCode());
        assertNull(exception.getMessage());
        assertSame(cause, exception.getCause());
    }

    @Test
    void shouldAllowNullCauseBecauseConstructorHasNoValidation() {
        BudgetProjectionProcessingException exception =
                new BudgetProjectionProcessingException(MESSAGE, null);

        assertNotNull(exception);
        assertEquals(CODE, exception.getCode());
        assertEquals(MESSAGE, exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    void shouldAllowNullMessageAndNullCauseBecauseConstructorHasNoValidation() {
        BudgetProjectionProcessingException exception =
                new BudgetProjectionProcessingException(null, null);

        assertNotNull(exception);
        assertEquals(CODE, exception.getCode());
        assertNull(exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    void shouldExtendBudgetingProcessingException() {
        assertEquals(
                BudgetingProcessingException.class,
                BudgetProjectionProcessingException.class.getSuperclass()
        );
    }

    @Test
    void shouldNotBeAbstract() {
        assertFalse(
                Modifier.isAbstract(
                        BudgetProjectionProcessingException.class.getModifiers()
                )
        );
    }

    @Test
    void shouldHavePublicConstructorWithMessageAndCause() throws Exception {
        Constructor<BudgetProjectionProcessingException> constructor =
                BudgetProjectionProcessingException.class.getDeclaredConstructor(
                        String.class,
                        Throwable.class
                );

        assertTrue(Modifier.isPublic(constructor.getModifiers()));
    }

    @Test
    void shouldHaveOnlyExpectedConstructor() {
        Constructor<?>[] constructors =
                BudgetProjectionProcessingException.class.getDeclaredConstructors();

        assertEquals(1, constructors.length);
    }

    @Test
    void shouldNotHaveMessageOnlyConstructor() {
        assertThrows(
                NoSuchMethodException.class,
                () -> BudgetProjectionProcessingException.class
                        .getDeclaredConstructor(String.class)
        );
    }
}