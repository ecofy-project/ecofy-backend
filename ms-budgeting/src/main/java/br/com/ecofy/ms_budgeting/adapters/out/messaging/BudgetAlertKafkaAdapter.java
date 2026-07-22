package br.com.ecofy.ms_budgeting.adapters.out.messaging;

import br.com.ecofy.ms_budgeting.adapters.out.messaging.mapper.EventMapper;
import br.com.ecofy.ms_budgeting.config.BudgetingProperties;
import br.com.ecofy.ms_budgeting.core.domain.BudgetAlert;
import br.com.ecofy.ms_budgeting.core.port.out.PublishBudgetAlertEventPort;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.springframework.kafka.core.KafkaTemplate;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.Objects;

@Deprecated(since = "Etapa 6", forRemoval = true)
@Slf4j
// Publica alertas de orçamento diretamente no Kafka como integração legada.
public class BudgetAlertKafkaAdapter implements PublishBudgetAlertEventPort {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final BudgetingProperties props;
    private final Clock clock;

    public BudgetAlertKafkaAdapter(
            KafkaTemplate<String, Object> kafkaTemplate,
            BudgetingProperties props,
            Clock clock
    ) {
        this.kafkaTemplate = Objects.requireNonNull(kafkaTemplate, "kafkaTemplate must not be null");
        this.props = Objects.requireNonNull(props, "props must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Override
    // Publica o alerta convertido no tópico configurado.
    public void publish(BudgetAlert alert) {
        Objects.requireNonNull(alert, "alert must not be null");

        String topic = requireNonBlank(props.topics().budgetAlert(), "budgetAlert topic");
        String key = String.valueOf(alert.getBudgetId());

        var event = EventMapper.toEvent(alert, clock);
        String eventId = event.metadata() != null ? event.metadata().eventId() : null;

        ProducerRecord<String, Object> record = new ProducerRecord<>(topic, key, event);
        addHeaders(record, eventId, alert);

        kafkaTemplate.send(record).whenComplete((result, ex) -> {
            if (ex != null) {
                log.error(
                        "[BudgetAlertKafkaAdapter] PUBLISH_FAIL topic={} key={} eventId={} budgetId={} severity={} error={}",
                        topic, key, eventId, alert.getBudgetId(), alert.getSeverity(), ex.getMessage(), ex
                );
                return;
            }

            var md = result.getRecordMetadata();
            log.info(
                    "[BudgetAlertKafkaAdapter] PUBLISHED topic={} key={} partition={} offset={} eventId={} budgetId={} severity={}",
                    topic, key, md.partition(), md.offset(), eventId, alert.getBudgetId(), alert.getSeverity()
            );
        });
    }

    // Propaga identificadores de rastreio nos cabeçalhos do evento.
    private static void addHeaders(
            ProducerRecord<String, Object> record,
            String eventId,
            BudgetAlert alert
    ) {
        record.headers().add(new RecordHeader("eventId", bytes(eventId)));
        record.headers().add(new RecordHeader("budgetId", bytes(String.valueOf(alert.getBudgetId()))));
        record.headers().add(new RecordHeader("severity", bytes(String.valueOf(alert.getSeverity()))));

        if (alert.getConsumptionId() != null) {
            record.headers().add(
                    new RecordHeader("consumptionId", bytes(String.valueOf(alert.getConsumptionId())))
            );
        }
    }

    private static byte[] bytes(String s) {
        return s == null ? null : s.getBytes(StandardCharsets.UTF_8);
    }

    private static String requireNonBlank(String value, String field) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalStateException(field + " must not be blank");
        }
        return value.trim();
    }
}
