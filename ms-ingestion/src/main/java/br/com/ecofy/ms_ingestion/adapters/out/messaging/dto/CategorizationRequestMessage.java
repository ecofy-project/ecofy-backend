package br.com.ecofy.ms_ingestion.adapters.out.messaging.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record CategorizationRequestMessage(

        UUID transactionId,
        UUID importJobId,
        String description,
        BigDecimal amount,
        String currency,
        LocalDate transactionDate,
        String sourceType

) { }
