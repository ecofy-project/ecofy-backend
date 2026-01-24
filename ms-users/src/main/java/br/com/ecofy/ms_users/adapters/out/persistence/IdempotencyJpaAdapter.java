package br.com.ecofy.ms_users.adapters.out.persistence;

import br.com.ecofy.ms_users.adapters.out.persistence.entity.IdempotencyKeyEntity;
import br.com.ecofy.ms_users.adapters.out.persistence.repository.IdempotencyRepository;
import br.com.ecofy.ms_users.core.port.out.IdempotencyPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Component
public class IdempotencyJpaAdapter implements IdempotencyPort {

    private static final Duration DEFAULT_TTL = Duration.ofHours(24);

    private final IdempotencyRepository repo;

    // Inicializa o adapter JPA de idempotência com o repositório responsável por persistir as chaves.
    public IdempotencyJpaAdapter(IdempotencyRepository repo) {
        this.repo = Objects.requireNonNull(repo, "repo must not be null");
    }

    // Registra uma chave de idempotência uma única vez para uma operação, retornando true se registrou e false se já existia.
    @Override
    public boolean registerOnce(String operation, String key, String requestHash, Duration ttl) {
        String op = normalizeRequired(operation, "operation");
        String k = normalizeRequired(key, "key");

        Duration effectiveTtl = (ttl == null || ttl.isNegative() || ttl.isZero()) ? DEFAULT_TTL : ttl;
        String hash = normalizeOptional(requestHash);

        log.debug(
                "[IdempotencyJpaAdapter] - [registerOnce] -> operation={} keyLen={} hasRequestHash={} ttlSeconds={}",
                op,
                k.length(),
                hash != null,
                effectiveTtl.toSeconds()
        );

        Optional<IdempotencyKeyEntity> existing = repo.findByOperationAndKey(op, k);
        if (existing.isPresent()) {
            boolean sameHash = Objects.equals(
                    normalizeOptional(existing.get().getRequestHash()),
                    hash
            );

            log.info(
                    "[IdempotencyJpaAdapter] - [registerOnce] -> alreadyRegistered operation={} keyLen={} sameRequestHash={}",
                    op,
                    k.length(),
                    sameHash
            );

            // Política atual: se já existe, não registra novamente.
            // Observação: "sameHash" pode ser usado para diferenciar "retry idempotente" vs "conflito".
            return false;
        }

        Instant now = Instant.now();

        var e = new IdempotencyKeyEntity();
        e.setId(UUID.randomUUID());
        e.setOperation(op);
        e.setKey(k);
        e.setRequestHash(hash);
        e.setCreatedAt(now);
        e.setExpiresAt(now.plus(effectiveTtl));

        repo.save(e);

        log.info(
                "[IdempotencyJpaAdapter] - [registerOnce] -> registered operation={} keyLen={} expiresAt={}",
                op,
                k.length(),
                e.getExpiresAt()
        );

        return true;
    }

    // Normaliza e valida um campo obrigatório (não nulo/não em branco), retornando o valor trimado.
    private static String normalizeRequired(String v, String field) {
        if (v == null || v.isBlank()) {
            throw new IllegalArgumentException(field + " must not be null/blank");
        }
        return v.trim();
    }

    // Normaliza um campo opcional, retornando null quando ausente/vazio e trimando quando presente.
    private static String normalizeOptional(String v) {
        if (v == null || v.isBlank()) return null;
        return v.trim();
    }

}
