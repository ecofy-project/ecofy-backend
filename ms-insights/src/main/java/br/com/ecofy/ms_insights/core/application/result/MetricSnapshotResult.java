package br.com.ecofy.ms_insights.core.application.result;

import br.com.ecofy.ms_insights.core.domain.enums.MetricType;

import java.time.Instant;
import java.util.UUID;

public record MetricSnapshotResult(

        UUID id,

        UUID userId,

        MetricType metricType,

        long valueCents,

        String currency,

        Instant createdAt

) { }
