package br.com.ecofy.ms_ingestion.adapters.out.messaging;

import br.com.ecofy.ms_ingestion.adapters.out.messaging.dto.CategorizationRequestMessage;
import br.com.ecofy.ms_ingestion.adapters.out.messaging.mapper.CategorizationMessageMapper;
import br.com.ecofy.ms_ingestion.config.KafkaConfig;
import br.com.ecofy.ms_ingestion.core.domain.RawTransaction;
import br.com.ecofy.ms_ingestion.core.port.out.PublishTransactionForCategorizationPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

@Slf4j
@Component
public class CategorizationRequestKafkaAdapter implements PublishTransactionForCategorizationPort {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final KafkaConfig.IngestionTopics topics;

    public CategorizationRequestKafkaAdapter(KafkaTemplate<String, Object> kafkaTemplate,
                                             KafkaConfig.IngestionTopics topics) {
        this.kafkaTemplate = Objects.requireNonNull(kafkaTemplate, "kafkaTemplate must not be null");
        this.topics = Objects.requireNonNull(topics, "topics must not be null");
    }

    // Publica as transações recebidas como mensagens de request de categorização no tópico Kafka configurado.
    @Override
    public void publish(List<RawTransaction> transactions) {
        Objects.requireNonNull(transactions, "transactions must not be null");

        String topic = topics.getCategorizationRequest();
        if (transactions.isEmpty()) {
            log.info("[CategorizationRequestKafkaAdapter] - [publish] -> Nenhuma transação para enviar topic={}", topic);
            return;
        }

        log.info("[CategorizationRequestKafkaAdapter] - [publish] -> Enviando {} transações para categorização topic={}",
                transactions.size(), topic
        );

        for (RawTransaction tx : transactions) {
            CategorizationRequestMessage message = CategorizationMessageMapper.from(tx);
            kafkaTemplate.send(topic, tx.id().toString(), message);
        }
    }

}
