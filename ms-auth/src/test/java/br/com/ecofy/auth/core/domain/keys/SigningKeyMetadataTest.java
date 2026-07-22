package br.com.ecofy.auth.core.domain.keys;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Testes unitários dos metadados da chave de assinatura")
class SigningKeyMetadataTest {

    @Test
    @DisplayName("Deve criar os metadados e preservar todos os valores informados")
    void constructor_dadosValidos_deveCriarMetadados() {
        // Arrange
        String kid = "signing-key-001";
        String algorithm = "RS256";
        SigningKeyMetadata.Status status =
                SigningKeyMetadata.Status.ACTIVE;

        Instant activeFrom =
                Instant.parse("2026-07-22T12:00:00Z");

        Instant retireAt =
                Instant.parse("2026-08-22T12:00:00Z");

        Instant expiresAt =
                Instant.parse("2026-09-22T12:00:00Z");

        // Act
        SigningKeyMetadata metadata =
                new SigningKeyMetadata(
                        kid,
                        algorithm,
                        status,
                        activeFrom,
                        retireAt,
                        expiresAt
                );

        SigningKeyMetadata equivalentMetadata =
                new SigningKeyMetadata(
                        kid,
                        algorithm,
                        status,
                        activeFrom,
                        retireAt,
                        expiresAt
                );

        SigningKeyMetadata differentMetadata =
                new SigningKeyMetadata(
                        "signing-key-002",
                        algorithm,
                        status,
                        activeFrom,
                        retireAt,
                        expiresAt
                );

        // Assert
        assertAll(
                () -> assertEquals(
                        kid,
                        metadata.kid()
                ),
                () -> assertEquals(
                        algorithm,
                        metadata.algorithm()
                ),
                () -> assertEquals(
                        status,
                        metadata.status()
                ),
                () -> assertEquals(
                        activeFrom,
                        metadata.activeFrom()
                ),
                () -> assertEquals(
                        retireAt,
                        metadata.retireAt()
                ),
                () -> assertEquals(
                        expiresAt,
                        metadata.expiresAt()
                ),
                () -> assertEquals(
                        equivalentMetadata,
                        metadata
                ),
                () -> assertEquals(
                        equivalentMetadata.hashCode(),
                        metadata.hashCode()
                ),
                () -> assertNotEquals(
                        differentMetadata,
                        metadata
                ),
                () -> assertNotEquals(
                        null,
                        metadata
                ),
                () -> assertNotEquals(
                        "outro tipo",
                        metadata
                ),
                () -> assertTrue(
                        metadata.toString().contains(kid)
                ),
                () -> assertTrue(
                        metadata.toString().contains(algorithm)
                )
        );
    }

    @Test
    @DisplayName("Deve aceitar instantes opcionais nulos")
    void constructor_instantesOpcionaisNulos_deveCriarMetadados() {
        // Arrange e Act
        SigningKeyMetadata metadata =
                new SigningKeyMetadata(
                        "signing-key-001",
                        "RS256",
                        SigningKeyMetadata.Status.ACTIVE,
                        null,
                        null,
                        null
                );

        // Assert
        assertAll(
                () -> assertNull(metadata.activeFrom()),
                () -> assertNull(metadata.retireAt()),
                () -> assertNull(metadata.expiresAt())
        );
    }

    @Test
    @DisplayName("Deve rejeitar o identificador nulo")
    void constructor_kidNulo_deveLancarNullPointerException() {
        // Arrange, Act e Assert
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> new SigningKeyMetadata(
                        null,
                        "RS256",
                        SigningKeyMetadata.Status.ACTIVE,
                        null,
                        null,
                        null
                )
        );

