package br.com.ecofy.auth.core.application.service;

import br.com.ecofy.auth.core.application.exception.AuthErrorCode;
import br.com.ecofy.auth.core.application.exception.AuthException;
import br.com.ecofy.auth.core.port.out.PublicSigningKeyProviderPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Testes unitários do serviço de geração do documento JWKS")
class JwksServiceTest {

    @Mock
    private PublicSigningKeyProviderPort publicSigningKeyProviderPort;

    @Test
    @DisplayName("Deve rejeitar o provedor de chaves públicas nulo")
    void constructor_provedorNulo_deveLancarNullPointerException() {
        // Arrange e Act
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> new JwksService(null)
        );

        // Assert
        assertEquals(
                "publicSigningKeyProviderPort must not be null",
                exception.getMessage()
        );

        verifyNoInteractions(publicSigningKeyProviderPort);
    }

    @Test
    @DisplayName("Deve retornar o documento JWKS quando houver chave pública disponível")
    void getJwks_chavePublicaDisponivel_deveRetornarDocumentoJwks() {
        // Arrange
        JwksService service = createService();

        Map<String, Object> jwk = Map.of(
                "kty", "RSA",
                "kid", "ecofy-key-1",
                "use", "sig",
                "alg", "RS256",
                "n", "modulus",
                "e", "AQAB"
        );

        List<Map<String, Object>> jwkList = List.of(jwk);

        when(publicSigningKeyProviderPort.currentPublicJwks())
                .thenReturn(jwkList);

        // Act
        Map<String, Object> result = service.getJwks();

        // Assert
        assertAll(
                () -> assertEquals(
                        1,
                        result.size()
                ),
                () -> assertSame(
                        jwkList,
                        result.get("keys")
                ),
                () -> assertEquals(
                        List.of(jwk),
                        result.get("keys")
                )
        );

        verify(publicSigningKeyProviderPort)
                .currentPublicJwks();
    }

    @Test
    @DisplayName("Deve lançar erro de indisponibilidade quando o provedor retornar nulo")
    void getJwks_listaNula_deveLancarAuthException() {
        // Arrange
        JwksService service = createService();

        when(publicSigningKeyProviderPort.currentPublicJwks())
                .thenReturn(null);

        // Act
        AuthException exception = assertThrows(
                AuthException.class,
                service::getJwks
        );

        // Assert
        assertJwksNotAvailableException(exception);

        verify(publicSigningKeyProviderPort)
                .currentPublicJwks();
    }

    @Test
    @DisplayName("Deve lançar erro de indisponibilidade quando não houver chaves públicas")
    void getJwks_listaVazia_deveLancarAuthException() {
        // Arrange
        JwksService service = createService();

        when(publicSigningKeyProviderPort.currentPublicJwks())
                .thenReturn(List.of());

        // Act
        AuthException exception = assertThrows(
                AuthException.class,
                service::getJwks
        );

        // Assert
        assertJwksNotAvailableException(exception);

        verify(publicSigningKeyProviderPort)
                .currentPublicJwks();
    }

    @Test
    @DisplayName("Deve propagar a exceção quando o provedor de chaves públicas falhar")
    void getJwks_provedorLancaExcecao_devePropagarExcecao() {
        // Arrange
        JwksService service = createService();

        IllegalStateException expectedException =
                new IllegalStateException(
                        "Signing key provider unavailable"
                );

        when(publicSigningKeyProviderPort.currentPublicJwks())
                .thenThrow(expectedException);

        // Act
        IllegalStateException actualException = assertThrows(
                IllegalStateException.class,
                service::getJwks
        );

        // Assert
        assertSame(
                expectedException,
                actualException
        );

        verify(publicSigningKeyProviderPort)
                .currentPublicJwks();
    }

    private JwksService createService() {
        return new JwksService(
                publicSigningKeyProviderPort
        );
    }

    private void assertJwksNotAvailableException(
            AuthException exception
    ) {
        assertAll(
                () -> assertEquals(
                        AuthErrorCode.JWKS_NOT_AVAILABLE,
                        exception.getErrorCode()
                ),
                () -> assertEquals(
                        "No active signing keys available",
                        exception.getMessage()
                )
        );
    }
}
