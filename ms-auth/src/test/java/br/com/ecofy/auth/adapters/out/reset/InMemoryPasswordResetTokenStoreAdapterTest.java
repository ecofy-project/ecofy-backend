package br.com.ecofy.auth.adapters.out.reset;

import br.com.ecofy.auth.core.domain.AuthUser;
import br.com.ecofy.auth.core.domain.enums.AuthUserStatus;
import br.com.ecofy.auth.core.domain.valueobject.AuthUserId;
import br.com.ecofy.auth.core.domain.valueobject.EmailAddress;
import br.com.ecofy.auth.core.domain.valueobject.PasswordHash;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Testes unitários do armazenamento de tokens de redefinição de senha em memória")
class InMemoryPasswordResetTokenStoreAdapterTest {

    private static final String VALID_TOKEN =
            "password-reset-token-123456789";
    private static final String SECOND_TOKEN =
            "another-reset-token-987654321";

    @Test
    @DisplayName("Deve ignorar o armazenamento quando o usuário for nulo")
    void store_usuarioNulo_deveIgnorarArmazenamento() {
        // Arrange
        InMemoryPasswordResetTokenStoreAdapter adapter =
                createAdapter();

        // Act
        adapter.store(null, VALID_TOKEN);
        Optional<AuthUser> result = adapter.consume(VALID_TOKEN);

        // Assert
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Deve ignorar o armazenamento quando o token for nulo")
    void store_tokenNulo_deveIgnorarArmazenamento() {
        // Arrange
        InMemoryPasswordResetTokenStoreAdapter adapter =
                createAdapter();
        AuthUser user = createUser(
                "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
                "primeiro@ecofy.com"
        );

        // Act
        adapter.store(user, null);

        // Assert
        assertTrue(adapter.consume(VALID_TOKEN).isEmpty());
    }

    @Test
    @DisplayName("Deve ignorar o armazenamento quando o token estiver vazio")
    void store_tokenVazio_deveIgnorarArmazenamento() {
        // Arrange
        InMemoryPasswordResetTokenStoreAdapter adapter =
                createAdapter();
        AuthUser user = createUser(
                "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
                "primeiro@ecofy.com"
        );

        // Act
        adapter.store(user, "");

        // Assert
        assertTrue(adapter.consume("").isEmpty());
    }

    @Test
    @DisplayName("Deve ignorar o armazenamento quando o token contiver apenas espaços")
    void store_tokenEmBranco_deveIgnorarArmazenamento() {
        // Arrange
        InMemoryPasswordResetTokenStoreAdapter adapter =
                createAdapter();
        AuthUser user = createUser(
                "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
                "primeiro@ecofy.com"
        );

        // Act
        adapter.store(user, "   ");

        // Assert
        assertTrue(adapter.consume("   ").isEmpty());
    }

    @Test
    @DisplayName("Deve armazenar e recuperar o usuário associado ao token")
    void store_tokenValido_deveArmazenarUsuarioAssociado() {
        // Arrange
        InMemoryPasswordResetTokenStoreAdapter adapter =
                createAdapter();
        AuthUser user = createUser(
                "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
                "primeiro@ecofy.com"
        );

        // Act
        adapter.store(user, VALID_TOKEN);
        Optional<AuthUser> result = adapter.consume(VALID_TOKEN);

        // Assert
        assertTrue(result.isPresent());
        assertSame(user, result.orElseThrow());
    }

    @Test
    @DisplayName("Deve remover o token após o primeiro consumo bem-sucedido")
    void consume_tokenConsumidoDuasVezes_deveRetornarUsuarioSomenteNaPrimeira() {
        // Arrange
        InMemoryPasswordResetTokenStoreAdapter adapter =
                createAdapter();
        AuthUser user = createUser(
                "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
                "primeiro@ecofy.com"
        );
        adapter.store(user, VALID_TOKEN);

        // Act
        Optional<AuthUser> firstResult =
                adapter.consume(VALID_TOKEN);
        Optional<AuthUser> secondResult =
                adapter.consume(VALID_TOKEN);

        // Assert
        assertTrue(firstResult.isPresent());
        assertSame(user, firstResult.orElseThrow());
        assertTrue(secondResult.isEmpty());
    }

    @Test
    @DisplayName("Deve substituir o usuário quando o mesmo token for armazenado novamente")
    void store_tokenJaExistente_deveSubstituirUsuarioAssociado() {
        // Arrange
        InMemoryPasswordResetTokenStoreAdapter adapter =
                createAdapter();
        AuthUser firstUser = createUser(
                "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
                "primeiro@ecofy.com"
        );
        AuthUser secondUser = createUser(
                "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb",
                "segundo@ecofy.com"
        );

        // Act
        adapter.store(firstUser, VALID_TOKEN);
        adapter.store(secondUser, VALID_TOKEN);
        Optional<AuthUser> result = adapter.consume(VALID_TOKEN);

        // Assert
        assertTrue(result.isPresent());
        assertSame(secondUser, result.orElseThrow());
    }

    @Test
    @DisplayName("Deve armazenar tokens diferentes de forma independente")
    void store_tokensDiferentes_deveManterUsuariosIndependentes() {
        // Arrange
        InMemoryPasswordResetTokenStoreAdapter adapter =
                createAdapter();
        AuthUser firstUser = createUser(
                "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
                "primeiro@ecofy.com"
        );
        AuthUser secondUser = createUser(
                "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb",
                "segundo@ecofy.com"
        );

        // Act
        adapter.store(firstUser, VALID_TOKEN);
        adapter.store(secondUser, SECOND_TOKEN);

        Optional<AuthUser> firstResult =
                adapter.consume(VALID_TOKEN);
        Optional<AuthUser> secondResult =
                adapter.consume(SECOND_TOKEN);

        // Assert
        assertTrue(firstResult.isPresent());
        assertTrue(secondResult.isPresent());
        assertSame(firstUser, firstResult.orElseThrow());
        assertSame(secondUser, secondResult.orElseThrow());
    }

    @Test
    @DisplayName("Deve retornar vazio quando o token consumido for nulo")
    void consume_tokenNulo_deveRetornarOptionalVazio() {
        // Arrange
        InMemoryPasswordResetTokenStoreAdapter adapter =
                createAdapter();

        // Act
        Optional<AuthUser> result = adapter.consume(null);

        // Assert
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Deve retornar vazio quando o token consumido estiver vazio")
    void consume_tokenVazio_deveRetornarOptionalVazio() {
        // Arrange
        InMemoryPasswordResetTokenStoreAdapter adapter =
                createAdapter();

        // Act
        Optional<AuthUser> result = adapter.consume("");

        // Assert
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Deve retornar vazio quando o token consumido estiver em branco")
    void consume_tokenEmBranco_deveRetornarOptionalVazio() {
        // Arrange
        InMemoryPasswordResetTokenStoreAdapter adapter =
                createAdapter();

        // Act
        Optional<AuthUser> result = adapter.consume("   ");

        // Assert
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Deve retornar vazio quando o token curto não estiver armazenado")
    void consume_tokenCurtoInexistente_deveRetornarOptionalVazio() {
        // Arrange
        InMemoryPasswordResetTokenStoreAdapter adapter =
                createAdapter();

        // Act
        Optional<AuthUser> result = adapter.consume("1234567890");

        // Assert
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Deve retornar vazio quando o token longo não estiver armazenado")
    void consume_tokenLongoInexistente_deveRetornarOptionalVazio() {
        // Arrange
        InMemoryPasswordResetTokenStoreAdapter adapter =
                createAdapter();

        // Act
        Optional<AuthUser> result =
                adapter.consume("12345678901");

        // Assert
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Deve mascarar token nulo com três asteriscos")
    void maskToken_tokenNulo_deveRetornarMascaraCompleta()
            throws Exception {
        // Arrange
        InMemoryPasswordResetTokenStoreAdapter adapter =
                createAdapter();

        // Act
        String result = invokeMaskToken(adapter, null);

        // Assert
        assertEquals("***", result);
    }

    @Test
    @DisplayName("Deve mascarar token vazio com três asteriscos")
    void maskToken_tokenVazio_deveRetornarMascaraCompleta()
            throws Exception {
        // Arrange
        InMemoryPasswordResetTokenStoreAdapter adapter =
                createAdapter();

        // Act
        String result = invokeMaskToken(adapter, "");

        // Assert
        assertEquals("***", result);
    }

    @Test
    @DisplayName("Deve mascarar token em branco com três asteriscos")
    void maskToken_tokenEmBranco_deveRetornarMascaraCompleta()
            throws Exception {
        // Arrange
        InMemoryPasswordResetTokenStoreAdapter adapter =
                createAdapter();

        // Act
        String result = invokeMaskToken(adapter, "   ");

        // Assert
        assertEquals("***", result);
    }

    @Test
    @DisplayName("Deve mascarar completamente token com exatamente dez caracteres")
    void maskToken_tokenComDezCaracteres_deveRetornarMascaraCompleta()
            throws Exception {
        // Arrange
        InMemoryPasswordResetTokenStoreAdapter adapter =
                createAdapter();

        // Act
        String result = invokeMaskToken(
                adapter,
                "1234567890"
        );

        // Assert
        assertEquals("***", result);
    }

    @Test
    @DisplayName("Deve exibir os primeiros dez caracteres quando o token ultrapassar o limite")
    void maskToken_tokenComMaisDeDezCaracteres_deveRetornarPrefixoMascarado()
            throws Exception {
        // Arrange
        InMemoryPasswordResetTokenStoreAdapter adapter =
                createAdapter();

        // Act
        String result = invokeMaskToken(
                adapter,
                "12345678901"
        );

        // Assert
        assertEquals("1234567890...", result);
    }

    private InMemoryPasswordResetTokenStoreAdapter createAdapter() {
        return new InMemoryPasswordResetTokenStoreAdapter();
    }

    private AuthUser createUser(
            String id,
            String email
    ) {
        Instant now = Instant.parse("2026-07-20T12:00:00Z");

        return new AuthUser(
                new AuthUserId(UUID.fromString(id)),
                new EmailAddress(email),
                new PasswordHash("password-hash"),
                AuthUserStatus.ACTIVE,
                true,
                "Usuário",
                "EcoFy",
                "pt-BR",
                Set.of(),
                Set.of(),
                now,
                now,
                null,
                0
        );
    }

    private String invokeMaskToken(
            InMemoryPasswordResetTokenStoreAdapter adapter,
            String token
    ) throws Exception {
        Method method =
                InMemoryPasswordResetTokenStoreAdapter.class
                        .getDeclaredMethod(
                                "maskToken",
                                String.class
                        );

        method.setAccessible(true);

        return (String) method.invoke(adapter, token);
    }
}
