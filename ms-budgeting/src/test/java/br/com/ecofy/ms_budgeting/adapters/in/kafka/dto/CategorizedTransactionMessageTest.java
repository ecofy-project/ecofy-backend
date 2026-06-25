package br.com.ecofy.ms_budgeting.adapters.in.kafka.dto;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class CategorizedTransactionMessageTest {

    private static final UUID TRANSACTION_ID =
            UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

    private static final UUID USER_ID =
            UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

    private static final UUID CATEGORY_ID =
            UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");

    private static final BigDecimal AMOUNT = BigDecimal.valueOf(150.75);
    private static final String CURRENCY = "BRL";
    private static final LocalDate TRANSACTION_DATE = LocalDate.of(2026, 6, 25);

    @Test
    void shouldCreateCategorizedTransactionMessageWithAllFields() {
        MessageMetadata metadata = metadata();

        CategorizedTransactionMessage message = new CategorizedTransactionMessage(
                TRANSACTION_ID,
                USER_ID,
                CATEGORY_ID,
                AMOUNT,
                CURRENCY,
                TRANSACTION_DATE,
                metadata
        );

        assertEquals(TRANSACTION_ID, message.transactionId());
        assertEquals(USER_ID, message.userId());
        assertEquals(CATEGORY_ID, message.categoryId());
        assertEquals(AMOUNT, message.amount());
        assertEquals(CURRENCY, message.currency());
        assertEquals(TRANSACTION_DATE, message.transactionDate());
        assertSame(metadata, message.metadata());
    }

    @Test
    void shouldCreateCategorizedTransactionMessageWithNullFields() {
        CategorizedTransactionMessage message = new CategorizedTransactionMessage(
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );

        assertNull(message.transactionId());
        assertNull(message.userId());
        assertNull(message.categoryId());
        assertNull(message.amount());
        assertNull(message.currency());
        assertNull(message.transactionDate());
        assertNull(message.metadata());
    }

    @Test
    void shouldCompareMessagesByAllRecordComponents() {
        MessageMetadata metadata = metadata();

        CategorizedTransactionMessage message = new CategorizedTransactionMessage(
                TRANSACTION_ID,
                USER_ID,
                CATEGORY_ID,
                AMOUNT,
                CURRENCY,
                TRANSACTION_DATE,
                metadata
        );

        CategorizedTransactionMessage sameMessage = new CategorizedTransactionMessage(
                TRANSACTION_ID,
                USER_ID,
                CATEGORY_ID,
                AMOUNT,
                CURRENCY,
                TRANSACTION_DATE,
                metadata
        );

        CategorizedTransactionMessage differentMessage = new CategorizedTransactionMessage(
                UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd"),
                USER_ID,
                CATEGORY_ID,
                AMOUNT,
                CURRENCY,
                TRANSACTION_DATE,
                metadata
        );

        assertEquals(message, message);
        assertEquals(message, sameMessage);
        assertNotEquals(message, differentMessage);
        assertNotEquals(message, null);
        assertNotEquals(message, "not-a-message");
    }

    @Test
    void shouldGenerateHashCodeUsingAllRecordComponents() {
        MessageMetadata metadata = metadata();

        CategorizedTransactionMessage message = new CategorizedTransactionMessage(
                TRANSACTION_ID,
                USER_ID,
                CATEGORY_ID,
                AMOUNT,
                CURRENCY,
                TRANSACTION_DATE,
                metadata
        );

        CategorizedTransactionMessage sameMessage = new CategorizedTransactionMessage(
                TRANSACTION_ID,
                USER_ID,
                CATEGORY_ID,
                AMOUNT,
                CURRENCY,
                TRANSACTION_DATE,
                metadata
        );

        assertEquals(message, sameMessage);
        assertEquals(message.hashCode(), sameMessage.hashCode());
    }

    @Test
    void shouldNotBeEqualWhenMainComponentChanges() {
        MessageMetadata metadata = metadata();

        CategorizedTransactionMessage message = new CategorizedTransactionMessage(
                TRANSACTION_ID,
                USER_ID,
                CATEGORY_ID,
                AMOUNT,
                CURRENCY,
                TRANSACTION_DATE,
                metadata
        );

        CategorizedTransactionMessage differentMessage = new CategorizedTransactionMessage(
                UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd"),
                USER_ID,
                CATEGORY_ID,
                AMOUNT,
                CURRENCY,
                TRANSACTION_DATE,
                metadata
        );

        assertNotEquals(message, differentMessage);
    }

    @Test
    void shouldReturnToStringWithRecordComponents() {
        MessageMetadata metadata = metadata();

        CategorizedTransactionMessage message = new CategorizedTransactionMessage(
                TRANSACTION_ID,
                USER_ID,
                CATEGORY_ID,
                AMOUNT,
                CURRENCY,
                TRANSACTION_DATE,
                metadata
        );

        String result = message.toString();

        assertTrue(result.contains("CategorizedTransactionMessage"));
        assertTrue(result.contains("transactionId=" + TRANSACTION_ID));
        assertTrue(result.contains("userId=" + USER_ID));
        assertTrue(result.contains("categoryId=" + CATEGORY_ID));
        assertTrue(result.contains("amount=" + AMOUNT));
        assertTrue(result.contains("currency=" + CURRENCY));
        assertTrue(result.contains("transactionDate=" + TRANSACTION_DATE));
        assertTrue(result.contains("metadata="));
    }

    private static MessageMetadata metadata() {
        return mock(MessageMetadata.class);
    }
}