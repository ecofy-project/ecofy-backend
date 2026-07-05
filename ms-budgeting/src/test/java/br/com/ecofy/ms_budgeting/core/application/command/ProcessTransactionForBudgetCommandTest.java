package br.com.ecofy.ms_budgeting.core.application.command;

import org.junit.jupiter.api.Test;

import java.lang.reflect.RecordComponent;
import java.time.Instant;
import java.util.Arrays;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ProcessTransactionForBudgetCommandTest {

    private static final UUID RUN_ID =
            UUID.fromString("11111111-1111-1111-1111-111111111111");

    private static final UUID TRANSACTION_ID =
            UUID.fromString("22222222-2222-2222-2222-222222222222");

    private static final UUID USER_ID =
            UUID.fromString("33333333-3333-3333-3333-333333333333");

    private static final UUID CATEGORY_ID =
            UUID.fromString("44444444-4444-4444-4444-444444444444");

    private static final Instant RECEIVED_AT =
            Instant.parse("2026-07-05T10:00:00Z");

    @Test
    void shouldCreateProcessTransactionForBudgetCommand() {
        ProcessTransactionForBudgetCommand command = command();

        assertNotNull(command);
        assertEquals(RUN_ID, command.runId());
        assertEquals(TRANSACTION_ID, command.transactionId());
        assertEquals(USER_ID, command.userId());
        assertEquals(CATEGORY_ID, command.categoryId());
        assertEquals(metadata(), command.metadata());
    }

    @Test
    void shouldAllowAllCommandFieldsNullBecauseRecordHasNoValidation() {
        ProcessTransactionForBudgetCommand command =
                new ProcessTransactionForBudgetCommand(
                        null,
                        null,
                        null,
                        null,
                        null
                );

        assertNotNull(command);
        assertNull(command.runId());
        assertNull(command.transactionId());
        assertNull(command.userId());
        assertNull(command.categoryId());
        assertNull(command.metadata());
    }

    @Test
    void shouldCreateEventMetadata() {
        ProcessTransactionForBudgetCommand.EventMetadata metadata = metadata();

        assertNotNull(metadata);
        assertEquals("event-001", metadata.eventId());
        assertEquals("correlation-001", metadata.correlationId());
        assertEquals("transactions.categorized", metadata.topic());
        assertEquals(1, metadata.partition());
        assertEquals(100L, metadata.offset());
        assertEquals("transaction-key-001", metadata.key());
        assertEquals(RECEIVED_AT, metadata.receivedAt());
    }

    @Test
    void shouldAllowAllMetadataReferenceFieldsNullBecauseRecordHasNoValidation() {
        ProcessTransactionForBudgetCommand.EventMetadata metadata =
                new ProcessTransactionForBudgetCommand.EventMetadata(
                        null,
                        null,
                        null,
                        -1,
                        -100L,
                        null,
                        null
                );

        assertNotNull(metadata);
        assertNull(metadata.eventId());
        assertNull(metadata.correlationId());
        assertNull(metadata.topic());
        assertEquals(-1, metadata.partition());
        assertEquals(-100L, metadata.offset());
        assertNull(metadata.key());
        assertNull(metadata.receivedAt());
    }

    @Test
    void shouldBeEqualWhenAllCommandComponentsAreEqual() {
        ProcessTransactionForBudgetCommand command = command();
        ProcessTransactionForBudgetCommand sameCommand = command();

        assertEquals(command, sameCommand);
        assertEquals(command.hashCode(), sameCommand.hashCode());
    }

    @Test
    void shouldReturnTrueWhenComparingSameCommandInstance() {
        ProcessTransactionForBudgetCommand command = command();

        assertEquals(command, command);
    }

    @Test
    void shouldNotBeEqualWhenRunIdIsDifferent() {
        ProcessTransactionForBudgetCommand different =
                new ProcessTransactionForBudgetCommand(
                        UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"),
                        TRANSACTION_ID,
                        USER_ID,
                        CATEGORY_ID,
                        metadata()
                );

        assertNotEquals(command(), different);
    }

    @Test
    void shouldNotBeEqualWhenTransactionIdIsDifferent() {
        ProcessTransactionForBudgetCommand different =
                new ProcessTransactionForBudgetCommand(
                        RUN_ID,
                        UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"),
                        USER_ID,
                        CATEGORY_ID,
                        metadata()
                );

        assertNotEquals(command(), different);
    }

    @Test
    void shouldNotBeEqualWhenUserIdIsDifferent() {
        ProcessTransactionForBudgetCommand different =
                new ProcessTransactionForBudgetCommand(
                        RUN_ID,
                        TRANSACTION_ID,
                        UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc"),
                        CATEGORY_ID,
                        metadata()
                );

        assertNotEquals(command(), different);
    }

    @Test
    void shouldNotBeEqualWhenCategoryIdIsDifferent() {
        ProcessTransactionForBudgetCommand different =
                new ProcessTransactionForBudgetCommand(
                        RUN_ID,
                        TRANSACTION_ID,
                        USER_ID,
                        UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd"),
                        metadata()
                );

        assertNotEquals(command(), different);
    }

    @Test
    void shouldNotBeEqualWhenMetadataIsDifferent() {
        ProcessTransactionForBudgetCommand different =
                new ProcessTransactionForBudgetCommand(
                        RUN_ID,
                        TRANSACTION_ID,
                        USER_ID,
                        CATEGORY_ID,
                        new ProcessTransactionForBudgetCommand.EventMetadata(
                                "event-999",
                                "correlation-001",
                                "transactions.categorized",
                                1,
                                100L,
                                "transaction-key-001",
                                RECEIVED_AT
                        )
                );

        assertNotEquals(command(), different);
    }

    @Test
    void shouldNotBeEqualToNullCommand() {
        assertNotEquals(null, command());
    }

    @Test
    void shouldNotBeEqualToDifferentCommandType() {
        assertNotEquals("not-a-command", command());
    }

    @Test
    void shouldBeEqualWhenAllCommandComponentsAreNull() {
        ProcessTransactionForBudgetCommand command =
                new ProcessTransactionForBudgetCommand(
                        null,
                        null,
                        null,
                        null,
                        null
                );

        ProcessTransactionForBudgetCommand sameCommand =
                new ProcessTransactionForBudgetCommand(
                        null,
                        null,
                        null,
                        null,
                        null
                );

        assertEquals(command, sameCommand);
        assertEquals(command.hashCode(), sameCommand.hashCode());
    }

    @Test
    void shouldGenerateCommandToStringWithAllComponents() {
        ProcessTransactionForBudgetCommand command = command();

        String result = command.toString();

        assertNotNull(result);
        assertTrue(result.contains("ProcessTransactionForBudgetCommand"));
        assertTrue(result.contains("runId=" + RUN_ID));
        assertTrue(result.contains("transactionId=" + TRANSACTION_ID));
        assertTrue(result.contains("userId=" + USER_ID));
        assertTrue(result.contains("categoryId=" + CATEGORY_ID));
        assertTrue(result.contains("metadata="));
    }

    @Test
    void shouldGenerateCommandToStringWithNullComponents() {
        ProcessTransactionForBudgetCommand command =
                new ProcessTransactionForBudgetCommand(
                        null,
                        null,
                        null,
                        null,
                        null
                );

        String result = command.toString();

        assertNotNull(result);
        assertTrue(result.contains("ProcessTransactionForBudgetCommand"));
        assertTrue(result.contains("runId=null"));
        assertTrue(result.contains("transactionId=null"));
        assertTrue(result.contains("userId=null"));
        assertTrue(result.contains("categoryId=null"));
        assertTrue(result.contains("metadata=null"));
    }

    @Test
    void shouldBeEqualWhenAllMetadataComponentsAreEqual() {
        ProcessTransactionForBudgetCommand.EventMetadata metadata = metadata();
        ProcessTransactionForBudgetCommand.EventMetadata sameMetadata = metadata();

        assertEquals(metadata, sameMetadata);
        assertEquals(metadata.hashCode(), sameMetadata.hashCode());
    }

    @Test
    void shouldReturnTrueWhenComparingSameMetadataInstance() {
        ProcessTransactionForBudgetCommand.EventMetadata metadata = metadata();

        assertEquals(metadata, metadata);
    }

    @Test
    void shouldNotBeEqualWhenMetadataEventIdIsDifferent() {
        ProcessTransactionForBudgetCommand.EventMetadata different =
                new ProcessTransactionForBudgetCommand.EventMetadata(
                        "event-999",
                        "correlation-001",
                        "transactions.categorized",
                        1,
                        100L,
                        "transaction-key-001",
                        RECEIVED_AT
                );

        assertNotEquals(metadata(), different);
    }

    @Test
    void shouldNotBeEqualWhenMetadataCorrelationIdIsDifferent() {
        ProcessTransactionForBudgetCommand.EventMetadata different =
                new ProcessTransactionForBudgetCommand.EventMetadata(
                        "event-001",
                        "correlation-999",
                        "transactions.categorized",
                        1,
                        100L,
                        "transaction-key-001",
                        RECEIVED_AT
                );

        assertNotEquals(metadata(), different);
    }

    @Test
    void shouldNotBeEqualWhenMetadataTopicIsDifferent() {
        ProcessTransactionForBudgetCommand.EventMetadata different =
                new ProcessTransactionForBudgetCommand.EventMetadata(
                        "event-001",
                        "correlation-001",
                        "other.topic",
                        1,
                        100L,
                        "transaction-key-001",
                        RECEIVED_AT
                );

        assertNotEquals(metadata(), different);
    }

    @Test
    void shouldNotBeEqualWhenMetadataPartitionIsDifferent() {
        ProcessTransactionForBudgetCommand.EventMetadata different =
                new ProcessTransactionForBudgetCommand.EventMetadata(
                        "event-001",
                        "correlation-001",
                        "transactions.categorized",
                        2,
                        100L,
                        "transaction-key-001",
                        RECEIVED_AT
                );

        assertNotEquals(metadata(), different);
    }

    @Test
    void shouldNotBeEqualWhenMetadataOffsetIsDifferent() {
        ProcessTransactionForBudgetCommand.EventMetadata different =
                new ProcessTransactionForBudgetCommand.EventMetadata(
                        "event-001",
                        "correlation-001",
                        "transactions.categorized",
                        1,
                        200L,
                        "transaction-key-001",
                        RECEIVED_AT
                );

        assertNotEquals(metadata(), different);
    }

    @Test
    void shouldNotBeEqualWhenMetadataKeyIsDifferent() {
        ProcessTransactionForBudgetCommand.EventMetadata different =
                new ProcessTransactionForBudgetCommand.EventMetadata(
                        "event-001",
                        "correlation-001",
                        "transactions.categorized",
                        1,
                        100L,
                        "transaction-key-999",
                        RECEIVED_AT
                );

        assertNotEquals(metadata(), different);
    }

    @Test
    void shouldNotBeEqualWhenMetadataReceivedAtIsDifferent() {
        ProcessTransactionForBudgetCommand.EventMetadata different =
                new ProcessTransactionForBudgetCommand.EventMetadata(
                        "event-001",
                        "correlation-001",
                        "transactions.categorized",
                        1,
                        100L,
                        "transaction-key-001",
                        RECEIVED_AT.plusSeconds(1)
                );

        assertNotEquals(metadata(), different);
    }

    @Test
    void shouldNotBeEqualToNullMetadata() {
        assertNotEquals(null, metadata());
    }

    @Test
    void shouldNotBeEqualToDifferentMetadataType() {
        assertNotEquals("not-metadata", metadata());
    }

    @Test
    void shouldBeEqualWhenAllMetadataReferenceComponentsAreNull() {
        ProcessTransactionForBudgetCommand.EventMetadata metadata =
                new ProcessTransactionForBudgetCommand.EventMetadata(
                        null,
                        null,
                        null,
                        0,
                        0L,
                        null,
                        null
                );

        ProcessTransactionForBudgetCommand.EventMetadata sameMetadata =
                new ProcessTransactionForBudgetCommand.EventMetadata(
                        null,
                        null,
                        null,
                        0,
                        0L,
                        null,
                        null
                );

        assertEquals(metadata, sameMetadata);
        assertEquals(metadata.hashCode(), sameMetadata.hashCode());
    }

    @Test
    void shouldGenerateMetadataToStringWithAllComponents() {
        ProcessTransactionForBudgetCommand.EventMetadata metadata = metadata();

        String result = metadata.toString();

        assertNotNull(result);
        assertTrue(result.contains("EventMetadata"));
        assertTrue(result.contains("eventId=event-001"));
        assertTrue(result.contains("correlationId=correlation-001"));
        assertTrue(result.contains("topic=transactions.categorized"));
        assertTrue(result.contains("partition=1"));
        assertTrue(result.contains("offset=100"));
        assertTrue(result.contains("key=transaction-key-001"));
        assertTrue(result.contains("receivedAt=" + RECEIVED_AT));
    }

    @Test
    void shouldGenerateMetadataToStringWithNullComponents() {
        ProcessTransactionForBudgetCommand.EventMetadata metadata =
                new ProcessTransactionForBudgetCommand.EventMetadata(
                        null,
                        null,
                        null,
                        0,
                        0L,
                        null,
                        null
                );

        String result = metadata.toString();

        assertNotNull(result);
        assertTrue(result.contains("EventMetadata"));
        assertTrue(result.contains("eventId=null"));
        assertTrue(result.contains("correlationId=null"));
        assertTrue(result.contains("topic=null"));
        assertTrue(result.contains("partition=0"));
        assertTrue(result.contains("offset=0"));
        assertTrue(result.contains("key=null"));
        assertTrue(result.contains("receivedAt=null"));
    }

    @Test
    void shouldBeRecord() {
        assertTrue(ProcessTransactionForBudgetCommand.class.isRecord());
    }

    @Test
    void shouldEventMetadataBeRecord() {
        assertTrue(ProcessTransactionForBudgetCommand.EventMetadata.class.isRecord());
    }

    @Test
    void shouldHaveExpectedCommandRecordComponents() {
        RecordComponent[] components =
                ProcessTransactionForBudgetCommand.class.getRecordComponents();

        assertNotNull(components);
        assertEquals(5, components.length);

        assertArrayEquals(
                new String[]{
                        "runId",
                        "transactionId",
                        "userId",
                        "categoryId",
                        "metadata"
                },
                Arrays.stream(components)
                        .map(RecordComponent::getName)
                        .toArray(String[]::new)
        );

        assertArrayEquals(
                new Class<?>[]{
                        UUID.class,
                        UUID.class,
                        UUID.class,
                        UUID.class,
                        ProcessTransactionForBudgetCommand.EventMetadata.class
                },
                Arrays.stream(components)
                        .map(RecordComponent::getType)
                        .toArray(Class<?>[]::new)
        );
    }

    @Test
    void shouldHaveExpectedEventMetadataRecordComponents() {
        RecordComponent[] components =
                ProcessTransactionForBudgetCommand.EventMetadata.class.getRecordComponents();

        assertNotNull(components);
        assertEquals(7, components.length);

        assertArrayEquals(
                new String[]{
                        "eventId",
                        "correlationId",
                        "topic",
                        "partition",
                        "offset",
                        "key",
                        "receivedAt"
                },
                Arrays.stream(components)
                        .map(RecordComponent::getName)
                        .toArray(String[]::new)
        );

        assertArrayEquals(
                new Class<?>[]{
                        String.class,
                        String.class,
                        String.class,
                        int.class,
                        long.class,
                        String.class,
                        Instant.class
                },
                Arrays.stream(components)
                        .map(RecordComponent::getType)
                        .toArray(Class<?>[]::new)
        );
    }

    private static ProcessTransactionForBudgetCommand command() {
        return new ProcessTransactionForBudgetCommand(
                RUN_ID,
                TRANSACTION_ID,
                USER_ID,
                CATEGORY_ID,
                metadata()
        );
    }

    private static ProcessTransactionForBudgetCommand.EventMetadata metadata() {
        return new ProcessTransactionForBudgetCommand.EventMetadata(
                "event-001",
                "correlation-001",
                "transactions.categorized",
                1,
                100L,
                "transaction-key-001",
                RECEIVED_AT
        );
    }
}