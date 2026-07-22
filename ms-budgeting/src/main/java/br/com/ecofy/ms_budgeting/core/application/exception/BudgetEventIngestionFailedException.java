package br.com.ecofy.ms_budgeting.core.application.exception;

import java.util.UUID;

public class BudgetEventIngestionFailedException extends BudgetingProcessingException {

    private final String eventId;
    private final String correlationId;
    private final UUID transactionId;

    public BudgetEventIngestionFailedException(UUID transactionId, String eventId, String correlationId, Throwable cause) {
        super("Failed to ingest budget transaction event for txId: " + transactionId
                + " (eventId: " + eventId
                + ", correlationId: " + correlationId + ")", cause);
        this.transactionId = transactionId;
        this.eventId = eventId;
        this.correlationId = correlationId;
    }

    public String getEventId() {
        return eventId;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public UUID getTransactionId() {
        return transactionId;
    }
}
