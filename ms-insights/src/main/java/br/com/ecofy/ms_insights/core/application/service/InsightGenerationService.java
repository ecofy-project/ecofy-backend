package br.com.ecofy.ms_insights.core.application.service;

import br.com.ecofy.ms_insights.config.InsightsProperties;
import br.com.ecofy.ms_insights.core.application.command.GenerateInsightsCommand;
import br.com.ecofy.ms_insights.core.application.result.*;
import br.com.ecofy.ms_insights.core.application.service.InsightPersistencePhase.PreparedInsight;
import br.com.ecofy.ms_insights.core.domain.enums.InsightType;
import br.com.ecofy.ms_insights.core.domain.exception.IdempotencyViolationException;
import br.com.ecofy.ms_insights.core.domain.valueobject.Period;
import br.com.ecofy.ms_insights.core.domain.valueobject.UserId;
import br.com.ecofy.ms_insights.core.port.in.GenerateInsightsUseCase;
import br.com.ecofy.ms_insights.core.port.out.*;
import br.com.ecofy.ms_insights.core.port.out.CategorizedTxView;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.*;

// Coordena a geração de insights sem prolongar transações durante integrações externas.
@Slf4j
@Service
public class InsightGenerationService implements GenerateInsightsUseCase {

    private static final InsightType DEFAULT_INSIGHT_TYPE =
            InsightType.SPENDING_BREAKDOWN;
    private static final String DEFAULT_CURRENCY = "BRL";
    private static final String IDEM_PREFIX = "insights.generate|";
    private static final int DEFAULT_MAX_TX = 2_000;

    private final InsightsProperties properties;

    private final LoadCategorizedTransactionsPort loadCategorizedTransactionsPort;
    private final LoadBudgetsForUserPort loadBudgetsForUserPort;

    private final IdempotencyPort idempotencyPort;
    private final InsightPersistencePhase persistencePhase;
    private final MeterRegistry meterRegistry;
    private final Clock clock;

    public InsightGenerationService(
            InsightsProperties properties,
            LoadCategorizedTransactionsPort loadCategorizedTransactionsPort,
            LoadBudgetsForUserPort loadBudgetsForUserPort,
            IdempotencyPort idempotencyPort,
            InsightPersistencePhase persistencePhase,
            MeterRegistry meterRegistry,
            Clock clock
    ) {
        this.properties = Objects.requireNonNull(
                properties,
                "properties must not be null"
        );
        this.loadCategorizedTransactionsPort = Objects.requireNonNull(
                loadCategorizedTransactionsPort,
                "loadCategorizedTransactionsPort must not be null"
        );
        this.loadBudgetsForUserPort = Objects.requireNonNull(
                loadBudgetsForUserPort,
                "loadBudgetsForUserPort must not be null"
        );
        this.idempotencyPort = Objects.requireNonNull(
                idempotencyPort,
                "idempotencyPort must not be null"
        );
        this.persistencePhase = Objects.requireNonNull(
                persistencePhase,
                "persistencePhase must not be null"
        );
        this.meterRegistry = Objects.requireNonNull(
                meterRegistry,
                "meterRegistry must not be null"
        );
        this.clock = Objects.requireNonNull(
                clock,
                "clock must not be null"
        );
    }

