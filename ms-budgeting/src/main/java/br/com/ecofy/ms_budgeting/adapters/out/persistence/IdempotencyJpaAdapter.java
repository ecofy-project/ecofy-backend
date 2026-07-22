package br.com.ecofy.ms_budgeting.adapters.out.persistence;

import br.com.ecofy.ms_budgeting.adapters.out.persistence.entity.IdempotencyKeyEntity;
import br.com.ecofy.ms_budgeting.adapters.out.persistence.repository.IdempotencyRepository;
import br.com.ecofy.ms_budgeting.core.port.out.IdempotencyPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

@Slf4j
@Component
// Centraliza o controle persistente de idempotência com expiração.
public class IdempotencyJpaAdapter implements IdempotencyPort {

    private final IdempotencyRepository repo;
    private final Clock clock;

    public IdempotencyJpaAdapter(IdempotencyRepository repo, Clock clock) {
        this.repo = Objects.requireNonNull(repo, "repo must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    // Registra uma chave temporária para impedir o processamento duplicado.
    @Override
    @Transactional
    public boolean tryAcquire(String key, Duration ttl, String scope) {
        String k = requireNonBlank(key, "key");
        String sc = requireNonBlank(scope, "scope");
        Objects.requireNonNull(ttl, "ttl must not be null");
        if (ttl.isZero() || ttl.isNegative()) {
            throw new IllegalArgumentException("ttl must be positive");
        }

        Instant now = Instant.now(clock);

        try {
            repo.save(IdempotencyKeyEntity.builder()
                    .key(k)
                    .scope(sc)
                    .createdAt(now)
                    .expiresAt(now.plus(ttl))
                    .build());

            log.debug("[IdempotencyJpaAdapter] - [tryAcquire] -> Chave de idempotência adquirida key={} scope={} ttl={}s", k, sc, ttl.toSeconds());
            return true;

        } catch (DataIntegrityViolationException ex) {
            boolean reacquired = tryReacquireIfExpired(k, sc, ttl, now);

            if (!reacquired) {
                log.debug("[IdempotencyJpaAdapter] - [tryAcquire] -> REJECTED key={} scope={}", k, sc);
            }

            return reacquired;
        }
    }

    // Renova a chave quando o registro existente já expirou.
    private boolean tryReacquireIfExpired(String key, String scope, Duration ttl, Instant now) {
        return repo.findByKey(key)
                .filter(existing -> existing.getExpiresAt() != null && existing.getExpiresAt().isBefore(now))
                .map(existing -> {
                    repo.deleteById(existing.getId());

                    try {
                        repo.save(IdempotencyKeyEntity.builder()
                                .key(key)
                                .scope(scope)
                                .createdAt(now)
                                .expiresAt(now.plus(ttl))
                                .build());

                        log.debug("[IdempotencyJpaAdapter] - [tryAcquire] -> REACQUIRED key={} scope={}", key, scope);
                        return true;

                    } catch (DataIntegrityViolationException ignoreRace) {
                        return false;
                    }
                })
                .orElse(false);
    }

    private static String requireNonBlank(String v, String field) {
        if (v == null || v.trim().isEmpty()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return v.trim();
    }
}
