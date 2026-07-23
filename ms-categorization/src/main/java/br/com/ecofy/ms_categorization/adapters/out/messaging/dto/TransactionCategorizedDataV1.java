package br.com.ecofy.ms_categorization.adapters.out.messaging.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

// Define o payload v1 de TRANSACTION_CATEGORIZED; mudança incompatível exige uma nova versão.
public record TransactionCategorizedDataV1(
        UUID transactionId,
        UUID importJobId,
        String externalId,
        LocalDate transactionDate,
        BigDecimal amount,
        String currency,
        UUID categoryId,
        String mode
) {
}
