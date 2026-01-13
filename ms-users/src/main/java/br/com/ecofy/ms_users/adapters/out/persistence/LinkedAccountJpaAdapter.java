package br.com.ecofy.ms_users.adapters.out.persistence;

import br.com.ecofy.ms_users.adapters.out.persistence.entity.LinkedAccountEntity;
import br.com.ecofy.ms_users.adapters.out.persistence.mapper.LinkedAccountMapper;
import br.com.ecofy.ms_users.adapters.out.persistence.repository.LinkedAccountRepository;
import br.com.ecofy.ms_users.core.domain.LinkedAccount;
import br.com.ecofy.ms_users.core.port.out.LoadLinkedAccountsPort;
import br.com.ecofy.ms_users.core.port.out.SaveLinkedAccountPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Component
public class LinkedAccountJpaAdapter implements SaveLinkedAccountPort, LoadLinkedAccountsPort {

    private final LinkedAccountRepository repo;
    private final LinkedAccountMapper mapper;

    public LinkedAccountJpaAdapter(LinkedAccountRepository repo, LinkedAccountMapper mapper) {
        this.repo = Objects.requireNonNull(repo, "repo must not be null");
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
    }

    @Override
    public LinkedAccount save(LinkedAccount acc) {
        Objects.requireNonNull(acc, "acc must not be null");
        Objects.requireNonNull(acc.getUserId(), "acc.userId must not be null");
        Objects.requireNonNull(acc.getProvider(), "acc.provider must not be null");
        Objects.requireNonNull(acc.getExternalAccountRef(), "acc.externalAccountRef must not be null");

        UUID userId = acc.getUserId().value();

        log.debug(
                "[LinkedAccountJpaAdapter] - [save] -> userId={} provider={} refLen={} active={}",
                userId,
                acc.getProvider(),
                safeLen(acc.getExternalAccountRef()),
                acc.isActive()
        );

        var existing = repo.findByUserIdAndProviderAndExternalAccountRef(
                userId,
                acc.getProvider(),
                acc.getExternalAccountRef()
        );

        boolean isInsert = existing.isEmpty();
        LinkedAccountEntity e = existing.orElseGet(() -> mapper.toEntity(acc));

        // Garantia explícita de chave natural (upsert por userId+provider+ref)
        e.setUserId(userId);
        e.setProvider(acc.getProvider());
        e.setExternalAccountRef(acc.getExternalAccountRef());
        e.setActive(acc.isActive());
        e.setLinkedAt(acc.getLinkedAt() != null ? acc.getLinkedAt() : Instant.now());

        if (e.getId() == null) {
            e.setId(UUID.randomUUID());
        }

        LinkedAccountEntity saved = repo.save(e);

        log.info(
                "[LinkedAccountJpaAdapter] - [save] -> {} linkedAccountId={} userId={} provider={} active={} linkedAt={}",
                isInsert ? "created" : "updated",
                saved.getId(),
                saved.getUserId(),
                saved.getProvider(),
                saved.isActive(),
                saved.getLinkedAt()
        );

        return mapper.toDomain(saved);
    }

    @Override
    public List<LinkedAccount> findByUserId(UUID userId) {
        Objects.requireNonNull(userId, "userId must not be null");

        log.debug("[LinkedAccountJpaAdapter] - [findByUserId] -> userId={}", userId);

        var list = repo.findByUserId(userId).stream()
                .map(mapper::toDomain)
                .toList();

        log.info(
                "[LinkedAccountJpaAdapter] - [findByUserId] -> userId={} resultSize={}",
                userId,
                list.size()
        );

        return list;
    }

    private static int safeLen(String v) {
        return (v == null) ? 0 : v.length();
    }
}
