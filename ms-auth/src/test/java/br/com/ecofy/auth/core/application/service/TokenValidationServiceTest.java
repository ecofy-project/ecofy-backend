package br.com.ecofy.auth.core.application.service;

import br.com.ecofy.auth.core.application.exception.AuthErrorCode;
import br.com.ecofy.auth.core.application.exception.AuthException;
import br.com.ecofy.auth.core.port.out.JwtTokenProviderPort;
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
@DisplayName("Testes unitários do serviço de validação de tokens")
class TokenValidationServiceTest {

    @Mock
    private JwtTokenProviderPort jwtTokenProviderPort;

    @Test
    @DisplayName("Deve rejeitar a dependência nula recebida pelo construtor")
    void constructor_dependenciaNula_deveLancarNullPointerException() {
        // Arrange, Act e Assert
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> new TokenValidationService(null)
        );

        assertEquals(
                "jwtTokenProviderPort must not be null",
                exception.getMessage()
        );

        verifyNoInteractions(jwtTokenProviderPort);
    }

    @Test
    @DisplayName("Deve rejeitar o token nulo sem acessar o provedor JWT")
    void validate_tokenNulo_deveLancarNullPointerException() {
        // Arrange
        TokenValidationService service = createService();

        // Act
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> service.validate(null)
        );

        // Assert
        assertEquals(
                "token must not be null",
                exception.getMessage()
        );

        verifyNoInteractions(jwtTokenProviderPort);
    }

    @Test
    @DisplayName("Deve validar o token em branco e retornar as claims fornecidas pelo provedor")
    void validate_tokenEmBranco_deveRetornarClaims() {
        // Arrange
        TokenValidationService service = createService();
        String token = "   ";

        Map<String, Object> claims = Map.of(
                "sub",
                "user-id"
        );

        when(jwtTokenProviderPort.verifyAndParseClaims(token))
                .thenReturn(claims);

        // Act
        Map<String, Object> result = service.validate(token);

        // Assert
        assertSame(
                claims,
                result
        );

        verify(jwtTokenProviderPort)
                .verifyAndParseClaims(token);
    }

    @Test
    @DisplayName("Deve validar o token com exatamente doze caracteres e retornar as claims")
    void validate_tokenComDozeCaracteres_deveRetornarClaims() {
        // Arrange
        TokenValidationService service = createService();
        String token = "123456789012";

        Map<String, Object> claims = Map.of(
                "sub",
                "user-id"
        );

        when(jwtTokenProviderPort.verifyAndParseClaims(token))
                .thenReturn(claims);

        // Act
        Map<String, Object> result = service.validate(token);

        // Assert
        assertSame(
                claims,
                result
        );

        verify(jwtTokenProviderPort)
                .verifyAndParseClaims(token);
    }

    @Test
    @DisplayName("Deve validar o token com mais de doze caracteres e retornar todas as claims")
    void validate_tokenComMaisDeDozeCaracteres_deveRetornarClaims() {
        // Arrange
        TokenValidationService service = createService();
        String token = "valid-jwt-token-with-more-than-twelve-characters";

        Map<String, Object> claims = Map.of(
                "sub",
                "user-id",
                "email",
                "matheus@ecofy.com",
                "roles",
                List.of("ROLE_USER")
        );

        when(jwtTokenProviderPort.verifyAndParseClaims(token))
                .thenReturn(claims);

        // Act
        Map<String, Object> result = service.validate(token);

        // Assert
        assertAll(
                () -> assertSame(
                        claims,
                        result
                ),
                () -> assertEquals(
                        "user-id",
                        result.get("sub")
                ),
                () -> assertEquals(
                        "matheus@ecofy.com",
                        result.get("email")
                ),
                () -> assertEquals(
                        List.of("ROLE_USER"),
                        result.get("roles")
                )
        );

        verify(jwtTokenProviderPort)
                .verifyAndParseClaims(token);
    }

    @Test
    @DisplayName("Deve converter a falha de validação do provedor em erro de assinatura inválida")
    void validate_provedorLancaIllegalArgumentException_deveLancarAuthException() {
        // Arrange
        TokenValidationService service = createService();
        String token = "invalid-jwt-token";

        when(jwtTokenProviderPort.verifyAndParseClaims(token))
                .thenThrow(new IllegalArgumentException("Invalid signature"));

        // Act
        AuthException exception = assertThrows(
                AuthException.class,
                () -> service.validate(token)
        );

        // Assert
        assertAll(
                () -> assertEquals(
                        AuthErrorCode.INVALID_TOKEN_SIGNATURE,
                        exception.getErrorCode()
                ),
                () -> assertEquals(
                        "Invalid token",
                        exception.getMessage()
                )
        );

        verify(jwtTokenProviderPort)
                .verifyAndParseClaims(token);
    }

    @Test
    @DisplayName("Deve propagar exceção inesperada lançada pelo provedor JWT")
    void validate_provedorLancaExcecaoInesperada_devePropagarExcecao() {
        // Arrange
        TokenValidationService service = createService();
        String token = "valid-jwt-token";

        IllegalStateException expectedException =
                new IllegalStateException("JWT provider unavailable");

        when(jwtTokenProviderPort.verifyAndParseClaims(token))
                .thenThrow(expectedException);

        // Act
        IllegalStateException actualException = assertThrows(
                IllegalStateException.class,
                () -> service.validate(token)
        );

        // Assert
        assertSame(
                expectedException,
                actualException
        );

        verify(jwtTokenProviderPort)
                .verifyAndParseClaims(token);
    }

    private TokenValidationService createService() {
        return new TokenValidationService(
                jwtTokenProviderPort
        );
    }
}
