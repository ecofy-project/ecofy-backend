package br.com.ecofy.ms_categorization.adapters.out.persistence;

import br.com.ecofy.ms_categorization.adapters.out.persistence.entity.IdempotencyKeyEntity;
import br.com.ecofy.ms_categorization.adapters.out.persistence.repository.IdempotencyRepository;
import br.com.ecofy.ms_categorization.config.CategorizationProperties;
import br.com.ecofy.ms_categorization.core.port.out.IdempotencyPortOut;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

// Centraliza o controle persistente de idempotência com expiração.
@Slf4j
@Component
@RequiredArgsConstructor
public class IdempotencyJpaAdapter implements IdempotencyPortOut {

    private static final long MIN_TTL_SECONDS = 60;

    private final IdempotencyRepository repo;
    private final CategorizationProperties props;
    private final Clock clock = Clock.systemUTC();

    // Registra a chave de forma atômica para impedir o processamento duplicado.
    @Override
    @Transactional
    public boolean tryAcquire(String key, Instant now) {
        Objects.requireNonNull(key, "key must not be null");

        final Instant instant = now != null ? now : Instant.now(clock);
        final long ttl = Math.max(MIN_TTL_SECONDS, props.getIdempotency().getTtlSeconds());
        final Instant expiresAt = instant.plusSeconds(ttl);

        int inserted = repo.tryInsert(key, instant, expiresAt);

        if (inserted == 1) {
            log.debug("[IdempotencyJpaAdapter] - [tryAcquire] -> Chave de idempotência adquirida key={} expiresAt={}", key, expiresAt);
            return true;
        }

        log.info("[IdempotencyJpaAdapter] - [tryAcquire] -> COLLISION key={} (replay ignorado)", key);
        return false;
    }
}