    // Coordena idempotência, integrações externas, processamento e persistência dos insights.
    @Override
    public InsightsBundleResult generate(GenerateInsightsCommand cmd) {
        Objects.requireNonNull(cmd, "cmd must not be null");
        Objects.requireNonNull(
                cmd.userId(),
                "cmd.userId must not be null"
        );
        Objects.requireNonNull(
                cmd.start(),
                "cmd.start must not be null"
        );
        Objects.requireNonNull(
                cmd.end(),
                "cmd.end must not be null"
        );
        Objects.requireNonNull(
                cmd.granularity(),
                "cmd.granularity must not be null"
        );

        Period period = new Period(
                cmd.start(),
                cmd.end(),
                cmd.granularity()
        );
        UserId userId = new UserId(cmd.userId());
        Instant now = Instant.now(clock);

        String idemKey = buildIdempotencyKey(
                userId,
                period,
                cmd.idempotencyKey()
        );
        int ttlSeconds = normalizeTtlSeconds(
                properties.idempotency() == null
                        ? null
                        : properties.idempotency().ttlSeconds()
        );

        boolean acquired = idempotencyPort.tryAcquire(
                idemKey,
                ttlSeconds
        );
        if (!acquired) {
            log.info(
                    "[InsightGenerationService] - [generate] -> IDEMPOTENCY_REJECTED userId={} idemKey={}",
                    userId.value(),
                    idemKey
            );
            throw new IdempotencyViolationException(
                    "Idempotency violation for key=" + idemKey
            );
        }

        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            int maxTx = normalizeMaxTransactions(
                    properties.engine() == null
                            ? null
                            : properties.engine().maxTransactionsToAnalyze()
            );

            List<CategorizedTxView> txs = safeList(
                    loadTransactions(userId, period, maxTx)
            );
            List<LoadBudgetsForUserPort.BudgetView> budgets = safeList(
                    loadBudgets(userId)
            );

            meterRegistry.summary(
                    "ecofy.insights.transactions.analyzed"
            ).record(txs.size());

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

            Map<String, Object> payload = buildPayload(
                    period,
                    currency,
                    totalSpentCents,
                    totalIncomeCents,
                    txs,
                    budgets
            );
            int score = computeScore(
                    totalSpentCents,
                    budgets
            );
            int minScoreToPublish = normalizeMinScore(
                    properties.engine() == null
                            ? null
                            : properties.engine().minScoreToPublish()
            );

            InsightsBundleResult result = persistencePhase.persist(
                    new PreparedInsight(
                            userId,
                            period,
                            DEFAULT_INSIGHT_TYPE,
                            currency,
                            totalSpentCents,
                            totalIncomeCents,
                            score,
                            minScoreToPublish,
                            "Spending breakdown",
                            "Top spending categories for the selected period.",
                            payload,
                            now
                    )
            );

            sample.stop(meterRegistry.timer(
                    "ecofy.insights.generation.duration",
                    "outcome",
                    "success"
            ));
            return result;

        } catch (Exception ex) {
            sample.stop(meterRegistry.timer(
                    "ecofy.insights.generation.duration",
                    "outcome",
                    "failure"
            ));
            meterRegistry.counter(
                    "ecofy.insights.generation.failed.total"
            ).increment();
            log.error(
                    "[InsightGenerationService] - [generate] -> Falha ao gerar insight userId={} start={} end={} g={} idemKey={}",
                    userId.value(),
                    period.start(),
                    period.end(),
                    period.granularity(),
                    idemKey,
                    ex
            );
            throw ex;
        }
    }

    // Carrega transações categorizadas e registra o resultado da integração.
    private List<CategorizedTxView> loadTransactions(
            UserId userId,
            Period period,
            int maxTx
    ) {
        try {
            var result = loadCategorizedTransactionsPort
                    .loadForUserAndPeriod(
                            userId.value(),
                            period,
                            maxTx
                    );
            meterRegistry.counter(
                    "ecofy.insights.external.call.total",
                    "provider",
                    "categorization",
                    "outcome",
                    "success"
            ).increment();
            return result;
        } catch (RuntimeException ex) {
            meterRegistry.counter(
                    "ecofy.insights.external.failure.total",
                    "provider",
                    "categorization"
            ).increment();
            throw ex;
        }
    }

    // Carrega orçamentos e registra o resultado da integração.
    private List<LoadBudgetsForUserPort.BudgetView> loadBudgets(
            UserId userId
    ) {
        try {
            var result = loadBudgetsForUserPort.loadBudgets(
                    userId.value()
            );
            meterRegistry.counter(
                    "ecofy.insights.external.call.total",
                    "provider",
                    "budgeting",
                    "outcome",
                    "success"
            ).increment();
            return result;
        } catch (RuntimeException ex) {
            meterRegistry.counter(
                    "ecofy.insights.external.failure.total",
                    "provider",
                    "budgeting"
            ).increment();
            throw ex;
        }
    }

    private static Map<String, Object> buildPayload(
            Period period,
            String currency,
            long totalSpentCents,
            long totalIncomeCents,
            List<CategorizedTxView> txs,
            List<LoadBudgetsForUserPort.BudgetView> budgets
    ) {
        Map<UUID, Long> spentByCategory = buildSpentByCategory(txs);
        List<Map.Entry<UUID, Long>> topCategories =
                spentByCategory.entrySet()
                        .stream()
                        .sorted((a, b) ->
                                Long.compare(
                                        b.getValue(),
                                        a.getValue()
                                )
                        )
                        .limit(10)
                        .toList();

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put(
                "periodStart",
                period.start().toString()
        );
        payload.put(
                "periodEnd",
                period.end().toString()
        );
        payload.put(
                "granularity",
                period.granularity().name()
        );
        payload.put("currency", currency);
        payload.put("totalSpentCents", totalSpentCents);
        payload.put("totalIncomeCents", totalIncomeCents);
        payload.put(
                "topCategories",
                topCategories.stream()
                        .filter(e -> e.getKey() != null)
                        .map(e -> Map.of(
                                "categoryId",
                                e.getKey().toString(),
                                "spentCents",
                                e.getValue()
                        ))
                        .toList()
        );
        payload.put("budgetsCount", budgets.size());
        return payload;
    }

    // Gera a chave usada para controlar a idempotência da operação.
    private static String buildIdempotencyKey(
            UserId userId,
            Period period,
            String rawKey
    ) {
        String base = rawKey == null || rawKey.trim().isEmpty()
                ? userId.value()
                + "|"
                + period.start()
                + "|"
                + period.end()
                + "|"
                + period.granularity()
                : rawKey.trim();
        return IDEM_PREFIX + base;
    }

    private static int normalizeTtlSeconds(Integer ttlSeconds) {
        if (ttlSeconds == null || ttlSeconds <= 0) {
            return 300;
        }
        return ttlSeconds;
    }

    private static int normalizeMaxTransactions(Integer max) {
        if (max == null || max <= 0) {
            return DEFAULT_MAX_TX;
        }
        return max;
    }

    private static int normalizeMinScore(Integer minScore) {
        if (minScore == null) {
            return 50;
        }
        if (minScore < 0) {
            return 0;
        }
        if (minScore > 100) {
            return 100;
        }
        return minScore;
    }

    private static <T> List<T> safeList(List<T> v) {
        return v == null ? List.of() : v;
    }

    private static String resolveCurrency(
            List<CategorizedTxView> txs
    ) {
        return txs.stream()
                .filter(Objects::nonNull)
                .map(CategorizedTxView::currency)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .findFirst()
                .orElse(DEFAULT_CURRENCY);
    }

    private static Map<UUID, Long> buildSpentByCategory(
            List<CategorizedTxView> txs
    ) {
        Map<UUID, Long> byCat = new HashMap<>();
        for (var t : txs) {
            if (t == null) {
                continue;
            }
            if (t.income()) {
                continue;
            }
            byCat.merge(
                    t.categoryId(),
                    t.amountCents(),
                    Long::sum
            );
        }
        return byCat;
    }

    private static int computeScore(
            long totalSpentCents,
            List<LoadBudgetsForUserPort.BudgetView> budgets
    ) {
        if (budgets == null || budgets.isEmpty()) {
            return 30;
        }

        long totalLimit = budgets.stream()
                .filter(Objects::nonNull)
                .mapToLong(
                        LoadBudgetsForUserPort.BudgetView::limitCents
                )
                .sum();

        if (totalLimit <= 0) {
            return 40;
        }

        double ratio = (double) totalSpentCents
                / (double) totalLimit;

        if (ratio >= 1.2) {
            return 95;
        }
        if (ratio >= 1.0) {
            return 85;
        }
        if (ratio >= 0.8) {
            return 70;
        }
        return 55;
    }
}
