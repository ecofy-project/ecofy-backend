package br.com.ecofy.ms_budgeting.core.application.service;

import br.com.ecofy.ms_budgeting.core.application.command.ProcessTransactionCommand;
import br.com.ecofy.ms_budgeting.core.application.exception.BudgetEventIngestionFailedException;
import br.com.ecofy.ms_budgeting.core.application.exception.InvalidFieldException;
import br.com.ecofy.ms_budgeting.core.port.in.ProcessTransactionForBudgetUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class BudgetEventIngestionService {

    private static final String MDC_CORRELATION_ID = "correlationId";
    private static final String MDC_EVENT_ID = "eventId";
    private static final String MDC_RUN_ID = "runId";

    private final ProcessTransactionForBudgetUseCase useCase;

    // Orquestra a ingestão do evento de transação: valida campos obrigatórios, configura MDC, loga início/fim e chama o caso de uso.
    public void ingest(ProcessTransactionCommand cmd) {
        if (cmd == null) throw InvalidFieldException.required("cmd");
        if (cmd.transactionId() == null) throw InvalidFieldException.required("transactionId");
        if (cmd.userId() == null) throw InvalidFieldException.required("userId");
        if (cmd.categoryId() == null) throw InvalidFieldException.required("categoryId");

        var md = cmd.metadata();
        String runId = cmd.runId() != null ? cmd.runId().toString() : null;
        String eventId = md != null ? md.eventId() : null;
        String correlationId = md != null ? md.correlationId() : null;

        UUID txId = UUID.fromString(String.valueOf(cmd.transactionId()));

        try (var ignored = mdcScope(runId, eventId, correlationId)) {

            log.info(
                    "[BudgetEventIngestionService] - [ingest] -> START txId={} userId={} categoryId={} eventId={} correlationId={} topic={} partition={} offset={}",
                    cmd.transactionId(),
                    cmd.userId(),
                    cmd.categoryId(),
                    eventId,
                    correlationId,
                    md != null ? md.topic() : null,
                    md != null ? md.partition() : null,
                    md != null ? md.offset() : null
            );

            useCase.process(cmd);

            log.info(
                    "[BudgetEventIngestionService] - [ingest] -> DONE txId={} eventId={} correlationId={}",
                    cmd.transactionId(), eventId, correlationId
            );

        } catch (Exception ex) {
            log.error(
                    "[BudgetEventIngestionService] - [ingest] -> FAIL txId={} eventId={} correlationId={} message={}",
                    cmd.transactionId(), eventId, correlationId, ex.getMessage(), ex
            );
            throw new BudgetEventIngestionFailedException(txId, eventId, correlationId, ex);
        }
    }

    // Cria um escopo de logging com MDC para runId/eventId/correlationId e garante a limpeza automática ao final (try-with-resources).
    private static AutoCloseable mdcScope(String runId, String eventId, String correlationId) {
        if (runId != null) MDC.put(MDC_RUN_ID, runId);
        if (eventId != null) MDC.put(MDC_EVENT_ID, eventId);
        if (correlationId != null) MDC.put(MDC_CORRELATION_ID, correlationId);

        return () -> {
            MDC.remove(MDC_RUN_ID);
            MDC.remove(MDC_EVENT_ID);
            MDC.remove(MDC_CORRELATION_ID);
        };
    }

}
