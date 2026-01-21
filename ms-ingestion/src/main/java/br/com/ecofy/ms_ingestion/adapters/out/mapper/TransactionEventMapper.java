package br.com.ecofy.ms_ingestion.adapters.out.mapper;

import br.com.ecofy.ms_ingestion.core.domain.RawTransaction;
import br.com.ecofy.ms_ingestion.core.domain.enums.TransactionSourceType;
import br.com.ecofy.ms_ingestion.core.domain.valueobject.Money;
import br.com.ecofy.ms_ingestion.core.domain.valueobject.TransactionDate;
import br.com.ecofy.ms_ingestion.core.port.in.IngestTransactionEventUseCase;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Component
public class TransactionEventMapper {

    private static final String DEFAULT_CURRENCY = "BRL";
    private static final BigDecimal DEFAULT_AMOUNT = BigDecimal.ZERO;
    private static final String DEFAULT_SOURCE_SYSTEM = "unknown";

    /**
     * Mapeia um registro Kafka bruto + payload JSON para o comando de ingestão.
     * Hoje gera um RawTransaction "dummy", mas já estruturado para evoluir o parsing depois.
     */

    // Converte ConsumerRecord + payload em um comando de ingestão contendo RawTransaction(s).
    public IngestTransactionEventUseCase.IngestEventCommand toCommand(ConsumerRecord<String, String> record,
                                                                      String payload) {

        Objects.requireNonNull(record, "record must not be null");

        String key = record.key();
        int payloadLength = payload != null ? payload.length() : 0;

        log.debug(
                "[TransactionEventMapper] - [toCommand] -> Transformando payload em comando topic={} partition={} offset={} key={} payloadLength={}",
                record.topic(),
                record.partition(),
                record.offset(),
                key,
                payloadLength
        );

        // Futuro: parse real do JSON -> DTO -> RawTransaction
        // Ex.: TransactionEventDto dto = objectMapper.readValue(payload, TransactionEventDto.class);

        RawTransaction tx = RawTransaction.create(
                syntheticJobIdFrom(record),
                key,
                "TX from event topic=%s key=%s".formatted(record.topic(), key),
                new TransactionDate(LocalDate.now()),
                new Money(DEFAULT_AMOUNT, DEFAULT_CURRENCY),
                TransactionSourceType.KAFKA_EVENT
        );

        return new IngestTransactionEventUseCase.IngestEventCommand(
                DEFAULT_SOURCE_SYSTEM,
                key,
                List.of(tx)
        );
    }

    // Gera um identificador de job sintético para agrupar/traquear ingestões originadas de eventos.
    private UUID syntheticJobIdFrom(ConsumerRecord<String, String> record) {
        // Estratégia simples: UUID aleatório.
        return UUID.randomUUID();
    }

}
