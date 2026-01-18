package br.com.ecofy.ms_categorization.adapters.in.kafka.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record CategorizationRequestMessage(

        MessageMetadata metadata,

        UUID transactionId,
        UUID importJobId,

        String externalId,

        String description,

        LocalDate transactionDate,

        BigDecimal amount,

        String currency,

        String sourceType

) { }
