package br.com.ecofy.auth.core.application.service;

import br.com.ecofy.auth.core.application.exception.AuthErrorCode;
import br.com.ecofy.auth.core.application.exception.AuthException;
import br.com.ecofy.auth.core.domain.bruteforce.BlockStatus;
import br.com.ecofy.auth.core.port.in.AuthenticateUserUseCase;
import br.com.ecofy.auth.core.port.out.BruteForceProtectionPort;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Testes unitários do serviço de autenticação protegido contra força bruta")
class BruteForceProtectedAuthenticationServiceTest {

    private static final String METRIC_NAME = "ecofy.auth.login";

    @Mock
    private AuthService delegate;

    @Mock
    private BruteForceProtectionPort bruteForceProtectionPort;

    @Mock
    private MeterRegistry meterRegistry;

    @Mock
    private Counter counter;

    @Test
    @DisplayName("Deve rejeitar dependências nulas recebidas pelo construtor")
    void constructor_dependenciasNulas_deveLancarNullPointerException() {
        // Arrange, Act e Assert
        assertAll(
                () -> assertNullDependency(
                        "delegate must not be null",
                        () -> new BruteForceProtectedAuthenticationService(
                                null,
                                bruteForceProtectionPort,
                                meterRegistry
                        )
                ),
                () -> assertNullDependency(
                        "bruteForceProtectionPort must not be null",
                        () -> new BruteForceProtectedAuthenticationService(
                                delegate,
                                null,
                                meterRegistry
                        )
                ),
                () -> assertNullDependency(
                        "meterRegistry must not be null",
                        () -> new BruteForceProtectedAuthenticationService(
                                delegate,
                                bruteForceProtectionPort,
                                null
                        )
                )
        );
    }

    @Test
    @DisplayName("Deve rejeitar o comando de autenticação nulo")
    void authenticate_comandoNulo_deveLancarNullPointerException() {
        // Arrange
        BruteForceProtectedAuthenticationService service =
                createService();

        // Act
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> service.authenticate(null)
        );

        // Assert
        assertEquals(
                "command must not be null",
                exception.getMessage()
        );

