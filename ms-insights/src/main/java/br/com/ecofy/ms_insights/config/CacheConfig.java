package br.com.ecofy.ms_insights.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.support.NoOpCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import java.util.HashMap;
import java.util.Map;

// Configura cache distribuído apenas para as leituras de dashboard e métricas, com passthrough quando desligado.
@Configuration
@EnableCaching
public class CacheConfig {

    public static final String CACHE_DASHBOARD = "dashboard";
    public static final String CACHE_METRICS = "metrics";

    @Bean
    @ConditionalOnProperty(name = "ecofy.insights.cache.enabled", havingValue = "true", matchIfMissing = true)
    public CacheManager redisCacheManager(RedisConnectionFactory connectionFactory,
                                          InsightsProperties props, ObjectMapper objectMapper) {
        var serializer = new GenericJackson2JsonRedisSerializer(objectMapper);
        RedisCacheConfiguration base = RedisCacheConfiguration.defaultCacheConfig()
                .disableCachingNullValues()
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(serializer));

        Map<String, RedisCacheConfiguration> perCache = new HashMap<>();
        var cache = props.cache();
        perCache.put(CACHE_DASHBOARD, base.entryTtl(cache.dashboardTtl()));
        perCache.put(CACHE_METRICS, base.entryTtl(cache.metricsTtl()));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(base)
                .withInitialCacheConfigurations(perCache)
                .build();
    }

    @Bean
    @ConditionalOnProperty(name = "ecofy.insights.cache.enabled", havingValue = "false")
    public CacheManager noOpCacheManager() {
        // Passthrough: get() sempre miss, put() no-op. Não requer Redis.
        return new NoOpCacheManager();
    }
}
