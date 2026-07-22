package br.com.ecofy.auth.core.domain;

import br.com.ecofy.auth.core.domain.enums.TokenType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Testes unitários do value object JwtToken")
class JwtTokenTest {

    @Test
    @DisplayName("Deve criar o token normalizando o valor e preservando seus atributos")
    void constructor_valoresValidos_deveCriarTokenNormalizado() {
        // Arrange
        String value = "  header.payload.signature  ";
        Instant expiresAt = Instant.now().plusSeconds(3600);
        TokenType type = TokenType.ACCESS;

        // Act
        JwtToken token = new JwtToken(value, expiresAt, type);

        // Assert
        assertAll(
                () -> assertEquals("header.payload.signature", token.value()),
                () -> assertEquals(expiresAt, token.expiresAt()),
                () -> assertEquals(type, token.type())
        );
    }

    @Test
    @DisplayName("Deve lançar NullPointerException quando o valor do token for nulo")
    void constructor_valorNulo_deveLancarNullPointerException() {
        // Arrange
        Instant expiresAt = Instant.now().plusSeconds(3600);

        // Act
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> new JwtToken(null, expiresAt, TokenType.ACCESS)
        );

        // Assert
        assertEquals("value must not be null", exception.getMessage());
    }

    @Test
    @DisplayName("Deve lançar IllegalArgumentException quando o valor do token estiver vazio")
    void constructor_valorVazio_deveLancarIllegalArgumentException() {
        // Arrange
        Instant expiresAt = Instant.now().plusSeconds(3600);

        // Act
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new JwtToken("", expiresAt, TokenType.ACCESS)
        );

        // Assert
        assertEquals("JWT value must not be blank", exception.getMessage());
    }

    @Test
    @DisplayName("Deve lançar IllegalArgumentException quando o valor do token contiver apenas espaços")
    void constructor_valorComApenasEspacos_deveLancarIllegalArgumentException() {
        // Arrange
        Instant expiresAt = Instant.now().plusSeconds(3600);

        // Act
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new JwtToken("   ", expiresAt, TokenType.ACCESS)
        );

        // Assert
        assertEquals("JWT value must not be blank", exception.getMessage());
    }

    @Test
    @DisplayName("Deve lançar NullPointerException quando a expiração for nula")
    void constructor_expiracaoNula_deveLancarNullPointerException() {
        // Act
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> new JwtToken("header.payload.signature", null, TokenType.ACCESS)
        );

        // Assert
        assertEquals("expiresAt must not be null", exception.getMessage());
    }

    @Test
    @DisplayName("Deve lançar NullPointerException quando o tipo do token for nulo")
    void constructor_tipoNulo_deveLancarNullPointerException() {
        // Arrange
        Instant expiresAt = Instant.now().plusSeconds(3600);

        // Act
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> new JwtToken("header.payload.signature", expiresAt, null)
        );

        // Assert
        assertEquals("type must not be null", exception.getMessage());
    }

    @Test
    @DisplayName("Deve lançar IllegalArgumentException quando a expiração ultrapassar a tolerância permitida")
    void constructor_expiracaoAnteriorATolerancia_deveLancarIllegalArgumentException() {
        // Arrange
        Instant expiresAt = Instant.now().minusSeconds(10);

        // Act
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new JwtToken("header.payload.signature", expiresAt, TokenType.ACCESS)
        );

        // Assert
        assertEquals("expiresAt cannot be in the past", exception.getMessage());
    }

    @Test
    @DisplayName("Deve permitir a criação quando a expiração estiver dentro da tolerância de relógio")
    void constructor_expiracaoPassadaDentroDaTolerancia_deveCriarToken() {
        // Arrange
        Instant expiresAt = Instant.now().minusSeconds(1);

        // Act
        JwtToken token = assertDoesNotThrow(
                () -> new JwtToken(
                        "header.payload.signature",
                        expiresAt,
                        TokenType.ACCESS
                )
        );

        // Assert
        assertEquals(expiresAt, token.expiresAt());
    }

    @Test
    @DisplayName("Deve retornar verdadeiro quando o token estiver expirado")
    void isExpired_tokenExpirado_deveRetornarVerdadeiro() {
        // Arrange
        JwtToken token = new JwtToken(
                "header.payload.signature",
                Instant.now().minusMillis(100),
                TokenType.ACCESS
        );

        // Act
        boolean expired = token.isExpired();

        // Assert
        assertTrue(expired);
    }

    @Test
    @DisplayName("Deve retornar falso quando o token ainda não estiver expirado")
    void isExpired_tokenNaoExpirado_deveRetornarFalso() {
        // Arrange
        JwtToken token = new JwtToken(
                "header.payload.signature",
                Instant.now().plusSeconds(3600),
                TokenType.ACCESS
        );

        // Act
        boolean expired = token.isExpired();

        // Assert
        assertFalse(expired);
    }

    @Test
    @DisplayName("Deve retornar verdadeiro quando o token estiver ativo")
    void isActive_tokenNaoExpirado_deveRetornarVerdadeiro() {
        // Arrange
        JwtToken token = new JwtToken(
                "header.payload.signature",
                Instant.now().plusSeconds(3600),
                TokenType.ACCESS
        );

        // Act
        boolean active = token.isActive();

        // Assert
        assertTrue(active);
    }

    @Test
    @DisplayName("Deve retornar falso quando o token estiver expirado")
    void isActive_tokenExpirado_deveRetornarFalso() {
        // Arrange
        JwtToken token = new JwtToken(
                "header.payload.signature",
                Instant.now().minusMillis(100),
                TokenType.ACCESS
        );

        // Act
        boolean active = token.isActive();

        // Assert
        assertFalse(active);
    }

    @Test
    @DisplayName("Deve calcular uma duração positiva para um token ainda ativo")
    void timeToExpire_tokenAtivo_deveRetornarTempoRestantePositivo() {
        // Arrange
        JwtToken token = new JwtToken(
                "header.payload.signature",
                Instant.now().plusSeconds(3600),
                TokenType.ACCESS
        );

        // Act
        Duration timeToExpire = token.timeToExpire();

        // Assert
        assertTrue(timeToExpire.compareTo(Duration.ZERO) > 0);
        assertTrue(timeToExpire.compareTo(Duration.ofHours(1)) <= 0);
    }

    @Test
    @DisplayName("Deve calcular uma duração negativa para um token expirado")
    void timeToExpire_tokenExpirado_deveRetornarTempoRestanteNegativo() {
        // Arrange
        JwtToken token = new JwtToken(
                "header.payload.signature",
                Instant.now().minusMillis(100),
                TokenType.ACCESS
        );

        // Act
        Duration timeToExpire = token.timeToExpire();

        // Assert
        assertTrue(timeToExpire.isNegative());
    }

    @Test
    @DisplayName("Deve retornar verdadeiro quando o token ativo expirar dentro do limite informado")
    void isAboutToExpire_tokenDentroDoLimite_deveRetornarVerdadeiro() {
        // Arrange
        JwtToken token = new JwtToken(
                "header.payload.signature",
                Instant.now().plusSeconds(30),
                TokenType.ACCESS
        );

        // Act
        boolean aboutToExpire = token.isAboutToExpire(Duration.ofMinutes(1));

        // Assert
        assertTrue(aboutToExpire);
    }

    @Test
    @DisplayName("Deve retornar falso quando o token ativo expirar depois do limite informado")
    void isAboutToExpire_tokenForaDoLimite_deveRetornarFalso() {
        // Arrange
        JwtToken token = new JwtToken(
                "header.payload.signature",
                Instant.now().plusSeconds(120),
                TokenType.ACCESS
        );

        // Act
        boolean aboutToExpire = token.isAboutToExpire(Duration.ofMinutes(1));

        // Assert
        assertFalse(aboutToExpire);
    }

    @Test
    @DisplayName("Deve retornar falso quando o token já estiver expirado")
    void isAboutToExpire_tokenExpirado_deveRetornarFalso() {
        // Arrange
        JwtToken token = new JwtToken(
                "header.payload.signature",
                Instant.now().minusMillis(100),
                TokenType.ACCESS
        );

        // Act
        boolean aboutToExpire = token.isAboutToExpire(Duration.ofMinutes(1));

        // Assert
        assertFalse(aboutToExpire);
    }

    @Test
    @DisplayName("Deve lançar NullPointerException quando o limite de expiração for nulo")
    void isAboutToExpire_limiteNulo_deveLancarNullPointerException() {
        // Arrange
        JwtToken token = new JwtToken(
                "header.payload.signature",
                Instant.now().plusSeconds(3600),
                TokenType.ACCESS
        );

        // Act
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> token.isAboutToExpire(null)
        );

        // Assert
        assertEquals("threshold must not be null", exception.getMessage());
    }

    @Test
    @DisplayName("Deve considerar o mesmo objeto igual a si próprio")
    void equals_mesmaInstancia_deveRetornarVerdadeiro() {
        // Arrange
        JwtToken token = new JwtToken(
                "header.payload.signature",
                Instant.now().plusSeconds(3600),
                TokenType.ACCESS
        );

        // Act
        boolean equal = token.equals(token);

        // Assert
        assertTrue(equal);
    }

    @Test
    @DisplayName("Deve considerar iguais tokens com o mesmo valor serializado")
    void equals_tokensComMesmoValor_deveRetornarVerdadeiro() {
        // Arrange
        JwtToken firstToken = new JwtToken(
                "header.payload.signature",
                Instant.now().plusSeconds(3600),
                TokenType.ACCESS
        );
        JwtToken secondToken = new JwtToken(
                "header.payload.signature",
                Instant.now().plusSeconds(7200),
                TokenType.REFRESH
        );

        // Act
        boolean equal = firstToken.equals(secondToken);

        // Assert
        assertTrue(equal);
    }

    @Test
    @DisplayName("Deve considerar diferentes tokens com valores serializados distintos")
    void equals_tokensComValoresDiferentes_deveRetornarFalso() {
        // Arrange
        JwtToken firstToken = new JwtToken(
                "first.header.payload.signature",
                Instant.now().plusSeconds(3600),
                TokenType.ACCESS
        );
        JwtToken secondToken = new JwtToken(
                "second.header.payload.signature",
                Instant.now().plusSeconds(3600),
                TokenType.ACCESS
        );

        // Act
        boolean equal = firstToken.equals(secondToken);

        // Assert
        assertFalse(equal);
    }

    @Test
    @DisplayName("Deve considerar o token diferente de nulo e de objetos de outro tipo")
    void equals_objetoIncompativel_deveRetornarFalso() {
        // Arrange
        JwtToken token = new JwtToken(
                "header.payload.signature",
                Instant.now().plusSeconds(3600),
                TokenType.ACCESS
        );

        // Act
        boolean equalToNull = token.equals(null);
        boolean equalToString = token.equals("header.payload.signature");

        // Assert
        assertAll(
                () -> assertFalse(equalToNull),
                () -> assertFalse(equalToString)
        );
    }

    @Test
    @DisplayName("Deve gerar o mesmo hash para tokens com o mesmo valor serializado")
    void hashCode_tokensComMesmoValor_deveRetornarMesmoHash() {
        // Arrange
        JwtToken firstToken = new JwtToken(
                "header.payload.signature",
                Instant.now().plusSeconds(3600),
                TokenType.ACCESS
        );
        JwtToken secondToken = new JwtToken(
                "header.payload.signature",
                Instant.now().plusSeconds(7200),
                TokenType.REFRESH
        );

        // Act
        int firstHash = firstToken.hashCode();
        int secondHash = secondToken.hashCode();

        // Assert
        assertEquals(firstHash, secondHash);
    }

    @Test
    @DisplayName("Deve mascarar tokens longos mantendo somente os primeiros doze caracteres")
    void toString_tokenLongo_deveMascararValor() {
        // Arrange
        String value = "header.payload.signature";
        Instant expiresAt = Instant.now().plusSeconds(3600);
        JwtToken token = new JwtToken(value, expiresAt, TokenType.ACCESS);
        String expected = "JwtToken{" +
                "value='" + value.substring(0, 12) + "...'" +
                ", expiresAt=" + expiresAt +
                ", type=" + TokenType.ACCESS +
                '}';

        // Act
        String representation = token.toString();

        // Assert
        assertEquals(expected, representation);
        assertFalse(representation.contains(value));
    }

    @Test
    @DisplayName("Deve ocultar completamente o valor de tokens com até doze caracteres")
    void toString_tokenCurto_deveOcultarCompletamenteValor() {
        // Arrange
        String value = "short-token";
        Instant expiresAt = Instant.now().plusSeconds(3600);
        JwtToken token = new JwtToken(value, expiresAt, TokenType.ACCESS);
        String expected = "JwtToken{" +
                "value='***'" +
                ", expiresAt=" + expiresAt +
                ", type=" + TokenType.ACCESS +
                '}';

        // Act
        String representation = token.toString();

        // Assert
        assertEquals(expected, representation);
        assertFalse(representation.contains(value));
    }
}
