package br.com.ecofy.ms_budgeting.core.application.service;

import br.com.ecofy.ms_budgeting.core.application.command.ProcessTransactionCommand;
import br.com.ecofy.ms_budgeting.core.application.exception.BudgetEventIngestionFailedException;
import br.com.ecofy.ms_budgeting.core.application.exception.InvalidFieldException;
import br.com.ecofy.ms_budgeting.core.port.in.ProcessTransactionForBudgetUseCase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BudgetEventIngestionServiceTest {

    private static final String MDC_CORRELATION_ID = "correlationId";
    private static final String MDC_EVENT_ID = "eventId";
    private static final String MDC_RUN_ID = "runId";

    private final ProcessTransactionForBudgetUseCase useCase = mock(ProcessTransactionForBudgetUseCase.class);

    private final BudgetEventIngestionService service = new BudgetEventIngestionService(useCase);

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    void shouldIngestTransactionEventSuccessfullyWithMetadataAndCleanMdc() {
        UUID transactionId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        UUID runId = UUID.randomUUID();

        ProcessTransactionCommand command = mock(ProcessTransactionCommand.class, RETURNS_DEEP_STUBS);

        when(command.transactionId()).thenReturn(transactionId);
        when(command.userId()).thenReturn(userId);
        when(command.categoryId()).thenReturn(categoryId);
        when(command.runId()).thenReturn(runId);

        when(command.metadata().eventId()).thenReturn("event-123");
        when(command.metadata().correlationId()).thenReturn("correlation-123");
        when(command.metadata().topic()).thenReturn("transactions");
        when(command.metadata().partition()).thenReturn(1);
        when(command.metadata().offset()).thenReturn(100L);

        doAnswer(invocation -> {
            assertEquals(runId.toString(), MDC.get(MDC_RUN_ID));
            assertEquals("event-123", MDC.get(MDC_EVENT_ID));
            assertEquals("correlation-123", MDC.get(MDC_CORRELATION_ID));
            return null;
        }).when(useCase).process(command);

        service.ingest(command);

        verify(useCase).process(command);

        assertNull(MDC.get(MDC_RUN_ID));
        assertNull(MDC.get(MDC_EVENT_ID));
        assertNull(MDC.get(MDC_CORRELATION_ID));
    }

    @Test
    void shouldIngestTransactionEventSuccessfullyWithoutMetadataAndWithoutRunId() {
        UUID transactionId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();

        ProcessTransactionCommand command = mock(ProcessTransactionCommand.class);

        when(command.transactionId()).thenReturn(transactionId);
        when(command.userId()).thenReturn(userId);
        when(command.categoryId()).thenReturn(categoryId);
        when(command.runId()).thenReturn(null);
        when(command.metadata()).thenReturn(null);

        doAnswer(invocation -> {
            assertNull(MDC.get(MDC_RUN_ID));
            assertNull(MDC.get(MDC_EVENT_ID));
            assertNull(MDC.get(MDC_CORRELATION_ID));
            return null;
        }).when(useCase).process(command);

        service.ingest(command);

        verify(useCase).process(command);

        assertNull(MDC.get(MDC_RUN_ID));
        assertNull(MDC.get(MDC_EVENT_ID));
        assertNull(MDC.get(MDC_CORRELATION_ID));
    }

    @Test
    void shouldThrowInvalidFieldExceptionWhenCommandIsNull() {
        assertThrows(
                InvalidFieldException.class,
                () -> service.ingest(null)
        );

        verify(useCase, never()).process(null);
    }

    @Test
    void shouldThrowInvalidFieldExceptionWhenTransactionIdIsNull() {
        ProcessTransactionCommand command = mock(ProcessTransactionCommand.class);

        when(command.transactionId()).thenReturn(null);

        assertThrows(
                InvalidFieldException.class,
                () -> service.ingest(command)
        );

        verify(useCase, never()).process(command);
    }

    @Test
    void shouldThrowInvalidFieldExceptionWhenUserIdIsNull() {
        ProcessTransactionCommand command = mock(ProcessTransactionCommand.class);

        when(command.transactionId()).thenReturn(UUID.randomUUID());
        when(command.userId()).thenReturn(null);

        assertThrows(
                InvalidFieldException.class,
                () -> service.ingest(command)
        );

        verify(useCase, never()).process(command);
    }

    @Test
    void shouldThrowInvalidFieldExceptionWhenCategoryIdIsNull() {
        ProcessTransactionCommand command = mock(ProcessTransactionCommand.class);

        when(command.transactionId()).thenReturn(UUID.randomUUID());
        when(command.userId()).thenReturn(UUID.randomUUID());
        when(command.categoryId()).thenReturn(null);

        assertThrows(
                InvalidFieldException.class,
                () -> service.ingest(command)
        );

        verify(useCase, never()).process(command);
    }

    @Test
    void shouldWrapUseCaseExceptionIntoBudgetEventIngestionFailedExceptionAndCleanMdc() {
        UUID transactionId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        UUID runId = UUID.randomUUID();

        RuntimeException cause = new RuntimeException("processing error");

        ProcessTransactionCommand command = mock(ProcessTransactionCommand.class, RETURNS_DEEP_STUBS);

        when(command.transactionId()).thenReturn(transactionId);
        when(command.userId()).thenReturn(userId);
        when(command.categoryId()).thenReturn(categoryId);
        when(command.runId()).thenReturn(runId);

        when(command.metadata().eventId()).thenReturn("event-error");
        when(command.metadata().correlationId()).thenReturn("correlation-error");
        when(command.metadata().topic()).thenReturn("transactions");
        when(command.metadata().partition()).thenReturn(2);
        when(command.metadata().offset()).thenReturn(200L);

        doThrow(cause).when(useCase).process(command);

        BudgetEventIngestionFailedException exception = assertThrows(
                BudgetEventIngestionFailedException.class,
                () -> service.ingest(command)
        );

        assertSame(cause, exception.getCause());

        verify(useCase).process(command);

        assertNull(MDC.get(MDC_RUN_ID));
        assertNull(MDC.get(MDC_EVENT_ID));
        assertNull(MDC.get(MDC_CORRELATION_ID));
    }
}