package br.com.ecofy.auth.core.application.service;

import br.com.ecofy.auth.core.application.exception.AuthErrorCode;
import br.com.ecofy.auth.core.application.exception.AuthException;
import br.com.ecofy.auth.core.domain.ClientApplication;
import br.com.ecofy.auth.core.domain.enums.ClientType;
import br.com.ecofy.auth.core.domain.enums.GrantType;
import br.com.ecofy.auth.core.port.in.RegisterClientApplicationUseCase;
import br.com.ecofy.auth.core.port.out.PasswordHashingPort;
import br.com.ecofy.auth.core.port.out.SaveClientApplicationPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Testes unitários do serviço de aplicações cliente")
class ClientApplicationServiceTest {

    private static final String CLIENT_NAME = "EcoFy Web";
    private static final String REDIRECT_URI =
            "https://app.ecofy.com/callback";
    private static final String HASHED_SECRET =
            "hashed-client-secret";

    @Mock
    private SaveClientApplicationPort saveClientApplicationPort;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private PasswordHashingPort passwordHashingPort;

    @Test
    @DisplayName("Deve rejeitar dependências nulas recebidas pelo construtor")
    void constructor_dependenciasNulas_deveLancarNullPointerException() {
        // Arrange, Act e Assert
        assertAll(
                () -> assertNullDependency(
                        "saveClientApplicationPort must not be null",
                        () -> new ClientApplicationService(
                                null,
                                passwordHashingPort
                        )
                ),
                () -> assertNullDependency(
                        "passwordHashingPort must not be null",
                        () -> new ClientApplicationService(
                                saveClientApplicationPort,
                                null
                        )
                )
        );

        verifyNoInteractions(
                saveClientApplicationPort,
                passwordHashingPort
        );
    }

    @Test
    @DisplayName("Deve rejeitar o comando de registro nulo")
    void register_comandoNulo_deveLancarNullPointerException() {
        // Arrange
        ClientApplicationService service = createService();

        // Act
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> service.register(null)
        );

        // Assert
        assertEquals(
                "command must not be null",
                exception.getMessage()
        );

