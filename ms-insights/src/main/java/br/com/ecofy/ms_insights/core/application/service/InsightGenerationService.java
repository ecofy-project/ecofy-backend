package br.com.ecofy.ms_insights.core.application.service;

import br.com.ecofy.ms_insights.config.InsightsProperties;
import br.com.ecofy.ms_insights.core.application.command.GenerateInsightsCommand;
import br.com.ecofy.ms_insights.core.application.result.*;
import br.com.ecofy.ms_insights.core.domain.Insight;
import br.com.ecofy.ms_insights.core.domain.MetricSnapshot;
import br.com.ecofy.ms_insights.core.domain.enums.InsightType;
import br.com.ecofy.ms_insights.core.domain.enums.MetricType;
import br.com.ecofy.ms_insights.core.domain.exception.IdempotencyViolationException;
import br.com.ecofy.ms_insights.core.domain.valueobject.InsightKey;
import br.com.ecofy.ms_insights.core.domain.valueobject.Money;
import br.com.ecofy.ms_insights.core.domain.valueobject.Period;
import br.com.ecofy.ms_insights.core.domain.valueobject.UserId;
import br.com.ecofy.ms_insights.core.port.in.GenerateInsightsUseCase;
import br.com.ecofy.ms_insights.core.port.out.*;
import br.com.ecofy.ms_insights.core.port.out.CategorizedTxView;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.*;

@Slf4j
@Service
public class InsightGenerationService implements GenerateInsightsUseCase {

    private static final InsightType DEFAULT_INSIGHT_TYPE = InsightType.SPENDING_BREAKDOWN;
    private static final String DEFAULT_CURRENCY = "BRL";
    private static final String IDEM_PREFIX = "insights.generate|";
    private static final int DEFAULT_MAX_TX = 2_000;

    private final InsightsProperties properties;

    private final LoadCategorizedTransactionsPort loadCategorizedTransactionsPort;
    private final LoadBudgetsForUserPort loadBudgetsForUserPort;
    private final LoadGoalsPort loadGoalsPort;

    private final SaveInsightPort saveInsightPort;
    private final SaveMetricSnapshotPort saveMetricSnapshotPort;
    private final PublishInsightCreatedEventPort publishInsightCreatedEventPort;

    private final IdempotencyPort idempotencyPort;
    private final Clock clock;

