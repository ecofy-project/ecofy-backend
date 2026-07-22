package br.com.ecofy.auth.core.domain;

import br.com.ecofy.auth.core.domain.enums.TokenType;
import br.com.ecofy.auth.core.domain.valueobject.AuthUserId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Testes unitários do agregado RefreshToken")
class RefreshTokenTest {

    @Test
    @DisplayName("Deve criar o refresh token normalizando os valores e preservando os atributos")
    void constructor_valoresValidos_deveCriarRefreshTokenNormalizado() {
        // Arrange
        UUID id = UUID.randomUUID();
        AuthUserId userId = new AuthUserId(UUID.randomUUID());
        Instant issuedAt = Instant.parse("2026-07-22T12:00:00Z");
        Instant expiresAt = issuedAt.plusSeconds(3600);

        // Act
        RefreshToken refreshToken = new RefreshToken(
                id,
                "  refresh-token-value  ",
                userId,
                "  ecofy-web  ",
                issuedAt,
                expiresAt,
                false,
                TokenType.REFRESH
        );

        // Assert
        assertAll(
                () -> assertEquals(id, refreshToken.id()),
                () -> assertEquals("refresh-token-value", refreshToken.tokenValue()),
                () -> assertSame(userId, refreshToken.userId()),
                () -> assertEquals("ecofy-web", refreshToken.clientId()),
                () -> assertEquals(issuedAt, refreshToken.issuedAt()),
                () -> assertEquals(expiresAt, refreshToken.expiresAt()),
                () -> assertFalse(refreshToken.isRevoked()),
                () -> assertEquals(TokenType.REFRESH, refreshToken.type())
        );
    }

    @Test
    @DisplayName("Deve permitir a criação quando a expiração for igual ao instante de emissão")
    void constructor_expiracaoIgualAEmissao_deveCriarRefreshToken() {
        // Arrange
        Instant instant = Instant.parse("2026-07-22T12:00:00Z");

        // Act
        RefreshToken refreshToken = new RefreshToken(
                UUID.randomUUID(),
                "refresh-token",
                new AuthUserId(UUID.randomUUID()),
                "ecofy-web",
                instant,
                instant,
                false,
                TokenType.REFRESH
        );

        // Assert
        assertEquals(refreshToken.issuedAt(), refreshToken.expiresAt());
    }

    @Test
    @DisplayName("Deve criar o refresh token preservando o estado inicial revogado")
    void constructor_tokenInicialmenteRevogado_devePreservarEstadoRevogado() {
        // Arrange
        Instant issuedAt = Instant.now().plusSeconds(3600);

        // Act
        RefreshToken refreshToken = new RefreshToken(
                UUID.randomUUID(),
                "refresh-token",
                new AuthUserId(UUID.randomUUID()),
                "ecofy-web",
                issuedAt,
                issuedAt.plusSeconds(3600),
                true,
                TokenType.REFRESH
        );

        // Assert
        assertTrue(refreshToken.isRevoked());
    }

    @Test
    @DisplayName("Deve lançar NullPointerException quando o identificador for nulo")
    void constructor_identificadorNulo_deveLancarNullPointerException() {
        // Arrange
        Instant issuedAt = Instant.now();

        // Act
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> new RefreshToken(
                        null,
                        "refresh-token",
                        new AuthUserId(UUID.randomUUID()),
                        "ecofy-web",
                        issuedAt,
                        issuedAt.plusSeconds(3600),
                        false,
                        TokenType.REFRESH
                )
        );

