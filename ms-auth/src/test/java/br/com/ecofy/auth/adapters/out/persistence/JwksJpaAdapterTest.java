package br.com.ecofy.auth.adapters.out.persistence;

import br.com.ecofy.auth.adapters.out.persistence.entity.JwkKeyEntity;
import br.com.ecofy.auth.adapters.out.persistence.mapper.PersistenceMapper;
import br.com.ecofy.auth.adapters.out.persistence.repository.JwkKeyRepository;
import br.com.ecofy.auth.core.domain.JwkKey;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwksJpaAdapterTest {

    @Mock
    private JwkKeyRepository repository;

    @Test
    void constructor_shouldRejectNullRepository() {
        NullPointerException ex = assertThrows(NullPointerException.class, () -> new JwksJpaAdapter(null));
        assertEquals("repository must not be null", ex.getMessage());
    }

    @Test
    void findActiveSigningKeys_shouldReturnEmpty_whenRepositoryReturnsNull() {
        JwksJpaAdapter adapter = new JwksJpaAdapter(repository);

        when(repository.findByActiveTrue()).thenReturn(null);

        List<JwkKey> result = adapter.findActiveSigningKeys();

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(repository).findByActiveTrue();
        verifyNoMoreInteractions(repository);
    }

    @Test
    void findActiveSigningKeys_shouldReturnEmpty_whenRepositoryReturnsEmptyList() {
        JwksJpaAdapter adapter = new JwksJpaAdapter(repository);

        when(repository.findByActiveTrue()).thenReturn(List.of());

        List<JwkKey> result = adapter.findActiveSigningKeys();

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(repository).findByActiveTrue();
        verifyNoMoreInteractions(repository);
    }

    @Test
    void findActiveSigningKeys_shouldMapEntitiesToDomain_andReturnUnmodifiableCopy() {
        JwksJpaAdapter adapter = new JwksJpaAdapter(repository);

        JwkKeyEntity e1 = new JwkKeyEntity();
        JwkKeyEntity e2 = new JwkKeyEntity();
        List<JwkKeyEntity> entities = new ArrayList<>(List.of(e1, e2));

        when(repository.findByActiveTrue()).thenReturn(entities);

        JwkKey k1 = mock(JwkKey.class);
        JwkKey k2 = mock(JwkKey.class);

        try (MockedStatic<PersistenceMapper> mocked = mockStatic(PersistenceMapper.class)) {
            mocked.when(() -> PersistenceMapper.toDomain(same(e1))).thenReturn(k1);
            mocked.when(() -> PersistenceMapper.toDomain(same(e2))).thenReturn(k2);

            List<JwkKey> result = adapter.findActiveSigningKeys();

            assertEquals(2, result.size());
            assertSame(k1, result.get(0));
            assertSame(k2, result.get(1));
            assertThrows(UnsupportedOperationException.class, () -> result.add(mock(JwkKey.class)));
        }

        verify(repository).findByActiveTrue();
        verifyNoMoreInteractions(repository);
    }
}