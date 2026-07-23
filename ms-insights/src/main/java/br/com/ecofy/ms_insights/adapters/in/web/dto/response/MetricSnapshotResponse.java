package br.com.ecofy.ms_insights.adapters.in.web.dto.response;

import br.com.ecofy.ms_insights.core.domain.enums.MetricType;

import java.time.Instant;
import java.util.UUID;

// Expõe um snapshot de métrica do dashboard em contrato tipado.
public record MetricSnapshotResponse(

        UUID id,

        UUID userId,

        MetricType metricType,

        long valueCents,

        String currency,

        Instant createdAt

) { }
