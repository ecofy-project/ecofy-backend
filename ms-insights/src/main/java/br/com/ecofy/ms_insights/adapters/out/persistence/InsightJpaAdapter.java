package br.com.ecofy.ms_insights.adapters.out.persistence;

import br.com.ecofy.ms_insights.adapters.out.persistence.mapper.InsightMapper;
import br.com.ecofy.ms_insights.adapters.out.persistence.repository.InsightRepository;
import br.com.ecofy.ms_insights.core.domain.Insight;
import br.com.ecofy.ms_insights.core.domain.enums.InsightType;
import br.com.ecofy.ms_insights.core.domain.valueobject.Period;
import br.com.ecofy.ms_insights.core.port.out.LoadInsightsPort;
import br.com.ecofy.ms_insights.core.port.out.SaveInsightPort;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Component
public class InsightJpaAdapter implements SaveInsightPort, LoadInsightsPort {

    private static final int LIMIT_20 = 20;
    private static final int LIMIT_50 = 50;

    private final InsightRepository repository;
    private final ObjectMapper objectMapper;

    // Injeta o repositório JPA e o ObjectMapper usados para persistir e (de)serializar o payload de Insight.
    public InsightJpaAdapter(InsightRepository repository, ObjectMapper objectMapper) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
    }

    // Persiste um Insight no banco (domain -> entity -> save) com observabilidade (tempo) e logs de erro diferenciando falhas de acesso a dados.
    @Override
    @Transactional
    public Insight save(Insight insight) {
        Objects.requireNonNull(insight, "insight must not be null");

        Instant startedAt = Instant.now();

        try {
            var entity = InsightMapper.toEntity(insight, objectMapper);
            var saved = repository.save(entity);

            log.debug("[InsightJpaAdapter] - [save] -> OK insightId={} userId={} type={} score={} elapsedMs={}",
                    saved.getId(), saved.getUserId(), saved.getType(), saved.getScore(), elapsedMs(startedAt));

            return InsightMapper.toDomain(saved, objectMapper);

        } catch (DataAccessException ex) {
            log.error("[InsightJpaAdapter] - [save] -> DB_ERROR insightId={} userId={} type={} elapsedMs={} msg={}",
                    safeInsightId(insight), safeUserId(insight), safeType(insight), elapsedMs(startedAt), ex.getMessage(), ex);
            throw ex;

        } catch (RuntimeException ex) {
            // inclui erro de serialização no mapper
            log.error("[InsightJpaAdapter] - [save] -> ERROR insightId={} userId={} type={} elapsedMs={} msg={}",
                    safeInsightId(insight), safeUserId(insight), safeType(insight), elapsedMs(startedAt), ex.getMessage(), ex);
            throw ex;
        }
    }

    // Busca insights mais recentes do usuário com limite normalizado (20 ou 50), mapeando entities para domínio e registrando métricas/erros.
    @Override
    @Transactional(readOnly = true)
    public List<Insight> findRecentForUser(UUID userId, int limit) {
        Objects.requireNonNull(userId, "userId must not be null");

        Instant startedAt = Instant.now();
        int normalized = normalizeLimit(limit);

        try {
            var entities = (normalized == LIMIT_20)
                    ? repository.findTop20ByUserIdOrderByCreatedAtDesc(userId)
                    : repository.findTop50ByUserIdOrderByCreatedAtDesc(userId);

            log.debug("[InsightJpaAdapter] - [findRecentForUser] -> OK userId={} requestedLimit={} normalizedLimit={} returned={} elapsedMs={}",
                    userId, limit, normalized, entities.size(), elapsedMs(startedAt));

            return entities.stream().map(e -> InsightMapper.toDomain(e, objectMapper)).toList();

        } catch (DataAccessException ex) {
            log.error("[InsightJpaAdapter] - [findRecentForUser] -> DB_ERROR userId={} requestedLimit={} normalizedLimit={} elapsedMs={} msg={}",
                    userId, limit, normalized, elapsedMs(startedAt), ex.getMessage(), ex);
            throw ex;

        } catch (RuntimeException ex) {
            log.error("[InsightJpaAdapter] - [findRecentForUser] -> ERROR userId={} requestedLimit={} normalizedLimit={} elapsedMs={} msg={}",
                    userId, limit, normalized, elapsedMs(startedAt), ex.getMessage(), ex);
            throw ex;
        }
    }

    // Busca insights por usuário/tipo/período usando estratégia simples (pega últimos 50 e filtra em memória) para reduzir complexidade de query.
    @Override
    @Transactional(readOnly = true)
    public List<Insight> findForUserTypePeriod(UUID userId, InsightType type, Period period) {
        Objects.requireNonNull(userId, "userId must not be null");
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(period, "period must not be null");

        Instant startedAt = Instant.now();

        try {
            // Estratégia simples: buscar últimos 50 e filtrar em memória.
            var entities = repository.findTop50ByUserIdOrderByCreatedAtDesc(userId);

            var filtered = entities.stream()
                    .map(e -> InsightMapper.toDomain(e, objectMapper))
                    .filter(i -> i.getType() == type)
                    .filter(i -> samePeriod(i.getKey().period(), period))
                    .toList();

            log.debug("[InsightJpaAdapter] - [findForUserTypePeriod] -> OK userId={} type={} start={} end={} granularity={} scanned={} returned={} elapsedMs={}",
                    userId, type, period.start(), period.end(), period.granularity(), entities.size(), filtered.size(), elapsedMs(startedAt));

            return filtered;

        } catch (DataAccessException ex) {
            log.error("[InsightJpaAdapter] - [findForUserTypePeriod] -> DB_ERROR userId={} type={} start={} end={} granularity={} elapsedMs={} msg={}",
                    userId, type, period.start(), period.end(), period.granularity(), elapsedMs(startedAt), ex.getMessage(), ex);
            throw ex;

        } catch (RuntimeException ex) {
            log.error("[InsightJpaAdapter] - [findForUserTypePeriod] -> ERROR userId={} type={} start={} end={} granularity={} elapsedMs={} msg={}",
                    userId, type, period.start(), period.end(), period.granularity(), elapsedMs(startedAt), ex.getMessage(), ex);
            throw ex;
        }
    }

    // Normaliza o limite solicitado para um conjunto suportado (20 ou 50), aplicando fallback seguro quando inválido.
    private static int normalizeLimit(int limit) {
        // Contrato simples: 20 ou 50 (com fallback seguro).
        if (limit <= 0) return LIMIT_20;
        if (limit <= LIMIT_20) return LIMIT_20;
        return LIMIT_50;
    }

    // Compara dois períodos por start/end e granularity para garantir equivalência exata no filtro em memória.
    private static boolean samePeriod(Period a, Period b) {
        return a.start().equals(b.start())
                && a.end().equals(b.end())
                && a.granularity() == b.granularity();
    }

    // Calcula o tempo decorrido em milissegundos desde startedAt para métricas de observabilidade.
    private static long elapsedMs(Instant startedAt) {
        return Duration.between(startedAt, Instant.now()).toMillis();
    }

    // Recupera o id do Insight com segurança para logging, evitando exceções caso o objeto esteja inconsistente.
    private static Object safeInsightId(Insight insight) {
        try {
            return insight.getId();
        } catch (Exception ignore) {
            return "n/a";
        }
    }

    // Recupera o userId do Insight com segurança para logging, evitando exceções caso a key esteja ausente/inválida.
    private static Object safeUserId(Insight insight) {
        try {
            return insight.getKey().userId().value();
        } catch (Exception ignore) {
            return "n/a";
        }
    }

    // Recupera o tipo do Insight com segurança para logging, evitando exceções em casos de inconsistência do domínio.
    private static Object safeType(Insight insight) {
        try {
            return insight.getType();
        } catch (Exception ignore) {
            return "n/a";
        }
    }

}
