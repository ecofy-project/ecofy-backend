package br.com.ecofy.ms_budgeting.core.application.exception;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

import static org.junit.jupiter.api.Assertions.*;

class MissingIdempotencyKeyExceptionTest {

    private static final String APPLICATION_CODE = "BUDGETING_VALIDATION_ERROR";
    private static final String MESSAGE = "Idempotency-Key header must be provided";

    @Test
    void shouldCreateMissingIdempotencyKeyException() {
        MissingIdempotencyKeyException exception =
                new MissingIdempotencyKeyException();

        assertNotNull(exception);
        assertInstanceOf(BudgetingApplicationException.class, exception);
        assertInstanceOf(BudgetingValidationException.class, exception);
        assertInstanceOf(RuntimeException.class, exception);
        assertEquals(APPLICATION_CODE, exception.getCode());
        assertEquals(MESSAGE, exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    void shouldExtendBudgetingValidationException() {
        assertEquals(
                BudgetingValidationException.class,
                MissingIdempotencyKeyException.class.getSuperclass()
        );
    }

    @Test
    void shouldNotBeAbstract() {
        assertFalse(
                Modifier.isAbstract(
                        MissingIdempotencyKeyException.class.getModifiers()
                )
        );
    }

    @Test
    void shouldHavePublicNoArgsConstructor() throws Exception {
        Constructor<MissingIdempotencyKeyException> constructor =
                MissingIdempotencyKeyException.class.getDeclaredConstructor();

        assertTrue(Modifier.isPublic(constructor.getModifiers()));
    }

    @Test
    void shouldHaveOnlyExpectedConstructor() {
        Constructor<?>[] constructors =
                MissingIdempotencyKeyException.class.getDeclaredConstructors();

        assertEquals(1, constructors.length);
    }

    @Test
    void shouldNotHaveConstructorWithMessage() {
        assertThrows(
                NoSuchMethodException.class,
                () -> MissingIdempotencyKeyException.class
                        .getDeclaredConstructor(String.class)
        );
    }

    @Test
    void shouldNotHaveConstructorWithMessageAndCause() {
        assertThrows(
                NoSuchMethodException.class,
                () -> MissingIdempotencyKeyException.class
                        .getDeclaredConstructor(String.class, Throwable.class)
        );
    }

    @Test
    void shouldNotDeclareFields() {
        assertEquals(
                0,
                MissingIdempotencyKeyException.class.getDeclaredFields().length
        );
    }
}