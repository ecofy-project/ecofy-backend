package br.com.ecofy.ms_insights.adapters.in.kafka.dto;

import java.time.LocalDate;
import java.util.UUID;

public record CategorizedTransactionMessage(

        MessageMetadata metadata,

        UUID transactionId,

        UUID userId,

        UUID categoryId,

        long amountCents,

        String currency,

        LocalDate bookingDate,

        boolean income

) { }
