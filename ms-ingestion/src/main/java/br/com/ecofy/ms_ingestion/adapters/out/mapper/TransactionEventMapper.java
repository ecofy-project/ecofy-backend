package br.com.ecofy.ms_ingestion.adapters.out.mapper;

import br.com.ecofy.ms_ingestion.adapters.in.kafka.dto.TransactionEventEnvelope;
import br.com.ecofy.ms_ingestion.adapters.in.web.correlation.CorrelationId;
import br.com.ecofy.ms_ingestion.core.application.exception.InvalidKafkaMessageException;
import br.com.ecofy.ms_ingestion.core.application.exception.UnsupportedEventVersionException;
import br.com.ecofy.ms_ingestion.core.domain.RawTransaction;
import br.com.ecofy.ms_ingestion.core.domain.enums.TransactionSourceType;
import br.com.ecofy.ms_ingestion.core.domain.valueobject.Money;
import br.com.ecofy.ms_ingestion.core.domain.valueobject.TransactionDate;
import br.com.ecofy.ms_ingestion.core.port.in.IngestTransactionEventUseCase;
import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

// Converte eventos Kafka em comandos seguros de ingestão.
@Slf4j
@Component
public class TransactionEventMapper {

    public static final String SUPPORTED_VERSION = "1";
    private static final String EXPECTED_EVENT_TYPE = "transaction.raw";
    private static final String DEFAULT_CURRENCY = "BRL";
    private static final String DEFAULT_SOURCE_SYSTEM = "unknown";

    // Limita a quantidade de transações aceita por evento.
    private static final int MAX_TRANSACTIONS_PER_EVENT = 1_000;

    private final ObjectMapper objectMapper;

    public TransactionEventMapper(ObjectMapper objectMapper) {
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
    }

    // Converte e valida o evento recebido para processamento pela aplicação.
    public IngestTransactionEventUseCase.IngestEventCommand toCommand(ConsumerRecord<String, String> record,
                                                                      String payload) {
        Objects.requireNonNull(record, "record must not be null");

        log.debug("[TransactionEventMapper] - [toCommand] -> topic={} partition={} offset={} payloadLength={}",
                record.topic(), record.partition(), record.offset(), payload != null ? payload.length() : 0);

        if (payload == null || payload.isBlank()) {
            throw new InvalidKafkaMessageException("reason=emptyPayload");
        }

        TransactionEventEnvelope envelope;
        try {
            envelope = objectMapper.readValue(payload, TransactionEventEnvelope.class);
        } catch (JacksonException e) {
            throw new InvalidKafkaMessageException("reason=malformedJson");
        }

        validateVersion(envelope);

        UUID ownerId = parseOwnerId(envelope.userId());
        List<TransactionEventEnvelope.Transaction> items = envelope.transactions();

        if (items == null || items.isEmpty()) {
            throw new InvalidKafkaMessageException("reason=noTransactions");
        }
        if (items.size() > MAX_TRANSACTIONS_PER_EVENT) {
            throw new InvalidKafkaMessageException("reason=tooManyTransactions, max=" + MAX_TRANSACTIONS_PER_EVENT);
        }

        String eventId = envelope.eventId() != null ? envelope.eventId() : record.key();

        UUID placeholderJobId = UUID.randomUUID();
        List<RawTransaction> transactions = new ArrayList<>(items.size());
        for (int i = 0; i < items.size(); i++) {
            transactions.add(toRawTransaction(placeholderJobId, items.get(i), i));
        }

        return new IngestTransactionEventUseCase.IngestEventCommand(
                ownerId,
                envelope.sourceSystem() != null ? envelope.sourceSystem() : DEFAULT_SOURCE_SYSTEM,
                eventId,
                resolveCorrelationId(record),
                transactions
        );
    }

    // Valida o tipo e a versão do evento recebido.
    private void validateVersion(TransactionEventEnvelope envelope) {
        if (envelope.eventType() != null && !EXPECTED_EVENT_TYPE.equalsIgnoreCase(envelope.eventType())) {
            throw new InvalidKafkaMessageException("reason=unexpectedEventType");
        }
        String version = envelope.eventVersion();
        if (version != null && !SUPPORTED_VERSION.equals(version)) {
            throw new UnsupportedEventVersionException(version, SUPPORTED_VERSION);
        }
    }

    // Valida e converte o identificador textual do proprietário.
    private UUID parseOwnerId(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new InvalidKafkaMessageException("reason=missingUserId");
        }
        try {
            return UUID.fromString(userId);
        } catch (IllegalArgumentException e) {
            throw new InvalidKafkaMessageException("reason=invalidUserId");
        }
    }

    // Converte e valida cada item recebido em uma transação bruta.
    private RawTransaction toRawTransaction(UUID jobId, TransactionEventEnvelope.Transaction item, int index) {
        if (item == null) {
            throw new InvalidKafkaMessageException("reason=nullTransaction, index=" + index);
        }
        if (item.transactionDate() == null) {
            throw new InvalidKafkaMessageException("reason=missingTransactionDate, index=" + index);
        }

        BigDecimal amount = item.amount();
        if (amount == null) {
            throw new InvalidKafkaMessageException("reason=missingAmount, index=" + index);
        }
        if (amount.precision() - amount.scale() > 15 || amount.scale() > 4) {
            throw new InvalidKafkaMessageException("reason=amountOutOfRange, index=" + index);
        }

        String currency = item.currency() == null || item.currency().isBlank()
                ? DEFAULT_CURRENCY
                : item.currency().toUpperCase(Locale.ROOT);

        if (currency.length() != 3) {
            throw new InvalidKafkaMessageException("reason=invalidCurrency, index=" + index);
        }

        return RawTransaction.create(
                jobId,
                item.externalId(),
                item.description(),
                new TransactionDate(item.transactionDate()),
                new Money(amount, currency),
                TransactionSourceType.KAFKA_EVENT
        );
    }

    // Resolve o identificador de correlação do header Kafka.
    private String resolveCorrelationId(ConsumerRecord<String, String> record) {
        var header = record.headers().lastHeader(CorrelationId.KAFKA_HEADER);
        if (header == null || header.value() == null) {
            return CorrelationId.generate();
        }
        return CorrelationId.sanitizeOrGenerate(new String(header.value(), StandardCharsets.UTF_8));
    }
}