        verifyNoInteractions(
                saveClientApplicationPort,
                passwordHashingPort
        );
    }

    @Test
    @DisplayName("Deve registrar cliente confidencial com grants padrão e segredo protegido")
    void register_clienteConfidencialSemGrants_deveAplicarPadroesEGerarSegredo() {
        // Arrange
        ClientApplicationService service = createService();

        RegisterClientApplicationUseCase.RegisterClientCommand command =
                createCommand(
                        ClientType.CONFIDENTIAL,
                        null,
                        Set.of(REDIRECT_URI)
                );

        prepareSecretHashing();
        prepareSuccessfulSave();

        ArgumentCaptor<String> secretCaptor =
                ArgumentCaptor.forClass(String.class);

        // Act
        ClientApplication result = service.register(command);

        // Assert
        assertRegisteredClient(
                result,
                ClientType.CONFIDENTIAL
        );

        verify(passwordHashingPort)
                .hash(secretCaptor.capture());

        assertGeneratedSecret(secretCaptor.getValue());

        verify(saveClientApplicationPort)
                .save(result);
    }

    @Test
    @DisplayName("Deve registrar cliente público com grants padrão sem gerar segredo")
    void register_clientePublicoSemGrants_deveAplicarPadroesSemGerarSegredo() {
        // Arrange
        ClientApplicationService service = createService();

        RegisterClientApplicationUseCase.RegisterClientCommand command =
                createCommand(
                        ClientType.PUBLIC,
                        Set.of(),
                        Set.of(REDIRECT_URI)
                );

        prepareSuccessfulSave();

        // Act
        ClientApplication result = service.register(command);

        // Assert
        assertRegisteredClient(
                result,
                ClientType.PUBLIC
        );

        verifyNoInteractions(passwordHashingPort);

        verify(saveClientApplicationPort)
                .save(result);
    }

    @Test
    @DisplayName("Deve registrar cliente SPA com grants padrão sem gerar segredo")
    void register_clienteSpaSemGrants_deveAplicarPadroesSemGerarSegredo() {
        // Arrange
        ClientApplicationService service = createService();

        RegisterClientApplicationUseCase.RegisterClientCommand command =
                createCommand(
                        ClientType.SPA,
                        null,
                        Set.of(REDIRECT_URI)
                );

        prepareSuccessfulSave();

        // Act
        ClientApplication result = service.register(command);

        // Assert
        assertRegisteredClient(
                result,
                ClientType.SPA
        );

        verifyNoInteractions(passwordHashingPort);

        verify(saveClientApplicationPort)
                .save(result);
    }

    @Test
    @DisplayName("Deve registrar cliente máquina a máquina com grant padrão e segredo protegido")
    void register_clienteM2mSemGrants_deveAplicarClientCredentialsEGerarSegredo() {
        // Arrange
        ClientApplicationService service = createService();

        RegisterClientApplicationUseCase.RegisterClientCommand command =
                createCommand(
                        ClientType.MACHINE_TO_MACHINE,
                        Set.of(),
                        Set.of()
                );

        prepareSecretHashing();
        prepareSuccessfulSave();

        ArgumentCaptor<String> secretCaptor =
                ArgumentCaptor.forClass(String.class);

        // Act
        ClientApplication result = service.register(command);

        // Assert
        assertRegisteredClient(
                result,
                ClientType.MACHINE_TO_MACHINE
        );

        verify(passwordHashingPort)
                .hash(secretCaptor.capture());

        assertGeneratedSecret(secretCaptor.getValue());

        verify(saveClientApplicationPort)
                .save(result);
    }

    @Test
    @DisplayName("Deve registrar cliente confidencial com grant explícito sem exigir redirecionamento")
    void register_clienteConfidencialComClientCredentials_deveManterGrantInformado() {
        // Arrange
        ClientApplicationService service = createService();

        RegisterClientApplicationUseCase.RegisterClientCommand command =
                createCommand(
                        ClientType.CONFIDENTIAL,
                        Set.of(GrantType.CLIENT_CREDENTIALS),
                        Set.of()
                );

        prepareSecretHashing();
        prepareSuccessfulSave();

        // Act
        ClientApplication result = service.register(command);

        // Assert
        assertRegisteredClient(
                result,
                ClientType.CONFIDENTIAL
        );

        verify(passwordHashingPort)
                .hash(anyString());

        verify(saveClientApplicationPort)
                .save(result);
    }

    @Test
    @DisplayName("Deve rejeitar cliente máquina a máquina sem grant client credentials")
    void register_clienteM2mSemClientCredentials_deveLancarGrantNaoPermitido() {
        // Arrange
        ClientApplicationService service = createService();

        RegisterClientApplicationUseCase.RegisterClientCommand command =
                createCommand(
                        ClientType.MACHINE_TO_MACHINE,
                        Set.of(GrantType.REFRESH_TOKEN),
                        Set.of()
                );

        prepareSecretHashing();

        // Act
        AuthException exception = assertThrows(
                AuthException.class,
                () -> service.register(command)
        );

        // Assert
        assertAuthException(
                exception,
                AuthErrorCode.CLIENT_NOT_ALLOWED_FOR_GRANT_TYPE,
                "M2M client must support CLIENT_CREDENTIALS grant"
        );

        verify(passwordHashingPort)
                .hash(anyString());

        verifyNoInteractions(saveClientApplicationPort);
    }

    @Test
    @DisplayName("Deve rejeitar grant authorization code para cliente máquina a máquina")
    void register_clienteM2mComAuthorizationCode_deveLancarGrantNaoPermitido() {
        // Arrange
        ClientApplicationService service = createService();

        RegisterClientApplicationUseCase.RegisterClientCommand command =
                createCommand(
                        ClientType.MACHINE_TO_MACHINE,
                        Set.of(
                                GrantType.CLIENT_CREDENTIALS,
                                GrantType.AUTHORIZATION_CODE
                        ),
                        Set.of(REDIRECT_URI)
                );

        prepareSecretHashing();

        // Act
        AuthException exception = assertThrows(
                AuthException.class,
                () -> service.register(command)
        );

        // Assert
        assertAuthException(
                exception,
                AuthErrorCode.CLIENT_NOT_ALLOWED_FOR_GRANT_TYPE,
                "M2M client cannot use AUTHORIZATION_CODE grant"
        );

        verify(passwordHashingPort)
                .hash(anyString());

        verifyNoInteractions(saveClientApplicationPort);
    }

    @Test
    @DisplayName("Deve rejeitar grant password para cliente máquina a máquina")
    void register_clienteM2mComPassword_deveLancarGrantNaoPermitido() {
        // Arrange
        ClientApplicationService service = createService();

        RegisterClientApplicationUseCase.RegisterClientCommand command =
                createCommand(
                        ClientType.MACHINE_TO_MACHINE,
                        Set.of(
                                GrantType.CLIENT_CREDENTIALS,
                                GrantType.PASSWORD
                        ),
                        Set.of()
                );

        prepareSecretHashing();

        // Act
        AuthException exception = assertThrows(
                AuthException.class,
                () -> service.register(command)
        );

        // Assert
        assertAuthException(
                exception,
                AuthErrorCode.CLIENT_NOT_ALLOWED_FOR_GRANT_TYPE,
                "M2M client cannot use PASSWORD grant"
        );

        verify(passwordHashingPort)
                .hash(anyString());

        verifyNoInteractions(saveClientApplicationPort);
    }

    @Test
    @DisplayName("Deve rejeitar grant client credentials para cliente público")
    void register_clientePublicoComClientCredentials_deveLancarGrantNaoPermitido() {
        // Arrange
        ClientApplicationService service = createService();

        RegisterClientApplicationUseCase.RegisterClientCommand command =
                createCommand(
                        ClientType.PUBLIC,
                        Set.of(GrantType.CLIENT_CREDENTIALS),
                        Set.of()
                );

        // Act
        AuthException exception = assertThrows(
                AuthException.class,
                () -> service.register(command)
        );

        // Assert
        assertAuthException(
                exception,
                AuthErrorCode.CLIENT_NOT_ALLOWED_FOR_GRANT_TYPE,
                "PUBLIC/SPA clients cannot use CLIENT_CREDENTIALS grant"
        );

        verifyNoInteractions(
                passwordHashingPort,
                saveClientApplicationPort
        );
    }

    @Test
    @DisplayName("Deve rejeitar grant client credentials para cliente SPA")
    void register_clienteSpaComClientCredentials_deveLancarGrantNaoPermitido() {
        // Arrange
        ClientApplicationService service = createService();

        RegisterClientApplicationUseCase.RegisterClientCommand command =
                createCommand(
                        ClientType.SPA,
                        Set.of(GrantType.CLIENT_CREDENTIALS),
                        Set.of()
                );

        // Act
        AuthException exception = assertThrows(
                AuthException.class,
                () -> service.register(command)
        );

        // Assert
        assertAuthException(
                exception,
                AuthErrorCode.CLIENT_NOT_ALLOWED_FOR_GRANT_TYPE,
                "PUBLIC/SPA clients cannot use CLIENT_CREDENTIALS grant"
        );

        verifyNoInteractions(
                passwordHashingPort,
                saveClientApplicationPort
        );
    }

    @Test
    @DisplayName("Deve rejeitar authorization code quando os redirecionamentos forem nulos")
    void register_authorizationCodeComRedirectUrisNulas_deveLancarRedirectInvalido() {
        // Arrange
        ClientApplicationService service = createService();

        RegisterClientApplicationUseCase.RegisterClientCommand command =
                createCommand(
                        ClientType.CONFIDENTIAL,
                        Set.of(GrantType.AUTHORIZATION_CODE),
                        null
                );

        prepareSecretHashing();

        // Act
        AuthException exception = assertThrows(
                AuthException.class,
                () -> service.register(command)
        );

        // Assert
        assertAuthException(
                exception,
                AuthErrorCode.INVALID_REDIRECT_URI,
                "AUTHORIZATION_CODE grant requires at least one redirectUri configured"
        );

        verify(passwordHashingPort)
                .hash(anyString());

        verifyNoInteractions(saveClientApplicationPort);
    }

    @Test
    @DisplayName("Deve rejeitar authorization code quando os redirecionamentos estiverem vazios")
    void register_authorizationCodeComRedirectUrisVazias_deveLancarRedirectInvalido() {
        // Arrange
        ClientApplicationService service = createService();

        RegisterClientApplicationUseCase.RegisterClientCommand command =
                createCommand(
                        ClientType.PUBLIC,
                        Set.of(GrantType.AUTHORIZATION_CODE),
                        Set.of()
                );

        // Act
        AuthException exception = assertThrows(
                AuthException.class,
                () -> service.register(command)
        );

        // Assert
        assertAuthException(
                exception,
                AuthErrorCode.INVALID_REDIRECT_URI,
                "AUTHORIZATION_CODE grant requires at least one redirectUri configured"
        );

        verifyNoInteractions(
                passwordHashingPort,
                saveClientApplicationPort
        );
    }

    @Test
    @DisplayName("Deve rejeitar o tipo de cliente nulo durante a resolução dos grants")
    void register_tipoDeClienteNulo_deveLancarNullPointerException() {
        // Arrange
        ClientApplicationService service = createService();

        RegisterClientApplicationUseCase.RegisterClientCommand command =
                createCommand(
                        null,
                        null,
                        Set.of()
                );

        // Act
        assertThrows(
                NullPointerException.class,
                () -> service.register(command)
        );

        // Assert
        verifyNoInteractions(
                passwordHashingPort,
                saveClientApplicationPort
        );
    }

    private ClientApplicationService createService() {
        return new ClientApplicationService(
                saveClientApplicationPort,
                passwordHashingPort
        );
    }

    private RegisterClientApplicationUseCase.RegisterClientCommand createCommand(
            ClientType clientType,
            Set<GrantType> grantTypes,
            Set<String> redirectUris
    ) {
        RegisterClientApplicationUseCase.RegisterClientCommand command =
                org.mockito.Mockito.mock(
                        RegisterClientApplicationUseCase.RegisterClientCommand.class
                );

        lenient().when(command.name())
                .thenReturn(CLIENT_NAME);

        lenient().when(command.clientType())
                .thenReturn(clientType);

        lenient().when(command.grantTypes())
                .thenReturn(grantTypes);

        lenient().when(command.redirectUris())
                .thenReturn(redirectUris);

        lenient().when(command.scopes())
                .thenReturn(Set.of("profile:read"));

        lenient().when(command.firstParty())
                .thenReturn(true);

        return command;
    }

    private void prepareSecretHashing() {
        when(
                passwordHashingPort.hash(anyString()).value()
        ).thenReturn(HASHED_SECRET);
    }

    private void prepareSuccessfulSave() {
        when(
                saveClientApplicationPort.save(
                        any(ClientApplication.class)
                )
        ).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private void assertRegisteredClient(
            ClientApplication client,
            ClientType expectedClientType
    ) {
        assertAll(
                () -> assertNotNull(client),
                () -> assertEquals(
                        expectedClientType,
                        client.clientType()
                ),
                () -> assertTrue(client.isActive()),
                () -> assertNotNull(client.clientId()),
                () -> assertTrue(
                        client.clientId().matches(
                                "^eco_[A-Za-z0-9_-]{16}$"
                        )
                )
        );
    }

    private void assertGeneratedSecret(String secret) {
        assertAll(
                () -> assertNotNull(secret),
                () -> assertEquals(43, secret.length()),
                () -> assertTrue(
                        secret.matches(
                                "^[A-Za-z0-9_-]{43}$"
                        )
                )
        );
    }

    private void assertAuthException(
            AuthException exception,
            AuthErrorCode expectedErrorCode,
            String expectedMessage
    ) {
        assertAll(
                () -> assertEquals(
                        expectedErrorCode,
                        exception.getErrorCode()
                ),
                () -> assertEquals(
                        expectedMessage,
                        exception.getMessage()
                )
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
}
