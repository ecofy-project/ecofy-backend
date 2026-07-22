package br.com.ecofy.auth.core.domain.keys;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.security.interfaces.RSAPublicKey;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

@DisplayName("Testes unitários da chave pública de verificação")
class VerificationKeyTest {

    @Test
    @DisplayName("Deve criar a chave de verificação e preservar os componentes informados")
    void constructor_dadosValidos_deveCriarChaveDeVerificacao() {
        // Arrange
        SigningKeyMetadata metadata =
                createMetadata("verification-key-001");

        RSAPublicKey publicKey =
                mock(RSAPublicKey.class);

        // Act
        VerificationKey result =
                new VerificationKey(
                        metadata,
                        publicKey
                );

        VerificationKey equivalentKey =
                new VerificationKey(
                        metadata,
                        publicKey
                );

        // Assert
        assertAll(
                () -> assertSame(
                        metadata,
                        result.metadata()
                ),
                () -> assertSame(
                        publicKey,
                        result.publicKey()
                ),
                () -> assertEquals(
                        metadata.kid(),
                        result.kid()
                ),
                () -> assertEquals(
                        equivalentKey,
                        result
                ),
                () -> assertEquals(
                        equivalentKey.hashCode(),
                        result.hashCode()
                ),
                () -> assertTrue(
                        result.toString()
                                .contains(metadata.toString())
                )
        );
    }

    @Test
    @DisplayName("Deve considerar diferentes as chaves que possuem componentes distintos")
    void equals_componentesDiferentes_deveRetornarFalse() {
        // Arrange
        SigningKeyMetadata metadata =
                createMetadata("verification-key-001");

        RSAPublicKey publicKey =
                mock(RSAPublicKey.class);

        RSAPublicKey differentPublicKey =
                mock(RSAPublicKey.class);

        VerificationKey verificationKey =
                new VerificationKey(
                        metadata,
                        publicKey
                );

        VerificationKey differentVerificationKey =
                new VerificationKey(
                        metadata,
                        differentPublicKey
                );

        // Act e Assert
        assertAll(
                () -> assertNotEquals(
                        differentVerificationKey,
                        verificationKey
                ),
                () -> assertNotEquals(
                        null,
                        verificationKey
                ),
                () -> assertNotEquals(
                        "outro tipo",
                        verificationKey
                )
        );
    }

    @Test
    @DisplayName("Deve rejeitar metadados nulos")
    void constructor_metadataNulo_deveLancarNullPointerException() {
        // Arrange
        RSAPublicKey publicKey =
                mock(RSAPublicKey.class);

        // Act
        NullPointerException exception =
                assertThrows(
                        NullPointerException.class,
                        () -> new VerificationKey(
                                null,
                                publicKey
                        )
                );

        // Assert
        assertEquals(
                "metadata must not be null",
                exception.getMessage()
        );
    }

    @Test
    @DisplayName("Deve rejeitar a chave pública nula")
    void constructor_chavePublicaNula_deveLancarNullPointerException() {
        // Arrange
        SigningKeyMetadata metadata =
                createMetadata("verification-key-001");

        // Act
        NullPointerException exception =
                assertThrows(
                        NullPointerException.class,
                        () -> new VerificationKey(
                                metadata,
                                null
                        )
                );

        // Assert
        assertEquals(
                "publicKey must not be null",
                exception.getMessage()
        );
    }

    private SigningKeyMetadata createMetadata(String kid) {
        return new SigningKeyMetadata(
                kid,
                "RS256",
                SigningKeyMetadata.Status.ACTIVE,
                Instant.parse("2026-07-22T12:00:00Z"),
                null,
                null
        );
    }
}