    // Injeta propriedades de engine/idempotência e todas as portas necessárias para carregar dados, persistir resultados e publicar eventos, usando Clock para timestamps determinísticos.
    public InsightGenerationService(
            InsightsProperties properties,
            LoadCategorizedTransactionsPort loadCategorizedTransactionsPort,
            LoadBudgetsForUserPort loadBudgetsForUserPort,
            LoadGoalsPort loadGoalsPort,
            SaveInsightPort saveInsightPort,
            SaveMetricSnapshotPort saveMetricSnapshotPort,
            PublishInsightCreatedEventPort publishInsightCreatedEventPort,
            IdempotencyPort idempotencyPort,
            Clock clock
    ) {
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
        this.loadCategorizedTransactionsPort = Objects.requireNonNull(loadCategorizedTransactionsPort, "loadCategorizedTransactionsPort must not be null");
        this.loadBudgetsForUserPort = Objects.requireNonNull(loadBudgetsForUserPort, "loadBudgetsForUserPort must not be null");
        this.loadGoalsPort = Objects.requireNonNull(loadGoalsPort, "loadGoalsPort must not be null");
        this.saveInsightPort = Objects.requireNonNull(saveInsightPort, "saveInsightPort must not be null");
        this.saveMetricSnapshotPort = Objects.requireNonNull(saveMetricSnapshotPort, "saveMetricSnapshotPort must not be null");
        this.publishInsightCreatedEventPort = Objects.requireNonNull(publishInsightCreatedEventPort, "publishInsightCreatedEventPort must not be null");
        this.idempotencyPort = Objects.requireNonNull(idempotencyPort, "idempotencyPort must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    // Orquestra a geração de insights para um usuário e período: aplica idempotência, carrega dados (tx/budgets/goals), calcula métricas/score, persiste e publica evento conforme limiar.
    @Override
    @Transactional
    public InsightsBundleResult generate(GenerateInsightsCommand cmd) {
        Objects.requireNonNull(cmd, "cmd must not be null");
        Objects.requireNonNull(cmd.userId(), "cmd.userId must not be null");
        Objects.requireNonNull(cmd.start(), "cmd.start must not be null");
        Objects.requireNonNull(cmd.end(), "cmd.end must not be null");
        Objects.requireNonNull(cmd.granularity(), "cmd.granularity must not be null");

        Period period = new Period(cmd.start(), cmd.end(), cmd.granularity());
        UserId userId = new UserId(cmd.userId());
        Instant now = Instant.now(clock);

        String idemKey = buildIdempotencyKey(userId, period, cmd.idempotencyKey());
        int ttlSeconds = normalizeTtlSeconds(properties.idempotency() == null ? null : properties.idempotency().ttlSeconds());

        log.info("[InsightGenerationService] - [generate] -> userId={} start={} end={} g={} idemKey={} ttlSeconds={}",
                userId.value(), period.start(), period.end(), period.granularity(), idemKey, ttlSeconds);

        boolean acquired = idempotencyPort.tryAcquire(idemKey, ttlSeconds);
        if (!acquired) {
            log.info("[InsightGenerationService] - [generate] -> IDEMPOTENCY_REJECTED userId={} idemKey={}", userId.value(), idemKey);
            throw new IdempotencyViolationException("Idempotency violation for key=" + idemKey);
        }

        int maxTx = normalizeMaxTransactions(properties.engine() == null ? null : properties.engine().maxTransactionsToAnalyze());

        List<CategorizedTxView> txs =
                safeList(loadCategorizedTransactionsPort.loadForUserAndPeriod(userId.value(), period, maxTx));

        List<LoadBudgetsForUserPort.BudgetView> budgets =
                safeList(loadBudgetsForUserPort.loadBudgets(userId.value()));

        List<br.com.ecofy.ms_insights.core.domain.Goal> goals =
                safeList(loadGoalsPort.findByUserId(userId.value()));

        String currency = resolveCurrency(txs);

        long totalSpentCents = txs.stream()
                .filter(Objects::nonNull)
                .filter(t -> !t.income())
                .mapToLong(CategorizedTxView::amountCents)
                .sum();

        long totalIncomeCents = txs.stream()
                .filter(Objects::nonNull)
                .filter(CategorizedTxView::income)
                .mapToLong(CategorizedTxView::amountCents)
                .sum();

        MetricSnapshot spentSnap = saveMetricSnapshotPort.save(new MetricSnapshot(
                UUID.randomUUID(),
                userId,
                period,
                MetricType.TOTAL_SPENT,
                new Money(totalSpentCents, currency),
                now
        ));

        MetricSnapshot incomeSnap = saveMetricSnapshotPort.save(new MetricSnapshot(
                UUID.randomUUID(),
                userId,
                period,
                MetricType.INCOME,
                new Money(totalIncomeCents, currency),
                now
        ));

        Map<UUID, Long> spentByCategory = buildSpentByCategory(txs);

        List<Map.Entry<UUID, Long>> topCategories = spentByCategory.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .limit(10)
                .toList();

        int score = computeScore(totalSpentCents, budgets);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("periodStart", period.start().toString());
        payload.put("periodEnd", period.end().toString());
        payload.put("granularity", period.granularity().name());
        payload.put("currency", currency);
        payload.put("totalSpentCents", totalSpentCents);
        payload.put("totalIncomeCents", totalIncomeCents);
        payload.put("topCategories", topCategories.stream().map(e -> Map.of(
                "categoryId", e.getKey() == null ? null : e.getKey().toString(),
                "spentCents", e.getValue()
        )).toList());
        payload.put("budgetsCount", budgets.size());
        payload.put("goalsCount", goals.size());

        InsightKey key = new InsightKey(userId, DEFAULT_INSIGHT_TYPE, period);

        Insight insight = new Insight(
                UUID.randomUUID(),
                key,
                DEFAULT_INSIGHT_TYPE,
                score,
                "Spending breakdown",
                "Top spending categories for the selected period.",
                payload,
                now
        );

        Insight saved = saveInsightPort.save(insight);

        int minScoreToPublish = normalizeMinScore(properties.engine() == null ? null : properties.engine().minScoreToPublish());
        boolean shouldPublish = saved.getScore() >= minScoreToPublish;

        log.info("[InsightGenerationService] - [generate] -> generated insightId={} userId={} type={} score={} shouldPublish={} minScoreToPublish={} txCount={} budgets={} goals={}",
                saved.getId(), userId.value(), saved.getType(), saved.getScore(), shouldPublish, minScoreToPublish,
                txs.size(), budgets.size(), goals.size());

        if (shouldPublish) {
            publishInsightCreatedEventPort.publish(saved);
        }

        return new InsightsBundleResult(
                List.of(toInsightResult(saved)),
                List.of(toMetricResult(spentSnap), toMetricResult(incomeSnap)),
                goals.stream().map(GoalService::toResult).toList()
        );
    }

    // Constrói a chave de idempotência final usando prefixo fixo e uma base derivada do rawKey (se informado) ou de user+período+granularidade.
    private static String buildIdempotencyKey(UserId userId, Period period, String rawKey) {
        String base = (rawKey == null || rawKey.trim().isEmpty())
                ? (userId.value() + "|" + period.start() + "|" + period.end() + "|" + period.granularity())
                : rawKey.trim();
        return IDEM_PREFIX + base;
    }

    // Normaliza o TTL da idempotência aplicando fallback seguro quando ausente ou inválido.
    private static int normalizeTtlSeconds(Integer ttlSeconds) {
        if (ttlSeconds == null || ttlSeconds <= 0) return 300;
        return ttlSeconds;
    }

    // Normaliza o limite de transações a analisar aplicando default quando ausente ou inválido.
    private static int normalizeMaxTransactions(Integer max) {
        if (max == null || max <= 0) return DEFAULT_MAX_TX;
        return max;
    }

    // Normaliza o score mínimo de publicação garantindo faixa 0..100 com fallback padrão.
    private static int normalizeMinScore(Integer minScore) {
        if (minScore == null) return 50;
        if (minScore < 0) return 0;
        if (minScore > 100) return 100;
        return minScore;
    }

    // Protege contra retornos null de integrações externas, padronizando para lista vazia.
    private static <T> List<T> safeList(List<T> v) {
        return v == null ? List.of() : v;
    }

    // Resolve a moeda do processamento a partir da primeira transação com currency válida, usando fallback quando ausente.
    private static String resolveCurrency(List<CategorizedTxView> txs) {
        return txs.stream()
                .filter(Objects::nonNull)
                .map(CategorizedTxView::currency)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .findFirst()
                .orElse(DEFAULT_CURRENCY);
    }

    // Agrega gasto (somente despesas) por categoryId somando amountCents para produzir ranking de categorias.
    private static Map<UUID, Long> buildSpentByCategory(List<CategorizedTxView> txs) {
        Map<UUID, Long> byCat = new HashMap<>();
        for (var t : txs) {
            if (t == null) continue;
            if (t.income()) continue;
            byCat.merge(t.categoryId(), t.amountCents(), Long::sum);
        }
        return byCat;
    }

    // Calcula um score heurístico do insight comparando gasto total com soma de limites de budgets (quando existirem).
    private static int computeScore(long totalSpentCents, List<LoadBudgetsForUserPort.BudgetView> budgets) {
        if (budgets == null || budgets.isEmpty()) return 30;

        long totalLimit = budgets.stream()
                .filter(Objects::nonNull)
                .mapToLong(LoadBudgetsForUserPort.BudgetView::limitCents)
                .sum();

        if (totalLimit <= 0) return 40;

        double ratio = (double) totalSpentCents / (double) totalLimit;

        if (ratio >= 1.2) return 95;
        if (ratio >= 1.0) return 85;
        if (ratio >= 0.8) return 70;
        return 55;
    }

    // Converte Insight (domínio) em InsightResult (DTO) para compor o retorno do bundle.
    private static InsightResult toInsightResult(Insight i) {
        return new InsightResult(
                i.getId(),
                i.getKey().userId().value(),
                i.getType(),
                i.getScore(),
                i.getTitle(),
                i.getSummary(),
                i.getPayload(),
                i.getCreatedAt()
        );
    }

    // Converte MetricSnapshot (domínio) em MetricSnapshotResult (DTO) para compor o retorno do bundle.
    private static MetricSnapshotResult toMetricResult(MetricSnapshot s) {
        return new MetricSnapshotResult(
                s.getId(),
                s.getUserId().value(),
                s.getMetricType(),
                s.getValue().cents(),
                s.getValue().currency(),
                s.getCreatedAt()
        );
    }

}
