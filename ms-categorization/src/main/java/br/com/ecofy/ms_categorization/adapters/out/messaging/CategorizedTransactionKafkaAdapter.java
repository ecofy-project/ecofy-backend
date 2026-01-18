package br.com.ecofy.ms_categorization.adapters.out.messaging;

import br.com.ecofy.ms_categorization.adapters.out.messaging.dto.CategorizationAppliedEvent;
import br.com.ecofy.ms_categorization.adapters.out.messaging.dto.CategorizedTransactionEvent;
import br.com.ecofy.ms_categorization.config.CategorizationProperties;
import br.com.ecofy.ms_categorization.core.port.out.PublishCategorizedTransactionEventPortOut;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
@RequiredArgsConstructor
public class CategorizedTransactionKafkaAdapter implements PublishCategorizedTransactionEventPortOut {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final CategorizationProperties props;

    // Publica o evento de transação categorizada no tópico Kafka configurado.
    @Override
    public void publish(CategorizedTransactionEvent event) {
        Objects.requireNonNull(event, "event must not be null");

        final String topic = props.getTopics().getTransactionCategorized();
        final String key = event.transactionId().toString();

        log.info("[CategorizedTransactionKafkaAdapter] - [publishCategorized] -> txId={} categoryId={} topic={}",
                event.transactionId(), event.categoryId(), topic);

        CompletableFuture<SendResult<String, Object>> fut = kafkaTemplate.send(topic, key, event);

        fut.whenComplete((res, ex) -> {
            if (ex != null) {
                log.error("[CategorizedTransactionKafkaAdapter] - [publishCategorized] -> FAILED txId={} topic={} error={}",
                        event.transactionId(), topic, ex.getMessage(), ex);
                return;
            }
            RecordMetadata md = res.getRecordMetadata();
            log.debug("[CategorizedTransactionKafkaAdapter] - [publishCategorized] -> OK txId={} topic={} partition={} offset={}",
                    event.transactionId(), md.topic(), md.partition(), md.offset());
        });
    }

    // Publica o evento de categorização aplicada no tópico Kafka configurado.
    @Override
    public void publish(CategorizationAppliedEvent event) {
        Objects.requireNonNull(event, "event must not be null");

        final String topic = props.getTopics().getCategorizationApplied();
        final String key = event.transactionId().toString();

        log.info("[CategorizedTransactionKafkaAdapter] - [publishApplied] -> txId={} mode={} topic={}",
                event.transactionId(), event.mode(), topic);

        CompletableFuture<SendResult<String, Object>> fut = kafkaTemplate.send(topic, key, event);

        fut.whenComplete((res, ex) -> {
            if (ex != null) {
                log.error("[CategorizedTransactionKafkaAdapter] - [publishApplied] -> FAILED txId={} topic={} error={}",
                        event.transactionId(), topic, ex.getMessage(), ex);
                return;
            }
            RecordMetadata md = res.getRecordMetadata();
            log.debug("[CategorizedTransactionKafkaAdapter] - [publishApplied] -> OK txId={} topic={} partition={} offset={}",
                    event.transactionId(), md.topic(), md.partition(), md.offset());
        });
    }

}
