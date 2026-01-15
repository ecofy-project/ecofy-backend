package br.com.ecofy.ms_budgeting.adapters.in.kafka.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record CategorizedTransactionMessage (

        UUID transactionId,

        UUID userId,

        UUID categoryId,

        BigDecimal amount,

        String currency,

        LocalDate transactionDate,

        MessageMetadata metadata

) { }