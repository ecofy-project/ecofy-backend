package br.com.ecofy.ms_insights.adapters.out.persistence.mapper;

import br.com.ecofy.ms_insights.adapters.out.persistence.entity.TrendEntity;
import br.com.ecofy.ms_insights.core.domain.Trend;
import br.com.ecofy.ms_insights.core.domain.enums.PeriodGranularity;
import br.com.ecofy.ms_insights.core.domain.enums.TrendType;
import br.com.ecofy.ms_insights.core.domain.valueobject.Period;
import br.com.ecofy.ms_insights.core.domain.valueobject.UserId;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class TrendMapper {

    // Impede instanciação e reforça o uso estático (classe utilitária de mapeamento domain <-> persistence).
    private TrendMapper() {
    }

    // Converte Trend (domínio) em TrendEntity (persistência), validando campos obrigatórios e serializando a série (List<Long>) em JSON.
    public static TrendEntity toEntity(Trend d, ObjectMapper om) {
        Objects.requireNonNull(d, "trend must not be null");
        Objects.requireNonNull(om, "objectMapper must not be null");

        Objects.requireNonNull(d.getId(), "trend.id must not be null");
        Objects.requireNonNull(d.getUserId(), "trend.userId must not be null");
        Objects.requireNonNull(d.getUserId().value(), "trend.userId.value must not be null");
        Objects.requireNonNull(d.getPeriod(), "trend.period must not be null");
        Objects.requireNonNull(d.getPeriod().start(), "trend.period.start must not be null");
        Objects.requireNonNull(d.getPeriod().end(), "trend.period.end must not be null");
        Objects.requireNonNull(d.getPeriod().granularity(), "trend.period.granularity must not be null");
        Objects.requireNonNull(d.getType(), "trend.type must not be null");
        Objects.requireNonNull(d.getSeriesCents(), "trend.seriesCents must not be null");

        String currency = requireCurrency3(d.getCurrency(), "trend.currency");

        try {
            return TrendEntity.builder()
                    .id(d.getId())
                    .userId(d.getUserId().value())
                    .trendType(d.getType().name())
                    .periodStart(d.getPeriod().start())
                    .periodEnd(d.getPeriod().end())
                    .granularity(d.getPeriod().granularity().name())
                    .seriesJson(om.writeValueAsString(d.getSeriesCents()))
                    .currency(currency)
                    .createdAt(d.getCreatedAt())
                    .build();
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to serialize series", ex);
        }
    }

    // Converte TrendEntity em Trend (domínio) usando Clock padrão UTC para timestamps de fallback.
    public static Trend toDomain(TrendEntity e, ObjectMapper om) {
        return toDomain(e, om, Clock.systemUTC());
    }

    // Converte TrendEntity em Trend (domínio) validando campos, parseando enums, desserializando a série e aplicando fallback de createdAt via Clock.
    @SuppressWarnings("unchecked")
    public static Trend toDomain(TrendEntity e, ObjectMapper om, Clock clock) {
        Objects.requireNonNull(e, "entity must not be null");
        Objects.requireNonNull(om, "objectMapper must not be null");
        Objects.requireNonNull(clock, "clock must not be null");

        UUID id = Objects.requireNonNull(e.getId(), "entity.id must not be null");
        UUID userId = Objects.requireNonNull(e.getUserId(), "entity.userId must not be null");

        TrendType trendType = parseEnum(TrendType.class, e.getTrendType(), "entity.trendType");
        PeriodGranularity granularity = parseEnum(PeriodGranularity.class, e.getGranularity(), "entity.granularity");

        var period = new Period(
                Objects.requireNonNull(e.getPeriodStart(), "entity.periodStart must not be null"),
                Objects.requireNonNull(e.getPeriodEnd(), "entity.periodEnd must not be null"),
                granularity
        );

        String currency = requireCurrency3(e.getCurrency(), "entity.currency");

        try {
            List<Long> series = om.readValue(e.getSeriesJson(), List.class);

            Instant createdAt = e.getCreatedAt() != null ? e.getCreatedAt() : Instant.now(clock);

            return new Trend(
                    id,
                    new UserId(userId),
                    period,
                    trendType,
                    series,
                    currency,
                    createdAt
            );

        } catch (Exception ex) {
            throw new IllegalStateException("Failed to deserialize series", ex);
        }
    }

    // Converte uma String persistida em enum do tipo informado, validando não vazio e lançando IllegalArgumentException quando inválido.
    private static <E extends Enum<E>> E parseEnum(Class<E> enumType, String raw, String field) {
        String v = requireNonBlank(raw, field);
        try {
            return Enum.valueOf(enumType, v);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException(field + " is invalid: " + v, ex);
        }
    }

    // Garante que uma String obrigatória esteja preenchida (não nula/não vazia), normalizando com trim e lançando IllegalArgumentException em caso de falha.
    private static String requireNonBlank(String v, String field) {
        if (v == null || v.trim().isEmpty()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return v.trim();
    }

    // Valida e normaliza um código de moeda de 3 letras (ISO-4217), garantindo uppercase e tamanho exato 3.
    private static String requireCurrency3(String v, String field) {
        String c = requireNonBlank(v, field).toUpperCase();
        if (c.length() != 3) {
            throw new IllegalArgumentException(field + " must have length 3");
        }
        return c;
    }

}
