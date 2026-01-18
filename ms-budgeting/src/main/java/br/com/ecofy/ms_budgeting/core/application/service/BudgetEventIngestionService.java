package br.com.ecofy.ms_budgeting.core.application.service;

import br.com.ecofy.ms_budgeting.core.application.command.ProcessTransactionCommand;
import br.com.ecofy.ms_budgeting.core.port.in.ProcessTransactionForBudgetUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class BudgetEventIngestionService {

    private static final String MDC_CORRELATION_ID = "correlationId";
    private static final String MDC_EVENT_ID = "eventId";
    private static final String MDC_RUN_ID = "runId";

    private final ProcessTransactionForBudgetUseCase useCase;

    // Ingere o evento de transação, aplicando validações mínimas, MDC e delegando ao caso de uso.
    public void ingest(ProcessTransactionCommand cmd) throws Exception {
        Objects.requireNonNull(cmd, "cmd must not be null");

        requireNonBlank(String.valueOf(cmd.transactionId()), "transactionId");
        requireNonBlank(String.valueOf(cmd.userId()), "userId");
        requireNonBlank(String.valueOf(cmd.categoryId()), "categoryId");

        var md = cmd.metadata();
        String runId = cmd.runId() != null ? cmd.runId().toString() : null;
        String eventId = md != null ? md.eventId() : null;
        String correlationId = md != null ? md.correlationId() : null;

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
            throw ex;
        }
    }

    // Abre um “escopo” de MDC para runId/eventId/correlationId e limpa automaticamente ao final.
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

    // Valida que um campo string não está nulo/vazio para evitar processamento de payload inválido.
    private static String requireNonBlank(String v, String field) {
        if (v == null || v.trim().isEmpty()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return v;
    }

}
