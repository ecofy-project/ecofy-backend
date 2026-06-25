package br.com.ecofy.ms_budgeting.adapters.in.kafka.mapper;

import br.com.ecofy.ms_budgeting.adapters.in.kafka.dto.CategorizedTransactionMessage;
import br.com.ecofy.ms_budgeting.core.application.command.ProcessTransactionCommand;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.apache.kafka.common.record.TimestampType;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class InboundEventMapperTest {

    private static final String TOPIC = "categorized-transactions";
    private static final int PARTITION = 2;
    private static final long OFFSET = 123L;
    private static final String KEY = "transaction-key";
    private static final long TIMESTAMP = 1_767_000_000_000L;

    private static final UUID TRANSACTION_ID =
            UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

    private static final UUID USER_ID =
            UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

    private static final UUID CATEGORY_ID =
            UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");

    private final InboundEventMapper mapper = new InboundEventMapper();

    @Test
    void shouldMapMessageAndRecordToCommandUsingHeadersFromKafkaRecord() {
        CategorizedTransactionMessage msg = validMessage();

        ConsumerRecord<String, CategorizedTransactionMessage> record = record(
                msg,
                new RecordHeaders()
                        .add(header("event_id", "event-001"))
                        .add(header("correlation_id", "correlation-001"))
        );

        ProcessTransactionCommand command = mapper.toCommand(msg, record);

        assertNotNull(command);

        Object metadata = metadata(command);

        assertNotNull(readAny(command, "runId"));
        assertEquals(TRANSACTION_ID, readAny(command, "transactionId"));
        assertEquals(USER_ID, readAny(command, "userId"));
        assertEquals(CATEGORY_ID, readAny(command, "categoryId"));
        assertEquals(BigDecimal.valueOf(150.75), readAny(command, "amount"));
        assertEquals("BRL", readAny(command, "currency"));
        assertEquals(LocalDate.of(2026, 1, 1), readAny(command, "transactionDate"));

        assertEquals("event-001", readAny(metadata, "eventId"));
        assertEquals("correlation-001", readAny(metadata, "correlationId"));
        assertEquals(TOPIC, readAny(metadata, "topic"));
        assertEquals(PARTITION, readAny(metadata, "partition"));
        assertEquals(OFFSET, readAny(metadata, "offset"));
        assertEquals(KEY, readAny(metadata, "key"));

        verify(msg).transactionId();
        verify(msg).userId();
        verify(msg).categoryId();
        verify(msg).amount();
        verify(msg).currency();
        verify(msg).transactionDate();
    }

    @Test
    void shouldMapMessageUsingExplicitRunIdEventIdAndCorrelationId() {
        CategorizedTransactionMessage msg = validMessage();
        UUID runId = UUID.fromString("11111111-1111-1111-1111-111111111111");

        ConsumerRecord<String, CategorizedTransactionMessage> record = record(
                msg,
                new RecordHeaders()
        );

        ProcessTransactionCommand command = mapper.toCommand(
                msg,
                runId,
                "  event-explicit-001  ",
                "  correlation-explicit-001  ",
                record
        );

        Object metadata = metadata(command);

        assertEquals(runId, readAny(command, "runId"));
        assertEquals(TRANSACTION_ID, readAny(command, "transactionId"));
        assertEquals(USER_ID, readAny(command, "userId"));
        assertEquals(CATEGORY_ID, readAny(command, "categoryId"));
        assertEquals(BigDecimal.valueOf(150.75), readAny(command, "amount"));
        assertEquals("BRL", readAny(command, "currency"));
        assertEquals(LocalDate.of(2026, 1, 1), readAny(command, "transactionDate"));

        assertEquals("event-explicit-001", readAny(metadata, "eventId"));
        assertEquals("correlation-explicit-001", readAny(metadata, "correlationId"));
        assertEquals(TOPIC, readAny(metadata, "topic"));
        assertEquals(PARTITION, readAny(metadata, "partition"));
        assertEquals(OFFSET, readAny(metadata, "offset"));
        assertEquals(KEY, readAny(metadata, "key"));
    }

    @Test
    void shouldConvertBlankExplicitEventIdAndCorrelationIdToNull() {
        CategorizedTransactionMessage msg = validMessage();
        UUID runId = UUID.fromString("22222222-2222-2222-2222-222222222222");

        ConsumerRecord<String, CategorizedTransactionMessage> record = record(
                msg,
                new RecordHeaders()
        );

        ProcessTransactionCommand command = mapper.toCommand(
                msg,
                runId,
                "   ",
                "\t",
                record
        );

        Object metadata = metadata(command);

        assertEquals(runId, readAny(command, "runId"));
        assertNull(readAny(metadata, "eventId"));
        assertNull(readAny(metadata, "correlationId"));
    }

    @Test
    void shouldReturnNullMetadataIdsWhenKafkaHeadersAreMissing() {
        CategorizedTransactionMessage msg = validMessage();

        ConsumerRecord<String, CategorizedTransactionMessage> record = record(
                msg,
                new RecordHeaders()
        );

        ProcessTransactionCommand command = mapper.toCommand(msg, record);

        Object metadata = metadata(command);

        assertNull(readAny(metadata, "eventId"));
        assertNull(readAny(metadata, "correlationId"));
    }

    @Test
    void shouldReturnNullMetadataIdsWhenKafkaHeadersAreBlank() {
        CategorizedTransactionMessage msg = validMessage();

        ConsumerRecord<String, CategorizedTransactionMessage> record = record(
                msg,
                new RecordHeaders()
                        .add(header("event_id", "   "))
                        .add(header("correlation_id", "\t"))
        );

        ProcessTransactionCommand command = mapper.toCommand(msg, record);

        Object metadata = metadata(command);

        assertNull(readAny(metadata, "eventId"));
        assertNull(readAny(metadata, "correlationId"));
    }

    @Test
    void shouldUseLastKafkaHeaderWhenHeaderAppearsMoreThanOnce() {
        CategorizedTransactionMessage msg = validMessage();

        ConsumerRecord<String, CategorizedTransactionMessage> record = record(
                msg,
                new RecordHeaders()
                        .add(header("event_id", "event-old"))
                        .add(header("event_id", "event-new"))
                        .add(header("correlation_id", "correlation-old"))
                        .add(header("correlation_id", "correlation-new"))
        );

        ProcessTransactionCommand command = mapper.toCommand(msg, record);

        Object metadata = metadata(command);

        assertEquals("event-new", readAny(metadata, "eventId"));
        assertEquals("correlation-new", readAny(metadata, "correlationId"));
    }

    @Test
    void shouldThrowExceptionWhenMessageIsNullInFullOverload() {
        UUID runId = UUID.fromString("33333333-3333-3333-3333-333333333333");

        ConsumerRecord<String, CategorizedTransactionMessage> record = record(
                validMessage(),
                new RecordHeaders()
        );

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> mapper.toCommand(null, runId, "event-001", "correlation-001", record)
        );

        assertEquals("msg must not be null", exception.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenRunIdIsNullInFullOverload() {
        CategorizedTransactionMessage msg = validMessage();

        ConsumerRecord<String, CategorizedTransactionMessage> record = record(
                msg,
                new RecordHeaders()
        );

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> mapper.toCommand(msg, null, "event-001", "correlation-001", record)
        );

        assertEquals("runId must not be null", exception.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenRecordIsNullInFullOverload() {
        CategorizedTransactionMessage msg = validMessage();
        UUID runId = UUID.fromString("44444444-4444-4444-4444-444444444444");

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> mapper.toCommand(msg, runId, "event-001", "correlation-001", null)
        );

        assertEquals("record must not be null", exception.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenRecordIsNullInRecommendedOverload() {
        CategorizedTransactionMessage msg = validMessage();

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> mapper.toCommand(msg, null)
        );

        assertEquals("record must not be null", exception.getMessage());
    }

    @Test
    void shouldReturnNullWhenFirstHeaderReceivesNullRecord() throws Exception {
        String value = invokeFirstHeaderAsString(null, "event_id");

        assertNull(value);
    }

    @Test
    void shouldReturnNullWhenRecordHeadersAreNull() throws Exception {
        @SuppressWarnings("unchecked")
        ConsumerRecord<String, CategorizedTransactionMessage> record = mock(ConsumerRecord.class);

        when(record.headers()).thenReturn(null);

        String value = invokeFirstHeaderAsString(record, "event_id");

        assertNull(value);
    }

    @Test
    void shouldReturnNullWhenHeaderDoesNotExist() throws Exception {
        ConsumerRecord<String, CategorizedTransactionMessage> record = record(
                validMessage(),
                new RecordHeaders()
        );

        String value = invokeFirstHeaderAsString(record, "event_id");

        assertNull(value);
    }

    @Test
    void shouldReturnNullWhenHeaderValueIsNull() throws Exception {
        ConsumerRecord<String, CategorizedTransactionMessage> record = record(
                validMessage(),
                new RecordHeaders().add(new RecordHeader("event_id", null))
        );

        String value = invokeFirstHeaderAsString(record, "event_id");

        assertNull(value);
    }

    @Test
    void shouldReturnNullWhenHeaderValueIsBlank() throws Exception {
        ConsumerRecord<String, CategorizedTransactionMessage> record = record(
                validMessage(),
                new RecordHeaders().add(header("event_id", "   "))
        );

        String value = invokeFirstHeaderAsString(record, "event_id");

        assertNull(value);
    }

    @Test
    void shouldReturnHeaderValueAsString() throws Exception {
        ConsumerRecord<String, CategorizedTransactionMessage> record = record(
                validMessage(),
                new RecordHeaders().add(header("event_id", "event-001"))
        );

        String value = invokeFirstHeaderAsString(record, "event_id");

        assertEquals("event-001", value);
    }

    @Test
    void shouldConvertNullBlankAndTrimmedValuesUsingBlankToNull() throws Exception {
        assertNull(invokeBlankToNull(null));
        assertNull(invokeBlankToNull("   "));
        assertEquals("event-001", invokeBlankToNull("  event-001  "));
    }

    private static CategorizedTransactionMessage validMessage() {
        CategorizedTransactionMessage msg = mock(CategorizedTransactionMessage.class);

        when(msg.transactionId()).thenReturn(TRANSACTION_ID);
        when(msg.userId()).thenReturn(USER_ID);
        when(msg.categoryId()).thenReturn(CATEGORY_ID);
        when(msg.amount()).thenReturn(BigDecimal.valueOf(150.75));
        when(msg.currency()).thenReturn("BRL");
        when(msg.transactionDate()).thenReturn(LocalDate.of(2026, 1, 1));

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

    private static Object metadata(ProcessTransactionCommand command) {
        return readAny(command, "metadata", "eventMetadata");
    }

    private static Object readAny(Object target, String... methodNames) {
        for (String methodName : methodNames) {
            try {
                Method method = target.getClass().getMethod(methodName);
                return method.invoke(target);
            } catch (NoSuchMethodException ignored) {
                // tenta o próximo nome
            } catch (Exception exception) {
                throw new AssertionError("Failed to read method: " + methodName, exception);
            }
        }

        throw new AssertionError(
                "None of the methods exist on "
                        + target.getClass().getName()
                        + ": "
                        + String.join(", ", methodNames)
        );
    }

    private static String invokeFirstHeaderAsString(
            ConsumerRecord<?, ?> record,
            String headerKey
    ) throws Exception {
        Method method = InboundEventMapper.class.getDeclaredMethod(
                "firstHeaderAsString",
                ConsumerRecord.class,
                String.class
        );

        method.setAccessible(true);

        return (String) method.invoke(null, record, headerKey);
    }

    private static String invokeBlankToNull(String value) throws Exception {
        Method method = InboundEventMapper.class.getDeclaredMethod(
                "blankToNull",
                String.class
        );

        method.setAccessible(true);

        return (String) method.invoke(null, value);
    }
}