        // Assert
        assertEquals("id must not be null", exception.getMessage());
    }

    @Test
    @DisplayName("Deve lançar NullPointerException quando o valor do token for nulo")
    void constructor_valorDoTokenNulo_deveLancarNullPointerException() {
        // Arrange
        Instant issuedAt = Instant.now();

        // Act
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> new RefreshToken(
                        UUID.randomUUID(),
                        null,
                        new AuthUserId(UUID.randomUUID()),
                        "ecofy-web",
                        issuedAt,
                        issuedAt.plusSeconds(3600),
                        false,
                        TokenType.REFRESH
                )
        );

        // Assert
        assertEquals("tokenValue must not be null", exception.getMessage());
    }

    @Test
    @DisplayName("Deve lançar IllegalArgumentException quando o valor do token estiver vazio")
    void constructor_valorDoTokenVazio_deveLancarIllegalArgumentException() {
        // Arrange
        Instant issuedAt = Instant.now();

        // Act
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new RefreshToken(
                        UUID.randomUUID(),
                        "",
                        new AuthUserId(UUID.randomUUID()),
                        "ecofy-web",
                        issuedAt,
                        issuedAt.plusSeconds(3600),
                        false,
                        TokenType.REFRESH
                )
        );

        // Assert
        assertEquals("tokenValue must not be blank", exception.getMessage());
    }

    @Test
    @DisplayName("Deve lançar IllegalArgumentException quando o valor do token contiver apenas espaços")
    void constructor_valorDoTokenComApenasEspacos_deveLancarIllegalArgumentException() {
        // Arrange
        Instant issuedAt = Instant.now();

        // Act
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new RefreshToken(
                        UUID.randomUUID(),
                        "   ",
                        new AuthUserId(UUID.randomUUID()),
                        "ecofy-web",
                        issuedAt,
                        issuedAt.plusSeconds(3600),
                        false,
                        TokenType.REFRESH
                )
        );

        // Assert
        assertEquals("tokenValue must not be blank", exception.getMessage());
    }

    @Test
    @DisplayName("Deve lançar NullPointerException quando o identificador do usuário for nulo")
    void constructor_identificadorDoUsuarioNulo_deveLancarNullPointerException() {
        // Arrange
        Instant issuedAt = Instant.now();

        // Act
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> new RefreshToken(
                        UUID.randomUUID(),
                        "refresh-token",
                        null,
                        "ecofy-web",
                        issuedAt,
                        issuedAt.plusSeconds(3600),
                        false,
                        TokenType.REFRESH
                )
        );

        // Assert
        assertEquals("userId must not be null", exception.getMessage());
    }

    @Test
    @DisplayName("Deve lançar NullPointerException quando o identificador do cliente for nulo")
    void constructor_identificadorDoClienteNulo_deveLancarNullPointerException() {
        // Arrange
        Instant issuedAt = Instant.now();

        // Act
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> new RefreshToken(
                        UUID.randomUUID(),
                        "refresh-token",
                        new AuthUserId(UUID.randomUUID()),
                        null,
                        issuedAt,
                        issuedAt.plusSeconds(3600),
                        false,
                        TokenType.REFRESH
                )
        );

        // Assert
        assertEquals("clientId must not be null", exception.getMessage());
    }

    @Test
    @DisplayName("Deve lançar IllegalArgumentException quando o identificador do cliente estiver vazio")
    void constructor_identificadorDoClienteVazio_deveLancarIllegalArgumentException() {
        // Arrange
        Instant issuedAt = Instant.now();

        // Act
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new RefreshToken(
                        UUID.randomUUID(),
                        "refresh-token",
                        new AuthUserId(UUID.randomUUID()),
                        "",
                        issuedAt,
                        issuedAt.plusSeconds(3600),
                        false,
                        TokenType.REFRESH
                )
        );

        // Assert
        assertEquals("clientId must not be blank", exception.getMessage());
    }

    @Test
    @DisplayName("Deve lançar IllegalArgumentException quando o identificador do cliente contiver apenas espaços")
    void constructor_identificadorDoClienteComApenasEspacos_deveLancarIllegalArgumentException() {
        // Arrange
        Instant issuedAt = Instant.now();

        // Act
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new RefreshToken(
                        UUID.randomUUID(),
                        "refresh-token",
                        new AuthUserId(UUID.randomUUID()),
                        "   ",
                        issuedAt,
                        issuedAt.plusSeconds(3600),
                        false,
                        TokenType.REFRESH
                )
        );

        // Assert
        assertEquals("clientId must not be blank", exception.getMessage());
    }

    @Test
    @DisplayName("Deve lançar NullPointerException quando o instante de emissão for nulo")
    void constructor_instanteDeEmissaoNulo_deveLancarNullPointerException() {
        // Arrange
        Instant expiresAt = Instant.now().plusSeconds(3600);

        // Act
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> new RefreshToken(
                        UUID.randomUUID(),
                        "refresh-token",
                        new AuthUserId(UUID.randomUUID()),
                        "ecofy-web",
                        null,
                        expiresAt,
                        false,
                        TokenType.REFRESH
                )
        );

        // Assert
        assertEquals("issuedAt must not be null", exception.getMessage());
    }

    @Test
    @DisplayName("Deve lançar NullPointerException quando o instante de expiração for nulo")
    void constructor_instanteDeExpiracaoNulo_deveLancarNullPointerException() {
        // Arrange
        Instant issuedAt = Instant.now();

        // Act
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> new RefreshToken(
                        UUID.randomUUID(),
                        "refresh-token",
                        new AuthUserId(UUID.randomUUID()),
                        "ecofy-web",
                        issuedAt,
                        null,
                        false,
                        TokenType.REFRESH
                )
        );

        // Assert
        assertEquals("expiresAt must not be null", exception.getMessage());
    }

    @Test
    @DisplayName("Deve lançar NullPointerException quando o tipo do token for nulo")
    void constructor_tipoNulo_deveLancarNullPointerException() {
        // Arrange
        Instant issuedAt = Instant.now();

        // Act
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> new RefreshToken(
                        UUID.randomUUID(),
                        "refresh-token",
                        new AuthUserId(UUID.randomUUID()),
                        "ecofy-web",
                        issuedAt,
                        issuedAt.plusSeconds(3600),
                        false,
                        null
                )
        );

        // Assert
        assertEquals("type must not be null", exception.getMessage());
    }

    @Test
    @DisplayName("Deve lançar IllegalArgumentException quando o tipo informado não for refresh")
    void constructor_tipoDiferenteDeRefresh_deveLancarIllegalArgumentException() {
        // Arrange
        Instant issuedAt = Instant.now();

        // Act
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new RefreshToken(
                        UUID.randomUUID(),
                        "refresh-token",
                        new AuthUserId(UUID.randomUUID()),
                        "ecofy-web",
                        issuedAt,
                        issuedAt.plusSeconds(3600),
                        false,
                        TokenType.ACCESS
                )
        );

        // Assert
        assertEquals("RefreshToken.type must be REFRESH", exception.getMessage());
    }

    @Test
    @DisplayName("Deve lançar IllegalArgumentException quando a expiração for anterior à emissão")
    void constructor_expiracaoAnteriorAEmissao_deveLancarIllegalArgumentException() {
        // Arrange
        Instant issuedAt = Instant.parse("2026-07-22T12:00:00Z");
        Instant expiresAt = issuedAt.minusSeconds(1);

        // Act
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new RefreshToken(
                        UUID.randomUUID(),
                        "refresh-token",
                        new AuthUserId(UUID.randomUUID()),
                        "ecofy-web",
                        issuedAt,
                        expiresAt,
                        false,
                        TokenType.REFRESH
                )
        );

        // Assert
        assertEquals(
                "expiresAt must be greater than or equal to issuedAt",
                exception.getMessage()
        );
    }

    @Test
    @DisplayName("Deve criar um novo refresh token ativo com o tempo de vida informado")
    void create_valoresValidos_deveCriarRefreshTokenAtivo() {
        // Arrange
        AuthUserId userId = new AuthUserId(UUID.randomUUID());
        long ttlSeconds = 3600;
        Instant beforeCreation = Instant.now();

        // Act
        RefreshToken refreshToken = RefreshToken.create(
                userId,
                "  ecofy-web  ",
                "  refresh-token-value  ",
                ttlSeconds
        );
        Instant afterCreation = Instant.now();

        // Assert
        assertAll(
                () -> assertNotNull(refreshToken.id()),
                () -> assertSame(userId, refreshToken.userId()),
                () -> assertEquals("ecofy-web", refreshToken.clientId()),
                () -> assertEquals("refresh-token-value", refreshToken.tokenValue()),
                () -> assertEquals(TokenType.REFRESH, refreshToken.type()),
                () -> assertFalse(refreshToken.isRevoked()),
                () -> assertFalse(refreshToken.issuedAt().isBefore(beforeCreation)),
                () -> assertFalse(refreshToken.issuedAt().isAfter(afterCreation)),
                () -> assertEquals(
                        refreshToken.issuedAt().plusSeconds(ttlSeconds),
                        refreshToken.expiresAt()
                )
        );
    }

    @Test
    @DisplayName("Deve criar um refresh token quando o tempo de vida for de um segundo")
    void create_tempoDeVidaMinimoValido_deveCriarRefreshToken() {
        // Arrange
        long ttlSeconds = 1;

        // Act
        RefreshToken refreshToken = RefreshToken.create(
                new AuthUserId(UUID.randomUUID()),
                "ecofy-web",
                "refresh-token",
                ttlSeconds
        );

        // Assert
        assertEquals(
                refreshToken.issuedAt().plusSeconds(ttlSeconds),
                refreshToken.expiresAt()
        );
    }

    @Test
    @DisplayName("Deve lançar IllegalArgumentException quando o tempo de vida for zero")
    void create_tempoDeVidaZero_deveLancarIllegalArgumentException() {
        // Act
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> RefreshToken.create(
                        new AuthUserId(UUID.randomUUID()),
                        "ecofy-web",
                        "refresh-token",
                        0
                )
        );

        // Assert
        assertEquals(
                "ttlSeconds must be greater than zero",
                exception.getMessage()
        );
    }

    @Test
    @DisplayName("Deve lançar IllegalArgumentException quando o tempo de vida for negativo")
    void create_tempoDeVidaNegativo_deveLancarIllegalArgumentException() {
        // Act
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> RefreshToken.create(
                        new AuthUserId(UUID.randomUUID()),
                        "ecofy-web",
                        "refresh-token",
                        -1
                )
        );

        // Assert
        assertEquals(
                "ttlSeconds must be greater than zero",
                exception.getMessage()
        );
    }

    @Test
    @DisplayName("Deve retornar verdadeiro quando o refresh token estiver expirado")
    void isExpired_tokenExpirado_deveRetornarVerdadeiro() {
        // Arrange
        Instant issuedAt = Instant.now().minusSeconds(60);
        RefreshToken refreshToken = createRefreshToken(
                UUID.randomUUID(),
                issuedAt,
                issuedAt.plusSeconds(30),
                false,
                "refresh-token"
        );

        // Act
        boolean expired = refreshToken.isExpired();

        // Assert
        assertTrue(expired);
    }

    @Test
    @DisplayName("Deve retornar falso quando o refresh token ainda não estiver expirado")
    void isExpired_tokenNaoExpirado_deveRetornarFalso() {
        // Arrange
        Instant issuedAt = Instant.now();
        RefreshToken refreshToken = createRefreshToken(
                UUID.randomUUID(),
                issuedAt,
                issuedAt.plusSeconds(3600),
                false,
                "refresh-token"
        );

        // Act
        boolean expired = refreshToken.isExpired();

        // Assert
        assertFalse(expired);
    }

    @Test
    @DisplayName("Deve retornar verdadeiro quando o refresh token não estiver revogado nem expirado")
    void isActive_tokenNaoRevogadoENaoExpirado_deveRetornarVerdadeiro() {
        // Arrange
        Instant issuedAt = Instant.now();
        RefreshToken refreshToken = createRefreshToken(
                UUID.randomUUID(),
                issuedAt,
                issuedAt.plusSeconds(3600),
                false,
                "refresh-token"
        );

        // Act
        boolean active = refreshToken.isActive();

        // Assert
        assertTrue(active);
    }

    @Test
    @DisplayName("Deve retornar falso quando o refresh token estiver revogado")
    void isActive_tokenRevogado_deveRetornarFalso() {
        // Arrange
        Instant issuedAt = Instant.now();
        RefreshToken refreshToken = createRefreshToken(
                UUID.randomUUID(),
                issuedAt,
                issuedAt.plusSeconds(3600),
                true,
                "refresh-token"
        );

        // Act
        boolean active = refreshToken.isActive();

        // Assert
        assertFalse(active);
    }

    @Test
    @DisplayName("Deve retornar falso quando o refresh token estiver expirado")
    void isActive_tokenExpirado_deveRetornarFalso() {
        // Arrange
        Instant issuedAt = Instant.now().minusSeconds(60);
        RefreshToken refreshToken = createRefreshToken(
                UUID.randomUUID(),
                issuedAt,
                issuedAt.plusSeconds(30),
                false,
                "refresh-token"
        );

        // Act
        boolean active = refreshToken.isActive();

        // Assert
        assertFalse(active);
    }

    @Test
    @DisplayName("Deve retornar duração positiva quando ainda houver tempo para expiração")
    void timeToExpire_tokenNaoExpirado_deveRetornarDuracaoPositiva() {
        // Arrange
        Instant issuedAt = Instant.now();
        RefreshToken refreshToken = createRefreshToken(
                UUID.randomUUID(),
                issuedAt,
                issuedAt.plusSeconds(3600),
                false,
                "refresh-token"
        );

        // Act
        Duration remainingTime = refreshToken.timeToExpire();

        // Assert
        assertAll(
                () -> assertTrue(remainingTime.isPositive()),
                () -> assertTrue(remainingTime.compareTo(Duration.ofSeconds(3600)) <= 0)
        );
    }

    @Test
    @DisplayName("Deve retornar duração negativa quando o refresh token estiver expirado")
    void timeToExpire_tokenExpirado_deveRetornarDuracaoNegativa() {
        // Arrange
        Instant issuedAt = Instant.now().minusSeconds(60);
        RefreshToken refreshToken = createRefreshToken(
                UUID.randomUUID(),
                issuedAt,
                issuedAt.plusSeconds(30),
                false,
                "refresh-token"
        );

        // Act
        Duration remainingTime = refreshToken.timeToExpire();

        // Assert
        assertTrue(remainingTime.isNegative());
    }

    @Test
    @DisplayName("Deve revogar um refresh token ativo")
    void revoke_tokenNaoRevogado_deveMarcarTokenComoRevogado() {
        // Arrange
        Instant issuedAt = Instant.now();
        RefreshToken refreshToken = createRefreshToken(
                UUID.randomUUID(),
                issuedAt,
                issuedAt.plusSeconds(3600),
                false,
                "refresh-token"
        );

        // Act
        refreshToken.revoke();

        // Assert
        assertTrue(refreshToken.isRevoked());
    }

    @Test
    @DisplayName("Deve manter o refresh token revogado quando a revogação for repetida")
    void revoke_tokenJaRevogado_deveManterEstadoRevogado() {
        // Arrange
        Instant issuedAt = Instant.now();
        RefreshToken refreshToken = createRefreshToken(
                UUID.randomUUID(),
                issuedAt,
                issuedAt.plusSeconds(3600),
                false,
                "refresh-token"
        );
        refreshToken.revoke();

        // Act
        refreshToken.revoke();

        // Assert
        assertTrue(refreshToken.isRevoked());
    }

    @Test
    @DisplayName("Deve considerar a mesma instância igual a si própria")
    void equals_mesmaInstancia_deveRetornarVerdadeiro() {
        // Arrange
        Instant issuedAt = Instant.now();
        RefreshToken refreshToken = createRefreshToken(
                UUID.randomUUID(),
                issuedAt,
                issuedAt.plusSeconds(3600),
                false,
                "refresh-token"
        );

        // Act
        boolean equal = refreshToken.equals(refreshToken);

        // Assert
        assertTrue(equal);
    }

    @Test
    @DisplayName("Deve considerar iguais refresh tokens com o mesmo identificador")
    void equals_mesmoIdentificador_deveRetornarVerdadeiro() {
        // Arrange
        UUID id = UUID.randomUUID();
        Instant issuedAt = Instant.now();
        RefreshToken firstToken = createRefreshToken(
                id,
                issuedAt,
                issuedAt.plusSeconds(3600),
                false,
                "first-refresh-token"
        );
        RefreshToken secondToken = createRefreshToken(
                id,
                issuedAt,
                issuedAt.plusSeconds(7200),
                true,
                "second-refresh-token"
        );

        // Act
        boolean equal = firstToken.equals(secondToken);

        // Assert
        assertTrue(equal);
    }

    @Test
    @DisplayName("Deve considerar diferentes refresh tokens com identificadores distintos")
    void equals_identificadoresDiferentes_deveRetornarFalso() {
        // Arrange
        Instant issuedAt = Instant.now();
        RefreshToken firstToken = createRefreshToken(
                UUID.randomUUID(),
                issuedAt,
                issuedAt.plusSeconds(3600),
                false,
                "refresh-token"
        );
        RefreshToken secondToken = createRefreshToken(
                UUID.randomUUID(),
                issuedAt,
                issuedAt.plusSeconds(3600),
                false,
                "refresh-token"
        );

        // Act
        boolean equal = firstToken.equals(secondToken);

        // Assert
        assertFalse(equal);
    }

    @Test
    @DisplayName("Deve considerar o refresh token diferente de nulo e de outro tipo")
    void equals_objetosIncompativeis_deveRetornarFalso() {
        // Arrange
        Instant issuedAt = Instant.now();
        RefreshToken refreshToken = createRefreshToken(
                UUID.randomUUID(),
                issuedAt,
                issuedAt.plusSeconds(3600),
                false,
                "refresh-token"
        );

        // Act
        boolean equalToNull = refreshToken.equals(null);
        boolean equalToString = refreshToken.equals("refresh-token");

        // Assert
        assertAll(
                () -> assertFalse(equalToNull),
                () -> assertFalse(equalToString)
        );
    }

    @Test
    @DisplayName("Deve gerar o mesmo hash para refresh tokens com o mesmo identificador")
    void hashCode_mesmoIdentificador_deveRetornarMesmoHash() {
        // Arrange
        UUID id = UUID.randomUUID();
        Instant issuedAt = Instant.now();
        RefreshToken firstToken = createRefreshToken(
                id,
                issuedAt,
                issuedAt.plusSeconds(3600),
                false,
                "first-refresh-token"
        );
        RefreshToken secondToken = createRefreshToken(
                id,
                issuedAt,
                issuedAt.plusSeconds(7200),
                true,
                "second-refresh-token"
        );

        // Act
        int firstHash = firstToken.hashCode();
        int secondHash = secondToken.hashCode();

        // Assert
        assertEquals(firstHash, secondHash);
    }

    @Test
    @DisplayName("Deve gerar hashes diferentes para refresh tokens com identificadores distintos")
    void hashCode_identificadoresDiferentes_deveRetornarHashesDiferentes() {
        // Arrange
        Instant issuedAt = Instant.now();
        RefreshToken firstToken = createRefreshToken(
                UUID.fromString("00000000-0000-0000-0000-000000000001"),
                issuedAt,
                issuedAt.plusSeconds(3600),
                false,
                "refresh-token"
        );
        RefreshToken secondToken = createRefreshToken(
                UUID.fromString("00000000-0000-0000-0000-000000000002"),
                issuedAt,
                issuedAt.plusSeconds(3600),
                false,
                "refresh-token"
        );

        // Act
        int firstHash = firstToken.hashCode();
        int secondHash = secondToken.hashCode();

        // Assert
        assertNotEquals(firstHash, secondHash);
    }

    @Test
    @DisplayName("Deve mascarar refresh tokens longos mantendo somente os primeiros doze caracteres")
    void toString_tokenLongo_deveMascararValor() {
        // Arrange
        UUID id = UUID.randomUUID();
        AuthUserId userId = new AuthUserId(UUID.randomUUID());
        Instant issuedAt = Instant.parse("2026-07-22T12:00:00Z");
        Instant expiresAt = issuedAt.plusSeconds(3600);
        String tokenValue = "12345678901234567890";
        RefreshToken refreshToken = new RefreshToken(
                id,
                tokenValue,
                userId,
                "ecofy-web",
                issuedAt,
                expiresAt,
                false,
                TokenType.REFRESH
        );
        String expected = "RefreshToken{" +
                "id=" + id +
                ", tokenValue='123456789012...'" +
                ", userId=" + userId.value() +
                ", clientId='ecofy-web'" +
                ", issuedAt=" + issuedAt +
                ", expiresAt=" + expiresAt +
                ", revoked=false" +
                '}';

        // Act
        String representation = refreshToken.toString();

        // Assert
        assertAll(
                () -> assertEquals(expected, representation),
                () -> assertFalse(representation.contains(tokenValue))
        );
    }

    @Test
    @DisplayName("Deve ocultar completamente refresh tokens com até doze caracteres")
    void toString_tokenComDozeCaracteres_deveOcultarCompletamenteValor() {
        // Arrange
        UUID id = UUID.randomUUID();
        AuthUserId userId = new AuthUserId(UUID.randomUUID());
        Instant issuedAt = Instant.parse("2026-07-22T12:00:00Z");
        Instant expiresAt = issuedAt.plusSeconds(3600);
        String tokenValue = "123456789012";
        RefreshToken refreshToken = new RefreshToken(
                id,
                tokenValue,
                userId,
                "ecofy-web",
                issuedAt,
                expiresAt,
                true,
                TokenType.REFRESH
        );
        String expected = "RefreshToken{" +
                "id=" + id +
                ", tokenValue='***'" +
                ", userId=" + userId.value() +
                ", clientId='ecofy-web'" +
                ", issuedAt=" + issuedAt +
                ", expiresAt=" + expiresAt +
                ", revoked=true" +
                '}';

        // Act
        String representation = refreshToken.toString();

        // Assert
        assertAll(
                () -> assertEquals(expected, representation),
                () -> assertFalse(representation.contains(tokenValue))
        );
    }

    private RefreshToken createRefreshToken(UUID id,
                                            Instant issuedAt,
                                            Instant expiresAt,
                                            boolean revoked,
                                            String tokenValue) {
        return new RefreshToken(
                id,
                tokenValue,
                new AuthUserId(UUID.randomUUID()),
                "ecofy-web",
                issuedAt,
                expiresAt,
                revoked,
                TokenType.REFRESH
        );
    }
}
