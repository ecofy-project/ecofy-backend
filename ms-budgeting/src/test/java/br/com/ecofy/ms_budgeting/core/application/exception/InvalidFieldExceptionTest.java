package br.com.ecofy.ms_budgeting.core.application.exception;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import static org.junit.jupiter.api.Assertions.*;

class InvalidFieldExceptionTest {

    private static final String APPLICATION_CODE = "BUDGETING_VALIDATION_ERROR";

    @Test
    void shouldCreateInvalidFieldExceptionWithFieldAndReason() {
        InvalidFieldException exception =
                new InvalidFieldException("amount", "must be positive");

        assertNotNull(exception);
        assertInstanceOf(BudgetingApplicationException.class, exception);
        assertInstanceOf(BudgetingValidationException.class, exception);
        assertInstanceOf(RuntimeException.class, exception);
        assertEquals(APPLICATION_CODE, exception.getCode());
        assertEquals("amount must be positive", exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    void shouldCreateRequiredInvalidFieldException() {
        InvalidFieldException exception =
                InvalidFieldException.required("amount");

        assertNotNull(exception);
        assertEquals(APPLICATION_CODE, exception.getCode());
        assertEquals("amount must be provided", exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    void shouldCreateNotBlankInvalidFieldException() {
        InvalidFieldException exception =
                InvalidFieldException.notBlank("currency");

        assertNotNull(exception);
        assertEquals(APPLICATION_CODE, exception.getCode());
        assertEquals("currency must not be blank", exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    void shouldCreateInvalidInvalidFieldException() {
        InvalidFieldException exception =
                InvalidFieldException.invalid("currency", "unsupported code");

        assertNotNull(exception);
        assertEquals(APPLICATION_CODE, exception.getCode());
        assertEquals("currency is invalid: unsupported code", exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    void shouldAllowNullFieldBecauseConstructorHasNoValidation() {
        InvalidFieldException exception =
                new InvalidFieldException(null, "must be positive");

        assertNotNull(exception);
        assertEquals(APPLICATION_CODE, exception.getCode());
        assertEquals("null must be positive", exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    void shouldAllowNullReasonBecauseConstructorHasNoValidation() {
        InvalidFieldException exception =
                new InvalidFieldException("amount", null);

        assertNotNull(exception);
        assertEquals(APPLICATION_CODE, exception.getCode());
        assertEquals("amount null", exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    void shouldAllowNullFieldAndNullReasonBecauseConstructorHasNoValidation() {
        InvalidFieldException exception =
                new InvalidFieldException(null, null);

        assertNotNull(exception);
        assertEquals(APPLICATION_CODE, exception.getCode());
        assertEquals("null null", exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    void shouldAllowEmptyFieldBecauseConstructorHasNoValidation() {
        InvalidFieldException exception =
                new InvalidFieldException("", "must be positive");

        assertNotNull(exception);
        assertEquals(APPLICATION_CODE, exception.getCode());
        assertEquals(" must be positive", exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    void shouldAllowBlankFieldBecauseConstructorHasNoValidation() {
        InvalidFieldException exception =
                new InvalidFieldException("   ", "must be positive");

        assertNotNull(exception);
        assertEquals(APPLICATION_CODE, exception.getCode());
        assertEquals("    must be positive", exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    void shouldAllowEmptyReasonBecauseConstructorHasNoValidation() {
        InvalidFieldException exception =
                new InvalidFieldException("amount", "");

        assertNotNull(exception);
        assertEquals(APPLICATION_CODE, exception.getCode());
        assertEquals("amount ", exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    void shouldAllowBlankReasonBecauseConstructorHasNoValidation() {
        InvalidFieldException exception =
                new InvalidFieldException("amount", "   ");

        assertNotNull(exception);
        assertEquals(APPLICATION_CODE, exception.getCode());
        assertEquals("amount    ", exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    void shouldAllowNullFieldInRequiredFactoryBecauseThereIsNoValidation() {
        InvalidFieldException exception =
                InvalidFieldException.required(null);

        assertNotNull(exception);
        assertEquals(APPLICATION_CODE, exception.getCode());
        assertEquals("null must be provided", exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    void shouldAllowBlankFieldInRequiredFactoryBecauseThereIsNoValidation() {
        InvalidFieldException exception =
                InvalidFieldException.required("   ");

        assertNotNull(exception);
        assertEquals(APPLICATION_CODE, exception.getCode());
        assertEquals("    must be provided", exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    void shouldAllowNullFieldInNotBlankFactoryBecauseThereIsNoValidation() {
        InvalidFieldException exception =
                InvalidFieldException.notBlank(null);

        assertNotNull(exception);
        assertEquals(APPLICATION_CODE, exception.getCode());
        assertEquals("null must not be blank", exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    void shouldAllowBlankFieldInNotBlankFactoryBecauseThereIsNoValidation() {
        InvalidFieldException exception =
                InvalidFieldException.notBlank("   ");

        assertNotNull(exception);
        assertEquals(APPLICATION_CODE, exception.getCode());
        assertEquals("    must not be blank", exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    void shouldAllowNullFieldInInvalidFactoryBecauseThereIsNoValidation() {
        InvalidFieldException exception =
                InvalidFieldException.invalid(null, "unsupported value");

        assertNotNull(exception);
        assertEquals(APPLICATION_CODE, exception.getCode());
        assertEquals("null is invalid: unsupported value", exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    void shouldAllowNullReasonInInvalidFactoryBecauseThereIsNoValidation() {
        InvalidFieldException exception =
                InvalidFieldException.invalid("currency", null);

        assertNotNull(exception);
        assertEquals(APPLICATION_CODE, exception.getCode());
        assertEquals("currency is invalid: null", exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    void shouldAllowNullFieldAndNullReasonInInvalidFactoryBecauseThereIsNoValidation() {
        InvalidFieldException exception =
                InvalidFieldException.invalid(null, null);

        assertNotNull(exception);
        assertEquals(APPLICATION_CODE, exception.getCode());
        assertEquals("null is invalid: null", exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    void shouldExtendBudgetingValidationException() {
        assertEquals(
                BudgetingValidationException.class,
                InvalidFieldException.class.getSuperclass()
        );
    }

    @Test
    void shouldNotBeAbstract() {
        assertFalse(Modifier.isAbstract(InvalidFieldException.class.getModifiers()));
    }

    @Test
    void shouldHavePublicConstructorWithFieldAndReason() throws Exception {
        Constructor<InvalidFieldException> constructor =
                InvalidFieldException.class.getDeclaredConstructor(
                        String.class,
                        String.class
                );

        assertTrue(Modifier.isPublic(constructor.getModifiers()));
    }

    @Test
    void shouldHaveOnlyExpectedConstructor() {
        Constructor<?>[] constructors =
                InvalidFieldException.class.getDeclaredConstructors();

        assertEquals(1, constructors.length);
    }

    @Test
    void shouldNotHaveNoArgsConstructor() {
        assertThrows(
                NoSuchMethodException.class,
                () -> InvalidFieldException.class.getDeclaredConstructor()
        );
    }

    @Test
    void shouldNotHaveConstructorWithCause() {
        assertThrows(
                NoSuchMethodException.class,
                () -> InvalidFieldException.class.getDeclaredConstructor(
                        String.class,
                        String.class,
                        Throwable.class
                )
        );
    }

    @Test
    void shouldHavePublicStaticRequiredFactoryMethod() throws Exception {
        Method method =
                InvalidFieldException.class.getDeclaredMethod("required", String.class);

        assertEquals(InvalidFieldException.class, method.getReturnType());
        assertTrue(Modifier.isPublic(method.getModifiers()));
        assertTrue(Modifier.isStatic(method.getModifiers()));
    }

    @Test
    void shouldHavePublicStaticNotBlankFactoryMethod() throws Exception {
        Method method =
                InvalidFieldException.class.getDeclaredMethod("notBlank", String.class);

        assertEquals(InvalidFieldException.class, method.getReturnType());
        assertTrue(Modifier.isPublic(method.getModifiers()));
        assertTrue(Modifier.isStatic(method.getModifiers()));
    }

    @Test
    void shouldHavePublicStaticInvalidFactoryMethod() throws Exception {
        Method method =
                InvalidFieldException.class.getDeclaredMethod(
                        "invalid",
                        String.class,
                        String.class
                );

        assertEquals(InvalidFieldException.class, method.getReturnType());
        assertTrue(Modifier.isPublic(method.getModifiers()));
        assertTrue(Modifier.isStatic(method.getModifiers()));
    }
}