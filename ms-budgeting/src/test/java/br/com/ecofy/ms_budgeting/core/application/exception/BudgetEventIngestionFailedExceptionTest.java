package br.com.ecofy.ms_budgeting.core.application.exception;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class BudgetEventIngestionFailedExceptionTest {

    private static final UUID TRANSACTION_ID =
            UUID.fromString("11111111-1111-1111-1111-111111111111");

    private static final String EVENT_ID =
            "event-001";

    private static final String CORRELATION_ID =
            "correlation-001";

    @Test
    void shouldCreateBudgetEventIngestionFailedException() {
        RuntimeException cause = new RuntimeException("kafka failure");

        BudgetEventIngestionFailedException exception =
                new BudgetEventIngestionFailedException(
                        TRANSACTION_ID,
                        EVENT_ID,
                        CORRELATION_ID,
                        cause
                );

        assertNotNull(exception);
        assertInstanceOf(BudgetingProcessingException.class, exception);
        assertEquals(TRANSACTION_ID, exception.getTransactionId());
        assertEquals(EVENT_ID, exception.getEventId());
        assertEquals(CORRELATION_ID, exception.getCorrelationId());
        assertSame(cause, exception.getCause());
    }

    @Test
    void shouldBuildExpectedMessage() {
        RuntimeException cause = new RuntimeException("kafka failure");

        BudgetEventIngestionFailedException exception =
                new BudgetEventIngestionFailedException(
                        TRANSACTION_ID,
                        EVENT_ID,
                        CORRELATION_ID,
                        cause
                );

        assertEquals(
                "Failed to ingest budget transaction event. txId=" + TRANSACTION_ID
                        + " eventId=" + EVENT_ID
                        + " correlationId=" + CORRELATION_ID,
                exception.getMessage()
        );
    }

    @Test
    void shouldKeepCauseMessage() {
        RuntimeException cause = new RuntimeException("original cause");

        BudgetEventIngestionFailedException exception =
                new BudgetEventIngestionFailedException(
                        TRANSACTION_ID,
                        EVENT_ID,
                        CORRELATION_ID,
                        cause
                );

        assertNotNull(exception.getCause());
        assertEquals("original cause", exception.getCause().getMessage());
    }

    @Test
    void shouldAllowNullTransactionIdBecauseConstructorHasNoValidation() {
        RuntimeException cause = new RuntimeException("kafka failure");

        BudgetEventIngestionFailedException exception =
                new BudgetEventIngestionFailedException(
                        null,
                        EVENT_ID,
                        CORRELATION_ID,
                        cause
                );

        assertNull(exception.getTransactionId());
        assertEquals(EVENT_ID, exception.getEventId());
        assertEquals(CORRELATION_ID, exception.getCorrelationId());
        assertSame(cause, exception.getCause());
        assertEquals(
                "Failed to ingest budget transaction event. txId=null"
                        + " eventId=" + EVENT_ID
                        + " correlationId=" + CORRELATION_ID,
                exception.getMessage()
        );
    }

    @Test
    void shouldAllowNullEventIdBecauseConstructorHasNoValidation() {
        RuntimeException cause = new RuntimeException("kafka failure");

        BudgetEventIngestionFailedException exception =
                new BudgetEventIngestionFailedException(
                        TRANSACTION_ID,
                        null,
                        CORRELATION_ID,
                        cause
                );

        assertEquals(TRANSACTION_ID, exception.getTransactionId());
        assertNull(exception.getEventId());
        assertEquals(CORRELATION_ID, exception.getCorrelationId());
        assertSame(cause, exception.getCause());
        assertEquals(
                "Failed to ingest budget transaction event. txId=" + TRANSACTION_ID
                        + " eventId=null"
                        + " correlationId=" + CORRELATION_ID,
                exception.getMessage()
        );
    }

    @Test
    void shouldAllowNullCorrelationIdBecauseConstructorHasNoValidation() {
        RuntimeException cause = new RuntimeException("kafka failure");

        BudgetEventIngestionFailedException exception =
                new BudgetEventIngestionFailedException(
                        TRANSACTION_ID,
                        EVENT_ID,
                        null,
                        cause
                );

        assertEquals(TRANSACTION_ID, exception.getTransactionId());
        assertEquals(EVENT_ID, exception.getEventId());
        assertNull(exception.getCorrelationId());
        assertSame(cause, exception.getCause());
        assertEquals(
                "Failed to ingest budget transaction event. txId=" + TRANSACTION_ID
                        + " eventId=" + EVENT_ID
                        + " correlationId=null",
                exception.getMessage()
        );
    }

    @Test
    void shouldAllowNullCauseBecauseConstructorHasNoValidation() {
        BudgetEventIngestionFailedException exception =
                new BudgetEventIngestionFailedException(
                        TRANSACTION_ID,
                        EVENT_ID,
                        CORRELATION_ID,
                        null
                );

        assertEquals(TRANSACTION_ID, exception.getTransactionId());
        assertEquals(EVENT_ID, exception.getEventId());
        assertEquals(CORRELATION_ID, exception.getCorrelationId());
        assertNull(exception.getCause());
        assertEquals(
                "Failed to ingest budget transaction event. txId=" + TRANSACTION_ID
                        + " eventId=" + EVENT_ID
                        + " correlationId=" + CORRELATION_ID,
                exception.getMessage()
        );
    }

    @Test
    void shouldAllowAllConstructorArgumentsNullBecauseConstructorHasNoValidation() {
        BudgetEventIngestionFailedException exception =
                new BudgetEventIngestionFailedException(
                        null,
                        null,
                        null,
                        null
                );

        assertNull(exception.getTransactionId());
        assertNull(exception.getEventId());
        assertNull(exception.getCorrelationId());
        assertNull(exception.getCause());
        assertEquals(
                "Failed to ingest budget transaction event. txId=null"
                        + " eventId=null"
                        + " correlationId=null",
                exception.getMessage()
        );
    }

    @Test
    void shouldHaveExpectedPrivateFinalFields() throws Exception {
        Field eventIdField =
                BudgetEventIngestionFailedException.class.getDeclaredField("eventId");

        Field correlationIdField =
                BudgetEventIngestionFailedException.class.getDeclaredField("correlationId");

        Field transactionIdField =
                BudgetEventIngestionFailedException.class.getDeclaredField("transactionId");

        assertEquals(String.class, eventIdField.getType());
        assertEquals(String.class, correlationIdField.getType());
        assertEquals(UUID.class, transactionIdField.getType());

        assertTrue(Modifier.isPrivate(eventIdField.getModifiers()));
        assertTrue(Modifier.isPrivate(correlationIdField.getModifiers()));
        assertTrue(Modifier.isPrivate(transactionIdField.getModifiers()));

        assertTrue(Modifier.isFinal(eventIdField.getModifiers()));
        assertTrue(Modifier.isFinal(correlationIdField.getModifiers()));
        assertTrue(Modifier.isFinal(transactionIdField.getModifiers()));
    }

    @Test
    void shouldExtendBudgetingProcessingException() {
        assertEquals(
                BudgetingProcessingException.class,
                BudgetEventIngestionFailedException.class.getSuperclass()
        );
    }
}