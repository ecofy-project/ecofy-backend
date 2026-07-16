package br.com.ecofy.ms_categorization.core.domain.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Currency;
import java.util.UUID;

/**
 * Evento de DOMÍNIO (core) de transação categorizada — publicado para downstream (ms-budgeting).
 *
 * Este evento NÃO depende de DTOs Kafka, Jackson, Spring ou adapters. A conversão para o
 * DTO Kafka de saída fica no adapter (CategorizedTransactionKafkaAdapter / EventMapper).
 */
public record CategorizedTransactionDomainEvent(
        UUID eventId,
        UUID transactionId,
        UUID importJobId,
        String externalId,
        LocalDate transactionDate,
        BigDecimal amount,
        Currency currency,
        UUID categoryId,
        String mode,
        Instant occurredAt
) { }
