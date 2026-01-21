package br.com.ecofy.ms_ingestion.core.application.service;

import br.com.ecofy.ms_ingestion.core.application.exception.EmptyTransactionsPayloadException;
import br.com.ecofy.ms_ingestion.core.application.exception.IngestionException;
import br.com.ecofy.ms_ingestion.core.application.exception.IngestionErrorCode;
import br.com.ecofy.ms_ingestion.core.application.exception.PersistenceException;
import br.com.ecofy.ms_ingestion.core.application.exception.PublishException;
import br.com.ecofy.ms_ingestion.core.domain.RawTransaction;
import br.com.ecofy.ms_ingestion.core.port.in.IngestTransactionEventUseCase;
import br.com.ecofy.ms_ingestion.core.port.out.PublishTransactionForCategorizationPort;
import br.com.ecofy.ms_ingestion.core.port.out.SaveRawTransactionPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Slf4j
@Service
public class TransactionEventIngestionService implements IngestTransactionEventUseCase {

    private final SaveRawTransactionPort saveRawTransactionPort;
    private final PublishTransactionForCategorizationPort publishTransactionForCategorizationPort;

    // Inicializa e valida as dependências do serviço de ingestão via eventos.
    public TransactionEventIngestionService(SaveRawTransactionPort saveRawTransactionPort,
                                            PublishTransactionForCategorizationPort publishTransactionForCategorizationPort) {
        this.saveRawTransactionPort = Objects.requireNonNull(saveRawTransactionPort, "saveRawTransactionPort must not be null");
        this.publishTransactionForCategorizationPort =
                Objects.requireNonNull(publishTransactionForCategorizationPort, "publishTransactionForCategorizationPort must not be null");
    }

    // Persiste transações vindas de evento e publica para o fluxo de categorização, com validações e tratamento de falhas.
    @Override
    public void ingest(IngestEventCommand command) {
        if (command == null) {
            throw new IngestionException(IngestionErrorCode.INVALID_COMMAND, "command must not be null");
        }

        List<RawTransaction> transactions = Objects.requireNonNull(
                command.transactions(),
                "transactions must not be null"
        );

        log.info(
                "[TransactionEventIngestionService] - [ingest] -> Ingerindo evento sourceSystem={} payloadId={} totalTx={}",
                command.sourceSystem(), command.payloadId(), transactions.size()
        );

        if (transactions.isEmpty()) {
            throw new EmptyTransactionsPayloadException(command.sourceSystem(), command.payloadId());
        }

        try {
            saveRawTransactionPort.saveAll(transactions);
        } catch (Exception e) {
            throw new PersistenceException("Failed to persist raw transactions from event", e);
        }

        try {
            publishTransactionForCategorizationPort.publish(transactions);
        } catch (Exception e) {
            throw new PublishException("Failed to publish transactions for categorization", e);
        }
    }

}
