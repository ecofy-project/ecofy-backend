package br.com.ecofy.ms_insights.adapters.in.web.dto.request;

import br.com.ecofy.ms_insights.core.domain.enums.PeriodGranularity;

import java.time.LocalDate;
import java.util.UUID;

public record GenerateInsightsRequest(

        UUID userId,

        LocalDate start,

        LocalDate end,

        PeriodGranularity granularity

) { }
