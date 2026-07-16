package br.com.ecofy.ms_insights.adapters.in.web.dto.response;

import br.com.ecofy.ms_insights.core.domain.enums.MetricType;

import java.time.Instant;
import java.util.UUID;

/**
 * Correção Dia 8 (item #3): DTO tipado para métricas do dashboard.
 * Antes o bundle expunha {@code List<Object>} com aninhamento artificial (lista de lista de lista);
 * agora as métricas são {@code List<MetricSnapshotResponse>} — contrato previsível para o frontend/OpenAPI.
 */
public record MetricSnapshotResponse(

        UUID id,

        UUID userId,

        MetricType metricType,

        long valueCents,

        String currency,

        Instant createdAt

) { }
