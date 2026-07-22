package br.com.ecofy.auth.adapters.out.persistence;

import br.com.ecofy.auth.adapters.out.persistence.entity.RefreshTokenEntity;
import br.com.ecofy.auth.adapters.out.persistence.repository.RefreshTokenRepository;
import br.com.ecofy.auth.core.domain.RefreshToken;
import br.com.ecofy.auth.core.domain.enums.TokenType;
import br.com.ecofy.auth.core.domain.valueobject.AuthUserId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Testes unitários do adaptador JPA de refresh tokens")
class RefreshTokenJpaAdapterTest {

    private static final UUID TOKEN_ID = UUID.fromString(
            "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"
    );
    private static final UUID USER_ID = UUID.fromString(
            "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"
    );
    private static final String TOKEN_VALUE =
            "refresh-token-value-123456789";
    private static final String CLIENT_ID = "ecofy-client";
    private static final Instant ISSUED_AT =
            Instant.parse("2026-07-20T10:00:00Z");
    private static final Instant EXPIRES_AT =
            Instant.parse("2026-07-21T10:00:00Z");

    @Mock
    private RefreshTokenRepository repository;

    @Test
    @DisplayName("Deve rejeitar repositório nulo ao construir o adaptador")
    void constructor_repositorioNulo_deveLancarNullPointerException() {
        // Arrange
        RefreshTokenRepository nullRepository = null;

        // Act
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> new RefreshTokenJpaAdapter(nullRepository)
        );

