package br.com.ecofy.ms_categorization.adapters.out.persistence;

import br.com.ecofy.ms_categorization.adapters.out.persistence.entity.IdempotencyKeyEntity;
import br.com.ecofy.ms_categorization.adapters.out.persistence.repository.IdempotencyRepository;
import br.com.ecofy.ms_categorization.config.CategorizationProperties;
import br.com.ecofy.ms_categorization.core.port.out.IdempotencyPortOut;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

@Slf4j
@Component
@RequiredArgsConstructor
public class IdempotencyJpaAdapter implements IdempotencyPortOut {

    private static final long MIN_TTL_SECONDS = 60;

    private final IdempotencyRepository repo;
    private final CategorizationProperties props;
    private final Clock clock = Clock.systemUTC();

    // Tenta adquirir uma chave de idempotência persistindo-a com TTL, retornando false em caso de colisão.
    @Override
    @Transactional
    public boolean tryAcquire(String key, Instant now) {
        Objects.requireNonNull(key, "key must not be null");

        final Instant instant = now != null ? now : Instant.now(clock);
        final long ttl = Math.max(MIN_TTL_SECONDS, props.getIdempotency().getTtlSeconds());

        IdempotencyKeyEntity entity = IdempotencyKeyEntity.builder()
                .idempotencyKey(key)
                .createdAt(instant)
                .expiresAt(instant.plusSeconds(ttl))
                .build();

        try {
            repo.save(entity);

            log.debug(
                    "[IdempotencyJpaAdapter] - [tryAcquire] -> ACQUIRED key={} expiresAt={}",
                    key, entity.getExpiresAt()
            );

            return true;
        } catch (DataIntegrityViolationException ex) {
            log.info(
                    "[IdempotencyJpaAdapter] - [tryAcquire] -> COLLISION key={}",
                    key
            );
            return false;
        }
    }

}
