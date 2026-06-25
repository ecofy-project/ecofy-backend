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

    private static final UUID BUDGET_ID =
            UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

    private static final UUID CONSUMPTION_ID =
            UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

    private static final LocalDate PERIOD_START =
            LocalDate.of(2026, 6, 1);

    private static final LocalDate PERIOD_END =
            LocalDate.of(2026, 6, 30);

    private static final Instant FIXED_INSTANT =
            Instant.parse("2026-06-25T10:30:00Z");

    private static final Clock FIXED_CLOCK =
            Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC);

    @Test
    void shouldCreateAdapterWhenAllDependenciesAreValid() {
        KafkaTemplate<String, Object> kafkaTemplate = kafkaTemplate();
        BudgetingProperties props = propsWithTopic(TOPIC);

        BudgetAlertKafkaAdapter adapter =
                new BudgetAlertKafkaAdapter(kafkaTemplate, props, FIXED_CLOCK);

        assertNotNull(adapter);
    }

    @Test
    void shouldThrowNullPointerExceptionWhenKafkaTemplateIsNull() {
        BudgetingProperties props = propsWithTopic(TOPIC);

        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> new BudgetAlertKafkaAdapter(null, props, FIXED_CLOCK)
        );

        assertEquals("kafkaTemplate must not be null", exception.getMessage());
    }

    @Test
    void shouldThrowNullPointerExceptionWhenPropsIsNull() {
        KafkaTemplate<String, Object> kafkaTemplate = kafkaTemplate();

        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> new BudgetAlertKafkaAdapter(kafkaTemplate, null, FIXED_CLOCK)
        );

        assertEquals("props must not be null", exception.getMessage());
    }

    @Test
    void shouldThrowNullPointerExceptionWhenClockIsNull() {
        KafkaTemplate<String, Object> kafkaTemplate = kafkaTemplate();
        BudgetingProperties props = propsWithTopic(TOPIC);

        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> new BudgetAlertKafkaAdapter(kafkaTemplate, props, null)
        );

        assertEquals("clock must not be null", exception.getMessage());
    }

    @Test
    void shouldPublishBudgetAlertWithConsumptionIdAndSuccessCallback() {
        KafkaTemplate<String, Object> kafkaTemplate = kafkaTemplate();
        BudgetingProperties props = propsWithTopic("  " + TOPIC + "  ");

        BudgetAlertKafkaAdapter adapter =
                new BudgetAlertKafkaAdapter(kafkaTemplate, props, FIXED_CLOCK);

        BudgetAlert alert = alert(
                BUDGET_ID,
                CONSUMPTION_ID,
                anyAlertSeverity(),
                " Budget reached alert threshold ",
                PERIOD_START,
                PERIOD_END
        );

        stubKafkaSendSuccess(kafkaTemplate);

        adapter.publish(alert);

        ArgumentCaptor<ProducerRecord<String, Object>> recordCaptor =
                producerRecordCaptor();

        verify(kafkaTemplate).send(recordCaptor.capture());

        ProducerRecord<String, Object> record = recordCaptor.getValue();

        assertEquals(TOPIC, record.topic());
        assertEquals(BUDGET_ID.toString(), record.key());

        assertInstanceOf(BudgetAlertEvent.class, record.value());

        BudgetAlertEvent event = (BudgetAlertEvent) record.value();

        assertNotNull(event.eventId());
        assertEquals(FIXED_INSTANT, event.occurredAt());
        assertEquals(BUDGET_ID, event.budgetId());
        assertEquals(CONSUMPTION_ID, event.consumptionId());
        assertEquals(alert.getSeverity(), event.severity());
        assertEquals("Budget reached alert threshold", event.message());
        assertEquals(PERIOD_START, event.periodStart());
        assertEquals(PERIOD_END, event.periodEnd());

        assertEquals(event.eventId(), headerValue(record, "eventId"));
        assertEquals(BUDGET_ID.toString(), headerValue(record, "budgetId"));
        assertEquals(alert.getSeverity().toString(), headerValue(record, "severity"));
        assertEquals(CONSUMPTION_ID.toString(), headerValue(record, "consumptionId"));
    }

    @Test
    void shouldPublishBudgetAlertWithoutConsumptionId() {
        KafkaTemplate<String, Object> kafkaTemplate = kafkaTemplate();
        BudgetingProperties props = propsWithTopic(TOPIC);

        BudgetAlertKafkaAdapter adapter =
                new BudgetAlertKafkaAdapter(kafkaTemplate, props, FIXED_CLOCK);

        BudgetAlert alert = alert(
                BUDGET_ID,
                null,
                anyAlertSeverity(),
                "Budget reached alert threshold",
                PERIOD_START,
                PERIOD_END
        );

        stubKafkaSendSuccess(kafkaTemplate);

        adapter.publish(alert);

        ArgumentCaptor<ProducerRecord<String, Object>> recordCaptor =
                producerRecordCaptor();

        verify(kafkaTemplate).send(recordCaptor.capture());

        ProducerRecord<String, Object> record = recordCaptor.getValue();

        assertEquals(TOPIC, record.topic());
        assertEquals(BUDGET_ID.toString(), record.key());
        assertNull(record.headers().lastHeader("consumptionId"));

        BudgetAlertEvent event = (BudgetAlertEvent) record.value();

        assertNull(event.consumptionId());
        assertEquals(event.eventId(), headerValue(record, "eventId"));
        assertEquals(BUDGET_ID.toString(), headerValue(record, "budgetId"));
        assertEquals(alert.getSeverity().toString(), headerValue(record, "severity"));
    }

    @Test
    void shouldExecuteFailureCallbackWhenKafkaSendFails() {
        KafkaTemplate<String, Object> kafkaTemplate = kafkaTemplate();
        BudgetingProperties props = propsWithTopic(TOPIC);

        BudgetAlertKafkaAdapter adapter =
                new BudgetAlertKafkaAdapter(kafkaTemplate, props, FIXED_CLOCK);

        BudgetAlert alert = alert(
                BUDGET_ID,
                CONSUMPTION_ID,
                anyAlertSeverity(),
                "Budget reached alert threshold",
                PERIOD_START,
                PERIOD_END
        );

        CompletableFuture<SendResult<String, Object>> failedFuture =
                CompletableFuture.failedFuture(new RuntimeException("Kafka unavailable"));

        stubKafkaSendFailure(kafkaTemplate, failedFuture);

        assertDoesNotThrow(() -> adapter.publish(alert));

        verify(kafkaTemplate).send(anyProducerRecord());
    }

    @Test
    void shouldThrowNullPointerExceptionWhenAlertIsNull() {
        KafkaTemplate<String, Object> kafkaTemplate = kafkaTemplate();
        BudgetingProperties props = propsWithTopic(TOPIC);

        BudgetAlertKafkaAdapter adapter =
                new BudgetAlertKafkaAdapter(kafkaTemplate, props, FIXED_CLOCK);

        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> adapter.publish(null)
        );

        assertEquals("alert must not be null", exception.getMessage());

        verifyNoInteractions(kafkaTemplate);
    }

    @Test
    void shouldThrowIllegalStateExceptionWhenTopicIsNull() {
        KafkaTemplate<String, Object> kafkaTemplate = kafkaTemplate();
        BudgetingProperties props = propsWithTopic(null);

        BudgetAlertKafkaAdapter adapter =
                new BudgetAlertKafkaAdapter(kafkaTemplate, props, FIXED_CLOCK);

        BudgetAlert alert = validAlert();

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> adapter.publish(alert)
        );

        assertEquals("budgetAlert topic must not be blank", exception.getMessage());

        verify(kafkaTemplate, never()).send(anyProducerRecord());
    }

    @Test
    void shouldThrowIllegalStateExceptionWhenTopicIsBlank() {
        KafkaTemplate<String, Object> kafkaTemplate = kafkaTemplate();
        BudgetingProperties props = propsWithTopic("   ");

        BudgetAlertKafkaAdapter adapter =
                new BudgetAlertKafkaAdapter(kafkaTemplate, props, FIXED_CLOCK);

        BudgetAlert alert = validAlert();

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> adapter.publish(alert)
        );

        assertEquals("budgetAlert topic must not be blank", exception.getMessage());

        verify(kafkaTemplate, never()).send(anyProducerRecord());
    }

    @Test
    void shouldPropagateExceptionWhenAlertHasNullBudgetId() {
        KafkaTemplate<String, Object> kafkaTemplate = kafkaTemplate();
        BudgetingProperties props = propsWithTopic(TOPIC);

        BudgetAlertKafkaAdapter adapter =
                new BudgetAlertKafkaAdapter(kafkaTemplate, props, FIXED_CLOCK);

        BudgetAlert alert = alert(
                null,
                CONSUMPTION_ID,
                anyAlertSeverity(),
                "Budget reached alert threshold",
                PERIOD_START,
                PERIOD_END
        );

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> adapter.publish(alert)
        );

        assertEquals("alert.budgetId must not be null", exception.getMessage());

        verify(kafkaTemplate, never()).send(anyProducerRecord());
    }

    @Test
    void shouldPropagateExceptionWhenAlertHasNullSeverity() {
        KafkaTemplate<String, Object> kafkaTemplate = kafkaTemplate();
        BudgetingProperties props = propsWithTopic(TOPIC);

        BudgetAlertKafkaAdapter adapter =
                new BudgetAlertKafkaAdapter(kafkaTemplate, props, FIXED_CLOCK);

        BudgetAlert alert = alert(
                BUDGET_ID,
                CONSUMPTION_ID,
                null,
                "Budget reached alert threshold",
                PERIOD_START,
                PERIOD_END
        );

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> adapter.publish(alert)
        );

        assertEquals("alert.severity must not be null", exception.getMessage());

        verify(kafkaTemplate, never()).send(anyProducerRecord());
    }

    @Test
    void shouldPropagateExceptionWhenAlertMessageIsBlank() {
        KafkaTemplate<String, Object> kafkaTemplate = kafkaTemplate();
        BudgetingProperties props = propsWithTopic(TOPIC);

        BudgetAlertKafkaAdapter adapter =
                new BudgetAlertKafkaAdapter(kafkaTemplate, props, FIXED_CLOCK);

        BudgetAlert alert = alert(
                BUDGET_ID,
                CONSUMPTION_ID,
                anyAlertSeverity(),
                "   ",
                PERIOD_START,
                PERIOD_END
        );

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> adapter.publish(alert)
        );

        assertEquals("alert.message must not be blank", exception.getMessage());

        verify(kafkaTemplate, never()).send(anyProducerRecord());
    }

    @Test
    void shouldInvokePrivateBytesWithNullValue() throws Exception {
        Method method = BudgetAlertKafkaAdapter.class.getDeclaredMethod("bytes", String.class);
        method.setAccessible(true);

        byte[] result = (byte[]) method.invoke(null, new Object[]{null});

        assertNull(result);
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
        Method method = BudgetAlertKafkaAdapter.class.getDeclaredMethod(
                "requireNonBlank",
                String.class,
                String.class
        );
        method.setAccessible(true);

        String result = (String) method.invoke(null, "  value  ", "field");

        assertEquals("value", result);
    }

    @Test
    void shouldInvokePrivateRequireNonBlankWithNullValue() throws Exception {
        Method method = BudgetAlertKafkaAdapter.class.getDeclaredMethod(
                "requireNonBlank",
                String.class,
                String.class
        );
        method.setAccessible(true);

        InvocationTargetException exception = assertThrows(
                InvocationTargetException.class,
                () -> method.invoke(null, null, "field")
        );

        assertInstanceOf(IllegalStateException.class, exception.getCause());
        assertEquals("field must not be blank", exception.getCause().getMessage());
    }

    @Test
    void shouldInvokePrivateRequireNonBlankWithBlankValue() throws Exception {
        Method method = BudgetAlertKafkaAdapter.class.getDeclaredMethod(
                "requireNonBlank",
                String.class,
                String.class
        );
        method.setAccessible(true);

        InvocationTargetException exception = assertThrows(
                InvocationTargetException.class,
                () -> method.invoke(null, "   ", "field")
        );

        assertInstanceOf(IllegalStateException.class, exception.getCause());
        assertEquals("field must not be blank", exception.getCause().getMessage());
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

    private static BudgetAlert validAlert() {
        return alert(
                BUDGET_ID,
                CONSUMPTION_ID,
                anyAlertSeverity(),
                "Budget reached alert threshold",
                PERIOD_START,
                PERIOD_END
        );
    }

    private static BudgetAlert alert(
            UUID budgetId,
            UUID consumptionId,
            AlertSeverity severity,
            String message,
            LocalDate periodStart,
            LocalDate periodEnd
    ) {
        BudgetAlert alert = mock(BudgetAlert.class);

        when(alert.getBudgetId()).thenReturn(budgetId);
        when(alert.getConsumptionId()).thenReturn(consumptionId);
        when(alert.getSeverity()).thenReturn(severity);
        when(alert.getMessage()).thenReturn(message);
        when(alert.getPeriodStart()).thenReturn(periodStart);
        when(alert.getPeriodEnd()).thenReturn(periodEnd);

        return alert;
    }

    private static void stubKafkaSendSuccess(KafkaTemplate<String, Object> kafkaTemplate) {
        doReturn(successfulSend())
                .when(kafkaTemplate)
                .send(anyProducerRecord());
    }

    private static void stubKafkaSendFailure(
            KafkaTemplate<String, Object> kafkaTemplate,
            CompletableFuture<SendResult<String, Object>> failedFuture
    ) {
        doReturn(failedFuture)
                .when(kafkaTemplate)
                .send(anyProducerRecord());
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
                new TopicPartition(TOPIC, 1),
                0L,
                0,
                FIXED_INSTANT.toEpochMilli(),
                10,
                20
        );

        SendResult<String, Object> sendResult =
                new SendResult<>(producerRecord, metadata);

        return CompletableFuture.completedFuture(sendResult);
    }

    private static String headerValue(
            ProducerRecord<String, Object> record,
            String headerName
    ) {
        Header header = record.headers().lastHeader(headerName);

        assertNotNull(header);

        return new String(header.value(), StandardCharsets.UTF_8);
    }

    private static AlertSeverity anyAlertSeverity() {
        AlertSeverity[] values = AlertSeverity.values();

        if (values.length == 0) {
            throw new IllegalStateException("AlertSeverity enum must have at least one value");
        }

        return values[0];
    }
}
