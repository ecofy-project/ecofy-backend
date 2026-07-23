package br.com.ecofy.ms_insights.adapters.in.web.dto.request;

import br.com.ecofy.ms_insights.core.domain.enums.PeriodGranularity;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

// Transporta os dados de geração de insights, com a coerência do período validada no domínio.
public record GenerateInsightsRequest(

        @NotNull(message = "userId is required")
        UUID userId,

        @NotNull(message = "start date is required")
        LocalDate start,

        @NotNull(message = "end date is required")
        LocalDate end,

        @NotNull(message = "granularity is required")
        PeriodGranularity granularity

) { }
