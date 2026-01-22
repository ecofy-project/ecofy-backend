package br.com.ecofy.ms_insights.adapters.out.persistence.mapper;

import br.com.ecofy.ms_insights.adapters.out.persistence.entity.InsightEntity;
import br.com.ecofy.ms_insights.core.domain.Insight;
import br.com.ecofy.ms_insights.core.domain.enums.InsightType;
import br.com.ecofy.ms_insights.core.domain.enums.PeriodGranularity;
import br.com.ecofy.ms_insights.core.domain.valueobject.InsightKey;
import br.com.ecofy.ms_insights.core.domain.valueobject.Period;
import br.com.ecofy.ms_insights.core.domain.valueobject.UserId;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Clock;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public final class InsightMapper {

    private static final TypeReference<Map<String, Object>> MAP_REF = new TypeReference<>() {};

    // Impede instanciação e reforça o uso estático (classe utilitária de mapeamento domain <-> persistence).
    private InsightMapper() {
    }

    // Converte Insight (domínio) em InsightEntity (persistência), validando campos obrigatórios e serializando o payload para JSON.
    public static InsightEntity toEntity(Insight d, ObjectMapper om) {
        Objects.requireNonNull(d, "insight must not be null");
        Objects.requireNonNull(om, "objectMapper must not be null");

        Objects.requireNonNull(d.getId(), "insight.id must not be null");
        Objects.requireNonNull(d.getKey(), "insight.key must not be null");
        Objects.requireNonNull(d.getKey().userId(), "insight.key.userId must not be null");
        Objects.requireNonNull(d.getKey().userId().value(), "insight.key.userId.value must not be null");
        Objects.requireNonNull(d.getKey().period(), "insight.key.period must not be null");
        Objects.requireNonNull(d.getType(), "insight.type must not be null");
        Objects.requireNonNull(d.getKey().period().granularity(), "insight.key.period.granularity must not be null");

        String title = requireNonBlank(d.getTitle(), "insight.title");
        String summary = requireNonBlank(d.getSummary(), "insight.summary");

        String payloadJson = serializePayload(om, d.getPayload());

        return InsightEntity.builder()
                .id(d.getId())
                .userId(d.getKey().userId().value())
                .type(d.getType().name())
                .periodStart(d.getKey().period().start())
                .periodEnd(d.getKey().period().end())
                .granularity(d.getKey().period().granularity().name())
                .score(d.getScore())
                .title(title)
                .summary(summary)
                .payloadJson(payloadJson)
                .createdAt(d.getCreatedAt())
                .build();
    }

    // Converte InsightEntity em Insight (domínio) usando Clock padrão UTC para timestamps de fallback.
    public static Insight toDomain(InsightEntity e, ObjectMapper om) {
        return toDomain(e, om, Clock.systemUTC());
    }

    // Converte InsightEntity em Insight (domínio) validando campos, parseando enums, desserializando payload e aplicando fallback de createdAt via Clock.
    public static Insight toDomain(InsightEntity e, ObjectMapper om, Clock clock) {
        Objects.requireNonNull(e, "entity must not be null");
        Objects.requireNonNull(om, "objectMapper must not be null");
        Objects.requireNonNull(clock, "clock must not be null");

        UUID id = Objects.requireNonNull(e.getId(), "entity.id must not be null");
        UUID userId = Objects.requireNonNull(e.getUserId(), "entity.userId must not be null");

        InsightType type = parseEnum(InsightType.class, e.getType(), "entity.type");
        PeriodGranularity granularity = parseEnum(PeriodGranularity.class, e.getGranularity(), "entity.granularity");

        var period = new Period(
                Objects.requireNonNull(e.getPeriodStart(), "entity.periodStart must not be null"),
                Objects.requireNonNull(e.getPeriodEnd(), "entity.periodEnd must not be null"),
                granularity
        );

        Map<String, Object> payload = deserializePayload(om, e.getPayloadJson());

        Instant createdAt = e.getCreatedAt() != null ? e.getCreatedAt() : Instant.now(clock);

        var key = new InsightKey(new UserId(userId), type, period);

        return new Insight(
                id,
                key,
                type,
                e.getScore(),
                requireNonBlank(e.getTitle(), "entity.title"),
                requireNonBlank(e.getSummary(), "entity.summary"),
                payload,
                createdAt
        );
    }

    // Cria uma nova instância de Insight com id aleatório e createdAt em UTC, aplicando defaults (payload vazio) e validações básicas.
    public static Insight newInsight(
            UserId userId,
            InsightType type,
            Period period,
            int score,
            String title,
            String summary,
            Map<String, Object> payload
    ) {
        return newInsight(userId, type, period, score, title, summary, payload, Clock.systemUTC());
    }

    // Cria uma nova instância de Insight com Clock injetável para facilitar testes determinísticos (id aleatório, createdAt via Clock, payload vazio se null).
    public static Insight newInsight(
            UserId userId,
            InsightType type,
            Period period,
            int score,
            String title,
            String summary,
            Map<String, Object> payload,
            Clock clock
    ) {
        Objects.requireNonNull(userId, "userId must not be null");
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(period, "period must not be null");
        Objects.requireNonNull(clock, "clock must not be null");

        return new Insight(
                UUID.randomUUID(),
                new InsightKey(userId, type, period),
                type,
                score,
                requireNonBlank(title, "title"),
                requireNonBlank(summary, "summary"),
                payload == null ? Collections.emptyMap() : payload,
                Instant.now(clock)
        );
    }

    // Serializa o payload (Map) do insight para JSON, aplicando fallback para mapa vazio e encapsulando falhas em IllegalStateException.
    private static String serializePayload(ObjectMapper om, Map<String, Object> payload) {
        try {
            return om.writeValueAsString(payload == null ? Collections.emptyMap() : payload);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize insight payload", e);
        }
    }

    // Desserializa o JSON do payload para Map, retornando mapa vazio quando o JSON é nulo/vazio e encapsulando falhas em IllegalStateException.
    private static Map<String, Object> deserializePayload(ObjectMapper om, String json) {
        if (json == null || json.trim().isEmpty()) {
            return Collections.emptyMap();
        }
        try {
            return om.readValue(json, MAP_REF);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to deserialize insight payloadJson", e);
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

}
