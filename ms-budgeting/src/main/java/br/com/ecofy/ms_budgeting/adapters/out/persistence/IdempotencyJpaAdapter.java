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
public class IdempotencyJpaAdapter implements IdempotencyPort {

    private final IdempotencyRepository repo;
    private final Clock clock;

    public IdempotencyJpaAdapter(IdempotencyRepository repo, Clock clock) {
        this.repo = Objects.requireNonNull(repo, "repo must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    // Tenta adquirir uma chave de idempotência com TTL e escopo, retornando true se conseguiu bloquear a operação.
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

            log.debug("[IdempotencyJpaAdapter] - [tryAcquire] -> ACQUIRED key={} scope={} ttl={}s", k, sc, ttl.toSeconds());
            return true;

        } catch (DataIntegrityViolationException ex) {
            // Tentativa de recuperação: se existir mas estiver expirada, remove e tenta de novo.
            // Isso evita "chave fantasma" bloqueando operações depois do TTL.
            boolean reacquired = tryReacquireIfExpired(k, sc, ttl, now);

            if (!reacquired) {
                log.debug("[IdempotencyJpaAdapter] - [tryAcquire] -> REJECTED key={} scope={}", k, sc);
            }

            return reacquired;
        }
    }

    // Tenta re-adquirir uma chave existente se ela estiver expirada, removendo-a e recriando com novo TTL.
    // Correção Dia 6: a busca é pela CHAVE textual (idem_key), não por Long.valueOf(key) — a chave é
    // textual (ex.: "kafka:categorized-tx:tx:...", "alert:...") e converter para Long lançava
    // NumberFormatException, quebrando sistematicamente a recuperação de chaves expiradas.
    private boolean tryReacquireIfExpired(String key, String scope, Duration ttl, Instant now) {
        return repo.findByKey(key)
                .filter(existing -> existing.getExpiresAt() != null && existing.getExpiresAt().isBefore(now))
                .map(existing -> {
                    // Remove a linha expirada pelo PK numérico real da entidade encontrada.
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

    // Valida e normaliza uma String obrigatória, lançando exceção se estiver nula ou em branco.
    private static String requireNonBlank(String v, String field) {
        if (v == null || v.trim().isEmpty()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return v.trim();
    }
}
