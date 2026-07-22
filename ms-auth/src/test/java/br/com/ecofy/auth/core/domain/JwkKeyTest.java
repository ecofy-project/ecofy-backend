package br.com.ecofy.auth.core.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Testes unitários da chave pública JWK")
class JwkKeyTest {

    private static final Instant CREATED_AT =
            Instant.parse("2026-07-22T10:00:00Z");

    private static final String PUBLIC_KEY_PEM =
            "-----BEGIN PUBLIC KEY-----\npublic-key-content\n-----END PUBLIC KEY-----";

    @Test
    @DisplayName("Deve criar uma chave de assinatura normalizando o algoritmo e o uso")
    void constructor_dadosValidosDeAssinatura_deveCriarChaveNormalizada() {
        // Arrange e Act
        JwkKey key = new JwkKey(
                "key-1",
                PUBLIC_KEY_PEM,
                "  rs256  ",
                "  SIG  ",
                CREATED_AT,
                true
        );

        // Assert
        assertAll(
                () -> assertEquals("key-1", key.keyId()),
                () -> assertEquals(
                        PUBLIC_KEY_PEM,
                        key.publicKeyPem()
                ),
                () -> assertEquals("RS256", key.algorithm()),
                () -> assertEquals(
                        JwkKey.USE_SIGNING,
                        key.use()
                ),
                () -> assertEquals(CREATED_AT, key.createdAt()),
                () -> assertTrue(key.active()),
                () -> assertTrue(key.isSigningKey()),
                () -> assertFalse(key.isEncryptionKey())
        );
    }

    @Test
    @DisplayName("Deve criar uma chave de criptografia inativa normalizando o uso")
    void constructor_dadosValidosDeCriptografia_deveCriarChaveNormalizada() {
        // Arrange e Act
        JwkKey key = new JwkKey(
                "key-2",
                PUBLIC_KEY_PEM,
                "rsa-oaep",
                "  ENC  ",
                CREATED_AT,
                false
        );

        // Assert
        assertAll(
                () -> assertEquals("RSA-OAEP", key.algorithm()),
                () -> assertEquals(
                        JwkKey.USE_ENCRYPTION,
                        key.use()
                ),
                () -> assertFalse(key.active()),
                () -> assertFalse(key.isSigningKey()),
                () -> assertTrue(key.isEncryptionKey())
        );
    }

    @Test
    @DisplayName("Deve aplicar o uso de assinatura quando o uso for nulo")
    void constructor_usoNulo_deveAplicarUsoDeAssinatura() {
        // Arrange e Act
        JwkKey key = new JwkKey(
                "key-1",
                PUBLIC_KEY_PEM,
                "RS256",
                null,
                CREATED_AT,
                true
        );

        // Assert
        assertAll(
                () -> assertEquals(
                        JwkKey.USE_SIGNING,
                        key.use()
                ),
                () -> assertTrue(key.isSigningKey()),
                () -> assertFalse(key.isEncryptionKey())
        );
    }

    @Test
    @DisplayName("Deve aplicar o uso de assinatura quando o uso estiver em branco")
    void constructor_usoEmBranco_deveAplicarUsoDeAssinatura() {
        // Arrange e Act
        JwkKey key = new JwkKey(
                "key-1",
                PUBLIC_KEY_PEM,
                "RS256",
                "   ",
                CREATED_AT,
                true
        );

        // Assert
        assertAll(
                () -> assertEquals(
                        JwkKey.USE_SIGNING,
                        key.use()
                ),
                () -> assertTrue(key.isSigningKey())
        );
    }

    @Test
    @DisplayName("Deve rejeitar um identificador de chave nulo")
    void constructor_keyIdNulo_deveLancarNullPointerException() {
        // Arrange, Act e Assert
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> new JwkKey(
                        null,
                        PUBLIC_KEY_PEM,
                        "RS256",
                        JwkKey.USE_SIGNING,
                        CREATED_AT,
                        true
                )
        );

