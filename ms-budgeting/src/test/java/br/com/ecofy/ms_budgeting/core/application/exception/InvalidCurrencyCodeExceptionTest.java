package br.com.ecofy.ms_budgeting.core.application.exception;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

import static org.junit.jupiter.api.Assertions.*;

class InvalidCurrencyCodeExceptionTest {

    private static final String APPLICATION_CODE = "BUDGETING_VALIDATION_ERROR";
    private static final String CURRENCY_CODE = "XYZ";

    @Test
    void shouldCreateInvalidCurrencyCodeExceptionWithCode() {
        InvalidCurrencyCodeException exception =
                new InvalidCurrencyCodeException(CURRENCY_CODE);

        assertNotNull(exception);
        assertInstanceOf(BudgetingApplicationException.class, exception);
        assertInstanceOf(BudgetingValidationException.class, exception);
        assertInstanceOf(RuntimeException.class, exception);
        assertEquals(APPLICATION_CODE, exception.getCode());
        assertEquals("Invalid currency code: XYZ", exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    void shouldCreateInvalidCurrencyCodeExceptionWithCodeAndCause() {
        RuntimeException cause = new RuntimeException("root cause");

        InvalidCurrencyCodeException exception =
                new InvalidCurrencyCodeException(CURRENCY_CODE, cause);

        assertNotNull(exception);
        assertInstanceOf(BudgetingApplicationException.class, exception);
        assertInstanceOf(BudgetingValidationException.class, exception);
        assertInstanceOf(RuntimeException.class, exception);
        assertEquals(APPLICATION_CODE, exception.getCode());
        assertEquals("Invalid currency code: XYZ", exception.getMessage());
        assertSame(cause, exception.getCause());
        assertEquals("root cause", exception.getCause().getMessage());
    }

    @Test
    void shouldAllowNullCodeBecauseConstructorHasNoValidation() {
        InvalidCurrencyCodeException exception =
                new InvalidCurrencyCodeException(null);

        assertNotNull(exception);
        assertEquals(APPLICATION_CODE, exception.getCode());
        assertEquals("Invalid currency code: null", exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    void shouldAllowBlankCodeBecauseConstructorHasNoValidation() {
        InvalidCurrencyCodeException exception =
                new InvalidCurrencyCodeException("   ");

        assertNotNull(exception);
        assertEquals(APPLICATION_CODE, exception.getCode());
        assertEquals("Invalid currency code:    ", exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    void shouldAllowEmptyCodeBecauseConstructorHasNoValidation() {
        InvalidCurrencyCodeException exception =
                new InvalidCurrencyCodeException("");

        assertNotNull(exception);
        assertEquals(APPLICATION_CODE, exception.getCode());
        assertEquals("Invalid currency code: ", exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    void shouldAllowNullCodeWithCauseBecauseConstructorHasNoValidation() {
        RuntimeException cause = new RuntimeException("root cause");

        InvalidCurrencyCodeException exception =
                new InvalidCurrencyCodeException(null, cause);

        assertNotNull(exception);
        assertEquals(APPLICATION_CODE, exception.getCode());
        assertEquals("Invalid currency code: null", exception.getMessage());
        assertSame(cause, exception.getCause());
    }

    @Test
    void shouldAllowNullCauseBecauseConstructorHasNoValidation() {
        InvalidCurrencyCodeException exception =
                new InvalidCurrencyCodeException(CURRENCY_CODE, null);

        assertNotNull(exception);
        assertEquals(APPLICATION_CODE, exception.getCode());
        assertEquals("Invalid currency code: XYZ", exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    void shouldAllowNullCodeAndNullCauseBecauseConstructorHasNoValidation() {
        InvalidCurrencyCodeException exception =
                new InvalidCurrencyCodeException(null, null);

        assertNotNull(exception);
        assertEquals(APPLICATION_CODE, exception.getCode());
        assertEquals("Invalid currency code: null", exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    void shouldExtendBudgetingValidationException() {
        assertEquals(
                BudgetingValidationException.class,
                InvalidCurrencyCodeException.class.getSuperclass()
        );
    }

    @Test
    void shouldNotBeAbstract() {
        assertFalse(
                Modifier.isAbstract(
                        InvalidCurrencyCodeException.class.getModifiers()
                )
        );
    }

    @Test
    void shouldHavePublicConstructorWithCode() throws Exception {
        Constructor<InvalidCurrencyCodeException> constructor =
                InvalidCurrencyCodeException.class.getDeclaredConstructor(String.class);

        assertTrue(Modifier.isPublic(constructor.getModifiers()));
    }

    @Test
    void shouldHavePublicConstructorWithCodeAndCause() throws Exception {
        Constructor<InvalidCurrencyCodeException> constructor =
                InvalidCurrencyCodeException.class.getDeclaredConstructor(
                        String.class,
                        Throwable.class
                );

        assertTrue(Modifier.isPublic(constructor.getModifiers()));
    }

    @Test
    void shouldHaveOnlyExpectedConstructors() {
        Constructor<?>[] constructors =
                InvalidCurrencyCodeException.class.getDeclaredConstructors();

        assertEquals(2, constructors.length);
    }

    @Test
    void shouldNotHaveNoArgsConstructor() {
        assertThrows(
                NoSuchMethodException.class,
                () -> InvalidCurrencyCodeException.class.getDeclaredConstructor()
        );
    }

    @Test
    void shouldNotHaveMessageOnlyThrowableConstructor() {
        assertThrows(
                NoSuchMethodException.class,
                () -> InvalidCurrencyCodeException.class.getDeclaredConstructor(Throwable.class)
        );
    }
}