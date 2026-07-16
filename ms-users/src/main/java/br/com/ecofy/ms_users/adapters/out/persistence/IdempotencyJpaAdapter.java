package br.com.ecofy.ms_users.adapters.out.persistence;

import br.com.ecofy.ms_users.adapters.out.persistence.entity.IdempotencyKeyEntity;
import br.com.ecofy.ms_users.adapters.out.persistence.repository.IdempotencyRepository;
import br.com.ecofy.ms_users.core.port.out.IdempotencyOutcome;
import br.com.ecofy.ms_users.core.port.out.IdempotencyPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

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

    // Registra (ou reconhece) uma chave de idempotência de forma atômica, diferenciando retry legítimo de conflito real.
    @Override
    @Transactional
    public IdempotencyOutcome registerOnce(String operation, String key, String requestHash, Duration ttl) {
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

        // 1) Já existe? -> retry legítimo (mesmo hash) ou conflito (hash diferente).
        Optional<IdempotencyKeyEntity> existing = repo.findByOperationAndKey(op, k);
        if (existing.isPresent()) {
            return classifyExisting(op, k, existing.get(), hash);
        }

        // 2) Não existe -> tenta INSERT atômico. Em concorrência, a unique constraint
        //    (operation, idem_key) protege contra corrida: capturamos a violação e reclassificamos.
        Instant now = Instant.now();
        var e = new IdempotencyKeyEntity();
        e.setId(UUID.randomUUID());
        e.setOperation(op);
        e.setKey(k);
        e.setRequestHash(hash);
        e.setCreatedAt(now);
        e.setExpiresAt(now.plus(effectiveTtl));

        try {
            repo.saveAndFlush(e);

            log.info(
                    "[IdempotencyJpaAdapter] - [registerOnce] -> registered operation={} keyLen={} expiresAt={}",
                    op,
                    k.length(),
                    e.getExpiresAt()
            );
            return IdempotencyOutcome.REGISTERED;

        } catch (DataIntegrityViolationException ex) {
            // Corrida: outra transação inseriu a mesma (operation, key) concorrentemente.
            log.info(
                    "[IdempotencyJpaAdapter] - [registerOnce] -> concurrent insert detected, reclassifying operation={} keyLen={}",
                    op,
                    k.length()
            );

            IdempotencyKeyEntity now2 = repo.findByOperationAndKey(op, k).orElse(null);
            if (now2 == null) {
                // Situação inesperada (violação sem linha visível) -> propaga.
                throw ex;
            }
            return classifyExisting(op, k, now2, hash);
        }
    }

    // Classifica uma chave já existente como DUPLICATE (mesmo hash = retry) ou CONFLICT (hash diferente).
    private IdempotencyOutcome classifyExisting(String op, String k, IdempotencyKeyEntity existing, String hash) {
        boolean sameHash = Objects.equals(normalizeOptional(existing.getRequestHash()), hash);

        log.info(
                "[IdempotencyJpaAdapter] - [registerOnce] -> alreadyRegistered operation={} keyLen={} sameRequestHash={}",
                op,
                k.length(),
                sameHash
        );

        return sameHash ? IdempotencyOutcome.DUPLICATE : IdempotencyOutcome.CONFLICT;
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