        assertEquals(
                "keyId must not be null",
                exception.getMessage()
        );
    }

    @Test
    @DisplayName("Deve rejeitar uma chave pública PEM nula")
    void constructor_publicKeyPemNulo_deveLancarNullPointerException() {
        // Arrange, Act e Assert
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> new JwkKey(
                        "key-1",
                        null,
                        "RS256",
                        JwkKey.USE_SIGNING,
                        CREATED_AT,
                        true
                )
        );

        assertEquals(
                "publicKeyPem must not be null",
                exception.getMessage()
        );
    }

    @Test
    @DisplayName("Deve rejeitar um algoritmo nulo")
    void constructor_algoritmoNulo_deveLancarNullPointerException() {
        // Arrange, Act e Assert
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> new JwkKey(
                        "key-1",
                        PUBLIC_KEY_PEM,
                        null,
                        JwkKey.USE_SIGNING,
                        CREATED_AT,
                        true
                )
        );

        assertEquals(
                "algorithm must not be null",
                exception.getMessage()
        );
    }

    @Test
    @DisplayName("Deve rejeitar um algoritmo em branco")
    void constructor_algoritmoEmBranco_deveLancarIllegalArgumentException() {
        // Arrange, Act e Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new JwkKey(
                        "key-1",
                        PUBLIC_KEY_PEM,
                        "   ",
                        JwkKey.USE_SIGNING,
                        CREATED_AT,
                        true
                )
        );

        assertEquals(
                "algorithm must not be blank",
                exception.getMessage()
        );
    }

    @Test
    @DisplayName("Deve rejeitar um uso diferente de assinatura e criptografia")
    void constructor_usoInvalido_deveLancarIllegalArgumentException() {
        // Arrange, Act e Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new JwkKey(
                        "key-1",
                        PUBLIC_KEY_PEM,
                        "RS256",
                        "auth",
                        CREATED_AT,
                        true
                )
        );

        assertEquals(
                "Invalid JWK use: auth. Expected 'sig' or 'enc'.",
                exception.getMessage()
        );
    }

    @Test
    @DisplayName("Deve rejeitar uma data de criação nula")
    void constructor_createdAtNulo_deveLancarNullPointerException() {
        // Arrange, Act e Assert
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> new JwkKey(
                        "key-1",
                        PUBLIC_KEY_PEM,
                        "RS256",
                        JwkKey.USE_SIGNING,
                        null,
                        true
                )
        );

        assertEquals(
                "createdAt must not be null",
                exception.getMessage()
        );
    }

    @Test
    @DisplayName("Deve considerar a própria instância igual a ela mesma")
    void equals_mesmaInstancia_deveRetornarTrue() {
        // Arrange
        JwkKey key = createKey(
                "key-1",
                JwkKey.USE_SIGNING
        );

        // Act
        boolean result = key.equals(key);

        // Assert
        assertTrue(result);
    }

    @Test
    @DisplayName("Deve considerar a chave diferente de valor nulo e de outro tipo")
    void equals_objetoNuloOuDeOutroTipo_deveRetornarFalse() {
        // Arrange
        JwkKey key = createKey(
                "key-1",
                JwkKey.USE_SIGNING
        );

        // Act
        boolean equalsNull = key.equals(null);
        boolean equalsOtherType = key.equals("key-1");

        // Assert
        assertAll(
                () -> assertFalse(equalsNull),
                () -> assertFalse(equalsOtherType)
        );
    }

    @Test
    @DisplayName("Deve considerar iguais duas chaves com o mesmo identificador")
    void equals_mesmoKeyId_deveRetornarTrue() {
        // Arrange
        JwkKey firstKey = new JwkKey(
                "key-1",
                PUBLIC_KEY_PEM,
                "RS256",
                JwkKey.USE_SIGNING,
                CREATED_AT,
                true
        );

        JwkKey secondKey = new JwkKey(
                "key-1",
                "outro-pem",
                "RSA-OAEP",
                JwkKey.USE_ENCRYPTION,
                CREATED_AT.plusSeconds(60),
                false
        );

        // Act
        boolean result = firstKey.equals(secondKey);

        // Assert
        assertAll(
                () -> assertTrue(result),
                () -> assertTrue(secondKey.equals(firstKey)),
                () -> assertEquals(
                        firstKey.hashCode(),
                        secondKey.hashCode()
                )
        );
    }

    @Test
    @DisplayName("Deve considerar diferentes duas chaves com identificadores distintos")
    void equals_keyIdsDiferentes_deveRetornarFalse() {
        // Arrange
        JwkKey firstKey = createKey(
                "key-1",
                JwkKey.USE_SIGNING
        );

        JwkKey secondKey = createKey(
                "key-2",
                JwkKey.USE_SIGNING
        );

        // Act
        boolean result = firstKey.equals(secondKey);

        // Assert
        assertAll(
                () -> assertFalse(result),
                () -> assertNotEquals(
                        firstKey.hashCode(),
                        secondKey.hashCode()
                )
        );
    }

    @Test
    @DisplayName("Deve gerar representação textual sem expor a chave pública PEM")
    void toString_chaveValida_deveRetornarDadosSemExporPem() {
        // Arrange
        JwkKey key = createKey(
                "key-1",
                JwkKey.USE_SIGNING
        );

        // Act
        String result = key.toString();

        // Assert
        assertAll(
                () -> assertEquals(
                        "JwkKey{" +
                                "keyId='key-1'" +
                                ", algorithm='RS256'" +
                                ", use='sig'" +
                                ", active=true" +
                                ", createdAt=2026-07-22T10:00:00Z" +
                                '}',
                        result
                ),
                () -> assertFalse(result.contains(PUBLIC_KEY_PEM)),
                () -> assertFalse(result.contains("publicKeyPem"))
        );
    }

    private JwkKey createKey(String keyId, String use) {
        return new JwkKey(
                keyId,
                PUBLIC_KEY_PEM,
                "RS256",
                use,
                CREATED_AT,
                true
        );
    }
}
