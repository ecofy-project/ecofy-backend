package br.com.ecofy.ms_insights.adapters.out.persistence.mapper;

import br.com.ecofy.ms_insights.adapters.out.persistence.entity.MetricSnapshotEntity;
import br.com.ecofy.ms_insights.core.domain.MetricSnapshot;
import br.com.ecofy.ms_insights.core.domain.enums.MetricType;
import br.com.ecofy.ms_insights.core.domain.enums.PeriodGranularity;
import br.com.ecofy.ms_insights.core.domain.valueobject.Money;
import br.com.ecofy.ms_insights.core.domain.valueobject.Period;
import br.com.ecofy.ms_insights.core.domain.valueobject.UserId;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class MetricSnapshotMapper {

    // Impede instanciação e reforça o uso estático (classe utilitária de mapeamento domain <-> persistence).
    private MetricSnapshotMapper() {
    }

    // Converte MetricSnapshot (domínio) em MetricSnapshotEntity (persistência), validando campos obrigatórios e normalizando moeda.
    public static MetricSnapshotEntity toEntity(MetricSnapshot d) {
        Objects.requireNonNull(d, "metricSnapshot must not be null");
        Objects.requireNonNull(d.getId(), "metricSnapshot.id must not be null");
        Objects.requireNonNull(d.getUserId(), "metricSnapshot.userId must not be null");
        Objects.requireNonNull(d.getUserId().value(), "metricSnapshot.userId.value must not be null");
        Objects.requireNonNull(d.getPeriod(), "metricSnapshot.period must not be null");
        Objects.requireNonNull(d.getPeriod().granularity(), "metricSnapshot.period.granularity must not be null");
        Objects.requireNonNull(d.getMetricType(), "metricSnapshot.metricType must not be null");
        Objects.requireNonNull(d.getValue(), "metricSnapshot.value must not be null");

        return MetricSnapshotEntity.builder()
                .id(d.getId())
                .userId(d.getUserId().value())
                .metricType(d.getMetricType().name())
                .periodStart(Objects.requireNonNull(d.getPeriod().start(), "metricSnapshot.period.start must not be null"))
                .periodEnd(Objects.requireNonNull(d.getPeriod().end(), "metricSnapshot.period.end must not be null"))
                .granularity(d.getPeriod().granularity().name())
                .valueCents(d.getValue().cents())
                .currency(requireCurrency3(d.getValue().currency(), "metricSnapshot.value.currency"))
                .createdAt(d.getCreatedAt())
                .build();
    }

    // Converte MetricSnapshotEntity em MetricSnapshot (domínio) usando Clock padrão UTC para timestamps de fallback.
    public static MetricSnapshot toDomain(MetricSnapshotEntity e) {
        return toDomain(e, Clock.systemUTC());
    }

    // Converte MetricSnapshotEntity em MetricSnapshot (domínio) validando campos, parseando enums e aplicando fallback de createdAt via Clock.
    public static MetricSnapshot toDomain(MetricSnapshotEntity e, Clock clock) {
        Objects.requireNonNull(e, "entity must not be null");
        Objects.requireNonNull(clock, "clock must not be null");

        UUID id = Objects.requireNonNull(e.getId(), "entity.id must not be null");
        UUID userId = Objects.requireNonNull(e.getUserId(), "entity.userId must not be null");

        MetricType metricType = parseEnum(MetricType.class, e.getMetricType(), "entity.metricType");
        PeriodGranularity granularity = parseEnum(PeriodGranularity.class, e.getGranularity(), "entity.granularity");

        var period = new Period(
                Objects.requireNonNull(e.getPeriodStart(), "entity.periodStart must not be null"),
                Objects.requireNonNull(e.getPeriodEnd(), "entity.periodEnd must not be null"),
                granularity
        );

        var value = Money.ofCents(
                e.getValueCents(),
                requireCurrency3(e.getCurrency(), "entity.currency")
        );

        Instant createdAt = e.getCreatedAt() != null ? e.getCreatedAt() : Instant.now(clock);

        return new MetricSnapshot(
                id,
                new UserId(userId),
                period,
                metricType,
                value,
                createdAt
        );
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
