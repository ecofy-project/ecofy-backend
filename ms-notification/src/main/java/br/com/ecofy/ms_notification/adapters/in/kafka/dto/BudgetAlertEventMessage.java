package br.com.ecofy.ms_notification.adapters.in.kafka.dto;


import java.math.BigDecimal;
import java.util.UUID;

public record BudgetAlertEventMessage(

        UUID userId,

        UUID budgetId,

        UUID categoryId,

        BigDecimal limitAmount,

        BigDecimal consumedAmount,

        Integer consumedPct,

        String severity,

        MessageMetadata metadata

) { }