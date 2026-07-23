package br.com.ecofy.ms_insights.core.application.service;

import br.com.ecofy.ms_insights.config.CacheConfig;
import br.com.ecofy.ms_insights.core.application.result.GoalResult;
import br.com.ecofy.ms_insights.core.application.result.InsightResult;
import br.com.ecofy.ms_insights.core.application.result.InsightsBundleResult;
import br.com.ecofy.ms_insights.core.port.in.GetDashboardInsightsUseCase;
import br.com.ecofy.ms_insights.core.port.in.ListInsightsUseCase;
import br.com.ecofy.ms_insights.core.port.out.LoadGoalsPort;
import br.com.ecofy.ms_insights.core.port.out.LoadInsightsPort;
import br.com.ecofy.ms_insights.core.port.out.PageResult;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

// Compõe os dados do dashboard e disponibiliza o histórico de insights.
@Slf4j
@Service
public class DashboardInsightsService implements
        GetDashboardInsightsUseCase,
        ListInsightsUseCase {

    private static final int DEFAULT_INSIGHTS_LIMIT = 20;

    private final LoadInsightsPort loadInsightsPort;
    private final LoadGoalsPort loadGoalsPort;
    private final CacheManager cacheManager;
    private final MeterRegistry meterRegistry;

    public DashboardInsightsService(
            LoadInsightsPort loadInsightsPort,
            LoadGoalsPort loadGoalsPort,
            CacheManager cacheManager,
            MeterRegistry meterRegistry
    ) {
        this.loadInsightsPort = Objects.requireNonNull(
                loadInsightsPort,
                "loadInsightsPort must not be null"
        );
        this.loadGoalsPort = Objects.requireNonNull(
                loadGoalsPort,
                "loadGoalsPort must not be null"
        );
        this.cacheManager = Objects.requireNonNull(
                cacheManager,
                "cacheManager must not be null"
        );
        this.meterRegistry = Objects.requireNonNull(
                meterRegistry,
                "meterRegistry must not be null"
        );
    }

    // Compõe o dashboard e utiliza cache para reduzir consultas repetidas.
    @Override
    @Transactional(readOnly = true)
    public InsightsBundleResult getDashboard(UUID userId) {
        Objects.requireNonNull(userId, "userId must not be null");

        Cache cache = cacheManager.getCache(CacheConfig.CACHE_DASHBOARD);
        if (cache != null) {
            InsightsBundleResult cached = cache.get(
                    userId,
                    InsightsBundleResult.class
            );
            if (cached != null) {
                meterRegistry.counter(
                        "ecofy.insights.cache.hit.total",
                        "cache",
                        CacheConfig.CACHE_DASHBOARD
                ).increment();
                return cached;
            }
        }

        meterRegistry.counter(
                "ecofy.insights.cache.miss.total",
                "cache",
                CacheConfig.CACHE_DASHBOARD
        ).increment();

        var insights = loadInsightsPort
                .findRecentForUser(userId, DEFAULT_INSIGHTS_LIMIT)
                .stream()
                .map(DashboardInsightsService::toInsightResult)
                .toList();

        var goals = loadGoalsPort.findByUserId(userId)
                .stream()
                .map(GoalService::toResult)
                .toList();

        InsightsBundleResult result = new InsightsBundleResult(
                insights,
                List.of(),
                goals
        );

        if (cache != null) {
            cache.put(userId, result);
        }
        return result;
    }

    // Lista de forma paginada os insights pertencentes ao usuário.
    @Override
    @Transactional(readOnly = true)
    public PageResult<InsightResult> listByUser(
            UUID userId,
            int page,
            int size
    ) {
        Objects.requireNonNull(userId, "userId must not be null");
        var pageResult = loadInsightsPort.findByUserId(userId, page, size);

        return new PageResult<>(
                pageResult.content()
                        .stream()
                        .map(DashboardInsightsService::toInsightResult)
                        .toList(),
                pageResult.page(),
                pageResult.size(),
                pageResult.totalElements()
        );
    }

    // Converte o insight para o resultado exposto pelo caso de uso.
    private static InsightResult toInsightResult(
            br.com.ecofy.ms_insights.core.domain.Insight i
    ) {
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
}
