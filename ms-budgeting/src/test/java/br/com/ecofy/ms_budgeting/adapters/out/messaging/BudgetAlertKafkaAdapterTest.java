package br.com.ecofy.ms_budgeting.adapters.out.messaging;

import br.com.ecofy.ms_budgeting.adapters.out.messaging.dto.BudgetAlertEvent;
import br.com.ecofy.ms_budgeting.config.BudgetingProperties;
import br.com.ecofy.ms_budgeting.core.domain.BudgetAlert;
import br.com.ecofy.ms_budgeting.core.domain.enums.AlertSeverity;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.header.Header;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class BudgetAlertKafkaAdapterTest {

    private static final String TOPIC = "budget.alerts";

    private static final UUID USER_ID =
            UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID BUDGET_ID =
            UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID CATEGORY_ID =
            UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
    private static final UUID CONSUMPTION_ID =
            UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

    private static final BigDecimal LIMIT = new BigDecimal("1000.00");
    private static final BigDecimal CONSUMED = new BigDecimal("800.00");
    private static final Integer PCT = 80;

    private static final LocalDate PERIOD_START = LocalDate.of(2026, 6, 1);
    private static final LocalDate PERIOD_END = LocalDate.of(2026, 6, 30);

    private static final Instant FIXED_INSTANT = Instant.parse("2026-06-25T10:30:00Z");
    private static final Clock FIXED_CLOCK = Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC);

    @Test
    void shouldCreateAdapterWhenAllDependenciesAreValid() {
        BudgetAlertKafkaAdapter adapter =
                new BudgetAlertKafkaAdapter(kafkaTemplate(), propsWithTopic(TOPIC), FIXED_CLOCK);
        assertNotNull(adapter);
    }

    @Test
    void shouldThrowNullPointerExceptionWhenKafkaTemplateIsNull() {
        NullPointerException ex = assertThrows(NullPointerException.class,
                () -> new BudgetAlertKafkaAdapter(null, propsWithTopic(TOPIC), FIXED_CLOCK));
        assertEquals("kafkaTemplate must not be null", ex.getMessage());
    }

    @Test
    void shouldThrowNullPointerExceptionWhenPropsIsNull() {
        NullPointerException ex = assertThrows(NullPointerException.class,
                () -> new BudgetAlertKafkaAdapter(kafkaTemplate(), null, FIXED_CLOCK));
        assertEquals("props must not be null", ex.getMessage());
    }

    @Test
    void shouldThrowNullPointerExceptionWhenClockIsNull() {
        NullPointerException ex = assertThrows(NullPointerException.class,
                () -> new BudgetAlertKafkaAdapter(kafkaTemplate(), propsWithTopic(TOPIC), null));
        assertEquals("clock must not be null", ex.getMessage());
    }

    @Test
    void shouldPublishNotificationCompatibleEventWithConsumptionIdHeader() {
        KafkaTemplate<String, Object> kafkaTemplate = kafkaTemplate();
        BudgetAlertKafkaAdapter adapter =
                new BudgetAlertKafkaAdapter(kafkaTemplate, propsWithTopic("  " + TOPIC + "  "), FIXED_CLOCK);

        BudgetAlert alert = enrichedAlert(CONSUMPTION_ID);
        stubKafkaSendSuccess(kafkaTemplate);

        adapter.publish(alert);

        ArgumentCaptor<ProducerRecord<String, Object>> recordCaptor = producerRecordCaptor();
        verify(kafkaTemplate).send(recordCaptor.capture());
        ProducerRecord<String, Object> record = recordCaptor.getValue();

        assertEquals(TOPIC, record.topic());
        assertEquals(BUDGET_ID.toString(), record.key());
        assertInstanceOf(BudgetAlertEvent.class, record.value());

        BudgetAlertEvent event = (BudgetAlertEvent) record.value();
        assertEquals(USER_ID, event.userId());
        assertEquals(BUDGET_ID, event.budgetId());
        assertEquals(CATEGORY_ID, event.categoryId());
        assertEquals(LIMIT, event.limitAmount());
        assertEquals(CONSUMED, event.consumedAmount());
        assertEquals(PCT, event.consumedPct());
        assertEquals("WARNING", event.severity());
        assertNotNull(event.metadata());
        assertEquals(FIXED_INSTANT, event.metadata().occurredAt());
        assertEquals("ms-budgeting", event.metadata().source());

        assertEquals(event.metadata().eventId(), headerValue(record, "eventId"));
        assertEquals(BUDGET_ID.toString(), headerValue(record, "budgetId"));
        assertEquals("WARNING", headerValue(record, "severity"));
        assertEquals(CONSUMPTION_ID.toString(), headerValue(record, "consumptionId"));
    }

    @Test
    void shouldPublishWithoutConsumptionIdHeader() {
        KafkaTemplate<String, Object> kafkaTemplate = kafkaTemplate();
        BudgetAlertKafkaAdapter adapter =
                new BudgetAlertKafkaAdapter(kafkaTemplate, propsWithTopic(TOPIC), FIXED_CLOCK);

        BudgetAlert alert = enrichedAlert(null);
        stubKafkaSendSuccess(kafkaTemplate);

        adapter.publish(alert);

        ArgumentCaptor<ProducerRecord<String, Object>> recordCaptor = producerRecordCaptor();
        verify(kafkaTemplate).send(recordCaptor.capture());
        ProducerRecord<String, Object> record = recordCaptor.getValue();

        assertNull(record.headers().lastHeader("consumptionId"));
        BudgetAlertEvent event = (BudgetAlertEvent) record.value();
        assertEquals(USER_ID, event.userId());
        assertEquals(event.metadata().eventId(), headerValue(record, "eventId"));
    }

    @Test
    void shouldExecuteFailureCallbackWhenKafkaSendFails() {
        KafkaTemplate<String, Object> kafkaTemplate = kafkaTemplate();
        BudgetAlertKafkaAdapter adapter =
                new BudgetAlertKafkaAdapter(kafkaTemplate, propsWithTopic(TOPIC), FIXED_CLOCK);

        BudgetAlert alert = enrichedAlert(CONSUMPTION_ID);
        stubKafkaSendFailure(kafkaTemplate,
                CompletableFuture.failedFuture(new RuntimeException("Kafka unavailable")));

        assertDoesNotThrow(() -> adapter.publish(alert));
        verify(kafkaTemplate).send(anyProducerRecord());
    }

    @Test
    void shouldThrowNullPointerExceptionWhenAlertIsNull() {
        KafkaTemplate<String, Object> kafkaTemplate = kafkaTemplate();
        BudgetAlertKafkaAdapter adapter =
                new BudgetAlertKafkaAdapter(kafkaTemplate, propsWithTopic(TOPIC), FIXED_CLOCK);

        NullPointerException ex = assertThrows(NullPointerException.class, () -> adapter.publish(null));
        assertEquals("alert must not be null", ex.getMessage());
        verifyNoInteractions(kafkaTemplate);
    }

    @Test
    void shouldThrowIllegalStateExceptionWhenTopicIsNull() {
        KafkaTemplate<String, Object> kafkaTemplate = kafkaTemplate();
        BudgetAlertKafkaAdapter adapter =
                new BudgetAlertKafkaAdapter(kafkaTemplate, propsWithTopic(null), FIXED_CLOCK);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> adapter.publish(enrichedAlert(CONSUMPTION_ID)));
        assertEquals("budgetAlert topic must not be blank", ex.getMessage());
        verify(kafkaTemplate, never()).send(anyProducerRecord());
    }

    @Test
    void shouldThrowIllegalStateExceptionWhenTopicIsBlank() {
        KafkaTemplate<String, Object> kafkaTemplate = kafkaTemplate();
        BudgetAlertKafkaAdapter adapter =
                new BudgetAlertKafkaAdapter(kafkaTemplate, propsWithTopic("   "), FIXED_CLOCK);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> adapter.publish(enrichedAlert(CONSUMPTION_ID)));
        assertEquals("budgetAlert topic must not be blank", ex.getMessage());
        verify(kafkaTemplate, never()).send(anyProducerRecord());
    }

    @Test
    void shouldPropagateExceptionWhenAlertHasNullBudgetId() {
        KafkaTemplate<String, Object> kafkaTemplate = kafkaTemplate();
        BudgetAlertKafkaAdapter adapter =
                new BudgetAlertKafkaAdapter(kafkaTemplate, propsWithTopic(TOPIC), FIXED_CLOCK);

        BudgetAlert alert = mock(BudgetAlert.class);
        when(alert.getBudgetId()).thenReturn(null);
        lenient().when(alert.getSeverity()).thenReturn(AlertSeverity.WARNING);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> adapter.publish(alert));
        assertEquals("alert.budgetId must not be null", ex.getMessage());
        verify(kafkaTemplate, never()).send(anyProducerRecord());
    }

    @Test
    void shouldPropagateExceptionWhenAlertHasNullSeverity() {
        KafkaTemplate<String, Object> kafkaTemplate = kafkaTemplate();
        BudgetAlertKafkaAdapter adapter =
                new BudgetAlertKafkaAdapter(kafkaTemplate, propsWithTopic(TOPIC), FIXED_CLOCK);

        BudgetAlert alert = mock(BudgetAlert.class);
        when(alert.getBudgetId()).thenReturn(BUDGET_ID);
        when(alert.getSeverity()).thenReturn(null);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> adapter.publish(alert));
        assertEquals("alert.severity must not be null", ex.getMessage());
        verify(kafkaTemplate, never()).send(anyProducerRecord());
    }

    @Test
    void shouldInvokePrivateBytesWithNullValue() throws Exception {
        Method method = BudgetAlertKafkaAdapter.class.getDeclaredMethod("bytes", String.class);
        method.setAccessible(true);
        assertNull(method.invoke(null, new Object[]{null}));
    }

    @Test
    void shouldInvokePrivateBytesWithTextValue() throws Exception {
        Method method = BudgetAlertKafkaAdapter.class.getDeclaredMethod("bytes", String.class);
        method.setAccessible(true);
        byte[] result = (byte[]) method.invoke(null, "abc");
        assertArrayEquals("abc".getBytes(StandardCharsets.UTF_8), result);
    }

    @Test
    void shouldInvokePrivateRequireNonBlankWithValidValue() throws Exception {
        Method method = BudgetAlertKafkaAdapter.class.getDeclaredMethod("requireNonBlank", String.class, String.class);
        method.setAccessible(true);
        assertEquals("value", method.invoke(null, "  value  ", "field"));
    }

    @Test
    void shouldInvokePrivateRequireNonBlankWithNullValue() throws Exception {
        Method method = BudgetAlertKafkaAdapter.class.getDeclaredMethod("requireNonBlank", String.class, String.class);
        method.setAccessible(true);
        InvocationTargetException ex = assertThrows(InvocationTargetException.class,
                () -> method.invoke(null, null, "field"));
        assertInstanceOf(IllegalStateException.class, ex.getCause());
        assertEquals("field must not be blank", ex.getCause().getMessage());
    }

    @Test
    void shouldInvokePrivateRequireNonBlankWithBlankValue() throws Exception {
        Method method = BudgetAlertKafkaAdapter.class.getDeclaredMethod("requireNonBlank", String.class, String.class);
        method.setAccessible(true);
        InvocationTargetException ex = assertThrows(InvocationTargetException.class,
                () -> method.invoke(null, "   ", "field"));
        assertInstanceOf(IllegalStateException.class, ex.getCause());
        assertEquals("field must not be blank", ex.getCause().getMessage());
    }

    @SuppressWarnings("unchecked")
    private static KafkaTemplate<String, Object> kafkaTemplate() {
        return mock(KafkaTemplate.class);
    }

    private static BudgetingProperties propsWithTopic(String topic) {
        BudgetingProperties props = mock(BudgetingProperties.class);
        BudgetingProperties.Topics topics = mock(BudgetingProperties.Topics.class);
        when(props.topics()).thenReturn(topics);
        when(topics.budgetAlert()).thenReturn(topic);
        return props;
    }

    private static BudgetAlert enrichedAlert(UUID consumptionId) {
        BudgetAlert alert = mock(BudgetAlert.class);
        when(alert.getBudgetId()).thenReturn(BUDGET_ID);
        when(alert.getConsumptionId()).thenReturn(consumptionId);
        when(alert.getSeverity()).thenReturn(AlertSeverity.WARNING);
        when(alert.getUserId()).thenReturn(USER_ID);
        when(alert.getCategoryId()).thenReturn(CATEGORY_ID);
        when(alert.getLimitAmount()).thenReturn(LIMIT);
        when(alert.getConsumedAmount()).thenReturn(CONSUMED);
        when(alert.getConsumedPct()).thenReturn(PCT);
        lenient().when(alert.getPeriodStart()).thenReturn(PERIOD_START);
        lenient().when(alert.getPeriodEnd()).thenReturn(PERIOD_END);
        return alert;
    }

    private static void stubKafkaSendSuccess(KafkaTemplate<String, Object> kafkaTemplate) {
        doReturn(successfulSend()).when(kafkaTemplate).send(anyProducerRecord());
    }

    private static void stubKafkaSendFailure(KafkaTemplate<String, Object> kafkaTemplate,
                                             CompletableFuture<SendResult<String, Object>> failedFuture) {
        doReturn(failedFuture).when(kafkaTemplate).send(anyProducerRecord());
    }

    @SuppressWarnings("unchecked")
    private static ProducerRecord<String, Object> anyProducerRecord() {
        return (ProducerRecord<String, Object>) any(ProducerRecord.class);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static ArgumentCaptor<ProducerRecord<String, Object>> producerRecordCaptor() {
        return ArgumentCaptor.forClass((Class) ProducerRecord.class);
    }

    private static CompletableFuture<SendResult<String, Object>> successfulSend() {
        ProducerRecord<String, Object> producerRecord =
                new ProducerRecord<>(TOPIC, BUDGET_ID.toString(), new Object());
        RecordMetadata metadata = new RecordMetadata(
                new TopicPartition(TOPIC, 1), 0L, 0, FIXED_INSTANT.toEpochMilli(), 10, 20);
        return CompletableFuture.completedFuture(new SendResult<>(producerRecord, metadata));
    }

    private static String headerValue(ProducerRecord<String, Object> record, String headerName) {
        Header header = record.headers().lastHeader(headerName);
        assertNotNull(header);
        return new String(header.value(), StandardCharsets.UTF_8);
    }
}
