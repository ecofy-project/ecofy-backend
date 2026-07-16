package br.com.ecofy.ms_users.adapters.out.persistence;

import br.com.ecofy.ms_users.adapters.out.persistence.entity.IdempotencyKeyEntity;
import br.com.ecofy.ms_users.adapters.out.persistence.repository.IdempotencyRepository;
import br.com.ecofy.ms_users.core.port.out.IdempotencyOutcome;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IdempotencyJpaAdapterTest {

    @Mock
    private IdempotencyRepository repo;

    private final Duration ttl = Duration.ofHours(1);

    private IdempotencyJpaAdapter adapter() {
        return new IdempotencyJpaAdapter(repo);
    }

    private IdempotencyKeyEntity existing(String hash) {
        var e = new IdempotencyKeyEntity();
        e.setId(UUID.randomUUID());
        e.setOperation("op");
        e.setKey("k");
        e.setRequestHash(hash);
        e.setCreatedAt(Instant.now());
        e.setExpiresAt(Instant.now().plusSeconds(3600));
        return e;
    }

    @Test
    void shouldReturnRegistered_whenKeyIsNew() {
        var adapter = adapter();
        when(repo.findByOperationAndKey("op", "k")).thenReturn(Optional.empty());
        when(repo.saveAndFlush(any(IdempotencyKeyEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        assertEquals(IdempotencyOutcome.REGISTERED, adapter.registerOnce("op", "k", "hash-1", ttl));
        verify(repo).saveAndFlush(any(IdempotencyKeyEntity.class));
    }

    @Test
    void shouldReturnDuplicate_whenSameKeyAndSameHash() {
        var adapter = adapter();
        when(repo.findByOperationAndKey("op", "k")).thenReturn(Optional.of(existing("hash-1")));

        assertEquals(IdempotencyOutcome.DUPLICATE, adapter.registerOnce("op", "k", "hash-1", ttl));
        verify(repo, never()).saveAndFlush(any());
    }

    @Test
    void shouldReturnConflict_whenSameKeyButDifferentHash() {
        var adapter = adapter();
        when(repo.findByOperationAndKey("op", "k")).thenReturn(Optional.of(existing("hash-OTHER")));

        assertEquals(IdempotencyOutcome.CONFLICT, adapter.registerOnce("op", "k", "hash-1", ttl));
        verify(repo, never()).saveAndFlush(any());
    }

    @Test
    void shouldReclassifyAsDuplicate_whenConcurrentInsertHitsUniqueConstraint() {
        var adapter = adapter();
        // Primeira checagem: ausente. Após a violação: presente com o MESMO hash (retry concorrente).
        when(repo.findByOperationAndKey("op", "k"))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(existing("hash-1")));
        when(repo.saveAndFlush(any(IdempotencyKeyEntity.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate key"));

        assertEquals(IdempotencyOutcome.DUPLICATE, adapter.registerOnce("op", "k", "hash-1", ttl));
    }

    @Test
    void shouldReclassifyAsConflict_whenConcurrentInsertHitsUniqueConstraint_withDifferentHash() {
        var adapter = adapter();
        when(repo.findByOperationAndKey("op", "k"))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(existing("hash-OTHER")));
        when(repo.saveAndFlush(any(IdempotencyKeyEntity.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate key"));

        assertEquals(IdempotencyOutcome.CONFLICT, adapter.registerOnce("op", "k", "hash-1", ttl));
    }
}
