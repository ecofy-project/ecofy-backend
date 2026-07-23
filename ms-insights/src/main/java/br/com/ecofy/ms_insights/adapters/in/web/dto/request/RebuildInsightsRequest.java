package br.com.ecofy.ms_insights.adapters.in.web.dto.request;

import br.com.ecofy.ms_insights.core.domain.enums.InsightType;
import br.com.ecofy.ms_insights.core.domain.enums.PeriodGranularity;
import br.com.ecofy.ms_insights.core.domain.rebuild.RebuildMode;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

// Transporta o escopo do rebuild, aceitando tipo e modo opcionais.
public record RebuildInsightsRequest(

        @NotNull
        UUID userId,

        @NotNull
        LocalDate periodStart,

        @NotNull
        LocalDate periodEnd,

        @NotNull
        PeriodGranularity granularity,

        InsightType insightType,

        RebuildMode mode
) { }
