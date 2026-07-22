package br.com.ecofy.auth.core.domain.keys;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("Testes unitários da chave ativa de assinatura")
class ActiveSigningKeyTest {

    @Test
    @DisplayName("Deve criar a chave ativa e disponibilizar somente os metadados não sensíveis")
    void constructor_dadosValidos_deveCriarChaveAtiva() {
        // Arrange
        SigningKeyMetadata metadata = mock(
                SigningKeyMetadata.class
        );

        RSAPrivateKey privateKey = mock(
                RSAPrivateKey.class
        );

        RSAPublicKey publicKey = mock(
                RSAPublicKey.class
        );

        String kid = "signing-key-001";

        when(metadata.status())
                .thenReturn(SigningKeyMetadata.Status.ACTIVE);

        when(metadata.kid())
                .thenReturn(kid);

        // Act
        ActiveSigningKey result = new ActiveSigningKey(
                metadata,
                privateKey,
                publicKey
        );

        ActiveSigningKey equivalentKey = new ActiveSigningKey(
                metadata,
                privateKey,
                publicKey
        );

        // Assert
        assertAll(
                () -> assertSame(
                        metadata,
                        result.metadata()
                ),
                () -> assertSame(
                        privateKey,
                        result.privateKey()
                ),
                () -> assertSame(
                        publicKey,
                        result.publicKey()
                ),
                () -> assertEquals(
                        kid,
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
                () -> assertEquals(
                        "ActiveSigningKey[kid="
                                + kid
                                + ", alg="
                                + metadata.algorithm()
                                + "]",
                        result.toString()
                ),
                () -> assertFalse(
                        result.toString().contains(
                                privateKey.toString()
                        )
                ),
                () -> assertFalse(
                        result.toString().contains(
                                publicKey.toString()
                        )
                )
        );
    }

    @Test
    @DisplayName("Deve considerar diferentes as chaves que possuem componentes distintos")
    void equals_componentesDiferentes_deveRetornarFalse() {
        // Arrange
        SigningKeyMetadata metadata = mock(
                SigningKeyMetadata.class
        );

        RSAPrivateKey privateKey = mock(
                RSAPrivateKey.class
        );

        RSAPublicKey publicKey = mock(
                RSAPublicKey.class
        );

        RSAPublicKey differentPublicKey = mock(
                RSAPublicKey.class
        );

        when(metadata.status())
                .thenReturn(SigningKeyMetadata.Status.ACTIVE);

        ActiveSigningKey signingKey = new ActiveSigningKey(
                metadata,
                privateKey,
                publicKey
        );

        ActiveSigningKey differentSigningKey =
                new ActiveSigningKey(
                        metadata,
                        privateKey,
                        differentPublicKey
                );

        // Act e Assert
        assertAll(
                () -> assertNotEquals(
                        differentSigningKey,
                        signingKey
                ),
                () -> assertNotEquals(
                        null,
                        signingKey
                ),
                () -> assertNotEquals(
                        "outro tipo",
                        signingKey
                )
        );
    }

    @Test
    @DisplayName("Deve rejeitar metadados nulos")
    void constructor_metadataNulo_deveLancarNullPointerException() {
        // Arrange
        RSAPrivateKey privateKey = mock(
                RSAPrivateKey.class
        );

        RSAPublicKey publicKey = mock(
                RSAPublicKey.class
        );

        // Act
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> new ActiveSigningKey(
                        null,
                        privateKey,
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
    @DisplayName("Deve rejeitar a chave privada nula")
    void constructor_chavePrivadaNula_deveLancarNullPointerException() {
        // Arrange
        SigningKeyMetadata metadata = mock(
                SigningKeyMetadata.class
        );

        RSAPublicKey publicKey = mock(
                RSAPublicKey.class
        );

        // Act
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> new ActiveSigningKey(
                        metadata,
                        null,
                        publicKey
                )
        );

        // Assert
        assertEquals(
                "privateKey must not be null",
                exception.getMessage()
        );
    }

    @Test
    @DisplayName("Deve rejeitar a chave pública nula")
    void constructor_chavePublicaNula_deveLancarNullPointerException() {
        // Arrange
        SigningKeyMetadata metadata = mock(
                SigningKeyMetadata.class
        );

        RSAPrivateKey privateKey = mock(
                RSAPrivateKey.class
        );

        // Act
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> new ActiveSigningKey(
                        metadata,
                        privateKey,
                        null
                )
        );

        // Assert
        assertEquals(
                "publicKey must not be null",
                exception.getMessage()
        );
    }

    @Test
    @DisplayName("Deve rejeitar metadados cujo status não esteja ativo")
    void constructor_statusNaoAtivo_deveLancarIllegalArgumentException() {
        // Arrange
        SigningKeyMetadata metadata = mock(
                SigningKeyMetadata.class
        );

        RSAPrivateKey privateKey = mock(
                RSAPrivateKey.class
        );

        RSAPublicKey publicKey = mock(
                RSAPublicKey.class
        );

        SigningKeyMetadata.Status nonActiveStatus =
                Arrays.stream(
                                SigningKeyMetadata.Status.values()
                        )
                        .filter(
                                status ->
                                        status
                                                != SigningKeyMetadata.Status.ACTIVE
                        )
                        .findFirst()
                        .orElseThrow();

        when(metadata.status())
                .thenReturn(nonActiveStatus);

        // Act
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new ActiveSigningKey(
                        metadata,
                        privateKey,
                        publicKey
                )
        );

        // Assert
        assertEquals(
                "active signing key must have status ACTIVE",
                exception.getMessage()
        );
    }
}
