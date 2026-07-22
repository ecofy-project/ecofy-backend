package br.com.ecofy.auth.adapters.out.persistence;

import br.com.ecofy.auth.adapters.out.persistence.entity.JwkKeyEntity;
import br.com.ecofy.auth.adapters.out.persistence.repository.JwkKeyRepository;
import br.com.ecofy.auth.core.domain.JwkKey;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Testes unitários do adaptador JPA de chaves públicas")
class JwksJpaAdapterTest {

    private static final Instant FIRST_CREATED_AT =
            Instant.parse("2026-07-20T10:00:00Z");
    private static final Instant SECOND_CREATED_AT =
            Instant.parse("2026-07-20T11:00:00Z");

    @Mock
    private JwkKeyRepository repository;

    @Test
    @DisplayName("Deve rejeitar repositório nulo ao construir o adaptador")
    void constructor_repositorioNulo_deveLancarNullPointerException() {
        // Arrange
        JwkKeyRepository nullRepository = null;

        // Act
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> new JwksJpaAdapter(nullRepository)
        );

        // Assert
        assertEquals(
                "repository must not be null",
                exception.getMessage()
        );
    }

    @Test
    @DisplayName("Deve retornar lista vazia quando o repositório retornar nulo")
    void findActiveSigningKeys_repositorioRetornaNulo_deveRetornarListaVazia() {
        // Arrange
        when(repository.findByActiveTrue()).thenReturn(null);

        JwksJpaAdapter adapter = createAdapter();

        // Act
        List<JwkKey> result = adapter.findActiveSigningKeys();

        // Assert
        assertAll(
                () -> assertNotNull(result),
                () -> assertTrue(result.isEmpty()),
                () -> assertThrows(
                        UnsupportedOperationException.class,
                        () -> result.add(createExpectedJwkKey())
                )
        );

        verify(repository).findByActiveTrue();
        verifyNoMoreInteractions(repository);
    }

    @Test
    @DisplayName("Deve retornar lista vazia quando não existirem chaves ativas")
    void findActiveSigningKeys_repositorioRetornaListaVazia_deveRetornarListaVazia() {
        // Arrange
        when(repository.findByActiveTrue()).thenReturn(List.of());

        JwksJpaAdapter adapter = createAdapter();

        // Act
        List<JwkKey> result = adapter.findActiveSigningKeys();

        // Assert
        assertAll(
                () -> assertNotNull(result),
                () -> assertTrue(result.isEmpty()),
                () -> assertThrows(
                        UnsupportedOperationException.class,
                        () -> result.add(createExpectedJwkKey())
                )
        );

        verify(repository).findByActiveTrue();
        verifyNoMoreInteractions(repository);
    }

    @Test
    @DisplayName("Deve converter e retornar uma chave pública ativa")
    void findActiveSigningKeys_umaChaveAtiva_deveRetornarChaveMapeada() {
        // Arrange
        JwkKeyEntity entity = createJwkKeyEntity(
                "active-key-1",
                "public-key-pem-1",
                "RS256",
                "sig",
                FIRST_CREATED_AT,
                true
        );

        when(repository.findByActiveTrue())
                .thenReturn(List.of(entity));

        JwksJpaAdapter adapter = createAdapter();

        // Act
        List<JwkKey> result = adapter.findActiveSigningKeys();

        // Assert
        assertEquals(1, result.size());

        JwkKey key = result.getFirst();

        assertAll(
                () -> assertEquals("active-key-1", key.keyId()),
                () -> assertEquals(
                        "public-key-pem-1",
                        key.publicKeyPem()
                ),
                () -> assertEquals("RS256", key.algorithm()),
                () -> assertEquals("sig", key.use()),
                () -> assertEquals(
                        FIRST_CREATED_AT,
                        key.createdAt()
                ),
                () -> assertTrue(key.active()),
                () -> assertThrows(
                        UnsupportedOperationException.class,
                        () -> result.add(createExpectedJwkKey())
                )
        );

        verify(repository).findByActiveTrue();
    }

    @Test
    @DisplayName("Deve converter todas as chaves públicas ativas preservando a ordem")
    void findActiveSigningKeys_multiplasChavesAtivas_deveRetornarTodasMapeadas() {
        // Arrange
        JwkKeyEntity firstEntity = createJwkKeyEntity(
                "active-key-1",
                "public-key-pem-1",
                "RS256",
                "sig",
                FIRST_CREATED_AT,
                true
        );
        JwkKeyEntity secondEntity = createJwkKeyEntity(
                "active-key-2",
                "public-key-pem-2",
                "RS512",
                "sig",
                SECOND_CREATED_AT,
                true
        );

        when(repository.findByActiveTrue())
                .thenReturn(List.of(firstEntity, secondEntity));

        JwksJpaAdapter adapter = createAdapter();

        // Act
        List<JwkKey> result = adapter.findActiveSigningKeys();

        // Assert
        assertAll(
                () -> assertEquals(2, result.size()),
                () -> assertEquals(
                        "active-key-1",
                        result.get(0).keyId()
                ),
                () -> assertEquals(
                        "public-key-pem-1",
                        result.get(0).publicKeyPem()
                ),
                () -> assertEquals(
                        "RS256",
                        result.get(0).algorithm()
                ),
                () -> assertEquals(
                        FIRST_CREATED_AT,
                        result.get(0).createdAt()
                ),
                () -> assertTrue(result.get(0).active()),
                () -> assertEquals(
                        "active-key-2",
                        result.get(1).keyId()
                ),
                () -> assertEquals(
                        "public-key-pem-2",
                        result.get(1).publicKeyPem()
                ),
                () -> assertEquals(
                        "RS512",
                        result.get(1).algorithm()
                ),
                () -> assertEquals(
                        SECOND_CREATED_AT,
                        result.get(1).createdAt()
                ),
                () -> assertTrue(result.get(1).active()),
                () -> assertThrows(
                        UnsupportedOperationException.class,
                        () -> result.removeFirst()
                )
        );

        verify(repository).findByActiveTrue();
    }

    @Test
    @DisplayName("Deve propagar erro de mapeamento quando a lista contiver entidade nula")
    void findActiveSigningKeys_entidadeNulaNaLista_deveLancarNullPointerException() {
        // Arrange
        List<JwkKeyEntity> entities = new ArrayList<>();
        entities.add(null);

        when(repository.findByActiveTrue()).thenReturn(entities);

        JwksJpaAdapter adapter = createAdapter();

        // Act
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                adapter::findActiveSigningKeys
        );

        // Assert
        assertEquals(
                "JwkKeyEntity must not be null",
                exception.getMessage()
        );

        verify(repository).findByActiveTrue();
    }

    @Test
    @DisplayName("Deve propagar a exceção quando o repositório falhar")
    void findActiveSigningKeys_repositorioLancaExcecao_devePropagarExcecao() {
        // Arrange
        RuntimeException repositoryException =
                new RuntimeException("Falha ao consultar chaves");

        when(repository.findByActiveTrue())
                .thenThrow(repositoryException);

        JwksJpaAdapter adapter = createAdapter();

        // Act
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                adapter::findActiveSigningKeys
        );

        // Assert
        assertSame(repositoryException, exception);

        verify(repository).findByActiveTrue();
    }

    private JwksJpaAdapter createAdapter() {
        return new JwksJpaAdapter(repository);
    }

    private JwkKeyEntity createJwkKeyEntity(
            String keyId,
            String publicKeyPem,
            String algorithm,
            String use,
            Instant createdAt,
            boolean active
    ) {
        JwkKeyEntity entity = new JwkKeyEntity();
        entity.setKeyId(keyId);
        entity.setPublicKeyPem(publicKeyPem);
        entity.setAlgorithm(algorithm);
        entity.setUse(use);
        entity.setCreatedAt(createdAt);
        entity.setActive(active);
        return entity;
    }

    private JwkKey createExpectedJwkKey() {
        return new JwkKey(
                "expected-key",
                "expected-public-key",
                "RS256",
                "sig",
                FIRST_CREATED_AT,
                true
        );
    }
}
