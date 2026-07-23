package br.com.ecofy.ms_insights.adapters.out.cache;

import br.com.ecofy.ms_insights.config.CacheConfig;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.UUID;

// Invalida caches do usuário para manter a consistência do dashboard.
@Slf4j
@Component
public class CacheInvalidator {

    private static final String CACHE_EVICTIONS_METRIC =
            "ecofy.insights.cache.evictions.total";

    private final CacheManager cacheManager;
    private final MeterRegistry meterRegistry;

    public CacheInvalidator(CacheManager cacheManager, MeterRegistry meterRegistry) {
        this.cacheManager = Objects.requireNonNull(
                cacheManager,
                "cacheManager must not be null"
        );
        this.meterRegistry = Objects.requireNonNull(
                meterRegistry,
                "meterRegistry must not be null"
        );
    }

    public void evictUser(UUID userId) {
        if (userId == null) {
            return;
        }

        evict(CacheConfig.CACHE_DASHBOARD, userId);
        evict(CacheConfig.CACHE_METRICS, userId);
    }

    // Invalida a entrada e registra a operação nas métricas do serviço.
    private void evict(String cacheName, UUID userId) {
        Cache cache = cacheManager.getCache(cacheName);

        if (cache == null) {
            log.debug(
                    "[CacheInvalidator] - [evict] -> Cache não encontrado cache={} userId={}",
                    cacheName,
                    userId
            );
            return;
        }

        try {
            cache.evict(userId);

            meterRegistry.counter(
                    CACHE_EVICTIONS_METRIC,
                    "cache",
                    cacheName
            ).increment();

            log.debug(
                    "[CacheInvalidator] - [evict] -> Cache invalidado cache={} userId={}",
                    cacheName,
                    userId
            );
        } catch (RuntimeException exception) {
            log.warn(
                    "[CacheInvalidator] - [evict] -> Falha ao invalidar cache={} userId={} error={}",
                    cacheName,
                    userId,
                    exception.getMessage(),
                    exception
            );
        }
    }
}