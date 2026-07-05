package br.com.ecofy.ms_budgeting.adapters.out.persistence;

import br.com.ecofy.ms_budgeting.adapters.out.persistence.entity.IdempotencyKeyEntity;
import br.com.ecofy.ms_budgeting.adapters.out.persistence.repository.IdempotencyRepository;
import br.com.ecofy.ms_budgeting.core.port.out.IdempotencyPort;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class IdempotencyJpaAdapterTest {

    private static final String KEY = "123";
    private static final Long KEY_ID = 123L;
    private static final String SCOPE = "budget:create";
    private static final Duration TTL = Duration.ofMinutes(10);

    private static final Instant NOW =
            Instant.parse("2026-06-25T10:30:00Z");

    private static final Clock FIXED_CLOCK =
            Clock.fixed(NOW, ZoneOffset.UTC);

    @Test
    void shouldCreateAdapterWithRepositoryAndClock() {
        IdempotencyRepository repo = mock(IdempotencyRepository.class);

        IdempotencyJpaAdapter adapter =
                new IdempotencyJpaAdapter(repo, FIXED_CLOCK);

        assertNotNull(adapter);
        assertInstanceOf(IdempotencyPort.class, adapter);
    }

    @Test
    void shouldThrowNullPointerExceptionWhenRepositoryIsNull() {
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> new IdempotencyJpaAdapter(null, FIXED_CLOCK)
        );

        assertEquals("repo must not be null", exception.getMessage());
    }

    @Test
    void shouldThrowNullPointerExceptionWhenClockIsNull() {
        IdempotencyRepository repo = mock(IdempotencyRepository.class);

        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> new IdempotencyJpaAdapter(repo, null)
        );

        assertEquals("clock must not be null", exception.getMessage());
    }

    @Test
    void shouldAcquireIdempotencyKeySuccessfully() {
        IdempotencyRepository repo = mock(IdempotencyRepository.class);
        IdempotencyJpaAdapter adapter =
                new IdempotencyJpaAdapter(repo, FIXED_CLOCK);

        when(repo.save(any(IdempotencyKeyEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        boolean result = adapter.tryAcquire(
                "  " + KEY + "  ",
                TTL,
                "  " + SCOPE + "  "
        );

        assertTrue(result);

        ArgumentCaptor<IdempotencyKeyEntity> captor =
                ArgumentCaptor.forClass(IdempotencyKeyEntity.class);

        verify(repo).save(captor.capture());
        verifyNoMoreInteractions(repo);

        IdempotencyKeyEntity entity = captor.getValue();

        assertNotNull(entity);
        assertEquals(KEY, entity.getKey());
        assertEquals(SCOPE, entity.getScope());
        assertEquals(NOW, entity.getCreatedAt());
        assertEquals(NOW.plus(TTL), entity.getExpiresAt());
    }

    @Test
    void shouldThrowIllegalArgumentExceptionWhenKeyIsNull() {
        IdempotencyRepository repo = mock(IdempotencyRepository.class);
        IdempotencyJpaAdapter adapter =
                new IdempotencyJpaAdapter(repo, FIXED_CLOCK);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> adapter.tryAcquire(null, TTL, SCOPE)
        );

        assertEquals("key must not be blank", exception.getMessage());

        verifyNoInteractions(repo);
    }

    @Test
    void shouldThrowIllegalArgumentExceptionWhenKeyIsBlank() {
        IdempotencyRepository repo = mock(IdempotencyRepository.class);
        IdempotencyJpaAdapter adapter =
                new IdempotencyJpaAdapter(repo, FIXED_CLOCK);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> adapter.tryAcquire("   ", TTL, SCOPE)
        );

        assertEquals("key must not be blank", exception.getMessage());

        verifyNoInteractions(repo);
    }

    @Test
    void shouldThrowIllegalArgumentExceptionWhenScopeIsNull() {
        IdempotencyRepository repo = mock(IdempotencyRepository.class);
        IdempotencyJpaAdapter adapter =
                new IdempotencyJpaAdapter(repo, FIXED_CLOCK);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> adapter.tryAcquire(KEY, TTL, null)
        );

        assertEquals("scope must not be blank", exception.getMessage());

        verifyNoInteractions(repo);
    }

    @Test
    void shouldThrowIllegalArgumentExceptionWhenScopeIsBlank() {
        IdempotencyRepository repo = mock(IdempotencyRepository.class);
        IdempotencyJpaAdapter adapter =
                new IdempotencyJpaAdapter(repo, FIXED_CLOCK);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> adapter.tryAcquire(KEY, TTL, "   ")
        );

        assertEquals("scope must not be blank", exception.getMessage());

        verifyNoInteractions(repo);
    }

    @Test
    void shouldThrowNullPointerExceptionWhenTtlIsNull() {
        IdempotencyRepository repo = mock(IdempotencyRepository.class);
        IdempotencyJpaAdapter adapter =
                new IdempotencyJpaAdapter(repo, FIXED_CLOCK);

        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> adapter.tryAcquire(KEY, null, SCOPE)
        );

        assertEquals("ttl must not be null", exception.getMessage());

        verifyNoInteractions(repo);
    }

    @Test
    void shouldThrowIllegalArgumentExceptionWhenTtlIsZero() {
        IdempotencyRepository repo = mock(IdempotencyRepository.class);
        IdempotencyJpaAdapter adapter =
                new IdempotencyJpaAdapter(repo, FIXED_CLOCK);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> adapter.tryAcquire(KEY, Duration.ZERO, SCOPE)
        );

        assertEquals("ttl must be positive", exception.getMessage());

        verifyNoInteractions(repo);
    }

    @Test
    void shouldThrowIllegalArgumentExceptionWhenTtlIsNegative() {
        IdempotencyRepository repo = mock(IdempotencyRepository.class);
        IdempotencyJpaAdapter adapter =
                new IdempotencyJpaAdapter(repo, FIXED_CLOCK);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> adapter.tryAcquire(KEY, Duration.ofSeconds(-1), SCOPE)
        );

        assertEquals("ttl must be positive", exception.getMessage());

        verifyNoInteractions(repo);
    }

    @Test
    void shouldReturnFalseWhenKeyAlreadyExistsAndIsNotExpired() {
        IdempotencyRepository repo = mock(IdempotencyRepository.class);
        IdempotencyJpaAdapter adapter =
                new IdempotencyJpaAdapter(repo, FIXED_CLOCK);

        IdempotencyKeyEntity existing = entity(
                KEY,
                SCOPE,
                NOW.minusSeconds(60),
                NOW.plusSeconds(60)
        );

        when(repo.save(any(IdempotencyKeyEntity.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate key"));

        when(repo.findById(KEY_ID))
                .thenReturn(Optional.of(existing));

        boolean result = adapter.tryAcquire(KEY, TTL, SCOPE);

        assertFalse(result);

        verify(repo).save(any(IdempotencyKeyEntity.class));
        verify(repo).findById(KEY_ID);
        verify(repo, never()).deleteById(any(Long.class));
        verifyNoMoreInteractions(repo);
    }

    @Test
    void shouldReturnFalseWhenKeyAlreadyExistsAndExpiresAtIsNull() {
        IdempotencyRepository repo = mock(IdempotencyRepository.class);
        IdempotencyJpaAdapter adapter =
                new IdempotencyJpaAdapter(repo, FIXED_CLOCK);

        IdempotencyKeyEntity existing = entity(
                KEY,
                SCOPE,
                NOW.minusSeconds(60),
                null
        );

        when(repo.save(any(IdempotencyKeyEntity.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate key"));

        when(repo.findById(KEY_ID))
                .thenReturn(Optional.of(existing));

        boolean result = adapter.tryAcquire(KEY, TTL, SCOPE);

        assertFalse(result);

        verify(repo).save(any(IdempotencyKeyEntity.class));
        verify(repo).findById(KEY_ID);
        verify(repo, never()).deleteById(any(Long.class));
        verifyNoMoreInteractions(repo);
    }

    @Test
    void shouldReturnFalseWhenKeyAlreadyExistsButRepositoryFindReturnsEmpty() {
        IdempotencyRepository repo = mock(IdempotencyRepository.class);
        IdempotencyJpaAdapter adapter =
                new IdempotencyJpaAdapter(repo, FIXED_CLOCK);

        when(repo.save(any(IdempotencyKeyEntity.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate key"));

        when(repo.findById(KEY_ID))
                .thenReturn(Optional.empty());

        boolean result = adapter.tryAcquire(KEY, TTL, SCOPE);

        assertFalse(result);

        verify(repo).save(any(IdempotencyKeyEntity.class));
        verify(repo).findById(KEY_ID);
        verify(repo, never()).deleteById(any(Long.class));
        verifyNoMoreInteractions(repo);
    }

    @Test
    void shouldReacquireWhenExistingKeyIsExpired() {
        IdempotencyRepository repo = mock(IdempotencyRepository.class);
        IdempotencyJpaAdapter adapter =
                new IdempotencyJpaAdapter(repo, FIXED_CLOCK);

        IdempotencyKeyEntity expired = entity(
                KEY,
                SCOPE,
                NOW.minus(TTL.multipliedBy(2)),
                NOW.minusSeconds(1)
        );

        when(repo.save(any(IdempotencyKeyEntity.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate key"))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(repo.findById(KEY_ID))
                .thenReturn(Optional.of(expired));

        boolean result = adapter.tryAcquire(KEY, TTL, SCOPE);

        assertTrue(result);

        ArgumentCaptor<IdempotencyKeyEntity> captor =
                ArgumentCaptor.forClass(IdempotencyKeyEntity.class);

        verify(repo, times(2)).save(captor.capture());
        verify(repo).findById(KEY_ID);
        verify(repo).deleteById(KEY_ID);
        verifyNoMoreInteractions(repo);

        IdempotencyKeyEntity reacquired = captor.getAllValues().get(1);

        assertEquals(KEY, reacquired.getKey());
        assertEquals(SCOPE, reacquired.getScope());
        assertEquals(NOW, reacquired.getCreatedAt());
        assertEquals(NOW.plus(TTL), reacquired.getExpiresAt());
    }

    @Test
    void shouldReturnFalseWhenReacquireSaveFailsByRaceCondition() {
        IdempotencyRepository repo = mock(IdempotencyRepository.class);
        IdempotencyJpaAdapter adapter =
                new IdempotencyJpaAdapter(repo, FIXED_CLOCK);

        IdempotencyKeyEntity expired = entity(
                KEY,
                SCOPE,
                NOW.minus(TTL.multipliedBy(2)),
                NOW.minusSeconds(1)
        );

        when(repo.save(any(IdempotencyKeyEntity.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate key"))
                .thenThrow(new DataIntegrityViolationException("race condition"));

        when(repo.findById(KEY_ID))
                .thenReturn(Optional.of(expired));

        boolean result = adapter.tryAcquire(KEY, TTL, SCOPE);

        assertFalse(result);

        verify(repo, times(2)).save(any(IdempotencyKeyEntity.class));
        verify(repo).findById(KEY_ID);
        verify(repo).deleteById(KEY_ID);
        verifyNoMoreInteractions(repo);
    }

    @Test
    void shouldPropagateRepositoryExceptionWhenInitialSaveFailsWithNonDataIntegrityException() {
        IdempotencyRepository repo = mock(IdempotencyRepository.class);
        IdempotencyJpaAdapter adapter =
                new IdempotencyJpaAdapter(repo, FIXED_CLOCK);

        RuntimeException repositoryException =
                new RuntimeException("database unavailable");

        when(repo.save(any(IdempotencyKeyEntity.class)))
                .thenThrow(repositoryException);

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> adapter.tryAcquire(KEY, TTL, SCOPE)
        );

        assertSame(repositoryException, exception);

        verify(repo).save(any(IdempotencyKeyEntity.class));
        verifyNoMoreInteractions(repo);
    }

    @Test
    void shouldPropagateRepositoryExceptionWhenFindByIdFailsDuringReacquire() {
        IdempotencyRepository repo = mock(IdempotencyRepository.class);
        IdempotencyJpaAdapter adapter =
                new IdempotencyJpaAdapter(repo, FIXED_CLOCK);

        RuntimeException repositoryException =
                new RuntimeException("find failed");

        when(repo.save(any(IdempotencyKeyEntity.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate key"));

        when(repo.findById(KEY_ID))
                .thenThrow(repositoryException);

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> adapter.tryAcquire(KEY, TTL, SCOPE)
        );

        assertSame(repositoryException, exception);

        verify(repo).save(any(IdempotencyKeyEntity.class));
        verify(repo).findById(KEY_ID);
        verifyNoMoreInteractions(repo);
    }

    @Test
    void shouldPropagateNumberFormatExceptionWhenKeyIsNotNumericAndReacquireIsNeeded() {
        IdempotencyRepository repo = mock(IdempotencyRepository.class);
        IdempotencyJpaAdapter adapter =
                new IdempotencyJpaAdapter(repo, FIXED_CLOCK);

        when(repo.save(any(IdempotencyKeyEntity.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate key"));

        NumberFormatException exception = assertThrows(
                NumberFormatException.class,
                () -> adapter.tryAcquire("abc", TTL, SCOPE)
        );

        assertNotNull(exception);

        verify(repo).save(any(IdempotencyKeyEntity.class));
        verifyNoMoreInteractions(repo);
    }

    @Test
    void shouldHaveComponentAnnotation() {
        assertNotNull(IdempotencyJpaAdapter.class.getAnnotation(Component.class));
    }

    @Test
    void shouldHaveTransactionalAnnotationOnTryAcquireMethod() throws Exception {
        Method method = IdempotencyJpaAdapter.class.getDeclaredMethod(
                "tryAcquire",
                String.class,
                Duration.class,
                String.class
        );

        Transactional transactional = method.getAnnotation(Transactional.class);

        assertNotNull(transactional);
        assertFalse(transactional.readOnly());
    }

    @Test
    void shouldInvokePrivateRequireNonBlankWithValidValue() throws Exception {
        Method method = IdempotencyJpaAdapter.class.getDeclaredMethod(
                "requireNonBlank",
                String.class,
                String.class
        );

        method.setAccessible(true);

        String result = (String) method.invoke(null, "  value  ", "field");

        assertEquals("value", result);
    }

    @Test
    void shouldInvokePrivateRequireNonBlankWithNullValue() throws Exception {
        Method method = IdempotencyJpaAdapter.class.getDeclaredMethod(
                "requireNonBlank",
                String.class,
                String.class
        );

        method.setAccessible(true);

        InvocationTargetException exception = assertThrows(
                InvocationTargetException.class,
                () -> method.invoke(null, null, "field")
        );

        assertInstanceOf(IllegalArgumentException.class, exception.getCause());
        assertEquals("field must not be blank", exception.getCause().getMessage());
    }

    @Test
    void shouldInvokePrivateRequireNonBlankWithBlankValue() throws Exception {
        Method method = IdempotencyJpaAdapter.class.getDeclaredMethod(
                "requireNonBlank",
                String.class,
                String.class
        );

        method.setAccessible(true);

        InvocationTargetException exception = assertThrows(
                InvocationTargetException.class,
                () -> method.invoke(null, "   ", "field")
        );

        assertInstanceOf(IllegalArgumentException.class, exception.getCause());
        assertEquals("field must not be blank", exception.getCause().getMessage());
    }

    private static IdempotencyKeyEntity entity(
            String key,
            String scope,
            Instant createdAt,
            Instant expiresAt
    ) {
        return IdempotencyKeyEntity.builder()
                .key(key)
                .scope(scope)
                .createdAt(createdAt)
                .expiresAt(expiresAt)
                .build();
    }
}