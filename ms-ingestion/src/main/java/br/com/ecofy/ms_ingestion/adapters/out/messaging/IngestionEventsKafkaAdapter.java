package br.com.ecofy.ms_ingestion.adapters.out.messaging;

import br.com.ecofy.ms_ingestion.config.KafkaConfig;
import br.com.ecofy.ms_ingestion.core.domain.event.ImportJobStatusChangedEvent;
import br.com.ecofy.ms_ingestion.core.domain.event.TransactionsImportedEvent;
import br.com.ecofy.ms_ingestion.core.port.out.PublishIngestionEventPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

// Publica os eventos de domínio gerados pelo fluxo de ingestão.
@Slf4j
@Component
public class IngestionEventsKafkaAdapter implements PublishIngestionEventPort {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final KafkaConfig.IngestionTopics topics;

    public IngestionEventsKafkaAdapter(KafkaTemplate<String, Object> kafkaTemplate,
                                       KafkaConfig.IngestionTopics topics) {
        this.kafkaTemplate = kafkaTemplate;
        this.topics = topics;
    }

    // Publica as transações importadas usando o job como chave.
    @Override
    public void publish(TransactionsImportedEvent event) {
        String topic = topics.getTransactionImported();
        log.info("[IngestionEventsKafkaAdapter] - [publish] -> TransactionsImportedEvent jobId={} topic={}",
                event.importJobId(), topic);
        kafkaTemplate.send(topic, event.importJobId().toString(), event);
    }

    // Publica a mudança de status usando o job como chave.
    @Override
    public void publish(ImportJobStatusChangedEvent event) {
        String topic = topics.getImportJobStatusChanged();
        log.info("[IngestionEventsKafkaAdapter] - [publish] -> ImportJobStatusChangedEvent jobId={} newStatus={} topic={}",
                event.importJobId(), event.newStatus(), topic);
        kafkaTemplate.send(topic, event.importJobId().toString(), event);
    }
}