        verifyNoInteractions(
                delegate,
                bruteForceProtectionPort,
                meterRegistry
        );
    }

    @Test
    @DisplayName("Deve bloquear a autenticação e registrar a métrica quando a proteção estiver ativa")
    void authenticate_protecaoBloqueada_deveRegistrarMetricaELancarAuthException() {
        // Arrange
        BruteForceProtectedAuthenticationService service =
                createService();

        AuthenticateUserUseCase.AuthenticationCommand command =
                mock(AuthenticateUserUseCase.AuthenticationCommand.class);

        BlockStatus status = mock(BlockStatus.class);

        String expectedKey =
                "user:" + sha256("") + "|ip:unknown";

        when(command.username())
                .thenReturn(null);

        when(command.ipAddress())
                .thenReturn(null);

        when(command.clientId())
                .thenReturn("ecofy-web");

        when(bruteForceProtectionPort.status(expectedKey))
                .thenReturn(status);

        when(status.blocked())
                .thenReturn(true);

        when(
                meterRegistry.counter(
                        METRIC_NAME,
                        "outcome",
                        "blocked"
                )
        ).thenReturn(counter);

        // Act
        AuthException exception = assertThrows(
                AuthException.class,
                () -> service.authenticate(command)
        );

        // Assert
        assertAll(
                () -> assertEquals(
                        AuthErrorCode.AUTHENTICATION_TEMPORARILY_BLOCKED,
                        exception.getErrorCode()
                ),
                () -> assertEquals(
                        "Too many failed attempts. Please try again later.",
                        exception.getMessage()
                )
        );

        verify(bruteForceProtectionPort)
                .status(expectedKey);

        verify(counter)
                .increment();

        verifyNoInteractions(delegate);

        verify(bruteForceProtectionPort, never())
                .reset(expectedKey);

        verify(bruteForceProtectionPort, never())
                .registerFailure(expectedKey);
    }

    @Test
    @DisplayName("Deve normalizar usuário e IP, delegar a autenticação e registrar o sucesso")
    void authenticate_dadosValidos_deveResetarProtecaoERegistrarSucesso() {
        // Arrange
        BruteForceProtectedAuthenticationService service =
                createService();

        AuthenticateUserUseCase.AuthenticationCommand command =
                mock(AuthenticateUserUseCase.AuthenticationCommand.class);

        AuthenticateUserUseCase.AuthenticationResult expectedResult =
                mock(AuthenticateUserUseCase.AuthenticationResult.class);

        BlockStatus status = mock(BlockStatus.class);

        String expectedKey =
                "user:"
                        + sha256("user@ecofy.com")
                        + "|ip:192.168.0.10";

        when(command.username())
                .thenReturn("  User@Ecofy.COM  ");

        when(command.ipAddress())
                .thenReturn("  192.168.0.10  ");

        when(bruteForceProtectionPort.status(expectedKey))
                .thenReturn(status);

        when(status.blocked())
                .thenReturn(false);

        when(delegate.authenticate(command))
                .thenReturn(expectedResult);

        when(
                meterRegistry.counter(
                        METRIC_NAME,
                        "outcome",
                        "success"
                )
        ).thenReturn(counter);

        // Act
        AuthenticateUserUseCase.AuthenticationResult result =
                service.authenticate(command);

        // Assert
        assertSame(
                expectedResult,
                result
        );

        verify(bruteForceProtectionPort)
                .status(expectedKey);

        verify(delegate)
                .authenticate(command);

        verify(bruteForceProtectionPort)
                .reset(expectedKey);

        verify(counter)
                .increment();

        verify(bruteForceProtectionPort, never())
                .registerFailure(expectedKey);
    }

    @Test
    @DisplayName("Deve registrar a falha e usar IP desconhecido quando as credenciais forem inválidas e o IP estiver em branco")
    void authenticate_credenciaisInvalidasComIpEmBranco_deveRegistrarFalhaEMetrica() {
        // Arrange
        BruteForceProtectedAuthenticationService service =
                createService();

        AuthenticateUserUseCase.AuthenticationCommand command =
                mock(AuthenticateUserUseCase.AuthenticationCommand.class);

        BlockStatus status = mock(BlockStatus.class);

        AuthException expectedException = new AuthException(
                AuthErrorCode.INVALID_CREDENTIALS,
                "Invalid credentials"
        );

        String expectedKey =
                "user:"
                        + sha256("user@ecofy.com")
                        + "|ip:unknown";

        when(command.username())
                .thenReturn("USER@ECOFY.COM");

        when(command.ipAddress())
                .thenReturn("   ");

        when(bruteForceProtectionPort.status(expectedKey))
                .thenReturn(status);

        when(status.blocked())
                .thenReturn(false);

        when(delegate.authenticate(command))
                .thenThrow(expectedException);

        when(
                meterRegistry.counter(
                        METRIC_NAME,
                        "outcome",
                        "failure"
                )
        ).thenReturn(counter);

        // Act
        AuthException actualException = assertThrows(
                AuthException.class,
                () -> service.authenticate(command)
        );

        // Assert
        assertSame(
                expectedException,
                actualException
        );

        verify(bruteForceProtectionPort)
                .registerFailure(expectedKey);

        verify(counter)
                .increment();

        verify(bruteForceProtectionPort, never())
                .reset(expectedKey);
    }

    @Test
    @DisplayName("Deve propagar exceção diferente de credenciais inválidas sem registrar falha")
    void authenticate_authExceptionDiferenteDeCredenciaisInvalidas_deveApenasPropagarExcecao() {
        // Arrange
        BruteForceProtectedAuthenticationService service =
                createService();

        AuthenticateUserUseCase.AuthenticationCommand command =
                mock(AuthenticateUserUseCase.AuthenticationCommand.class);

        BlockStatus status = mock(BlockStatus.class);

        AuthException expectedException = new AuthException(
                AuthErrorCode.AUTHENTICATION_TEMPORARILY_BLOCKED,
                "Authentication unavailable"
        );

        String expectedKey =
                "user:"
                        + sha256("user@ecofy.com")
                        + "|ip:127.0.0.1";

        when(command.username())
                .thenReturn("user@ecofy.com");

        when(command.ipAddress())
                .thenReturn("127.0.0.1");

        when(bruteForceProtectionPort.status(expectedKey))
                .thenReturn(status);

        when(status.blocked())
                .thenReturn(false);

        when(delegate.authenticate(command))
                .thenThrow(expectedException);

        // Act
        AuthException actualException = assertThrows(
                AuthException.class,
                () -> service.authenticate(command)
        );

        // Assert
        assertSame(
                expectedException,
                actualException
        );

        verify(bruteForceProtectionPort, never())
                .registerFailure(expectedKey);

        verify(bruteForceProtectionPort, never())
                .reset(expectedKey);

        verifyNoInteractions(meterRegistry);
    }

    @Test
    @DisplayName("Deve lançar IllegalStateException quando o algoritmo SHA-256 não estiver disponível")
    void authenticate_sha256Indisponivel_deveLancarIllegalStateException() {
        // Arrange
        BruteForceProtectedAuthenticationService service =
                createService();

        AuthenticateUserUseCase.AuthenticationCommand command =
                mock(AuthenticateUserUseCase.AuthenticationCommand.class);

        when(command.username())
                .thenReturn("user@ecofy.com");

        when(command.ipAddress())
                .thenReturn("127.0.0.1");

        NoSuchAlgorithmException cause =
                new NoSuchAlgorithmException(
                        "Algoritmo indisponível"
                );

        try (
                MockedStatic<MessageDigest> messageDigestMock =
                        mockStatic(MessageDigest.class)
        ) {
            messageDigestMock.when(
                    () -> MessageDigest.getInstance("SHA-256")
            ).thenThrow(cause);

            // Act
            IllegalStateException exception = assertThrows(
                    IllegalStateException.class,
                    () -> service.authenticate(command)
            );

            // Assert
            assertEquals(
                    "SHA-256 not available",
                    exception.getMessage()
            );

            assertSame(
                    cause,
                    exception.getCause()
            );

            messageDigestMock.verify(
                    () -> MessageDigest.getInstance("SHA-256")
            );
        }

        verifyNoInteractions(
                delegate,
                bruteForceProtectionPort,
                meterRegistry
        );
    }

    private BruteForceProtectedAuthenticationService createService() {
        return new BruteForceProtectedAuthenticationService(
                delegate,
                bruteForceProtectionPort,
                meterRegistry
        );
    }

    private void assertNullDependency(
            String expectedMessage,
            Executable executable
    ) {
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                executable
        );

        assertEquals(
                expectedMessage,
                exception.getMessage()
        );
    }

    private String sha256(String value) {
        try {
            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");

            byte[] hash = digest.digest(
                    value.getBytes(StandardCharsets.UTF_8)
            );

            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new AssertionError(
                    "SHA-256 deve estar disponível durante os testes",
                    exception
            );
        }
    }
}
