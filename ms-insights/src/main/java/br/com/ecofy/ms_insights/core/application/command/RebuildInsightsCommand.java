package br.com.ecofy.ms_insights.core.application.command;

import br.com.ecofy.ms_insights.core.domain.enums.InsightType;
import br.com.ecofy.ms_insights.core.domain.enums.PeriodGranularity;
import br.com.ecofy.ms_insights.core.domain.rebuild.RebuildMode;

import java.time.LocalDate;
import java.util.UUID;

// Transporta o escopo do rebuild, cujo intervalo é validado contra o limite configurado.
public record RebuildInsightsCommand(
        UUID userId,
        LocalDate periodStart,
        LocalDate periodEnd,
        PeriodGranularity granularity,
        InsightType insightType,   // null = tipo default aplicável
        RebuildMode mode           // null = MISSING
) { }
