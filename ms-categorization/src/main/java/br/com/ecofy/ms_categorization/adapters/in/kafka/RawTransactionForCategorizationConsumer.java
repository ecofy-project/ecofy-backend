package br.com.ecofy.ms_categorization.adapters.in.kafka;

import br.com.ecofy.ms_categorization.adapters.in.kafka.dto.CategorizationRequestMessage;
import br.com.ecofy.ms_categorization.adapters.in.kafka.mapper.InboundMessageMapper;
import br.com.ecofy.ms_categorization.core.application.command.AutoCategorizeCommand;
import br.com.ecofy.ms_categorization.core.port.in.AutoCategorizeTransactionUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class RawTransactionForCategorizationConsumer {

    private static final String IDEMPOTENCY_PREFIX_MSG = "msg:";
    private static final String IDEMPOTENCY_PREFIX_TX  = "tx:";

    private final AutoCategorizeTransactionUseCase useCase;
    private final InboundMessageMapper mapper;

    // Consome eventos de transações brutas, aciona autocategorização e confirma o offset manualmente.
    @KafkaListener(
            topics = "${ecofy.categorization.topics.categorization-request}",
            // Herda o consumer group central (spring.kafka.consumer.group-id) em vez de hardcodar
            // spring.application.name, que ignorava a config do YAML.
            groupId = "${spring.kafka.consumer.group-id:ms-categorization}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(
            CategorizationRequestMessage message,
            Acknowledgment ack,
            ConsumerRecord<String, CategorizationRequestMessage> record
    ) {
        Objects.requireNonNull(message, "message must not be null");

        String msgId = extractMessageId(message);
        String idempotencyKey = buildIdempotencyKey(msgId, message.transactionId());

        log.info("[RawTransactionForCategorizationConsumer] - [consume] -> topic={} partition={} offset={} key={} messageId={} txId={} jobId={}",
                record.topic(), record.partition(), record.offset(), record.key(),
                msgId, message.transactionId(), message.importJobId());

        try {
            var tx = mapper.toDomain(message);
            var result = useCase.autoCategorize(new AutoCategorizeCommand(idempotencyKey, tx));

            log.info("[RawTransactionForCategorizationConsumer] - [consume] -> result txId={} categorized={} categoryId={} score={} decision={} suggestionId={}",
                    result.transactionId(),
                    result.categorized(),
                    result.categoryId(),
                    result.score(),
                    result.decision(),
                    result.suggestionId()
            );

            ack.acknowledge();
        } catch (Exception ex) {
            log.error("[RawTransactionForCategorizationConsumer] - [consume] -> FAILED messageId={} txId={} jobId={} error={}",
                    msgId, message.transactionId(), message.importJobId(), ex.getMessage(), ex);
            throw ex; // deixa o ErrorHandler/Retry/DLT do container decidir o destino
        }
    }

    // Extrai o messageId da metadata da mensagem (quando existir) para rastreio e idempotência.
    private static String extractMessageId(CategorizationRequestMessage message) {
        var meta = message.metadata();
        if (meta == null) return null;
        String id = meta.messageId();
        return (id == null || id.isBlank()) ? null : id.trim();
    }

    // Gera a chave de idempotência priorizando messageId e caindo para transactionId quando ausente.
    private static String buildIdempotencyKey(String messageId, UUID txId) {
        if (messageId != null) return IDEMPOTENCY_PREFIX_MSG + messageId;
        return IDEMPOTENCY_PREFIX_TX + Objects.requireNonNull(txId, "transactionId must not be null");
    }

}
