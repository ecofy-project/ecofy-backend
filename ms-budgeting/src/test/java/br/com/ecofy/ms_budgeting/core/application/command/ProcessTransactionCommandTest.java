package br.com.ecofy.ms_budgeting.core.application.command;

import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.RecordComponent;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ProcessTransactionCommandTest {

    private static final UUID RUN_ID =
            UUID.fromString("11111111-1111-1111-1111-111111111111");

    private static final UUID TRANSACTION_ID =
            UUID.fromString("22222222-2222-2222-2222-222222222222");

    private static final UUID USER_ID =
            UUID.fromString("33333333-3333-3333-3333-333333333333");

    private static final UUID CATEGORY_ID =
            UUID.fromString("44444444-4444-4444-4444-444444444444");

    private static final BigDecimal AMOUNT =
            new BigDecimal("150.75");

    private static final LocalDate TRANSACTION_DATE =
            LocalDate.of(2026, 7, 5);

    private static final Instant RECEIVED_AT =
            Instant.parse("2026-07-05T10:00:00Z");

    @Test
    void shouldCreateProcessTransactionCommand() {
        ProcessTransactionCommand command = command();

        assertNotNull(command);
        assertEquals(RUN_ID, command.runId());
        assertEquals(TRANSACTION_ID, command.transactionId());
        assertEquals(USER_ID, command.userId());
        assertEquals(CATEGORY_ID, command.categoryId());
        assertEquals(AMOUNT, command.amount());
        assertEquals("BRL", command.currency());
        assertEquals(TRANSACTION_DATE, command.transactionDate());
        assertEquals(metadata(), command.metadata());
    }

    @Test
    void shouldNormalizeCurrencyToUppercaseAndTrim() {
        ProcessTransactionCommand command = new ProcessTransactionCommand(
                RUN_ID,
                TRANSACTION_ID,
                USER_ID,
                CATEGORY_ID,
                AMOUNT,
                " brl ",
                TRANSACTION_DATE,
                metadata()
        );

        assertEquals("BRL", command.currency());
    }

    @Test
    void shouldAcceptUsdCurrency() {
        ProcessTransactionCommand command = new ProcessTransactionCommand(
                RUN_ID,
                TRANSACTION_ID,
                USER_ID,
                CATEGORY_ID,
                AMOUNT,
                "usd",
                TRANSACTION_DATE,
                metadata()
        );

        assertEquals("USD", command.currency());
    }

    @Test
    void shouldAcceptEurCurrency() {
        ProcessTransactionCommand command = new ProcessTransactionCommand(
                RUN_ID,
                TRANSACTION_ID,
                USER_ID,
                CATEGORY_ID,
                AMOUNT,
                "eur",
                TRANSACTION_DATE,
                metadata()
        );

        assertEquals("EUR", command.currency());
    }

    @Test
    void shouldAllowZeroAmount() {
        ProcessTransactionCommand command = new ProcessTransactionCommand(
                RUN_ID,
                TRANSACTION_ID,
                USER_ID,
                CATEGORY_ID,
                BigDecimal.ZERO,
                "BRL",
                TRANSACTION_DATE,
                metadata()
        );

        assertEquals(BigDecimal.ZERO, command.amount());
    }

    @Test
    void shouldAllowNullCategoryIdBecauseItIsNotRequired() {
        ProcessTransactionCommand command = new ProcessTransactionCommand(
                RUN_ID,
                TRANSACTION_ID,
                USER_ID,
                null,
                AMOUNT,
                "BRL",
                TRANSACTION_DATE,
                metadata()
        );

        assertNotNull(command);
        assertNull(command.categoryId());
    }

    @Test
    void shouldThrowIllegalArgumentExceptionWhenRunIdIsNull() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new ProcessTransactionCommand(
                        null,
                        TRANSACTION_ID,
                        USER_ID,
                        CATEGORY_ID,
                        AMOUNT,
                        "BRL",
                        TRANSACTION_DATE,
                        metadata()
                )
        );

        assertEquals("runId must not be null", exception.getMessage());
    }

    @Test
    void shouldThrowIllegalArgumentExceptionWhenTransactionIdIsNull() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new ProcessTransactionCommand(
                        RUN_ID,
                        null,
                        USER_ID,
                        CATEGORY_ID,
                        AMOUNT,
                        "BRL",
                        TRANSACTION_DATE,
                        metadata()
                )
        );

        assertEquals("transactionId must not be null", exception.getMessage());
    }

    @Test
    void shouldThrowIllegalArgumentExceptionWhenUserIdIsNull() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new ProcessTransactionCommand(
                        RUN_ID,
                        TRANSACTION_ID,
                        null,
                        CATEGORY_ID,
                        AMOUNT,
                        "BRL",
                        TRANSACTION_DATE,
                        metadata()
                )
        );

        assertEquals("userId must not be null", exception.getMessage());
    }

    @Test
    void shouldThrowIllegalArgumentExceptionWhenAmountIsNull() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new ProcessTransactionCommand(
                        RUN_ID,
                        TRANSACTION_ID,
                        USER_ID,
                        CATEGORY_ID,
                        null,
                        "BRL",
                        TRANSACTION_DATE,
                        metadata()
                )
        );

        assertEquals("amount must not be null", exception.getMessage());
    }

    @Test
    void shouldThrowIllegalArgumentExceptionWhenTransactionDateIsNull() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new ProcessTransactionCommand(
                        RUN_ID,
                        TRANSACTION_ID,
                        USER_ID,
                        CATEGORY_ID,
                        AMOUNT,
                        "BRL",
                        null,
                        metadata()
                )
        );

        assertEquals("transactionDate must not be null", exception.getMessage());
    }

    @Test
    void shouldThrowIllegalArgumentExceptionWhenMetadataIsNull() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new ProcessTransactionCommand(
                        RUN_ID,
                        TRANSACTION_ID,
                        USER_ID,
                        CATEGORY_ID,
                        AMOUNT,
                        "BRL",
                        TRANSACTION_DATE,
                        null
                )
        );

        assertEquals("metadata must not be null", exception.getMessage());
    }

    @Test
    void shouldThrowIllegalArgumentExceptionWhenAmountIsNegative() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new ProcessTransactionCommand(
                        RUN_ID,
                        TRANSACTION_ID,
                        USER_ID,
                        CATEGORY_ID,
                        new BigDecimal("-0.01"),
                        "BRL",
                        TRANSACTION_DATE,
                        metadata()
                )
        );

        assertEquals("amount must be >= 0", exception.getMessage());
    }

    @Test
    void shouldThrowIllegalArgumentExceptionWhenCurrencyIsNull() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new ProcessTransactionCommand(
                        RUN_ID,
                        TRANSACTION_ID,
                        USER_ID,
                        CATEGORY_ID,
                        AMOUNT,
                        null,
                        TRANSACTION_DATE,
                        metadata()
                )
        );

        assertEquals("currency must not be null", exception.getMessage());
    }

    @Test
    void shouldThrowIllegalArgumentExceptionWhenCurrencyIsBlank() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new ProcessTransactionCommand(
                        RUN_ID,
                        TRANSACTION_ID,
                        USER_ID,
                        CATEGORY_ID,
                        AMOUNT,
                        "   ",
                        TRANSACTION_DATE,
                        metadata()
                )
        );

        assertEquals("Unsupported currency: ", exception.getMessage());
    }

    @Test
    void shouldThrowIllegalArgumentExceptionWhenCurrencyIsUnsupported() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new ProcessTransactionCommand(
                        RUN_ID,
                        TRANSACTION_ID,
                        USER_ID,
                        CATEGORY_ID,
                        AMOUNT,
                        " gbp ",
                        TRANSACTION_DATE,
                        metadata()
                )
        );

        assertEquals("Unsupported currency: GBP", exception.getMessage());
    }

    @Test
    void shouldPrioritizeRequiredFieldsBeforeNegativeAmountValidation() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new ProcessTransactionCommand(
                        null,
                        null,
                        null,
                        CATEGORY_ID,
                        new BigDecimal("-1.00"),
                        null,
                        null,
                        null
                )
        );

        assertEquals("runId must not be null", exception.getMessage());
    }

    @Test
    void shouldPrioritizeAmountValidationBeforeCurrencyValidation() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new ProcessTransactionCommand(
                        RUN_ID,
                        TRANSACTION_ID,
                        USER_ID,
                        CATEGORY_ID,
                        new BigDecimal("-1.00"),
                        null,
                        TRANSACTION_DATE,
                        metadata()
                )
        );

        assertEquals("amount must be >= 0", exception.getMessage());
    }

    @Test
    void shouldCreateEventMetadata() {
        ProcessTransactionCommand.EventMetadata metadata = metadata();

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
    void shouldAllowNullableOptionalEventMetadataFields() {
        ProcessTransactionCommand.EventMetadata metadata =
                new ProcessTransactionCommand.EventMetadata(
                        null,
                        null,
                        "transactions.categorized",
                        -1,
                        -10L,
                        null,
                        RECEIVED_AT
                );

        assertNotNull(metadata);
        assertNull(metadata.eventId());
        assertNull(metadata.correlationId());
        assertEquals("transactions.categorized", metadata.topic());
        assertEquals(-1, metadata.partition());
        assertEquals(-10L, metadata.offset());
        assertNull(metadata.key());
        assertEquals(RECEIVED_AT, metadata.receivedAt());
    }

    @Test
    void shouldThrowIllegalArgumentExceptionWhenMetadataTopicIsNull() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new ProcessTransactionCommand.EventMetadata(
                        "event-001",
                        "correlation-001",
                        null,
                        1,
                        100L,
                        "transaction-key-001",
                        RECEIVED_AT
                )
        );

        assertEquals("topic is required", exception.getMessage());
    }

    @Test
    void shouldThrowIllegalArgumentExceptionWhenMetadataTopicIsBlank() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new ProcessTransactionCommand.EventMetadata(
                        "event-001",
                        "correlation-001",
                        "   ",
                        1,
                        100L,
                        "transaction-key-001",
                        RECEIVED_AT
                )
        );

        assertEquals("topic is required", exception.getMessage());
    }

    @Test
    void shouldThrowIllegalArgumentExceptionWhenMetadataReceivedAtIsNull() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new ProcessTransactionCommand.EventMetadata(
                        "event-001",
                        "correlation-001",
                        "transactions.categorized",
                        1,
                        100L,
                        "transaction-key-001",
                        null
                )
        );

        assertEquals("receivedAt must not be null", exception.getMessage());
    }

    @Test
    void shouldPrioritizeTopicValidationBeforeReceivedAtValidation() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new ProcessTransactionCommand.EventMetadata(
                        "event-001",
                        "correlation-001",
                        null,
                        1,
                        100L,
                        "transaction-key-001",
                        null
                )
        );

        assertEquals("topic is required", exception.getMessage());
    }

    @Test
    void shouldBeEqualWhenAllCommandComponentsAreEqual() {
        ProcessTransactionCommand command = command();
        ProcessTransactionCommand sameCommand = command();

        assertEquals(command, sameCommand);
        assertEquals(command.hashCode(), sameCommand.hashCode());
    }

    @Test
    void shouldReturnTrueWhenComparingSameCommandInstance() {
        ProcessTransactionCommand command = command();

        assertEquals(command, command);
    }

    @Test
    void shouldNotBeEqualWhenRunIdIsDifferent() {
        assertNotEquals(
                command(),
                new ProcessTransactionCommand(
                        UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"),
                        TRANSACTION_ID,
                        USER_ID,
                        CATEGORY_ID,
                        AMOUNT,
                        "BRL",
                        TRANSACTION_DATE,
                        metadata()
                )
        );
    }

    @Test
    void shouldNotBeEqualWhenTransactionIdIsDifferent() {
        assertNotEquals(
                command(),
                new ProcessTransactionCommand(
                        RUN_ID,
                        UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"),
                        USER_ID,
                        CATEGORY_ID,
                        AMOUNT,
                        "BRL",
                        TRANSACTION_DATE,
                        metadata()
                )
        );
    }

    @Test
    void shouldNotBeEqualWhenUserIdIsDifferent() {
        assertNotEquals(
                command(),
                new ProcessTransactionCommand(
                        RUN_ID,
                        TRANSACTION_ID,
                        UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc"),
                        CATEGORY_ID,
                        AMOUNT,
                        "BRL",
                        TRANSACTION_DATE,
                        metadata()
                )
        );
    }

    @Test
    void shouldNotBeEqualWhenCategoryIdIsDifferent() {
        assertNotEquals(
                command(),
                new ProcessTransactionCommand(
                        RUN_ID,
                        TRANSACTION_ID,
                        USER_ID,
                        UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd"),
                        AMOUNT,
                        "BRL",
                        TRANSACTION_DATE,
                        metadata()
                )
        );
    }

    @Test
    void shouldNotBeEqualWhenAmountIsDifferent() {
        assertNotEquals(
                command(),
                new ProcessTransactionCommand(
                        RUN_ID,
                        TRANSACTION_ID,
                        USER_ID,
                        CATEGORY_ID,
                        new BigDecimal("999.99"),
                        "BRL",
                        TRANSACTION_DATE,
                        metadata()
                )
        );
    }

    @Test
    void shouldNotBeEqualWhenCurrencyIsDifferent() {
        assertNotEquals(
                command(),
                new ProcessTransactionCommand(
                        RUN_ID,
                        TRANSACTION_ID,
                        USER_ID,
                        CATEGORY_ID,
                        AMOUNT,
                        "USD",
                        TRANSACTION_DATE,
                        metadata()
                )
        );
    }

    @Test
    void shouldNotBeEqualWhenTransactionDateIsDifferent() {
        assertNotEquals(
                command(),
                new ProcessTransactionCommand(
                        RUN_ID,
                        TRANSACTION_ID,
                        USER_ID,
                        CATEGORY_ID,
                        AMOUNT,
                        "BRL",
                        TRANSACTION_DATE.plusDays(1),
                        metadata()
                )
        );
    }

    @Test
    void shouldNotBeEqualWhenMetadataIsDifferent() {
        assertNotEquals(
                command(),
                new ProcessTransactionCommand(
                        RUN_ID,
                        TRANSACTION_ID,
                        USER_ID,
                        CATEGORY_ID,
                        AMOUNT,
                        "BRL",
                        TRANSACTION_DATE,
                        new ProcessTransactionCommand.EventMetadata(
                                "event-999",
                                "correlation-001",
                                "transactions.categorized",
                                1,
                                100L,
                                "transaction-key-001",
                                RECEIVED_AT
                        )
                )
        );
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
    void shouldGenerateCommandToStringWithAllComponents() {
        ProcessTransactionCommand command = command();

        String result = command.toString();

        assertNotNull(result);
        assertTrue(result.contains("ProcessTransactionCommand"));
        assertTrue(result.contains("runId=" + RUN_ID));
        assertTrue(result.contains("transactionId=" + TRANSACTION_ID));
        assertTrue(result.contains("userId=" + USER_ID));
        assertTrue(result.contains("categoryId=" + CATEGORY_ID));
        assertTrue(result.contains("amount=" + AMOUNT));
        assertTrue(result.contains("currency=BRL"));
        assertTrue(result.contains("transactionDate=" + TRANSACTION_DATE));
        assertTrue(result.contains("metadata="));
    }

    @Test
    void shouldBeEqualWhenAllMetadataComponentsAreEqual() {
        ProcessTransactionCommand.EventMetadata metadata = metadata();
        ProcessTransactionCommand.EventMetadata sameMetadata = metadata();

        assertEquals(metadata, sameMetadata);
        assertEquals(metadata.hashCode(), sameMetadata.hashCode());
    }

    @Test
    void shouldReturnTrueWhenComparingSameMetadataInstance() {
        ProcessTransactionCommand.EventMetadata metadata = metadata();

        assertEquals(metadata, metadata);
    }

    @Test
    void shouldNotBeEqualWhenMetadataEventIdIsDifferent() {
        assertNotEquals(
                metadata(),
                new ProcessTransactionCommand.EventMetadata(
                        "event-999",
                        "correlation-001",
                        "transactions.categorized",
                        1,
                        100L,
                        "transaction-key-001",
                        RECEIVED_AT
                )
        );
    }

    @Test
    void shouldNotBeEqualWhenMetadataCorrelationIdIsDifferent() {
        assertNotEquals(
                metadata(),
                new ProcessTransactionCommand.EventMetadata(
                        "event-001",
                        "correlation-999",
                        "transactions.categorized",
                        1,
                        100L,
                        "transaction-key-001",
                        RECEIVED_AT
                )
        );
    }

    @Test
    void shouldNotBeEqualWhenMetadataTopicIsDifferent() {
        assertNotEquals(
                metadata(),
                new ProcessTransactionCommand.EventMetadata(
                        "event-001",
                        "correlation-001",
                        "other.topic",
                        1,
                        100L,
                        "transaction-key-001",
                        RECEIVED_AT
                )
        );
    }

    @Test
    void shouldNotBeEqualWhenMetadataPartitionIsDifferent() {
        assertNotEquals(
                metadata(),
                new ProcessTransactionCommand.EventMetadata(
                        "event-001",
                        "correlation-001",
                        "transactions.categorized",
                        2,
                        100L,
                        "transaction-key-001",
                        RECEIVED_AT
                )
        );
    }

    @Test
    void shouldNotBeEqualWhenMetadataOffsetIsDifferent() {
        assertNotEquals(
                metadata(),
                new ProcessTransactionCommand.EventMetadata(
                        "event-001",
                        "correlation-001",
                        "transactions.categorized",
                        1,
                        200L,
                        "transaction-key-001",
                        RECEIVED_AT
                )
        );
    }

    @Test
    void shouldNotBeEqualWhenMetadataKeyIsDifferent() {
        assertNotEquals(
                metadata(),
                new ProcessTransactionCommand.EventMetadata(
                        "event-001",
                        "correlation-001",
                        "transactions.categorized",
                        1,
                        100L,
                        "transaction-key-999",
                        RECEIVED_AT
                )
        );
    }

    @Test
    void shouldNotBeEqualWhenMetadataReceivedAtIsDifferent() {
        assertNotEquals(
                metadata(),
                new ProcessTransactionCommand.EventMetadata(
                        "event-001",
                        "correlation-001",
                        "transactions.categorized",
                        1,
                        100L,
                        "transaction-key-001",
                        RECEIVED_AT.plusSeconds(1)
                )
        );
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
    void shouldGenerateMetadataToStringWithAllComponents() {
        ProcessTransactionCommand.EventMetadata metadata = metadata();

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
    void shouldBeRecord() {
        assertTrue(ProcessTransactionCommand.class.isRecord());
    }

    @Test
    void shouldEventMetadataBeRecord() {
        assertTrue(ProcessTransactionCommand.EventMetadata.class.isRecord());
    }

    @Test
    void shouldHaveExpectedCommandRecordComponents() {
        RecordComponent[] components =
                ProcessTransactionCommand.class.getRecordComponents();

        assertNotNull(components);
        assertEquals(8, components.length);

        assertArrayEquals(
                new String[]{
                        "runId",
                        "transactionId",
                        "userId",
                        "categoryId",
                        "amount",
                        "currency",
                        "transactionDate",
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
                        BigDecimal.class,
                        String.class,
                        LocalDate.class,
                        ProcessTransactionCommand.EventMetadata.class
                },
                Arrays.stream(components)
                        .map(RecordComponent::getType)
                        .toArray(Class<?>[]::new)
        );
    }

    @Test
    void shouldHaveExpectedEventMetadataRecordComponents() {
        RecordComponent[] components =
                ProcessTransactionCommand.EventMetadata.class.getRecordComponents();

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

    @Test
    void shouldInvokePrivateNormalizeCurrencyMethod() throws Exception {
        Method method = ProcessTransactionCommand.class.getDeclaredMethod(
                "normalizeCurrency",
                String.class
        );
        method.setAccessible(true);

        assertEquals("BRL", method.invoke(null, " brl "));
        assertEquals("USD", method.invoke(null, " usd "));
        assertEquals("EUR", method.invoke(null, " eur "));
    }

    @Test
    void shouldThrowFromPrivateNormalizeCurrencyMethodWhenRawIsNull()
            throws Exception {
        Method method = ProcessTransactionCommand.class.getDeclaredMethod(
                "normalizeCurrency",
                String.class
        );
        method.setAccessible(true);

        InvocationTargetException exception = assertThrows(
                InvocationTargetException.class,
                () -> method.invoke(null, (Object) null)
        );

        assertInstanceOf(IllegalArgumentException.class, exception.getCause());
        assertEquals("currency must not be null", exception.getCause().getMessage());
    }

    @Test
    void shouldThrowFromPrivateNormalizeCurrencyMethodWhenCurrencyIsUnsupported()
            throws Exception {
        Method method = ProcessTransactionCommand.class.getDeclaredMethod(
                "normalizeCurrency",
                String.class
        );
        method.setAccessible(true);

        InvocationTargetException exception = assertThrows(
                InvocationTargetException.class,
                () -> method.invoke(null, "jpy")
        );

        assertInstanceOf(IllegalArgumentException.class, exception.getCause());
        assertEquals("Unsupported currency: JPY", exception.getCause().getMessage());
    }

    @Test
    void shouldInvokePrivateRequireMethodWhenValueIsNotNull() throws Exception {
        Method method = ProcessTransactionCommand.class.getDeclaredMethod(
                "require",
                Object.class,
                String.class
        );
        method.setAccessible(true);

        assertDoesNotThrow(() -> method.invoke(null, "value", "field"));
    }

    @Test
    void shouldThrowFromPrivateRequireMethodWhenValueIsNull() throws Exception {
        Method method = ProcessTransactionCommand.class.getDeclaredMethod(
                "require",
                Object.class,
                String.class
        );
        method.setAccessible(true);

        InvocationTargetException exception = assertThrows(
                InvocationTargetException.class,
                () -> method.invoke(null, null, "field")
        );

        assertInstanceOf(IllegalArgumentException.class, exception.getCause());
        assertEquals("field must not be null", exception.getCause().getMessage());
    }

    private static ProcessTransactionCommand command() {
        return new ProcessTransactionCommand(
                RUN_ID,
                TRANSACTION_ID,
                USER_ID,
                CATEGORY_ID,
                AMOUNT,
                "BRL",
                TRANSACTION_DATE,
                metadata()
        );
    }

    private static ProcessTransactionCommand.EventMetadata metadata() {
        return new ProcessTransactionCommand.EventMetadata(
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