package br.com.ecofy.ms_categorization.core.domain.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Currency;
import java.util.UUID;

// Representa o evento de domínio de transação categorizada, independente de DTOs de transporte.
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
