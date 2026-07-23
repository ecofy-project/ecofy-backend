package br.com.ecofy.ms_ingestion.adapters.in.kafka;

import br.com.ecofy.ms_ingestion.adapters.in.web.correlation.CorrelationId;
import br.com.ecofy.ms_ingestion.adapters.out.mapper.TransactionEventMapper;
import br.com.ecofy.ms_ingestion.core.port.in.IngestTransactionEventUseCase;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

// Consome eventos de transação e coordena seu processamento.
@Slf4j
@Component
public class TransactionEventConsumer {

    private final IngestTransactionEventUseCase ingestTransactionEventUseCase;
    private final TransactionEventMapper mapper;
    private final MeterRegistry meterRegistry;

    public TransactionEventConsumer(IngestTransactionEventUseCase ingestTransactionEventUseCase,
                                    TransactionEventMapper mapper,
                                    MeterRegistry meterRegistry) {
        this.ingestTransactionEventUseCase =
                Objects.requireNonNull(ingestTransactionEventUseCase, "ingestTransactionEventUseCase must not be null");
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
        this.meterRegistry = Objects.requireNonNull(meterRegistry, "meterRegistry must not be null");
    }

    // Processa o evento com correlação e delega falhas ao mecanismo de retry e DLT.
    @KafkaListener(
            topics = "${ecofy.ingestion.kafka.transaction-event-topic:eco.tx.raw}",
            groupId = "${spring.kafka.consumer.group-id:ms-ingestion}"
    )
    public void consume(ConsumerRecord<String, String> record, @Payload(required = false) String payload) {
        String correlationId = correlationIdOf(record);
        MDC.put(CorrelationId.MDC_KEY, correlationId);

        try {
            meterRegistry.counter("ecofy.ingestion.kafka.consumed.total", "topic", record.topic()).increment();

            log.info("[TransactionEventConsumer] - [consume] -> Mensagem recebida topic={} partition={} offset={}",
                    record.topic(), record.partition(), record.offset());

            ingestTransactionEventUseCase.ingest(mapper.toCommand(record, payload));

            meterRegistry.counter("ecofy.ingestion.kafka.processed.total", "outcome", "success").increment();
        } finally {
            MDC.remove(CorrelationId.MDC_KEY);
        }
    }

    // Resolve a correlação do header ou gera um novo identificador.
    private static String correlationIdOf(ConsumerRecord<String, String> record) {
        var header = record.headers().lastHeader(CorrelationId.KAFKA_HEADER);
        if (header == null || header.value() == null) {
            return CorrelationId.generate();
        }
        return CorrelationId.sanitizeOrGenerate(new String(header.value(), StandardCharsets.UTF_8));
    }
}
