package br.com.ecofy.ms_insights.adapters.out.messaging;

import br.com.ecofy.ms_insights.config.InsightsProperties;
import br.com.ecofy.ms_insights.core.domain.Insight;
import br.com.ecofy.ms_insights.core.domain.enums.InsightType;
import br.com.ecofy.ms_insights.core.domain.enums.PeriodGranularity;
import br.com.ecofy.ms_insights.core.domain.valueobject.InsightKey;
import br.com.ecofy.ms_insights.core.domain.valueobject.Period;
import br.com.ecofy.ms_insights.core.domain.valueobject.UserId;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class InsightCreatedKafkaAdapterTest {

    @SuppressWarnings("unchecked")
    private final KafkaTemplate<String, String> kafkaTemplate = mock(KafkaTemplate.class);
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-01-15T10:00:00Z"), ZoneOffset.UTC);

    private InsightsProperties props() {
        return new InsightsProperties(
                new InsightsProperties.Topics("t.tx", "t.alert", "eco.insight.created", null),
                new InsightsProperties.Idempotency(300),
                new InsightsProperties.Engine(2000, 60, false));
    }

    private Insight insight(UUID userId) {
        var key = new InsightKey(new UserId(userId), InsightType.SPENDING_BREAKDOWN,
                new Period(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31), PeriodGranularity.MONTH));
        return new Insight(UUID.randomUUID(), key, InsightType.SPENDING_BREAKDOWN, 80,
                "Title", "Summary", Map.of("k", "v"), Instant.now(CLOCK));
    }

    @Test
    void publish_shouldSendToTopicWithUserIdKeyAndObserveFuture() {
        var adapter = new InsightCreatedKafkaAdapter(props(), kafkaTemplate, objectMapper, CLOCK);
        UUID userId = UUID.randomUUID();

        when(kafkaTemplate.send(anyString(), anyString(), anyString()))
                .thenReturn(CompletableFuture.completedFuture(mock(SendResult.class, RETURNS_DEEP_STUBS)));

        adapter.publish(insight(userId));

        ArgumentCaptor<String> topicC = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> keyC = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> payloadC = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate).send(topicC.capture(), keyC.capture(), payloadC.capture());

        assertThat(topicC.getValue()).isEqualTo("eco.insight.created");
        assertThat(keyC.getValue()).isEqualTo(userId.toString());
        // payload JSON contém os campos compatíveis com o ms-notification
        assertThat(payloadC.getValue()).contains("\"insightType\":\"SPENDING_BREAKDOWN\"")
                .contains("\"userId\":\"" + userId + "\"")
                .contains("\"metadata\"");
    }

    @Test
    void publish_shouldThrow_whenTopicBlank() {
        var badProps = new InsightsProperties(
                new InsightsProperties.Topics("t.tx", "t.alert", "   ", null),
                new InsightsProperties.Idempotency(300),
                new InsightsProperties.Engine(2000, 60, false));
        var adapter = new InsightCreatedKafkaAdapter(badProps, kafkaTemplate, objectMapper, CLOCK);

        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> adapter.publish(insight(UUID.randomUUID())));
        verifyNoInteractions(kafkaTemplate);
    }
}
