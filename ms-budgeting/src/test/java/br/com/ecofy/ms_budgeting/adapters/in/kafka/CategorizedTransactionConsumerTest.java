package br.com.ecofy.ms_budgeting.adapters.in.kafka;

import br.com.ecofy.ms_budgeting.adapters.in.kafka.dto.CategorizedTransactionMessage;
import br.com.ecofy.ms_budgeting.adapters.in.kafka.mapper.InboundEventMapper;
import br.com.ecofy.ms_budgeting.core.application.command.ProcessTransactionCommand;
import br.com.ecofy.ms_budgeting.core.application.service.BudgetEventIngestionService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.apache.kafka.common.record.TimestampType;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Correção Dia 6 (item #9): o consumer passou a delegar para o BudgetEventIngestionService,
 * que centraliza MDC/validação/logs/tratamento de exceções. A validação de campos obrigatórios
 * é coberta por BudgetEventIngestionServiceTest.
 */
class CategorizedTransactionConsumerTest {

    private static final String TOPIC = "categorized-transactions";
    private static final int PARTITION = 1;
    private static final long OFFSET = 10L;
    private static final String KEY = "transaction-key";
    private static final long TIMESTAMP = 1_767_000_000_000L;

    private static final UUID TRANSACTION_ID =
            UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

    private static final UUID USER_ID =
            UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

    private static final UUID CATEGORY_ID =
            UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");

    private final InboundEventMapper mapper = mock(InboundEventMapper.class);
    private final BudgetEventIngestionService ingestionService = mock(BudgetEventIngestionService.class);

    private final CategorizedTransactionConsumer consumer =
            new CategorizedTransactionConsumer(mapper, ingestionService);

    @Test
    void shouldConsumeMessageMapCommandAndDelegateToIngestionService() {
        CategorizedTransactionMessage msg = validMessage();
        ProcessTransactionCommand command = mock(ProcessTransactionCommand.class);

        ConsumerRecord<String, CategorizedTransactionMessage> record = record(
                msg,
                new RecordHeaders()
                        .add(header("eventId", "event-001"))
                        .add(header("correlationId", "correlation-001"))
        );

        when(mapper.toCommand(
                same(msg),
                any(UUID.class),
                eq("event-001"),
                eq("correlation-001"),
                same(record)
        )).thenReturn(command);

        consumer.onMessage(record);

        ArgumentCaptor<UUID> runIdCaptor = ArgumentCaptor.forClass(UUID.class);

        verify(mapper).toCommand(
                same(msg),
                runIdCaptor.capture(),
                eq("event-001"),
                eq("correlation-001"),
                same(record)
        );

        assertNotNull(runIdCaptor.getValue());

        verify(ingestionService).ingest(command);
    }

    @Test
    void shouldConsumeMessageWithMissingHeadersPassingNullEventIdAndCorrelationId() {
        CategorizedTransactionMessage msg = validMessage();
        ProcessTransactionCommand command = mock(ProcessTransactionCommand.class);

        ConsumerRecord<String, CategorizedTransactionMessage> record = record(
                msg,
                new RecordHeaders()
        );

        when(mapper.toCommand(
                same(msg),
                any(UUID.class),
                isNull(),
                isNull(),
                same(record)
        )).thenReturn(command);

        consumer.onMessage(record);

        verify(mapper).toCommand(
                same(msg),
                any(UUID.class),
                isNull(),
                isNull(),
                same(record)
        );

        verify(ingestionService).ingest(command);
    }

    @Test
    void shouldConsumeMessageWithNullHeaderValuesPassingNullEventIdAndCorrelationId() {
        CategorizedTransactionMessage msg = validMessage();
        ProcessTransactionCommand command = mock(ProcessTransactionCommand.class);

        ConsumerRecord<String, CategorizedTransactionMessage> record = record(
                msg,
                new RecordHeaders()
                        .add(new RecordHeader("eventId", null))
                        .add(new RecordHeader("correlationId", null))
        );

        when(mapper.toCommand(
                same(msg),
                any(UUID.class),
                isNull(),
                isNull(),
                same(record)
        )).thenReturn(command);

        consumer.onMessage(record);

        verify(mapper).toCommand(
                same(msg),
                any(UUID.class),
                isNull(),
                isNull(),
                same(record)
        );

        verify(ingestionService).ingest(command);
    }

    @Test
    void shouldIgnoreNullPayloadAndNotCallMapperOrIngestionService() {
        ConsumerRecord<String, CategorizedTransactionMessage> record = record(
                null,
                new RecordHeaders()
                        .add(header("eventId", "event-001"))
                        .add(header("correlationId", "correlation-001"))
        );

        consumer.onMessage(record);

        verifyNoInteractions(mapper);
        verifyNoInteractions(ingestionService);
    }

    @Test
    void shouldPropagateExceptionFromIngestionService() {
        CategorizedTransactionMessage msg = validMessage();
        ProcessTransactionCommand command = mock(ProcessTransactionCommand.class);

        ConsumerRecord<String, CategorizedTransactionMessage> record = record(
                msg,
                new RecordHeaders()
        );

        when(mapper.toCommand(same(msg), any(UUID.class), isNull(), isNull(), same(record)))
                .thenReturn(command);
        doThrow(new RuntimeException("ingestion boom")).when(ingestionService).ingest(command);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> consumer.onMessage(record));
        assertEquals("ingestion boom", ex.getMessage());

        verify(ingestionService).ingest(command);
    }

    @Test
    void shouldReturnHeaderAsStringUsingPrivateHeaderAsString() throws Exception {
        ConsumerRecord<String, CategorizedTransactionMessage> record = record(
                validMessage(),
                new RecordHeaders()
                        .add(header("eventId", "event-001"))
        );

        String result = invokeHeaderAsString(record, "eventId");

        assertEquals("event-001", result);
    }

    @Test
    void shouldReturnNullWhenHeaderDoesNotExistUsingPrivateHeaderAsString() throws Exception {
        ConsumerRecord<String, CategorizedTransactionMessage> record = record(
                validMessage(),
                new RecordHeaders()
        );

        String result = invokeHeaderAsString(record, "eventId");

        assertNull(result);
    }

    @Test
    void shouldReturnNullWhenHeaderValueIsNullUsingPrivateHeaderAsString() throws Exception {
        ConsumerRecord<String, CategorizedTransactionMessage> record = record(
                validMessage(),
                new RecordHeaders()
                        .add(new RecordHeader("eventId", null))
        );

        String result = invokeHeaderAsString(record, "eventId");

        assertNull(result);
    }

    private static CategorizedTransactionMessage validMessage() {
        CategorizedTransactionMessage msg = mock(CategorizedTransactionMessage.class);

        when(msg.transactionId()).thenReturn(TRANSACTION_ID);
        when(msg.userId()).thenReturn(USER_ID);
        when(msg.categoryId()).thenReturn(CATEGORY_ID);
        lenient().when(msg.amount()).thenReturn(BigDecimal.valueOf(150.75));
        lenient().when(msg.currency()).thenReturn("BRL");
        lenient().when(msg.transactionDate()).thenReturn(LocalDate.of(2026, 1, 1));

        return msg;
    }

    private static ConsumerRecord<String, CategorizedTransactionMessage> record(
            CategorizedTransactionMessage msg,
            Headers headers
    ) {
        return new ConsumerRecord<>(
                TOPIC,
                PARTITION,
                OFFSET,
                TIMESTAMP,
                TimestampType.CREATE_TIME,
                -1,
                -1,
                KEY,
                msg,
                headers,
                Optional.empty()
        );
    }

    private static Header header(String key, String value) {
        return new RecordHeader(
                key,
                value.getBytes(StandardCharsets.UTF_8)
        );
    }

    private static String invokeHeaderAsString(
            ConsumerRecord<?, ?> record,
            String key
    ) throws Exception {
        Method method = CategorizedTransactionConsumer.class.getDeclaredMethod(
                "headerAsString",
                ConsumerRecord.class,
                String.class
        );

        method.setAccessible(true);

        return (String) method.invoke(null, record, key);
    }
}
