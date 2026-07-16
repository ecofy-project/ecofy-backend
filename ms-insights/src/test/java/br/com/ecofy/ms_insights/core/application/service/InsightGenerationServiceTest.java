package br.com.ecofy.ms_insights.core.application.service;

import br.com.ecofy.ms_insights.config.InsightsProperties;
import br.com.ecofy.ms_insights.core.application.command.GenerateInsightsCommand;
import br.com.ecofy.ms_insights.core.application.result.InsightsBundleResult;
import br.com.ecofy.ms_insights.core.domain.Insight;
import br.com.ecofy.ms_insights.core.domain.MetricSnapshot;
import br.com.ecofy.ms_insights.core.domain.enums.PeriodGranularity;
import br.com.ecofy.ms_insights.core.domain.exception.IdempotencyViolationException;
import br.com.ecofy.ms_insights.core.port.out.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

class InsightGenerationServiceTest {

    private LoadCategorizedTransactionsPort loadTxPort;
    private LoadBudgetsForUserPort loadBudgetsPort;
    private LoadGoalsPort loadGoalsPort;
    private SaveInsightPort saveInsightPort;
    private SaveMetricSnapshotPort saveMetricPort;
    private PublishInsightCreatedEventPort publishPort;
    private IdempotencyPort idempotencyPort;

    private InsightGenerationService service;

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-01-15T10:00:00Z"), ZoneOffset.UTC);
    private static final UUID USER_ID = UUID.randomUUID();

    private InsightsProperties props(int minScoreToPublish) {
        return new InsightsProperties(
                new InsightsProperties.Topics("t.tx", "t.alert", "t.insight", null),
                new InsightsProperties.Idempotency(300),
                new InsightsProperties.Engine(2000, minScoreToPublish, false)
        );
    }

    @BeforeEach
    void setUp() {
        loadTxPort = mock(LoadCategorizedTransactionsPort.class);
        loadBudgetsPort = mock(LoadBudgetsForUserPort.class);
        loadGoalsPort = mock(LoadGoalsPort.class);
        saveInsightPort = mock(SaveInsightPort.class);
        saveMetricPort = mock(SaveMetricSnapshotPort.class);
        publishPort = mock(PublishInsightCreatedEventPort.class);
        idempotencyPort = mock(IdempotencyPort.class);

        when(saveInsightPort.save(any(Insight.class))).thenAnswer(inv -> inv.getArgument(0));
        when(saveMetricPort.save(any(MetricSnapshot.class))).thenAnswer(inv -> inv.getArgument(0));
        when(loadGoalsPort.findByUserId(any())).thenReturn(List.of());
    }

    private InsightGenerationService build(int minScore) {
        return new InsightGenerationService(props(minScore), loadTxPort, loadBudgetsPort, loadGoalsPort,
                saveInsightPort, saveMetricPort, publishPort, idempotencyPort, CLOCK);
    }

    private GenerateInsightsCommand cmd() {
        return new GenerateInsightsCommand(USER_ID, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31),
                PeriodGranularity.MONTH, "idem-key-123");
    }

    @Test
    void generate_shouldThrowIdempotencyViolation_whenKeyNotAcquired() {
        service = build(60);
        when(idempotencyPort.tryAcquire(any(), anyInt())).thenReturn(false);

        assertThatThrownBy(() -> service.generate(cmd())).isInstanceOf(IdempotencyViolationException.class);
        verifyNoInteractions(loadTxPort, saveInsightPort, publishPort);
    }

    @Test
    void generate_shouldPersistMetricsAndPublish_whenScoreAboveThreshold() {
        service = build(60);
        when(idempotencyPort.tryAcquire(any(), anyInt())).thenReturn(true);
        // gasto 1200 vs limite 1000 -> ratio 1.2 -> score 95 (>= 60 -> publica)
        when(loadTxPort.loadForUserAndPeriod(any(), any(), anyInt())).thenReturn(List.of(
                new CategorizedTxView(UUID.randomUUID(), USER_ID, UUID.randomUUID(), 1200L, "BRL", LocalDate.of(2026, 1, 10), false)));
        when(loadBudgetsPort.loadBudgets(any())).thenReturn(List.of(
                new LoadBudgetsForUserPort.BudgetView(UUID.randomUUID(), UUID.randomUUID(), 1000L, "BRL", "ACTIVE")));

        InsightsBundleResult result = service.generate(cmd());

        assertThat(result.insights()).hasSize(1);
        assertThat(result.metrics()).hasSize(2); // TOTAL_SPENT + INCOME
        verify(saveMetricPort, times(2)).save(any(MetricSnapshot.class));
        verify(saveInsightPort).save(any(Insight.class));
        verify(publishPort).publish(any(Insight.class));
    }

    @Test
    void generate_shouldNotPublish_whenScoreBelowThreshold() {
        service = build(60);
        when(idempotencyPort.tryAcquire(any(), anyInt())).thenReturn(true);
        // sem budgets -> score 30 (< 60 -> não publica)
        when(loadTxPort.loadForUserAndPeriod(any(), any(), anyInt())).thenReturn(List.of());
        when(loadBudgetsPort.loadBudgets(any())).thenReturn(List.of());

        InsightsBundleResult result = service.generate(cmd());

        assertThat(result.insights()).hasSize(1);
        verify(publishPort, never()).publish(any());
    }

    @Test
    void generate_shouldPropagateExternalUnavailable_notSwallow() {
        service = build(60);
        when(idempotencyPort.tryAcquire(any(), anyInt())).thenReturn(true);
        when(loadTxPort.loadForUserAndPeriod(any(), any(), anyInt()))
                .thenThrow(new br.com.ecofy.ms_insights.core.domain.exception.ExternalDataUnavailableException(
                        "categorization", "down", new RuntimeException()));

        assertThatThrownBy(() -> service.generate(cmd()))
                .isInstanceOf(br.com.ecofy.ms_insights.core.domain.exception.ExternalDataUnavailableException.class);
        verify(publishPort, never()).publish(any());
    }
}
