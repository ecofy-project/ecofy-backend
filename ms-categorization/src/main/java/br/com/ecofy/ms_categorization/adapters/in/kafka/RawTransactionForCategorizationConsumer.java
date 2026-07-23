package br.com.ecofy.ms_categorization.adapters.in.kafka;

import br.com.ecofy.ms_categorization.adapters.correlation.CorrelationContext;
import br.com.ecofy.ms_categorization.adapters.in.kafka.dto.CategorizationRequestMessage;
import br.com.ecofy.ms_categorization.adapters.in.kafka.mapper.InboundMessageMapper;
import br.com.ecofy.ms_categorization.config.CategorizationProperties;
import br.com.ecofy.ms_categorization.core.application.command.AutoCategorizeCommand;
import br.com.ecofy.ms_categorization.core.application.exception.InvalidEventException;
import br.com.ecofy.ms_categorization.core.application.exception.UnsupportedEventVersionException;
import br.com.ecofy.ms_categorization.core.port.in.AutoCategorizeTransactionUseCase;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.UUID;

// Consome eventos de categorização e delega falhas ao mecanismo de retry e DLT.
@Slf4j
@Component
@RequiredArgsConstructor
public class RawTransactionForCategorizationConsumer {

    private static final String IDEMPOTENCY_PREFIX_MSG = "msg:";
    private static final String IDEMPOTENCY_PREFIX_TX = "tx:";

    private final AutoCategorizeTransactionUseCase useCase;
    private final InboundMessageMapper mapper;
    private final CategorizationProperties props;
    private final MeterRegistry meterRegistry;

    // Processa o evento com validação, correlação e controle de idempotência.
    @KafkaListener(
            topics = "${ecofy.categorization.topics.categorization-request}",
            groupId = "${spring.kafka.consumer.group-id:ms-categorization}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(ConsumerRecord<String, CategorizationRequestMessage> record) {
        CategorizationRequestMessage message = record.value();

        if (message == null) {
            meterRegistry.counter("ecofy.categorization.kafka.invalid.total", "reason", "null_payload").increment();
            throw new InvalidEventException("null payload");
        }

        String correlationId = resolveCorrelationId(record, message);
        UUID causationId = headerAsUuid(record, CorrelationContext.KAFKA_EVENT_ID_HEADER);

        CorrelationContext.put(correlationId, causationId);
        try {
            meterRegistry.counter("ecofy.categorization.kafka.received.total", "topic", record.topic()).increment();

            validateEventVersion(record);
            validateRequiredFields(message);

            String msgId = extractMessageId(message);
            String idempotencyKey = buildIdempotencyKey(msgId, message.transactionId());

            log.info("[RawTransactionForCategorizationConsumer] - [consume] -> topic={} partition={} offset={} key={} "
                            + "txId={} jobId={} correlationId={} causationId={}",
                    record.topic(), record.partition(), record.offset(), record.key(),
                    message.transactionId(), message.importJobId(), correlationId, causationId);

            var tx = mapper.toDomain(message);
            var result = useCase.autoCategorize(new AutoCategorizeCommand(idempotencyKey, tx));

            meterRegistry.counter("ecofy.categorization.kafka.processed.total",
                    "outcome", result.decision() != null ? result.decision() : "UNKNOWN").increment();

            log.info("[RawTransactionForCategorizationConsumer] - [consume] -> result txId={} categorized={} "
                            + "categoryId={} score={} decision={} suggestionId={}",
                    result.transactionId(), result.categorized(), result.categoryId(),
                    result.score(), result.decision(), result.suggestionId());

        } finally {
            CorrelationContext.clear();
        }
    }

    // Valida a versão do evento e rejeita formatos ou versões incompatíveis.
    private void validateEventVersion(ConsumerRecord<String, CategorizationRequestMessage> record) {
        String raw = headerAsString(record, "eventVersion");
        if (raw == null || raw.isBlank()) {
            return;
        }

        int version;
        try {
            version = Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            meterRegistry.counter("ecofy.categorization.kafka.invalid.total", "reason", "malformed_version").increment();
            throw new InvalidEventException("malformed eventVersion header");
        }

        if (!props.getKafka().getSupportedEventVersions().contains(version)) {
            meterRegistry.counter("ecofy.categorization.kafka.unsupported.version.total",
                    "event_version", String.valueOf(version)).increment();
            throw new UnsupportedEventVersionException(
                    String.valueOf(version), props.getKafka().getSupportedEventVersions().toString());
        }
    }

    // Valida os dados obrigatórios para a categorização.
    private void validateRequiredFields(CategorizationRequestMessage message) {
        if (message.transactionId() == null) {
            countInvalid("missing_transaction_id");
            throw new InvalidEventException("missing transactionId");
        }
        if (message.importJobId() == null) {
            countInvalid("missing_import_job_id");
            throw new InvalidEventException("missing importJobId");
        }
        if (message.amount() == null) {
            countInvalid("missing_amount");
            throw new InvalidEventException("missing amount");
        }
        if (message.transactionDate() == null) {
            countInvalid("missing_transaction_date");
            throw new InvalidEventException("missing transactionDate");
        }
    }

    private void countInvalid(String reason) {
        meterRegistry.counter("ecofy.categorization.kafka.invalid.total", "reason", reason).increment();
    }

    // Resolve a correlação priorizando o header Kafka e usando o payload como fallback.
    private String resolveCorrelationId(ConsumerRecord<String, CategorizationRequestMessage> record,
                                        CategorizationRequestMessage message) {
        String fromHeader = headerAsString(record, CorrelationContext.KAFKA_HEADER);
        String fromPayload = message.metadata() != null ? message.metadata().traceId() : null;

        if (fromHeader != null && fromPayload != null && !fromHeader.equals(fromPayload)) {
            meterRegistry.counter("ecofy.categorization.correlation.divergence.total").increment();
            log.warn("[RawTransactionForCategorizationConsumer] - [resolveCorrelationId] -> divergência entre header e payload; usando header");
        }

        String candidate = fromHeader != null ? fromHeader : fromPayload;
        return CorrelationContext.sanitizeOrGenerate(candidate);
    }

    private static String headerAsString(ConsumerRecord<?, ?> record, String key) {
        Header header = record.headers().lastHeader(key);
        if (header == null || header.value() == null) {
            return null;
        }
        return new String(header.value(), StandardCharsets.UTF_8);
    }

    private static UUID headerAsUuid(ConsumerRecord<?, ?> record, String key) {
        String raw = headerAsString(record, key);
        if (raw == null) {
            return null;
        }
        try {
            return UUID.fromString(raw.trim());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    // Extrai o identificador da mensagem usado no rastreamento e na idempotência.
    private static String extractMessageId(CategorizationRequestMessage message) {
        var meta = message.metadata();
        if (meta == null) {
            return null;
        }
        String id = meta.messageId();
        return (id == null || id.isBlank()) ? null : id.trim();
    }

    // Gera a chave de idempotência priorizando o identificador da mensagem.
    private static String buildIdempotencyKey(String messageId, UUID txId) {
        if (messageId != null) {
            return IDEMPOTENCY_PREFIX_MSG + messageId;
        }
        return IDEMPOTENCY_PREFIX_TX + Objects.requireNonNull(txId, "transactionId must not be null");
    }
}
