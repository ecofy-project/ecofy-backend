package br.com.ecofy.ms_budgeting.adapters.out.messaging;

import br.com.ecofy.ms_budgeting.adapters.out.messaging.mapper.EventMapper;
import br.com.ecofy.ms_budgeting.config.BudgetingProperties;
import br.com.ecofy.ms_budgeting.core.domain.BudgetAlert;
import br.com.ecofy.ms_budgeting.core.port.out.PublishBudgetAlertEventPort;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.Objects;

@Slf4j
@Component
public class BudgetAlertKafkaAdapter implements PublishBudgetAlertEventPort {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final BudgetingProperties props;
    private final Clock clock;

    // Constrói o adapter e valida as dependências obrigatórias.
    public BudgetAlertKafkaAdapter(
            KafkaTemplate<String, Object> kafkaTemplate,
            BudgetingProperties props,
            Clock clock
    ) {
        this.kafkaTemplate = Objects.requireNonNull(kafkaTemplate, "kafkaTemplate must not be null");
        this.props = Objects.requireNonNull(props, "props must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    // Publica um alerta de budget no Kafka (mapeando domínio -> evento).
    @Override
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

    // Adiciona headers padrão de rastreio/observabilidade ao ProducerRecord.
    private static void addHeaders(ProducerRecord<String, Object> record, String eventId, BudgetAlert alert) {
        record.headers().add(new RecordHeader("eventId", bytes(eventId)));
        record.headers().add(new RecordHeader("budgetId", bytes(String.valueOf(alert.getBudgetId()))));
        record.headers().add(new RecordHeader("severity", bytes(String.valueOf(alert.getSeverity()))));

        if (alert.getConsumptionId() != null) {
            record.headers().add(new RecordHeader("consumptionId", bytes(String.valueOf(alert.getConsumptionId()))));
        }
    }

    // Converte String para bytes em UTF-8.
    private static byte[] bytes(String s) {
        return s == null ? null : s.getBytes(StandardCharsets.UTF_8);
    }

    // Valida string obrigatória (não nula e não blank) e retorna o valor normalizado.
    private static String requireNonBlank(String value, String field) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalStateException(field + " must not be blank");
        }
        return value.trim();
    }
}
