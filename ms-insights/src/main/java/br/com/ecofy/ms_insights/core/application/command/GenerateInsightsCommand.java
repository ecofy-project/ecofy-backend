package br.com.ecofy.ms_insights.core.application.command;

import br.com.ecofy.ms_insights.core.domain.enums.PeriodGranularity;

import java.time.LocalDate;
import java.util.UUID;

public record GenerateInsightsCommand(

        UUID userId,

        LocalDate start,

        LocalDate end,

        PeriodGranularity granularity,

        String idempotencyKey

) { }