        assertEquals(
                "kid must not be null",
                exception.getMessage()
        );
    }

    @Test
    @DisplayName("Deve rejeitar o algoritmo nulo")
    void constructor_algorithmNulo_deveLancarNullPointerException() {
        // Arrange, Act e Assert
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> new SigningKeyMetadata(
                        "signing-key-001",
                        null,
                        SigningKeyMetadata.Status.ACTIVE,
                        null,
                        null,
                        null
                )
        );

        assertEquals(
                "algorithm must not be null",
                exception.getMessage()
        );
    }

    @Test
    @DisplayName("Deve rejeitar o status nulo")
    void constructor_statusNulo_deveLancarNullPointerException() {
        // Arrange, Act e Assert
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> new SigningKeyMetadata(
                        "signing-key-001",
                        "RS256",
                        null,
                        null,
                        null,
                        null
                )
        );

        assertEquals(
                "status must not be null",
                exception.getMessage()
        );
    }

    @Test
    @DisplayName("Deve rejeitar o identificador vazio")
    void constructor_kidVazio_deveLancarIllegalArgumentException() {
        // Arrange, Act e Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new SigningKeyMetadata(
                        "",
                        "RS256",
                        SigningKeyMetadata.Status.ACTIVE,
                        null,
                        null,
                        null
                )
        );

        assertEquals(
                "kid must not be blank",
                exception.getMessage()
        );
    }

    @Test
    @DisplayName("Deve rejeitar o identificador composto somente por espaços")
    void constructor_kidComEspacos_deveLancarIllegalArgumentException() {
        // Arrange, Act e Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new SigningKeyMetadata(
                        "   ",
                        "RS256",
                        SigningKeyMetadata.Status.ACTIVE,
                        null,
                        null,
                        null
                )
        );

        assertEquals(
                "kid must not be blank",
                exception.getMessage()
        );
    }

    @Test
    @DisplayName("Deve considerar válida para verificação uma chave sem expiração")
    void isValidForVerificationAt_expiracaoNula_deveRetornarTrue() {
        // Arrange
        SigningKeyMetadata metadata = createMetadata(null);

        // Act
        boolean result = metadata.isValidForVerificationAt(
                Instant.parse("2026-07-22T12:00:00Z")
        );

        // Assert
        assertTrue(result);
    }

    @Test
    @DisplayName("Deve considerar válida uma chave antes do instante de expiração")
    void isValidForVerificationAt_antesDaExpiracao_deveRetornarTrue() {
        // Arrange
        Instant expiresAt =
                Instant.parse("2026-07-22T13:00:00Z");

        SigningKeyMetadata metadata =
                createMetadata(expiresAt);

        // Act
        boolean result = metadata.isValidForVerificationAt(
                Instant.parse("2026-07-22T12:59:59Z")
        );

        // Assert
        assertTrue(result);
    }

    @Test
    @DisplayName("Deve considerar inválida uma chave exatamente no instante de expiração")
    void isValidForVerificationAt_noInstanteDaExpiracao_deveRetornarFalse() {
        // Arrange
        Instant expiresAt =
                Instant.parse("2026-07-22T13:00:00Z");

        SigningKeyMetadata metadata =
                createMetadata(expiresAt);

        // Act
        boolean result =
                metadata.isValidForVerificationAt(expiresAt);

        // Assert
        assertFalse(result);
    }

    @Test
    @DisplayName("Deve considerar inválida uma chave após o instante de expiração")
    void isValidForVerificationAt_aposAExpiracao_deveRetornarFalse() {
        // Arrange
        Instant expiresAt =
                Instant.parse("2026-07-22T13:00:00Z");

        SigningKeyMetadata metadata =
                createMetadata(expiresAt);

        // Act
        boolean result = metadata.isValidForVerificationAt(
                Instant.parse("2026-07-22T13:00:01Z")
        );

        // Assert
        assertFalse(result);
    }

    @Test
    @DisplayName("Deve lançar exceção quando o instante atual for nulo e houver expiração")
    void isValidForVerificationAt_instanteAtualNulo_deveLancarNullPointerException() {
        // Arrange
        SigningKeyMetadata metadata = createMetadata(
                Instant.parse("2026-07-22T13:00:00Z")
        );

        // Act e Assert
        assertThrows(
                NullPointerException.class,
                () -> metadata.isValidForVerificationAt(null)
        );
    }

    private SigningKeyMetadata createMetadata(
            Instant expiresAt
    ) {
        return new SigningKeyMetadata(
                "signing-key-001",
                "RS256",
                SigningKeyMetadata.Status.ACTIVE,
                Instant.parse("2026-07-22T12:00:00Z"),
                null,
                expiresAt
        );
    }
}
