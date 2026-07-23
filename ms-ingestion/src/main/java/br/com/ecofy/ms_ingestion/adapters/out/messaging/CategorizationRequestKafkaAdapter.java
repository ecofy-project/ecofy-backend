package br.com.ecofy.ms_ingestion.adapters.out.messaging;

import br.com.ecofy.ms_ingestion.adapters.in.web.correlation.CorrelationId;
import br.com.ecofy.ms_ingestion.adapters.out.messaging.dto.CategorizationRequestMessage;
import br.com.ecofy.ms_ingestion.adapters.out.messaging.mapper.CategorizationMessageMapper;
import br.com.ecofy.ms_ingestion.config.KafkaConfig;
import br.com.ecofy.ms_ingestion.core.domain.RawTransaction;
import br.com.ecofy.ms_ingestion.core.port.out.PublishTransactionForCategorizationPort;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

// Publica transações para o fluxo assíncrono de categorização.
@Slf4j
@Component
public class CategorizationRequestKafkaAdapter implements PublishTransactionForCategorizationPort {

    public static final String HEADER_EVENT_ID = "eventId";
    public static final String HEADER_EVENT_TYPE = "eventType";
    public static final String HEADER_EVENT_VERSION = "eventVersion";
    public static final String HEADER_IMPORT_JOB_ID = "importJobId";

    public static final String EVENT_TYPE = "categorization.request";
    public static final String EVENT_VERSION = "1";

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final KafkaConfig.IngestionTopics topics;

    public CategorizationRequestKafkaAdapter(KafkaTemplate<String, Object> kafkaTemplate,
                                             KafkaConfig.IngestionTopics topics) {
        this.kafkaTemplate = Objects.requireNonNull(kafkaTemplate, "kafkaTemplate must not be null");
        this.topics = Objects.requireNonNull(topics, "topics must not be null");
    }

    // Publica o lote e contabiliza individualmente as confirmações do broker.
    @Override
    public int publish(List<RawTransaction> transactions, String correlationId) {
        Objects.requireNonNull(transactions, "transactions must not be null");

        if (transactions.isEmpty()) {
            return 0;
        }

        String topic = topics.getCategorizationRequest();
        String traceId = correlationId != null ? correlationId : CorrelationId.currentOrGenerate();

        List<CompletableFuture<SendResult<String, Object>>> futures = new ArrayList<>(transactions.size());
        for (RawTransaction tx : transactions) {
            futures.add(kafkaTemplate.send(toRecord(topic, tx, traceId)));
        }

        int confirmed = 0;
        for (int i = 0; i < futures.size(); i++) {
            try {
                futures.get(i).join();
                confirmed++;
            } catch (RuntimeException e) {
                log.error("[CategorizationRequestKafkaAdapter] - [publish] -> Falha ao publicar txId={} topic={} error={}",
                        transactions.get(i).id(), topic, e.getMessage());
            }
        }

        log.info("[CategorizationRequestKafkaAdapter] - [publish] -> Lote publicado topic={} enviadas={} confirmadas={}",
                topic, transactions.size(), confirmed);

        return confirmed;
    }

    // Converte a transação em registro particionado com metadados de rastreamento.
    private ProducerRecord<String, Object> toRecord(String topic, RawTransaction tx, String traceId) {
        CategorizationRequestMessage message = CategorizationMessageMapper.from(tx);

        ProducerRecord<String, Object> record = new ProducerRecord<>(topic, tx.id().toString(), message);

        record.headers()
                .add(header(CorrelationId.KAFKA_HEADER, traceId))
                .add(header(HEADER_EVENT_ID, tx.id().toString()))
                .add(header(HEADER_EVENT_TYPE, EVENT_TYPE))
                .add(header(HEADER_EVENT_VERSION, EVENT_VERSION))
                .add(header(HEADER_IMPORT_JOB_ID, tx.importJobId().toString()));

        return record;
    }

    private static RecordHeader header(String key, String value) {
        return new RecordHeader(key, value.getBytes(StandardCharsets.UTF_8));
    }
}
