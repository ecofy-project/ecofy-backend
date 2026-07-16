package br.com.ecofy.ms_insights.adapters.in.web.dto.request;

import br.com.ecofy.ms_insights.core.domain.enums.PeriodGranularity;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Correção Dia 8 (item #2): Bean Validation nos campos obrigatórios.
 * A coerência start &lt;= end é validada no domínio (Period) e mapeada para 400 pelo handler.
 */
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
