package br.com.ecofy.ms_budgeting.core.domain.outbox;

// Define os estados do ciclo de vida de um registro da Outbox, com DISCARDED preservado para auditoria.
public enum OutboxStatus {

    PENDING,
    PROCESSING,
    PUBLISHED,
    FAILED,
    DISCARDED

}
