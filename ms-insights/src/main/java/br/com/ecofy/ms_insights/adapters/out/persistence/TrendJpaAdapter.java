package br.com.ecofy.ms_insights.adapters.out.persistence;

import br.com.ecofy.ms_insights.adapters.out.persistence.mapper.TrendMapper;
import br.com.ecofy.ms_insights.adapters.out.persistence.repository.TrendRepository;
import br.com.ecofy.ms_insights.core.domain.Trend;
import br.com.ecofy.ms_insights.core.port.out.SaveTrendPort;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

// Persiste tendências e registra o resultado das operações de armazenamento.
@Slf4j
@Component
public class TrendJpaAdapter implements SaveTrendPort {

    private final TrendRepository repository;
    private final ObjectMapper objectMapper;

    public TrendJpaAdapter(
            TrendRepository repository,
            ObjectMapper objectMapper
    ) {
        this.repository = Objects.requireNonNull(
                repository,
                "repository must not be null"
        );
        this.objectMapper = Objects.requireNonNull(
                objectMapper,
                "objectMapper must not be null"
        );
    }

    // Persiste a tendência e registra o tempo e o resultado da operação.
    @Override
    @Transactional
    public Trend save(Trend trend) {
        Objects.requireNonNull(trend, "trend must not be null");

        Instant startedAt = Instant.now();

        try {
            var entity = TrendMapper.toEntity(trend, objectMapper);
            var saved = repository.save(entity);

            log.debug(
                    "[TrendJpaAdapter] - [save] -> OK trendId={} userId={} type={} start={} end={} granularity={} points={} currency={} elapsedMs={}",
                    saved.getId(),
                    saved.getUserId(),
                    saved.getTrendType(),
                    saved.getPeriodStart(),
                    saved.getPeriodEnd(),
                    saved.getGranularity(),
                    safeSeriesSize(trend),
                    saved.getCurrency(),
                    elapsedMs(startedAt)
            );

            return TrendMapper.toDomain(saved, objectMapper);

        } catch (DataAccessException ex) {
            log.error(
                    "[TrendJpaAdapter] - [save] -> DB_ERROR trendId={} userId={} type={} msg={}",
                    safeTrendId(trend),
                    safeUserId(trend),
                    safeType(trend),
                    ex.getMessage(),
                    ex
            );
            throw ex;

        } catch (RuntimeException ex) {
            log.error(
                    "[TrendJpaAdapter] - [save] -> ERROR trendId={} userId={} type={} msg={}",
                    safeTrendId(trend),
                    safeUserId(trend),
                    safeType(trend),
                    ex.getMessage(),
                    ex
            );
            throw ex;
        }
    }

    // Calcula o tempo decorrido para observabilidade.
    private static long elapsedMs(Instant startedAt) {
        return Duration.between(startedAt, Instant.now()).toMillis();
    }

    // Resolve a quantidade de pontos com fallback seguro.
    private static int safeSeriesSize(Trend t) {
        try {
            return t.getSeriesCents() == null
                    ? 0
                    : t.getSeriesCents().size();
        } catch (Exception ignore) {
            return 0;
        }
    }

    // Resolve o identificador da tendência com fallback seguro.
    private static Object safeTrendId(Trend t) {
        try {
            return t.getId();
        } catch (Exception ignore) {
            return "n/a";
        }
    }

    // Resolve o usuário da tendência com fallback seguro.
    private static Object safeUserId(Trend t) {
        try {
            return t.getUserId().value();
        } catch (Exception ignore) {
            return "n/a";
        }
    }

    // Resolve o tipo da tendência com fallback seguro.
    private static Object safeType(Trend t) {
        try {
            return t.getType();
        } catch (Exception ignore) {
            return "n/a";
        }
    }
}
