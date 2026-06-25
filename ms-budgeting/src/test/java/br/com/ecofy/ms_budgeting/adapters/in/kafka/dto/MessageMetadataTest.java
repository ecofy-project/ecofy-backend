package br.com.ecofy.ms_budgeting.adapters.in.kafka.dto;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class MessageMetadataTest {

    private static final String EVENT_ID = "event-001";
    private static final Instant OCCURRED_AT = Instant.parse("2026-06-25T10:30:00Z");
    private static final String PRODUCER = "ms-transactions";
    private static final String TRACE_ID = "trace-001";

    @Test
    void shouldCreateMessageMetadataWithAllFields() {
        MessageMetadata metadata = new MessageMetadata(
                EVENT_ID,
                OCCURRED_AT,
                PRODUCER,
                TRACE_ID
        );

        assertEquals(EVENT_ID, metadata.eventId());
        assertEquals(OCCURRED_AT, metadata.occurredAt());
        assertEquals(PRODUCER, metadata.producer());
        assertEquals(TRACE_ID, metadata.traceId());
    }

    @Test
    void shouldCreateMessageMetadataWithNullFields() {
        MessageMetadata metadata = new MessageMetadata(
                null,
                null,
                null,
                null
        );

        assertNull(metadata.eventId());
        assertNull(metadata.occurredAt());
        assertNull(metadata.producer());
        assertNull(metadata.traceId());
    }

    @Test
    void shouldCompareMessageMetadataByAllRecordComponents() {
        MessageMetadata metadata = new MessageMetadata(
                EVENT_ID,
                OCCURRED_AT,
                PRODUCER,
                TRACE_ID
        );

        MessageMetadata sameMetadata = new MessageMetadata(
                EVENT_ID,
                OCCURRED_AT,
                PRODUCER,
                TRACE_ID
        );

        MessageMetadata differentMetadata = new MessageMetadata(
                "event-002",
                OCCURRED_AT,
                PRODUCER,
                TRACE_ID
        );

        assertEquals(metadata, metadata);
        assertEquals(metadata, sameMetadata);
        assertNotEquals(metadata, differentMetadata);
        assertNotEquals(metadata, null);
        assertNotEquals(metadata, "not-a-message-metadata");
    }

    @Test
    void shouldGenerateHashCodeUsingAllRecordComponents() {
        MessageMetadata metadata = new MessageMetadata(
                EVENT_ID,
                OCCURRED_AT,
                PRODUCER,
                TRACE_ID
        );

        MessageMetadata sameMetadata = new MessageMetadata(
                EVENT_ID,
                OCCURRED_AT,
                PRODUCER,
                TRACE_ID
        );

        assertEquals(metadata, sameMetadata);
        assertEquals(metadata.hashCode(), sameMetadata.hashCode());
    }

    @Test
    void shouldNotBeEqualWhenEventIdChanges() {
        MessageMetadata metadata = new MessageMetadata(
                EVENT_ID,
                OCCURRED_AT,
                PRODUCER,
                TRACE_ID
        );

        MessageMetadata differentMetadata = new MessageMetadata(
                "event-002",
                OCCURRED_AT,
                PRODUCER,
                TRACE_ID
        );

        assertNotEquals(metadata, differentMetadata);
    }

    @Test
    void shouldNotBeEqualWhenOccurredAtChanges() {
        MessageMetadata metadata = new MessageMetadata(
                EVENT_ID,
                OCCURRED_AT,
                PRODUCER,
                TRACE_ID
        );

        MessageMetadata differentMetadata = new MessageMetadata(
                EVENT_ID,
                OCCURRED_AT.plusSeconds(60),
                PRODUCER,
                TRACE_ID
        );

        assertNotEquals(metadata, differentMetadata);
    }

    @Test
    void shouldNotBeEqualWhenProducerChanges() {
        MessageMetadata metadata = new MessageMetadata(
                EVENT_ID,
                OCCURRED_AT,
                PRODUCER,
                TRACE_ID
        );

        MessageMetadata differentMetadata = new MessageMetadata(
                EVENT_ID,
                OCCURRED_AT,
                "ms-other-producer",
                TRACE_ID
        );

        assertNotEquals(metadata, differentMetadata);
    }

    @Test
    void shouldNotBeEqualWhenTraceIdChanges() {
        MessageMetadata metadata = new MessageMetadata(
                EVENT_ID,
                OCCURRED_AT,
                PRODUCER,
                TRACE_ID
        );

        MessageMetadata differentMetadata = new MessageMetadata(
                EVENT_ID,
                OCCURRED_AT,
                PRODUCER,
                "trace-002"
        );

        assertNotEquals(metadata, differentMetadata);
    }

    @Test
    void shouldReturnToStringWithRecordComponents() {
        MessageMetadata metadata = new MessageMetadata(
                EVENT_ID,
                OCCURRED_AT,
                PRODUCER,
                TRACE_ID
        );

        String result = metadata.toString();

        assertTrue(result.contains("MessageMetadata"));
        assertTrue(result.contains("eventId=" + EVENT_ID));
        assertTrue(result.contains("occurredAt=" + OCCURRED_AT));
        assertTrue(result.contains("producer=" + PRODUCER));
        assertTrue(result.contains("traceId=" + TRACE_ID));
    }
}