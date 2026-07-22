package br.com.ecofy.auth.adapters.in.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.ecofy.auth.core.port.in.GetJwksUseCase;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
@DisplayName("Testes unitários do controlador JWKS")
class JwksControllerTest {

    @Mock
    private GetJwksUseCase getJwksUseCase;

    private JwksController controller;

    @BeforeEach
    void setUp() {
        controller = new JwksController(getJwksUseCase);
    }

    @Test
    @DisplayName("Deve retornar o documento JWKS e configurar cache público quando houver chaves")
    void jwks_chavesDisponiveis_deveRetornarDocumentoComCachePublico() {
        // Arrange
        Map<String, Object> firstKey = Map.of(
                "kid", "key-1",
                "kty", "RSA"
        );
        Map<String, Object> secondKey = Map.of(
                "kid", "key-2",
                "kty", "RSA"
        );
        Map<String, Object> jwks = Map.of(
                "keys",
                List.of(firstKey, secondKey)
        );

        when(getJwksUseCase.getJwks()).thenReturn(jwks);

        // Act
        ResponseEntity<Map<String, Object>> response =
                controller.jwks();

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(jwks, response.getBody());

        String cacheControl = response.getHeaders().getCacheControl();
        assertNotNull(cacheControl);
        assertTrue(cacheControl.contains("max-age=300"));
        assertTrue(cacheControl.contains("public"));

        verify(getJwksUseCase).getJwks();
    }

    @Test
    @DisplayName("Deve retornar o documento quando o campo keys não for uma coleção")
    void jwks_keysNaoColecao_deveRetornarDocumentoSemFalhar() {
        // Arrange
        Map<String, Object> jwks = Map.of(
                "keys",
                "invalid-value"
        );

        when(getJwksUseCase.getJwks()).thenReturn(jwks);

        // Act
        ResponseEntity<Map<String, Object>> response =
                controller.jwks();

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(jwks, response.getBody());
        assertTrue(
                response.getHeaders()
                        .getCacheControl()
                        .contains("max-age=300")
        );

        verify(getJwksUseCase).getJwks();
    }

    @Test
    @DisplayName("Deve retornar o documento quando o campo keys estiver ausente")
    void jwks_keysAusente_deveRetornarDocumentoSemFalhar() {
        // Arrange
        Map<String, Object> jwks = Map.of();

        when(getJwksUseCase.getJwks()).thenReturn(jwks);

        // Act
        ResponseEntity<Map<String, Object>> response =
                controller.jwks();

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(jwks, response.getBody());
        assertTrue(
                response.getHeaders()
                        .getCacheControl()
                        .contains("public")
        );

        verify(getJwksUseCase).getJwks();
    }

    @Test
    @DisplayName("Deve lançar exceção quando o caso de uso retornar um documento nulo")
    void jwks_documentoNulo_deveLancarNullPointerException() {
        // Arrange
        when(getJwksUseCase.getJwks()).thenReturn(null);

        // Act
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> controller.jwks()
        );

        // Assert
        assertNotNull(exception);
        verify(getJwksUseCase).getJwks();
    }

    @Test
    @DisplayName("Deve propagar a exceção quando a obtenção do documento JWKS falhar")
    void jwks_falhaNoCasoDeUso_devePropagarExcecao() {
        // Arrange
        IllegalStateException expectedException =
                new IllegalStateException("JWKS unavailable");

        when(getJwksUseCase.getJwks())
                .thenThrow(expectedException);

        // Act
        IllegalStateException actualException = assertThrows(
                IllegalStateException.class,
                () -> controller.jwks()
        );

        // Assert
        assertSame(expectedException, actualException);
        verify(getJwksUseCase).getJwks();
    }
}
