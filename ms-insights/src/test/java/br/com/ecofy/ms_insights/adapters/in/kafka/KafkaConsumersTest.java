package br.com.ecofy.ms_insights.adapters.in.kafka;

import br.com.ecofy.ms_insights.config.InsightsProperties;
import br.com.ecofy.ms_insights.core.application.service.InsightEventIngestionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Correção Dia 8 (item #6): consumers diferenciam payload irrecuperável (poison -> ACK, sem retry)
 * de falha transitória (relança -> DefaultErrorHandler faz retry). Antes engoliam tudo silenciosamente.
 */
class KafkaConsumersTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final InsightEventIngestionService ingestion = mock(InsightEventIngestionService.class);

    private InsightsProperties props() {
        return new InsightsProperties(
                new InsightsProperties.Topics("eco.transaction.categorized", "eco.budget.alert", "eco.insight.created", null),
                new InsightsProperties.Idempotency(300),
                new InsightsProperties.Engine(2000, 60, false));
    }

    private CategorizedTransactionConsumer txConsumer() {
        return new CategorizedTransactionConsumer(props(), objectMapper, ingestion);
    }

    private BudgetAlertConsumer alertConsumer() {
        return new BudgetAlertConsumer(props(), objectMapper, ingestion);
    }

    @Test
    void tx_validPayload_shouldSignalGenerate() {
        UUID userId = UUID.randomUUID();
        txConsumer().consume("{\"userId\":\"" + userId + "\"}");
        verify(ingestion).onSignalGenerate(userId);
    }

    @Test
    void tx_blankPayload_shouldSkip() {
        txConsumer().consume("   ");
        verifyNoInteractions(ingestion);
    }

    @Test
    void tx_poisonPayload_shouldAckWithoutSignalingOrThrowing() {
        // JSON malformado -> poison -> não lança, não sinaliza (ACK)
        txConsumer().consume("{ not json ");
        verifyNoInteractions(ingestion);
    }

    @Test
    void tx_missingUserId_shouldBePoisonAndNotThrow() {
        txConsumer().consume("{\"transactionId\":\"" + UUID.randomUUID() + "\"}");
        verifyNoInteractions(ingestion);
    }

    @Test
    void tx_transientFailure_shouldRethrowForRetry() {
        UUID userId = UUID.randomUUID();
        doThrow(new RuntimeException("db down")).when(ingestion).onSignalGenerate(any());

        assertThatThrownBy(() -> txConsumer().consume("{\"userId\":\"" + userId + "\"}"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Transient failure");
    }

    @Test
    void alert_validPayload_shouldSignalGenerate() {
        UUID userId = UUID.randomUUID();
        alertConsumer().consume("{\"userId\":\"" + userId + "\",\"severity\":\"CRITICAL\"}");
        verify(ingestion).onSignalGenerate(userId);
    }

    @Test
    void alert_poisonPayload_shouldAckWithoutThrowing() {
        alertConsumer().consume("{ broken ");
        verifyNoInteractions(ingestion);
    }

    @Test
    void alert_transientFailure_shouldRethrowForRetry() {
        UUID userId = UUID.randomUUID();
        doThrow(new RuntimeException("kafka hiccup")).when(ingestion).onSignalGenerate(any());

        assertThatThrownBy(() -> alertConsumer().consume("{\"userId\":\"" + userId + "\"}"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Transient failure");
    }
}