        // Assert
        assertEquals(
                "repository must not be null",
                exception.getMessage()
        );
    }

    @Test
    @DisplayName("Deve rejeitar refresh token nulo sem acessar o repositório")
    void save_tokenNulo_deveLancarNullPointerException() {
        // Arrange
        RefreshTokenJpaAdapter adapter = createAdapter();

        // Act
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> adapter.save(null)
        );

        // Assert
        assertAll(
                () -> assertEquals(
                        "token must not be null",
                        exception.getMessage()
                ),
                () -> verifyNoInteractions(repository)
        );
    }

    @Test
    @DisplayName("Deve converter e persistir o refresh token com todos os campos")
    void save_tokenValido_devePersistirERetornarDominioMapeado() {
        // Arrange
        RefreshToken token = createRefreshToken(false);

        when(repository.save(any(RefreshTokenEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        RefreshTokenJpaAdapter adapter = createAdapter();
        ArgumentCaptor<RefreshTokenEntity> captor =
                ArgumentCaptor.forClass(RefreshTokenEntity.class);

        // Act
        RefreshToken result = adapter.save(token);

        // Assert
        verify(repository).save(captor.capture());

        RefreshTokenEntity savedEntity = captor.getValue();

        assertAll(
                () -> assertEquals(TOKEN_ID, savedEntity.getId()),
                () -> assertEquals(
                        TOKEN_VALUE,
                        savedEntity.getTokenValue()
                ),
                () -> assertEquals(USER_ID, savedEntity.getUserId()),
                () -> assertEquals(
                        CLIENT_ID,
                        savedEntity.getClientId()
                ),
                () -> assertEquals(
                        ISSUED_AT,
                        savedEntity.getIssuedAt()
                ),
                () -> assertEquals(
                        EXPIRES_AT,
                        savedEntity.getExpiresAt()
                ),
                () -> assertFalse(savedEntity.isRevoked()),
                () -> assertEquals(
                        TokenType.REFRESH,
                        savedEntity.getType()
                )
        );

        assertAll(
                () -> assertEquals(TOKEN_ID, result.id()),
                () -> assertEquals(
                        TOKEN_VALUE,
                        result.tokenValue()
                ),
                () -> assertEquals(
                        USER_ID,
                        result.userId().value()
                ),
                () -> assertEquals(
                        CLIENT_ID,
                        result.clientId()
                ),
                () -> assertEquals(
                        ISSUED_AT,
                        result.issuedAt()
                ),
                () -> assertEquals(
                        EXPIRES_AT,
                        result.expiresAt()
                ),
                () -> assertFalse(result.isRevoked()),
                () -> assertEquals(
                        TokenType.REFRESH,
                        result.type()
                )
        );
    }

    @Test
    @DisplayName("Deve propagar a exceção quando o repositório falhar ao salvar")
    void save_repositorioLancaExcecao_devePropagarExcecao() {
        // Arrange
        RefreshToken token = createRefreshToken(false);
        RuntimeException repositoryException =
                new RuntimeException("Falha ao salvar refresh token");

        when(repository.save(any(RefreshTokenEntity.class)))
                .thenThrow(repositoryException);

        RefreshTokenJpaAdapter adapter = createAdapter();

        // Act
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> adapter.save(token)
        );

        // Assert
        assertSame(repositoryException, exception);
    }

    @Test
    @DisplayName("Deve rejeitar valor de token nulo ao realizar a busca")
    void findByTokenValue_tokenValueNulo_deveLancarNullPointerException() {
        // Arrange
        RefreshTokenJpaAdapter adapter = createAdapter();

        // Act
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> adapter.findByTokenValue(null)
        );

        // Assert
        assertAll(
                () -> assertEquals(
                        "tokenValue must not be null",
                        exception.getMessage()
                ),
                () -> verifyNoInteractions(repository)
        );
    }

    @Test
    @DisplayName("Deve consultar o repositório quando o valor do token estiver em branco")
    void findByTokenValue_tokenEmBranco_deveConsultarERetornarVazio() {
        // Arrange
        String blankToken = "   ";

        when(repository.findByTokenValue(blankToken))
                .thenReturn(Optional.empty());

        RefreshTokenJpaAdapter adapter = createAdapter();

        // Act
        Optional<RefreshToken> result =
                adapter.findByTokenValue(blankToken);

        // Assert
        assertAll(
                () -> assertTrue(result.isEmpty()),
                () -> verify(repository)
                        .findByTokenValue(blankToken)
        );
    }

    @Test
    @DisplayName("Deve retornar vazio quando o refresh token não existir")
    void findByTokenValue_tokenInexistente_deveRetornarOptionalVazio() {
        // Arrange
        String shortToken = "123456789012";

        when(repository.findByTokenValue(shortToken))
                .thenReturn(Optional.empty());

        RefreshTokenJpaAdapter adapter = createAdapter();

        // Act
        Optional<RefreshToken> result =
                adapter.findByTokenValue(shortToken);

        // Assert
        assertAll(
                () -> assertTrue(result.isEmpty()),
                () -> verify(repository)
                        .findByTokenValue(shortToken)
        );
    }

    @Test
    @DisplayName("Deve converter e retornar o refresh token encontrado")
    void findByTokenValue_tokenExistente_deveRetornarDominioMapeado() {
        // Arrange
        RefreshTokenEntity entity = createRefreshTokenEntity(false);

        when(repository.findByTokenValue(TOKEN_VALUE))
                .thenReturn(Optional.of(entity));

        RefreshTokenJpaAdapter adapter = createAdapter();

        // Act
        Optional<RefreshToken> result =
                adapter.findByTokenValue(TOKEN_VALUE);

        // Assert
        RefreshToken token = result.orElseThrow();

        assertAll(
                () -> assertEquals(TOKEN_ID, token.id()),
                () -> assertEquals(
                        TOKEN_VALUE,
                        token.tokenValue()
                ),
                () -> assertEquals(
                        USER_ID,
                        token.userId().value()
                ),
                () -> assertEquals(
                        CLIENT_ID,
                        token.clientId()
                ),
                () -> assertEquals(
                        ISSUED_AT,
                        token.issuedAt()
                ),
                () -> assertEquals(
                        EXPIRES_AT,
                        token.expiresAt()
                ),
                () -> assertFalse(token.isRevoked()),
                () -> assertEquals(
                        TokenType.REFRESH,
                        token.type()
                )
        );

        verify(repository).findByTokenValue(TOKEN_VALUE);
    }

    @Test
    @DisplayName("Deve propagar a exceção quando o repositório falhar durante a busca")
    void findByTokenValue_repositorioLancaExcecao_devePropagarExcecao() {
        // Arrange
        RuntimeException repositoryException =
                new RuntimeException("Falha ao buscar refresh token");

        when(repository.findByTokenValue(TOKEN_VALUE))
                .thenThrow(repositoryException);

        RefreshTokenJpaAdapter adapter = createAdapter();

        // Act
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> adapter.findByTokenValue(TOKEN_VALUE)
        );

        // Assert
        assertSame(repositoryException, exception);
    }

    @Test
    @DisplayName("Deve rejeitar valor de token nulo ao realizar a revogação")
    void revoke_tokenValueNulo_deveLancarNullPointerException() {
        // Arrange
        RefreshTokenJpaAdapter adapter = createAdapter();

        // Act
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> adapter.revoke(null)
        );

        // Assert
        assertAll(
                () -> assertEquals(
                        "tokenValue must not be null",
                        exception.getMessage()
                ),
                () -> verifyNoInteractions(repository)
        );
    }

    @Test
    @DisplayName("Deve revogar e persistir um refresh token que ainda estiver ativo")
    void revoke_tokenAtivo_deveMarcarComoRevogadoEPersistir() {
        // Arrange
        RefreshTokenEntity entity = createRefreshTokenEntity(false);

        when(repository.findByTokenValue(TOKEN_VALUE))
                .thenReturn(Optional.of(entity));
        when(repository.save(entity)).thenReturn(entity);

        RefreshTokenJpaAdapter adapter = createAdapter();

        // Act
        adapter.revoke(TOKEN_VALUE);

        // Assert
        assertTrue(entity.isRevoked());

        verify(repository).findByTokenValue(TOKEN_VALUE);
        verify(repository).save(entity);
    }

    @Test
    @DisplayName("Deve manter refresh token já revogado sem realizar nova persistência")
    void revoke_tokenJaRevogado_deveNaoSalvarNovamente() {
        // Arrange
        String shortToken = "123456789012";
        RefreshTokenEntity entity = createRefreshTokenEntity(true);
        entity.setTokenValue(shortToken);

        when(repository.findByTokenValue(shortToken))
                .thenReturn(Optional.of(entity));

        RefreshTokenJpaAdapter adapter = createAdapter();

        // Act
        adapter.revoke(shortToken);

        // Assert
        assertTrue(entity.isRevoked());

        verify(repository).findByTokenValue(shortToken);
        verify(repository, never()).save(any(RefreshTokenEntity.class));
    }

    @Test
    @DisplayName("Deve concluir sem persistir quando o refresh token não for encontrado")
    void revoke_tokenInexistente_deveNaoSalvarEntidade() {
        // Arrange
        when(repository.findByTokenValue(TOKEN_VALUE))
                .thenReturn(Optional.empty());

        RefreshTokenJpaAdapter adapter = createAdapter();

        // Act
        adapter.revoke(TOKEN_VALUE);

        // Assert
        verify(repository).findByTokenValue(TOKEN_VALUE);
        verify(repository, never()).save(any(RefreshTokenEntity.class));
    }

    @Test
    @DisplayName("Deve consultar o repositório ao revogar um token em branco")
    void revoke_tokenEmBranco_deveConsultarENaoPersistir() {
        // Arrange
        String blankToken = " ";

        when(repository.findByTokenValue(blankToken))
                .thenReturn(Optional.empty());

        RefreshTokenJpaAdapter adapter = createAdapter();

        // Act
        adapter.revoke(blankToken);

        // Assert
        verify(repository).findByTokenValue(blankToken);
        verify(repository, never()).save(any(RefreshTokenEntity.class));
    }

    @Test
    @DisplayName("Deve propagar a exceção quando o repositório falhar durante a revogação")
    void revoke_repositorioLancaExcecao_devePropagarExcecao() {
        // Arrange
        RuntimeException repositoryException =
                new RuntimeException("Falha ao revogar refresh token");

        when(repository.findByTokenValue(TOKEN_VALUE))
                .thenThrow(repositoryException);

        RefreshTokenJpaAdapter adapter = createAdapter();

        // Act
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> adapter.revoke(TOKEN_VALUE)
        );

        // Assert
        assertSame(repositoryException, exception);
    }

    @Test
    @DisplayName("Deve mascarar valor nulo com três asteriscos")
    void maskToken_tokenNulo_deveRetornarMascaraCompleta()
            throws Exception {
        // Arrange
        RefreshTokenJpaAdapter adapter = createAdapter();

        // Act
        String result = invokeMaskToken(adapter, null);

        // Assert
        assertEquals("***", result);
    }

    @Test
    @DisplayName("Deve mascarar valor em branco com três asteriscos")
    void maskToken_tokenEmBranco_deveRetornarMascaraCompleta()
            throws Exception {
        // Arrange
        RefreshTokenJpaAdapter adapter = createAdapter();

        // Act
        String result = invokeMaskToken(adapter, "   ");

        // Assert
        assertEquals("***", result);
    }

    @Test
    @DisplayName("Deve mascarar completamente token com exatamente doze caracteres")
    void maskToken_tokenComDozeCaracteres_deveRetornarMascaraCompleta()
            throws Exception {
        // Arrange
        RefreshTokenJpaAdapter adapter = createAdapter();

        // Act
        String result = invokeMaskToken(adapter, "123456789012");

        // Assert
        assertEquals("***", result);
    }

    @Test
    @DisplayName("Deve exibir os primeiros doze caracteres quando o token ultrapassar o limite")
    void maskToken_tokenComMaisDeDozeCaracteres_deveRetornarPrefixoMascarado()
            throws Exception {
        // Arrange
        RefreshTokenJpaAdapter adapter = createAdapter();

        // Act
        String result = invokeMaskToken(
                adapter,
                "1234567890123"
        );

        // Assert
        assertEquals("123456789012...", result);
    }

    private RefreshTokenJpaAdapter createAdapter() {
        return new RefreshTokenJpaAdapter(repository);
    }

    private RefreshToken createRefreshToken(boolean revoked) {
        return new RefreshToken(
                TOKEN_ID,
                TOKEN_VALUE,
                new AuthUserId(USER_ID),
                CLIENT_ID,
                ISSUED_AT,
                EXPIRES_AT,
                revoked,
                TokenType.REFRESH
        );
    }

    private RefreshTokenEntity createRefreshTokenEntity(
            boolean revoked
    ) {
        return RefreshTokenEntity.builder()
                .id(TOKEN_ID)
                .tokenValue(TOKEN_VALUE)
                .userId(USER_ID)
                .clientId(CLIENT_ID)
                .issuedAt(ISSUED_AT)
                .expiresAt(EXPIRES_AT)
                .revoked(revoked)
                .type(TokenType.REFRESH)
                .build();
    }

    private String invokeMaskToken(
            RefreshTokenJpaAdapter adapter,
            String token
    ) throws Exception {
        Method method = RefreshTokenJpaAdapter.class
                .getDeclaredMethod("maskToken", String.class);
        method.setAccessible(true);
        return (String) method.invoke(adapter, token);
    }
}
