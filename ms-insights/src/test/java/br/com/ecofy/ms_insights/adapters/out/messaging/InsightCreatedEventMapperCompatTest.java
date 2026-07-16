package br.com.ecofy.ms_insights.adapters.out.messaging;

import br.com.ecofy.ms_insights.adapters.out.messaging.dto.InsightCreatedEvent;
import br.com.ecofy.ms_insights.adapters.out.messaging.mapper.EventMapper;
import br.com.ecofy.ms_insights.core.domain.Insight;
import br.com.ecofy.ms_insights.core.domain.enums.InsightType;
import br.com.ecofy.ms_insights.core.domain.enums.PeriodGranularity;
import br.com.ecofy.ms_insights.core.domain.valueobject.InsightKey;
import br.com.ecofy.ms_insights.core.domain.valueobject.Period;
import br.com.ecofy.ms_insights.core.domain.valueobject.UserId;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Correção Dia 8 (item #8/#11): garante que o JSON de insight.created contém os campos que o consumidor
 * {@code InsightCreatedEventMessage} do ms-notification lê (userId, insightId, insightType, periodStart,
 * periodEnd, metadata.eventId) — antes o evento só tinha "type" e faltava insightType/período/metadata.
 */
class InsightCreatedEventMapperCompatTest {

    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-01-15T10:00:00Z"), ZoneOffset.UTC);

    private Insight insight(UUID userId, UUID insightId) {
        var key = new InsightKey(new UserId(userId), InsightType.SPENDING_BREAKDOWN,
                new Period(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31), PeriodGranularity.MONTH));
        return new Insight(insightId, key, InsightType.SPENDING_BREAKDOWN, 80,
                "Title", "Summary", Map.of("k", "v"), Instant.now(CLOCK));
    }

    @Test
    void toCreatedEvent_shouldPopulateNotificationCompatibleFields() {
        UUID userId = UUID.randomUUID();
        UUID insightId = UUID.randomUUID();

        InsightCreatedEvent evt = EventMapper.toCreatedEvent(insight(userId, insightId), CLOCK);

        assertThat(evt.userId()).isEqualTo(userId.toString());
        assertThat(evt.insightId()).isEqualTo(insightId.toString());
        assertThat(evt.insightType()).isEqualTo("SPENDING_BREAKDOWN");
        assertThat(evt.periodStart()).isEqualTo("2026-01-01");
        assertThat(evt.periodEnd()).isEqualTo("2026-01-31");
        assertThat(evt.metadata()).isNotNull();
        assertThat(evt.metadata().eventId()).isEqualTo(evt.eventId());
        assertThat(evt.metadata().source()).isEqualTo("ms-insights");
    }

    @Test
    void publishedJson_shouldContainFieldsRequiredByMsNotification() throws Exception {
        UUID userId = UUID.randomUUID();
        InsightCreatedEvent evt = EventMapper.toCreatedEvent(insight(userId, UUID.randomUUID()), CLOCK);

        JsonNode json = mapper.readTree(mapper.writeValueAsString(evt));

        assertThat(json.hasNonNull("userId")).isTrue();
        assertThat(json.get("userId").asText()).isEqualTo(userId.toString());
        assertThat(json.hasNonNull("insightId")).isTrue();
        assertThat(json.get("insightType").asText()).isEqualTo("SPENDING_BREAKDOWN");
        assertThat(json.get("periodStart").asText()).isEqualTo("2026-01-01");
        assertThat(json.get("periodEnd").asText()).isEqualTo("2026-01-31");
        assertThat(json.get("metadata").hasNonNull("eventId")).isTrue();
    }
}
